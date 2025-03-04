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
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Jainendra Kumar
 * ToDo:
 */

@Service
public class AggregatorService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    // Buckets keyed by LoadID.
    private final Map<String, MessageBucket> buckets = new ConcurrentHashMap<>();
    // Cache for created directories to reduce I/O overhead.
    private final Set<String> createdDirs = ConcurrentHashMap.newKeySet();
    // Executor for asynchronous disk archiving.
    private final ExecutorService archiveExecutor = Executors.newFixedThreadPool(4);

    @Value("${target.rest.url}")
    private String targetRestUrl;

    // Archive root directories for incoming and merged messages.
    @Value("${archive.incoming.root}")
    private String incomingArchiveRoot;

    @Value("${archive.merged.root}")
    private String mergedArchiveRoot;

    // Consolidation timeframe in seconds.
    @Value("${consolidation.timeframe}")
    private long consolidationTimeFrame;

    // Bucket flush threshold based on number of messages (0 disables size-based flush).
    @Value("${bucket.flush.size:0}")
    private int bucketFlushSize;

    // Switch to enable or disable archiving.
    @Value("${archiving.enabled:true}")
    private boolean archivingEnabled;

    private final RestTemplate restTemplate;

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
            // Extract LoadID from the first element.
            JsonNode firstElement = loadPipelineNode.get(0);
            JsonNode loadIdNode = firstElement.get("LoadID");
            if (loadIdNode == null) {
                System.err.println("Invalid message: 'LoadID' is missing in the first element.");
                return;
            }
            String loadId = loadIdNode.asText();

            // Group the message by LoadID.
            buckets.compute(loadId, (id, bucket) -> {
                if (bucket == null) {
                    bucket = new MessageBucket();
                }
                bucket.addMessage(loadPipelineNode);
                // Flush immediately if bucket size threshold is reached.
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
        // Build consolidated payload and flatten the arrays.
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

        // Archive merged message asynchronously if archiving is enabled.
        if (archivingEnabled) {
            archiveExecutor.submit(() -> archiveMessage(aggregatedStr, "merged"));
        }

        // Prepare custom headers.
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept-Encoding", "gzip,deflate");
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(aggregatedStr, headers);

        // Send the merged payload.
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    targetRestUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            // Optionally log response details.
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        buckets.remove(loadId);
    }

    private void archiveMessage(String message, String type) {
        try {
            // Build a dynamic folder path: archive/<type>/yyyyMMdd/HH/mm
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

    @PreDestroy
    public void shutdown() {
        archiveExecutor.shutdown();
    }

    private static class MessageBucket {
        private final long startTime;
        private final ConcurrentLinkedQueue<JsonNode> messages = new ConcurrentLinkedQueue<>();

        public MessageBucket() {
            this.startTime = System.currentTimeMillis();
        }

        public void addMessage(JsonNode message) {
            messages.add(message);
        }

        public long getStartTime() {
            return startTime;
        }

        public ConcurrentLinkedQueue<JsonNode> getMessages() {
            return messages;
        }
    }
}
