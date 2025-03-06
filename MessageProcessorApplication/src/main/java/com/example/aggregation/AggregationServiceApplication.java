package com.example.aggregation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * Main entry point for the Message Processor Service.
 * This application processes incoming JSON messages, groups them by LoadID,
 * archives the messages, reports metrics to CSV files, and sends merged messages
 * to a target REST service.
 *
 * @author JKR3
 */
@SpringBootApplication
@EnableScheduling
public class AggregationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AggregationServiceApplication.class, args);
    }

    /**
     * Create a RestTemplate bean for making outgoing REST calls.
     *
     * @return a RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
