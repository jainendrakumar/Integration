package com.example.aggregation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Updated AggregatorService with new features:
 * 1. Outgoing message switch.
 * 2. Throttling of merged messages.
 * 3. Dynamic input mode (zip vs folder).
 * 4. Dead letter queue handling.
 * 5. Reporting to CSV.
 *
 * @author Jainendra Kumar(jkr3)
 * TODO:
 */
@Service
public class AggregatorService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, MessageBucket> buckets = new ConcurrentHashMap<>();
    private final Set<String> createdDirs = ConcurrentHashMap.newKeySet();
    private final ExecutorService archiveExecutor = Executors.newFixedThreadPool(4);

    @Value("${target.rest.url}")
    private String targetRestUrl;

    // NEW: Switch to enable/disable sending outgoing messages.
    @Value("${target.rest.enabled:true}")
    private boolean targetRestEnabled;

    @Value("${archive.incoming.root}")
    private String incomingArchiveRoot;

    @Value("${archive.merged.root}")
    private String mergedArchiveRoot;

    // NEW: Dead letter queue root directory.
    @Value("${deadletterqueue}")
    private String deadLetterQueue;

    // NEW: Reporting file prefix.
    @Value("${report.file.prefix:report_}")
    private String reportFilePrefix;

    @Value("${consolidation.timeframe}")
    private long consolidationTimeFrame;

    @Value("${bucket.flush.size:0}")
    private int bucketFlushSize;

    @Value("${archiving.enabled:true}")
    private boolean archivingEnabled;

    private final RestTemplate restTemplate;

    // NEW: Throttling properties.
    @Value("${throttling.enabled:true}")
    private boolean throttlingEnabled;

    @Value("${throttling.limit:5}")
    private int throttlingLimit;

    // NEW: Fields for throttle control.
    private final Object throttleLock = new Object();
    private long lastThrottleReset = System.currentTimeMillis();
    private int throttleCount = 0;

    // NEW: Input source mode properties.
    @Value("${input.mode:folder}")
    private String inputMode;

    @Value("${input.zip.file:}")
    private String inputZipFile;

    @Value("${input.folder.path:input}")
    private String inputFolderPath;

    public AggregatorService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void processIncomingMessage(String message) {
        // Archive raw incoming message asynchronously if archiving is enabled.
        if (archivingEnabled) {
            archiveExecutor.submit(() -> archiveMessage(message, "incoming"));
        }

        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode loadPipelineNode = root.get("LoadPipeline");
            if (loadPipelineNode == null || !loadPipelineNode.isArray() || loadPipelineNode.size() == 0) {
                System.err.println("Invalid message: 'LoadPipeline' array is missing or empty.");
                return;
            }
            JsonNode firstElement = loadPipelineNode.get(0);
            JsonNode loadIdNode = firstElement.get("LoadID");
            if (loadIdNode == null) {
                System.err.println("Invalid message: 'LoadID' is missing in the first element.");
                return;
            }
            String loadId = loadIdNode.asText();

            buckets.compute(loadId, (id, bucket) -> {
                if (bucket == null) {
                    bucket = new MessageBucket();
                }
                bucket.addMessage(loadPipelineNode);
                if (bucketFlushSize > 0 && bucket.getMessages().size() >= bucketFlushSize) {
                    flushBucket(id, bucket);
                    return null;
                }
                return bucket;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void flushBuckets() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, MessageBucket> entry : buckets.entrySet()) {
            String loadId = entry.getKey();
            MessageBucket bucket = entry.getValue();
            if (now - bucket.getStartTime() >= consolidationTimeFrame * 1000) {
                flushBucket(loadId, bucket);
            }
        }
    }

    private void flushBucket(String loadId, MessageBucket bucket) {
        // Build consolidated payload.
        ObjectNode aggregated = objectMapper.createObjectNode();
        ArrayNode messagesArray = objectMapper.createArrayNode();
        for (JsonNode node : bucket.getMessages()) {
            if (node.isArray()) {
                node.forEach(messagesArray::add);
            } else {
                messagesArray.add(node);
            }
        }
        aggregated.set("LoadPipeline", messagesArray);
        String aggregatedStr = aggregated.toString();

        // Archive merged message asynchronously.
        if (archivingEnabled) {
            archiveExecutor.submit(() -> archiveMessage(aggregatedStr, "merged"));
        }

        // NEW: Prepare CSV report details.
        String minuteDetail = LocalDateTime.now().format(DateTimeFormatter.ofPattern("mm"));
        int messageCount = bucket.getMessages().size();

        // Check switch to send message out.
        if (targetRestEnabled) {
            // NEW: Apply throttling if enabled.
            if (throttlingEnabled) {
                throttleIfNeeded();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept-Encoding", "gzip,deflate");
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(aggregatedStr, headers);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        targetRestUrl,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );
                // Report success.
                writeReport(loadId, messageCount, minuteDetail, "SENT");
            } catch (Exception ex) {
                ex.printStackTrace();
                // On failure, move to dead letter queue.
                moveToDeadLetter(aggregatedStr);
                writeReport(loadId, messageCount, minuteDetail, "FAILED");
            }
        } else {
            // If outgoing messages are disabled, skip sending.
            writeReport(loadId, messageCount, minuteDetail, "SKIPPED");
        }
        buckets.remove(loadId);
    }

    // NEW: Simple throttling mechanism
    private void throttleIfNeeded() {
        synchronized (throttleLock) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastThrottleReset >= 1000) {
                lastThrottleReset = currentTime;
                throttleCount = 0;
            }
            if (throttleCount >= throttlingLimit) {
                long sleepTime = 1000 - (currentTime - lastThrottleReset);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                lastThrottleReset = System.currentTimeMillis();
                throttleCount = 0;
            }
            throttleCount++;
        }
    }

    // NEW: Move failed message to dead letter queue using the same folder hierarchy.
    private void moveToDeadLetter(String message) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String folderPath = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + File.separator + now.format(DateTimeFormatter.ofPattern("HH"))
                    + File.separator + now.format(DateTimeFormatter.ofPattern("mm"));
            archiveToFolder(message, deadLetterQueue, folderPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void archiveMessage(String message, String type) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String folderPath = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + File.separator + now.format(DateTimeFormatter.ofPattern("HH"))
                    + File.separator + now.format(DateTimeFormatter.ofPattern("mm"));
            String rootDir = "incoming".equals(type) ? incomingArchiveRoot : mergedArchiveRoot;
            archiveToFolder(message, rootDir, folderPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void archiveToFolder(String message, String rootDir, String folderPath) {
        try {
            String baseDir = rootDir + File.separator + folderPath;
            if (!createdDirs.contains(baseDir)) {
                Files.createDirectories(Paths.get(baseDir));
                createdDirs.add(baseDir);
            }
            String fileName = baseDir + File.separator + System.currentTimeMillis() + "_" + UUID.randomUUID() + ".json";
            try (FileWriter writer = new FileWriter(fileName)) {
                writer.write(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // NEW: Append a report line to report_YYYYMMDD.csv
    private void writeReport(String loadId, int count, String minute, String status) {
        try {
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String reportFile = reportFilePrefix + dateStr + ".csv";
            File file = new File(reportFile);
            boolean writeHeader = !file.exists();
            try (FileWriter fw = new FileWriter(file, true);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                if (writeHeader) {
                    bw.write("Minute,LoadID,EntryCount,Status");
                    bw.newLine();
                }
                bw.write(String.format("%s,%s,%d,%s", minute, loadId, count, status));
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // NEW: Scheduled method to process input messages from either a zip file or folder.
    @Scheduled(fixedDelay = 60000)
    public void processInputMessages() {
        if ("zip".equalsIgnoreCase(inputMode) && inputZipFile != null && !inputZipFile.isEmpty()) {
            processZipInput();
        } else {
            processFolderInput();
        }
    }

    // NEW: Process messages from a zip file.
    private void processZipInput() {
        // For example, unzip to a temporary directory then process files.
        File zipFile = new File(inputZipFile);
        if (!zipFile.exists()) {
            System.err.println("Zip file not found: " + inputZipFile);
            return;
        }
        File tempDir = new File("temp_unzip");
        tempDir.mkdirs();
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zipFile)) {
            Enumeration<? extends java.util.zip.ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                File outFile = new File(tempDir, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (InputStream in = zf.getInputStream(entry);
                         OutputStream out = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Once unzipped, process the folder structure.
        processMessagesFromDirectory(tempDir);
        // Optionally, delete the temporary directory after processing.
        deleteDirectory(tempDir);
    }

    // NEW: Process messages from a folder (inputFolderPath) following the hierarchy.
    private void processFolderInput() {
        File folder = new File(inputFolderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("Input folder not found: " + inputFolderPath);
            return;
        }
        processMessagesFromDirectory(folder);
    }

    // NEW: Traverse directories in ascending order (by day, hour, minute) and process JSON files.
    private void processMessagesFromDirectory(File root) {
        // List date directories (YYYYMMDD) and sort ascending.
        File[] dateDirs = root.listFiles(File::isDirectory);
        if (dateDirs == null) return;
        Arrays.sort(dateDirs, Comparator.comparing(File::getName));
        for (File dateDir : dateDirs) {
            File[] hourDirs = dateDir.listFiles(File::isDirectory);
            if (hourDirs == null) continue;
            Arrays.sort(hourDirs, Comparator.comparing(File::getName));
            for (File hourDir : hourDirs) {
                File[] minuteDirs = hourDir.listFiles(File::isDirectory);
                if (minuteDirs == null) continue;
                Arrays.sort(minuteDirs, Comparator.comparing(File::getName));
                for (File minuteDir : minuteDirs) {
                    File[] messageFiles = minuteDir.listFiles((dir, name) -> name.endsWith(".json"));
                    if (messageFiles == null) continue;
                    Arrays.sort(messageFiles, Comparator.comparing(File::getName));
                    for (File msgFile : messageFiles) {
                        try {
                            String content = new String(Files.readAllBytes(msgFile.toPath()));
                            processIncomingMessage(content);
                            // Optionally delete or archive the file after processing.
                            // Files.delete(msgFile.toPath());
                        } catch (Exception e) {
                            e.printStackTrace();
                            // On error, move file to dead letter queue preserving the hierarchy.
                            moveFileToDeadLetter(msgFile, dateDir.getName(), hourDir.getName(), minuteDir.getName());
                        }
                    }
                }
            }
        }
    }

    // NEW: Utility method to move a file to the dead letter queue under the same hierarchy.
    private void moveFileToDeadLetter(File file, String date, String hour, String minute) {
        try {
            String targetDir = deadLetterQueue + File.separator + date + File.separator + hour + File.separator + minute;
            Files.createDirectories(Paths.get(targetDir));
            Files.move(file.toPath(), Paths.get(targetDir, file.getName()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // NEW: Utility method to delete a directory recursively.
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            for (File sub : dir.listFiles()) {
                deleteDirectory(sub);
            }
        }
        dir.delete();
    }

    @PreDestroy
    public void shutdown() {
        archiveExecutor.shutdown();
    }

    private static class MessageBucket {
        private final long startTime;
        private final Queue<JsonNode> messages = new ConcurrentLinkedQueue<>();

        public MessageBucket() {
            this.startTime = System.currentTimeMillis();
        }

        public void addMessage(JsonNode message) {
            messages.add(message);
        }

        public long getStartTime() {
            return startTime;
        }

        public Queue<JsonNode> getMessages() {
            return messages;
        }
    }
}
