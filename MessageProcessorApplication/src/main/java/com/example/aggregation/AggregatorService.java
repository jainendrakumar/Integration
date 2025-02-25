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
    // Buckets keyed by LoadID
    private final Map<String, MessageBucket> buckets = new ConcurrentHashMap<>();

    // Cache of created directories to avoid repeated creation calls.
    private final Set<String> createdDirs = ConcurrentHashMap.newKeySet();

    // Executor for asynchronous disk archiving.
    private final ExecutorService archiveExecutor = Executors.newFixedThreadPool(4);

    // Target REST service URL.
    @Value("${target.rest.url}")
    private String targetRestUrl;

    // Root folder for archiving incoming messages.
    @Value("${archive.incoming.root}")
    private String incomingArchiveRoot;

    // Root folder for archiving merged messages.
    @Value("${archive.merged.root}")
    private String mergedArchiveRoot;

    // Consolidation timeframe in seconds (externalized configuration).
    @Value("${consolidation.timeframe}")
    private long consolidationTimeFrame;

    // Optional bucket flush threshold (if > 0, flush immediately when reached).
    @Value("${bucket.flush.size:0}")
    private int bucketFlushSize;

    private final RestTemplate restTemplate;

    public AggregatorService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Processes an incoming JSON message.
     * - Offloads disk archiving.
     * - Parses and extracts the LoadID.
     * - Adds the JSON to the corresponding bucket.
     */
    public void processIncomingMessage(String message) {
        // Offload archiving to avoid blocking.
        archiveExecutor.submit(() -> archiveMessage(message, "incoming"));

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
                // Flush immediately if bucket size threshold is enabled and reached.
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

    /**
     * Scheduled task that runs every second.
     * Flushes buckets that have been open longer than the consolidation timeframe.
     */
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

    /**
     * Flushes a bucket: aggregates messages, archives the merged payload, and sends it.
     */
    private void flushBucket(String loadId, MessageBucket bucket) {
        // Build consolidated payload.
        ObjectNode aggregated = objectMapper.createObjectNode();
        ArrayNode messagesArray = objectMapper.createArrayNode();
        // For each stored message in the bucket, check if it's an array.
        for (JsonNode node : bucket.getMessages()) {
            if (node.isArray()) {
                // If the node is an array, add each element to the consolidated array.
                node.forEach(messagesArray::add);
            } else {
                // Otherwise, add the node directly.
                messagesArray.add(node);
            }
        }
        aggregated.set("LoadPipeline", messagesArray);
        String aggregatedStr = aggregated.toString();

        // Archive merged message asynchronously.
        archiveExecutor.submit(() -> archiveMessage(aggregatedStr, "merged"));

        // Create headers for the outgoing request.
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept-Encoding", "gzip,deflate");
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(aggregatedStr, headers);

        // Send the merged payload to the target REST service.
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    targetRestUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            // Optionally log or process the response here.
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // Remove the bucket after processing.
        buckets.remove(loadId);
    }


    /**
     * Archives a JSON message by writing it to the dynamic folder structure.
     * It writes to both the configured hourly archive (now replaced by dynamic day/hour/minute structure)
     * and similarly for merged messages.
     *
     * @param message the JSON message.
     * @param type either "incoming" or "merged".
     */
    private void archiveMessage(String message, String type) {
        try {
            if ("incoming".equals(type)) {
                archiveToFolder(message, incomingArchiveRoot, getDynamicFolderName());
            } else if ("merged".equals(type)) {
                archiveToFolder(message, mergedArchiveRoot, getDynamicFolderName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Archives the message to a specific folder under the given root.
     *
     * @param message the JSON message.
     * @param rootDir the archive root directory (for incoming or merged).
     * @param folderPath the dynamic folder path in the format "yyyyMMdd/HH/mm".
     */
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

    /**
     * Returns a dynamic folder path in the format "yyyyMMdd/HH/mm".
     */
    private String getDynamicFolderName() {
        LocalDateTime now = LocalDateTime.now();
        String dayFolder = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String hourFolder = now.format(DateTimeFormatter.ofPattern("HH"));
        String minuteFolder = now.format(DateTimeFormatter.ofPattern("mm"));
        return dayFolder + File.separator + hourFolder + File.separator + minuteFolder;
    }

    @PreDestroy
    public void shutdown() {
        archiveExecutor.shutdown();
    }

    /**
     * Bucket for storing messages for a given LoadID.
     */
    private static class MessageBucket {
        private final long startTime;
        // Use a ConcurrentLinkedQueue for high throughput writes.
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

        public Collection<JsonNode> getMessages() {
            return messages;
        }
    }
}
