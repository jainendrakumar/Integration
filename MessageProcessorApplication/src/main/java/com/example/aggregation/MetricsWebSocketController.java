package com.example.aggregation;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * MetricsWebSocketController pushes real-time metrics to WebSocket clients.
 *
 * @author JKR3
 */
@Controller
public class MetricsWebSocketController {

    private final MeterRegistry meterRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    // Refresh interval in milliseconds (default: 5000ms).
    @Value("${dashboard.refresh.interval:5000}")
    private long refreshInterval;

    public MetricsWebSocketController(MeterRegistry meterRegistry, SimpMessagingTemplate messagingTemplate) {
        this.meterRegistry = meterRegistry;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Scheduled task that runs at the configured refresh interval to push metrics.
     */
    @Scheduled(fixedRateString = "${dashboard.refresh.interval:5000}")
    public void sendMetricsUpdate() {
        // Build a simple payload of key metrics.
        Map<String, Object> metricsData = Map.of(
                "arrivalRate", meterRegistry.get("message.arrival.count").counter().count(),
                "processedRate", meterRegistry.get("message.processed.count").counter().count(),
                "errorCount", meterRegistry.get("message.error.count").counter().count(),
                "processingLatency", meterRegistry.get("message.processing.latency").timer().mean(TimeUnit.MILLISECONDS),
                "queueDepth", meterRegistry.get("message.queue.depth").gauge().value()
        );
        // Send metrics to the /topic/metrics destination.
        messagingTemplate.convertAndSend("/topic/metrics", metricsData);
        System.out.println("Pushing metrics update: " + metricsData);
    }
}
