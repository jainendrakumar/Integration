package com.example.trainservicecrewsi.config;

import com.example.trainservicecrewsi.model.*;
import com.example.trainservicecrewsi.service.CrewProcessingService;
import com.example.trainservicecrewsi.util.CsvReportUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.http.dsl.Http;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Defines the Spring Integration flows for handling Train Service Crew requests.
 */
@Configuration
public class IntegrationConfig {

    private static final Logger log = LoggerFactory.getLogger(IntegrationConfig.class);

    public static final String HEADER_ARRIVAL_TIME = "arrivalTime";
    public static final String HEADER_REQUEST_JSON = "requestJson";
    public static final String HEADER_RESPONSE_JSON = "responseJson";
    public static final String HEADER_STATUS_RESPONSE_JSON = "statusResponseJson";
    public static final String HEADER_REQUEST_ID = "requestID";

    // --- Channels ---
    @Bean
    public MessageChannel requestChannel() {
        return MessageChannels.direct("requestChannel").getObject(); // Use getObject() instead of get()
    }

    @Bean
    public MessageChannel responseChannel() {
        return MessageChannels.direct("responseChannel").getObject(); // Use getObject() instead of get()
    }

    @Bean
    public MessageChannel statusRequestChannel() {
        return MessageChannels.direct("statusRequestChannel").getObject(); // Use getObject() instead of get()
    }

    @Bean
    public MessageChannel statusResponseChannel() {
        return MessageChannels.direct("statusResponseChannel").getObject(); // Use getObject() instead of get()
    }

    @Bean
    public MessageChannel loggingChannel() {
        return MessageChannels.publishSubscribe("loggingChannel").getObject(); // Use getObject() instead of get()
    }

    @Bean
    public MessageChannel errorChannel() {
        return MessageChannels.direct("errorChannel").getObject(); // Use getObject() instead of get()
    }

    // --- HTTP Inbound Gateways ---
    @Bean
    public IntegrationFlow httpInboundRequestFlow() {
        return IntegrationFlow.from(Http.inboundGateway("/api/v1/train-crew/request")
                                     .requestMapping(m -> m.methods(HttpMethod.POST))
                                     .errorChannel(errorChannel())
                                     .requestPayloadType(String.class)
                                     .replyTimeout(10000)
                                     .statusCodeFunction(m -> HttpStatus.OK)
                                     )
                .enrichHeaders(h -> h.header(HEADER_ARRIVAL_TIME, Instant.now()))
                .enrichHeaders(h -> h.headerFunction(HEADER_REQUEST_JSON, m -> m.getPayload()))
                .channel(requestChannel())
                .get();
    }

    @Bean
    public IntegrationFlow httpInboundStatusFlow() {
        return IntegrationFlow.from(Http.inboundGateway("/api/v1/train-crew/status")
                                     .requestMapping(m -> m.methods(HttpMethod.POST))
                                     .errorChannel(errorChannel())
                                     .requestPayloadType(String.class)
                                     .replyTimeout(10000)
                                     .statusCodeFunction(m -> HttpStatus.OK)
                                     )
                .enrichHeaders(h -> h.header(HEADER_ARRIVAL_TIME, Instant.now()))
                .enrichHeaders(h -> h.headerFunction(HEADER_REQUEST_JSON, m -> m.getPayload()))
                .channel(statusRequestChannel())
                .get();
    }

    // --- Main Processing Flow (/request) ---
    @Bean
    public IntegrationFlow crewRequestProcessingFlow(JsonObjectMapper<?, ?> jsonObjectMapper,
                                                   CrewProcessingService crewProcessingService,
                                                   RequestTransformer requestTransformer,
                                                   ResponseWrapperTransformer responseWrapperTransformer,
                                                   CsvReportHandler csvReportHandler) {
        return IntegrationFlow.from(requestChannel())
                .wireTap(loggingChannel(), wt -> wt.selector(m -> true))
                .transform(new JsonToObjectTransformer(Map.class, jsonObjectMapper))
                .transform(requestTransformer, "extractTrainServiceCrewRequest")
                .enrichHeaders(h -> h.headerFunction(HEADER_REQUEST_ID, m -> ((TrainServiceCrewRequest) m.getPayload()).getRequestID()))
                .handle(crewProcessingService, "processCrewRequest")
                .handle(csvReportHandler, "handleCrewRequestReport")
                .transform(responseWrapperTransformer, "wrapResponse")
                .transform(new ObjectToJsonTransformer(jsonObjectMapper))
                .enrichHeaders(h -> h.headerFunction(HEADER_RESPONSE_JSON, m -> m.getPayload()))
                .wireTap(loggingChannel(), wt -> wt.selector(m -> true))
                .channel(responseChannel())
                .get();
    }

    // --- Status Processing Flow (/status) ---
    @Bean
    public IntegrationFlow statusProcessingFlow(JsonObjectMapper<?, ?> jsonObjectMapper,
                                                CrewProcessingService crewProcessingService,
                                                RequestTransformer requestTransformer,
                                                CsvReportHandler csvReportHandler) {
        return IntegrationFlow.from(statusRequestChannel())
                .wireTap(loggingChannel(), wt -> wt.selector(m -> true))
                .transform(new JsonToObjectTransformer(Map.class, jsonObjectMapper))
                .transform(requestTransformer, "extractTrainServiceCrewResponse")
                .enrichHeaders(h -> h.headerFunction(HEADER_REQUEST_ID, m -> ((TrainServiceCrewResponse) m.getPayload()).getRequestID()))
                .handle(crewProcessingService, "processStatusUpdate")
                .handle(csvReportHandler, "handleStatusResponseReport")
                .transform(new ObjectToJsonTransformer(jsonObjectMapper))
                .enrichHeaders(h -> h.headerFunction(HEADER_STATUS_RESPONSE_JSON, m -> m.getPayload()))
                .wireTap(loggingChannel(), wt -> wt.selector(m -> true))
                .channel(statusResponseChannel())
                .get();
    }

    // --- Error Handling Flow ---
    @Bean
    public IntegrationFlow errorHandlingFlow(ErrorHandler errorHandler) {
        return IntegrationFlow.from(errorChannel())
                .transform(Throwable::getCause)
                .handle(errorHandler, "handleError")
                .get();
    }

    // --- ObjectMapper and Adapter Beans ---
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    @Bean
    public JsonObjectMapper<?, ?> jsonObjectMapper(ObjectMapper objectMapper) {
        return new Jackson2JsonObjectMapper(objectMapper);
    }

    // --- Helper Components for Flows ---

    @Component
    static class RequestTransformer {
        private final ObjectMapper objectMapper;

        RequestTransformer(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        public TrainServiceCrewRequest extractTrainServiceCrewRequest(Map<String, List<Map<String, Object>>> payload) {
            List<Map<String, Object>> list = payload.get("TrainServiceCrewRequest");
            if (list == null || list.isEmpty()) {
                throw new MessagingException("Invalid request structure: Missing or empty 'TrainServiceCrewRequest' array.");
            }
            try {
                String innerJson = objectMapper.writeValueAsString(list.get(0));
                return objectMapper.readValue(innerJson, TrainServiceCrewRequest.class);
            } catch (JsonProcessingException e) {
                throw new MessagingException("Failed to parse inner TrainServiceCrewRequest object", e);
            }
        }

        public TrainServiceCrewResponse extractTrainServiceCrewResponse(Map<String, List<Map<String, Object>>> payload) {
            List<Map<String, Object>> list = payload.get("TrainServiceCrewResponses");
            if (list == null || list.isEmpty()) {
                throw new MessagingException("Invalid status request structure: Missing or empty 'TrainServiceCrewResponses' array.");
            }
            try {
                String innerJson = objectMapper.writeValueAsString(list.get(0));
                return objectMapper.readValue(innerJson, TrainServiceCrewResponse.class);
            } catch (JsonProcessingException e) {
                throw new MessagingException("Failed to parse inner TrainServiceCrewResponse object", e);
            }
        }
    }

    @Component
    static class ResponseWrapperTransformer {
        public Map<String, List<TrainServiceCrewResponse>> wrapResponse(TrainServiceCrewResponse response) {
            return Map.of("TrainServiceCrewResponses", List.of(response));
        }
    }

    @Component
    static class CsvReportHandler {
        private final CsvReportUtil csvReportUtil;

        CsvReportHandler(CsvReportUtil csvReportUtil) {
            this.csvReportUtil = csvReportUtil;
        }

        public Message<TrainServiceCrewResponse> handleCrewRequestReport(Message<TrainServiceCrewResponse> message) {
            TrainServiceCrewResponse payload = message.getPayload();
            Instant arrivalTime = message.getHeaders().get(HEADER_ARRIVAL_TIME, Instant.class);
            String requestId = message.getHeaders().get(HEADER_REQUEST_ID, String.class);
            Instant sentTime = Instant.now();
            csvReportUtil.writeReportEntry(arrivalTime, requestId, "OK", sentTime, "CrewRequestEndpoint");
            return message;
        }

        public Message<StatusResponse> handleStatusResponseReport(Message<StatusResponse> message) {
            StatusResponse payload = message.getPayload();
            Instant arrivalTime = message.getHeaders().get(HEADER_ARRIVAL_TIME, Instant.class);
            String requestId = message.getHeaders().get(HEADER_REQUEST_ID, String.class);
            Instant sentTime = Instant.now();
            csvReportUtil.writeReportEntry(arrivalTime, requestId, payload.getSTATUS(), sentTime, "StatusEndpoint");
            return message;
        }
    }

    @Component
    static class ErrorHandler {
        private final CsvReportUtil csvReportUtil;
        private final MessageChannel loggingChannel;

        ErrorHandler(CsvReportUtil csvReportUtil, @Qualifier("loggingChannel") MessageChannel loggingChannel) {
            this.csvReportUtil = csvReportUtil;
            this.loggingChannel = loggingChannel;
        }

        public Message<?> handleError(Throwable payload, Map<String, Object> headers) {
            log.error("Integration flow error: {}", payload.getMessage(), payload);

            Message<?> errorMessage = MessageBuilder
                    .withPayload("Error processing request: " + payload.getMessage())
                    .copyHeaders(headers)
                    .setHeader("logType", "error")
                    .setHeader("exceptionMessage", payload.getMessage())
                    .build();
            loggingChannel.send(errorMessage);

            Instant arrivalTime = (Instant) headers.get(HEADER_ARRIVAL_TIME);
            String requestId = (String) headers.get(HEADER_REQUEST_ID);
            Instant errorTime = Instant.now();
            String context = headers.containsKey(IntegrationConfig.HEADER_RESPONSE_JSON) ? "CrewRequestEndpoint" 
                           : (headers.containsKey(IntegrationConfig.HEADER_STATUS_RESPONSE_JSON) ? "StatusEndpoint" : "UnknownEndpoint");
            if (arrivalTime != null) {
                csvReportUtil.writeReportEntry(arrivalTime, requestId != null ? requestId : "UNKNOWN", "Error: " + payload.getClass().getSimpleName(), errorTime, context);
            }

            HttpStatus status = payload instanceof JsonProcessingException || (payload.getMessage() != null && (payload.getMessage().contains("Invalid request structure") || payload.getMessage().contains("Failed to parse inner")))
                    ? HttpStatus.BAD_REQUEST
                    : HttpStatus.INTERNAL_SERVER_ERROR;

            return MessageBuilder
                    .withPayload("")
                    .setHeader("http_statusCode", status)
                    .build();
        }
    }
}

