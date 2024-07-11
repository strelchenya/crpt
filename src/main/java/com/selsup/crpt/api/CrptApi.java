package com.selsup.crpt.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class CrptApi {
    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final int NOT_AUTHORIZATION = 401;
    private static final MediaType CONTENT_TYPE = MediaType.get("application/json");
    private static final String PREFIX_BEARER = "Bearer ";
    private static final int DURATION = 1;

    private final OkHttpClient client;
    @Getter
    private final AtomicInteger requestCount;
    private final ObjectMapper objectMapper;
    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.client = new OkHttpClient();
        this.requestCount = new AtomicInteger(0);
        this.objectMapper = new ObjectMapper();
        this.semaphore = new Semaphore(requestLimit);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            requestCount.set(0);
            semaphore.release(requestLimit - semaphore.availablePermits());
        }, 0, DURATION, timeUnit);
    }

    public Response createDocument(Document document, String signature) {
        Response response = null;
        try {
            semaphore.acquire();
            requestCount.incrementAndGet();

            String content = objectMapper.writeValueAsString(document);
            var requestBody = RequestBody.create(content, CONTENT_TYPE);
            var request = new Request.Builder()
                    .url(URL)
                    .post(requestBody)
                    .addHeader("Authorization", PREFIX_BEARER.concat(signature))
                    .build();

            response = client.newCall(request).execute();
            if (isNotSuccessfulResponse(response)) {
                throw new IOException("Unexpected code " + response);
            }
            if (response.code() == NOT_AUTHORIZATION) {
                log.warn("[createDocument] NOT_AUTHORIZATION by signature={}, response={}", signature, response);
            }

        } catch (IOException | InterruptedException e) {
            log.error("[main] Document creation failed!", e);
            Thread.currentThread().interrupt();
        }
        return response;
    }

    private boolean isNotSuccessfulResponse(Response response) {
        return response == null
                || (!response.isSuccessful() && response.code() != NOT_AUTHORIZATION);
    }

    @Data
    public static class Document {
        @JsonProperty("description")
        private Description description;
        @JsonProperty("doc_id")
        private String docId;
        @JsonProperty("doc_status")
        private String docStatus;
        @JsonProperty("doc_type")
        private String docType;
        @JsonProperty("importRequest")
        private Boolean importRequest;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("participant_inn")
        private String participantInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        private String productionDate;
        @JsonProperty("production_type")
        private String productionType;
        @JsonProperty("products")
        private List<Product> products;
        @JsonProperty("reg_date")
        private String regDate;
        @JsonProperty("reg_number")
        private String regNumber;
    }

    @Data
    public static class Description {
        @JsonProperty("participantInn")
        private String participantInn;
    }

    @Data
    public static class Product {
        @JsonProperty("certificate_document")
        private String certificateDocument;
        @JsonProperty("certificate_document_date")
        private String certificateDocumentDate;
        @JsonProperty("certificate_document_number")
        private String certificateDocumentNumber;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        private String productionDate;
        @JsonProperty("tnved_code")
        private String tnvedCode;
        @JsonProperty("uit_code")
        private String uitCode;
        @JsonProperty("uitu_code")
        private String uituCode;
    }
}
