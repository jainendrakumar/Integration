package com.example.aggregation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.node.ObjectNode;


import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * AggregatorService processes incoming JSON messages:
 * - It increments Micrometer counters and timers to record:
 *      • Arrival count (messages received)
 *      • Processed count (messages processed)
 *      • Processing latency
 *      • Error count
 *      • Queue depth (via a Gauge)
 * - It also archives messages and sends merged payloads to a target REST endpoint.
 *
 * @author JKR3
 */
@Service
public class AggregatorService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    // Map for grouping messages by LoadID.
    private final Map<String, MessageBucket> buckets = new ConcurrentHashMap<>();
    private final Set<String> createdDirs = ConcurrentHashMap.newKeySet();
    private final ExecutorService archiveExecutor = Executors.newFixedThreadPool(4);

    @Value("${target.rest.url}")
    private String targetRestUrl;

    // Archive roots (not used in the dashboard part, but part of full implementation)
    @Value("${archive.incoming.root}")
    private String incomingArchiveRoot;
    @Value("${archive.merged.root}")
    private String mergedArchiveRoot;

    // Consolidation timeframe in seconds.
    @Value("${consolidation.timeframe}")
    private long consolidationTimeFrame;

    // Optional flush size threshold.
    @Value("${bucket.flush.size:0}")
    private int bucketFlushSize;

    // Switch for archiving.
    @Value("${archiving.enabled:true}")
    private boolean archivingEnabled;

    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;

    // Micrometer metrics.
    private final Counter arrivalCounter;
    private final Counter processedCounter;
    private final Counter errorCounter;
    private final Timer processingTimer;
    // Gauge for queue depth; we bind it to our buckets size.
    private final Gauge queueDepthGauge;

    public AggregatorService(RestTemplate restTemplate, MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.meterRegistry = meterRegistry;
        this.arrivalCounter = meterRegistry.counter("message.arrival.count");
        this.processedCounter = meterRegistry.counter("message.processed.count");
        this.errorCounter = meterRegistry.counter("message.error.count");
        this.processingTimer = meterRegistry.timer("message.processing.latency");
        // Gauge that reads total bucket size.
        this.queueDepthGauge = Gauge.builder("message.queue.depth", this, AggregatorService::calculateQueueDepth)
                .register(meterRegistry);
    }

    /**
     * Simulated processing of an incoming message.
     * Increments the arrival counter, archives raw messages if enabled,
     * and groups the message by LoadID.
     *
     * @param message the incoming JSON message string.
     */
    public void processIncomingMessage(String message) {
        arrivalCounter.increment();
        if (archivingEnabled) {
            archiveExecutor.submit(() -> archiveMessage(message, "incoming"));
        }
        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode loadPipelineNode = root.get("LoadPipeline");
            if (loadPipelineNode == null || !loadPipelineNode.isArray() || loadPipelineNode.size() == 0) {
                System.err.println("Invalid message: 'LoadPipeline' is missing or empty.");
                errorCounter.increment();
                return;
            }
            // Extract LoadID from the first element.
            JsonNode firstElement = loadPipelineNode.get(0);
            JsonNode loadIdNode = firstElement.get("LoadID");
            if (loadIdNode == null) {
                System.err.println("Invalid message: 'LoadID' missing.");
                errorCounter.increment();
                return;
            }
            String loadId = loadIdNode.asText();
            // Group the message.
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
            errorCounter.increment();
            e.printStackTrace();
        }
    }

    /**
     * Scheduled task to flush message buckets if their age exceeds the consolidation timeframe.
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
     * Flushes the bucket for a given LoadID:
     * - Merges messages (flattening nested arrays)
     * - Archives merged message if enabled
     * - Increments processed counter and records processing latency
     * - Sends the merged payload to the target REST service with custom HTTP headers.
     *
     * @param loadId the bucket key.
     * @param bucket the MessageBucket.
     */
    private void flushBucket(String loadId, MessageBucket bucket) {
        long start = System.nanoTime();
        ObjectNode aggregated = objectMapper.createObjectNode();
        // Flatten arrays so that we don't end up with nested arrays.
        // This produces a single "LoadPipeline" array.
        var messagesArray = objectMapper.createArrayNode();
        for (JsonNode node : bucket.getMessages()) {
            if (node.isArray()) {
                node.forEach(messagesArray::add);
            } else {
                messagesArray.add(node);
            }
        }
        aggregated.set("LoadPipeline", messagesArray);
        String aggregatedStr = aggregated.toString();

        if (archivingEnabled) {
            archiveExecutor.submit(() -> archiveMessage(aggregatedStr, "merged"));
        }
        // Record outgoing metrics.
        processedCounter.increment();
        // Send merged message with custom headers.
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept-Encoding", "gzip,deflate");
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(aggregatedStr, headers);
        try {
            restTemplate.exchange(targetRestUrl, HttpMethod.POST, requestEntity, String.class);
        } catch (Exception ex) {
            errorCounter.increment();
            ex.printStackTrace();
        }
        // Record processing latency.
        processingTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        buckets.remove(loadId);
    }

    /**
     * Archives a message into a dynamic folder structure:
     * <archiveRoot>/<yyyyMMdd>/<HH>/<mm>
     *
     * @param message the JSON message.
     * @param type "incoming" or "merged".
     */
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

    /**
     * Writes the message to a file in the specified folder.
     *
     * @param message the JSON message.
     * @param rootDir the archive root.
     * @param folderPath the dynamic folder path.
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
     * Calculates the total number of messages pending across all buckets.
     *
     * @return total count of queued messages.
     */
    private double calculateQueueDepth() {
        return buckets.values().stream().mapToInt(bucket -> bucket.getMessages().size()).sum();
    }

    @PreDestroy
    public void shutdown() {
        archiveExecutor.shutdown();
    }

    /**
     * Inner class representing a message bucket.
     */
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
