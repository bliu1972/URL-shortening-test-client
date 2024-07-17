package com.util.load;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTestClient {
    private static final String BASE_URL = "http://localhost:9000/api/v1";
    private static final String BASE_ORIGINAL_URL = "http://mydomain.com/";
    private static final int THREAD_COUNT = 10;
    private static final int REQUEST_COUNT = 10000;
    private static final double REQUESTS_PER_SECOND = 30.0;  // Throttle rate

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final RateLimiter rateLimiter = RateLimiter.create(REQUESTS_PER_SECOND);  // Create RateLimiter
    private static final long THROTTLE_DELAY_MS = 500 / (long) REQUESTS_PER_SECOND;  // Delay for throttling

    public static void main(String[] args) throws InterruptedException, IOException {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50);  // Increase total connections
        connectionManager.setDefaultMaxPerRoute(50);  // Increase max connections per route

        // Create the HttpClient with the connection manager
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(5000)
                        .setSocketTimeout(5000)
                        .build())
                .build()) {

            ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
            final AtomicInteger errCount = new AtomicInteger(0);
            final AtomicInteger totalCount = new AtomicInteger(0);

            Callable<Void> task = () -> {
                try {
                    for (int j = 0; j < REQUEST_COUNT; j++) {
                        rateLimiter.acquire();  // Acquire a permit from the RateLimiter
                        String originalUrl = BASE_ORIGINAL_URL + "test" + j;
                        String shortenedUrl = encodeUrl(httpClient, originalUrl);
                        int currCount = totalCount.incrementAndGet();
                        if (currCount % 100 == 0) {
                            System.out.println("Operation performed: " + currCount);
                        }
                        if (shortenedUrl != null) {
                            if (shortenedUrl.length() != BASE_ORIGINAL_URL.length() + 6) {
                                errCount.incrementAndGet();
                                System.err.println("Shortened URL length is incorrect: " + shortenedUrl);
                            } else {
                                String decodedUrl = decodeUrl(httpClient, shortenedUrl);
                                if (!originalUrl.equals(decodedUrl)) {
                                    errCount.incrementAndGet();
                                    System.err.println("Decoded URL mismatch: originalUrl=" + originalUrl + ", decodedUrl=" + decodedUrl);
                                }
                            }
                        }

                        Thread.sleep(THROTTLE_DELAY_MS);  // Sleep to enforce rate limit
                    }
                } catch (Exception e) {
                    System.err.println("Exception occurred: " + e.getMessage());
                }
                return null;
            };

            // Submit tasks and wait for them to complete
            try {
                executorService.invokeAll(java.util.Collections.nCopies(THREAD_COUNT, task));
            } finally {
                executorService.shutdown();
                executorService.awaitTermination(1, TimeUnit.MINUTES);
            }

            System.out.println("error count = " + errCount.get());
            if (errCount.get() > 0) {
                System.out.println("Load test completed with " + errCount.get() + " errors");
            } else {
                System.out.println("Load test completed without errors");
            }
        }
    }

    private static String encodeUrl(CloseableHttpClient httpClient, String originalUrl) throws IOException {
        HttpPost post = new HttpPost(BASE_URL + "/encode");
        post.setHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity("{\"originalUrl\":\"" + originalUrl + "\"}"));

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonNode jsonNode = objectMapper.readTree(jsonResponse);
                return jsonNode.get("shortenedUrl").asText();
            } else {
                System.err.println("Failed to encode URL: " + response.getStatusLine());
                return null;
            }
        }
    }

    private static String decodeUrl(CloseableHttpClient httpClient, String shortenedUrl) throws IOException {
        HttpGet get = new HttpGet(BASE_URL + "/decode?shortenedUrl=" + URLEncoder.encode(shortenedUrl, StandardCharsets.UTF_8.toString()));

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonNode jsonNode = objectMapper.readTree(jsonResponse);
                return jsonNode.get("originalUrl").asText();
            } else {
                System.err.println("Failed to decode URL: " + response.getStatusLine());
                return null;
            }
        }
    }
}
