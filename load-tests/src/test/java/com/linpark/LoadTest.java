package com.linpark;

import com.linpark.config.*;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Load testing class using Apache HttpClient for direct HTTP execution.
 * Executes scenarios defined in YAML configuration files.
 * 
 * Configuration can be overridden via CLI properties:
 * - mvn test -Dusers=100 -Dduration=120 -DtargetUrl=http://localhost:8080
 */
public class LoadTest {
    private static final Logger logger = LoggerFactory.getLogger(LoadTest.class);
    private static LoadTestConfig config;
    private static List<ScenarioDefinition> scenarios;

    @BeforeAll
    public static void setupConfig() throws IOException {
        logger.info("========== Loading Configuration ==========");
        ConfigLoader.Pair<LoadTestConfig, List<ScenarioDefinition>> loaded = ConfigLoader.loadConfig();
        config = loaded.first;
        scenarios = loaded.second;
        
        if (scenarios.isEmpty()) {
            throw new IllegalStateException("No scenarios found in configuration");
        }
        logger.info("========== Configuration Loaded Successfully ==========");
    }

    @Test
    public void runLoadTest() throws Exception {
        logger.info("========== Starting Load Test Execution ==========");
        logger.info("Test Configuration:");
        logger.info("  Virtual Users: {}", config.getVirtualUsers());
        logger.info("  Duration: {} seconds", config.getDuration());
        logger.info("  Ramp-up: {} seconds", config.getRampUp());
        logger.info("  Target URL: {}", config.getTargetUrl());
        logger.info("  Think Time: {} milliseconds", config.getThinkTime());
        logger.info("  Scenarios: {}", scenarios.size());
        
        // Log loaded scenarios
        for (ScenarioDefinition scenario : scenarios) {
            logger.info("Scenario: {} with {} steps", scenario.getName(), scenario.getSteps().size());
            for (Step step : scenario.getSteps()) {
                logger.info("  - {} {}", step.getMethod(), step.getPath());
            }
        }
        
        // Run load test with multiple virtual users
        loadTest();
        
        logger.info("========== Load Test Completed Successfully ==========");
    }

    /**
     * Execute load test by spawning virtual users that loop through scenarios
     */
    private void loadTest() throws InterruptedException {
        int numUsers = config.getVirtualUsers();
        long durationSeconds = config.getDuration();
        long rampUpSeconds = config.getRampUp();
        
        logger.info("Creating {} virtual users with {} second ramp-up", numUsers, rampUpSeconds);
        
        ExecutorService executor = Executors.newFixedThreadPool(numUsers);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numUsers);
        
        AtomicInteger totalRequests = new AtomicInteger(0);
        AtomicInteger totalErrors = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        long testStartTime = System.currentTimeMillis();
        long rampUpIntervalMs = (rampUpSeconds * 1000) / numUsers;
        
        // Start all virtual users
        for (int userId = 1; userId <= numUsers; userId++) {
            final int userNumber = userId;
            long delayMs = (userId - 1) * rampUpIntervalMs;
            
            executor.submit(() -> {
                try {
                    // Wait for start signal
                    if (delayMs > 0) {
                        Thread.sleep(delayMs);
                    }
                    startSignal.await();
                    
                    // Execute scenarios until duration is reached
                    long userStartTime = System.currentTimeMillis();
                    while (System.currentTimeMillis() - testStartTime < durationSeconds * 1000) {
                        for (ScenarioDefinition scenario : scenarios) {
                            executeScenarioWithHttpClient(userNumber, scenario, totalRequests, totalErrors, totalResponseTime);
                            
                            // Check if we've exceeded duration
                            if (System.currentTimeMillis() - testStartTime >= durationSeconds * 1000) {
                                break;
                            }
                        }
                    }
                    
                    long userElapsedSeconds = (System.currentTimeMillis() - userStartTime) / 1000;
                    logger.debug("User {} completed after {} seconds", userNumber, userElapsedSeconds);
                } catch (InterruptedException e) {
                    logger.warn("User {} was interrupted", userNumber);
                    Thread.currentThread().interrupt();
                } finally {
                    doneSignal.countDown();
                }
            });
        }
        
        // Give all threads a moment to be ready, then start
        Thread.sleep(100);
        startSignal.countDown();
        logger.info("Load test started with {} virtual users", numUsers);
        
        // Wait for all users to finish
        doneSignal.await();
        
        long totalElapsedMs = System.currentTimeMillis() - testStartTime;
        long totalElapsedSeconds = totalElapsedMs / 1000;
        
        executor.shutdownNow();
        
        // Calculate metrics
        int failedRequests = totalErrors.get();
        int successfulRequests = totalRequests.get() - failedRequests;
        double successRate = totalRequests.get() > 0 ? (successfulRequests * 100.0 / totalRequests.get()) : 0;
        double requestsPerSecond = totalElapsedSeconds > 0 ? (totalRequests.get() * 1.0 / totalElapsedSeconds) : 0;
        double avgResponseTime = totalRequests.get() > 0 ? (totalResponseTime.get() * 1.0 / totalRequests.get()) : 0;
        
        // Print results to console
        logger.info("========== Load Test Results ==========");
        logger.info("Total Requests: {}", totalRequests.get());
        logger.info("Failed Requests: {}", failedRequests);
        logger.info("Success Rate: {:.2f}%", successRate);
        logger.info("Total Elapsed Time: {} seconds", totalElapsedSeconds);
        logger.info("Requests/Second: {:.2f}", requestsPerSecond);
        logger.info("Average Response Time: {:.0f} ms", avgResponseTime);
        
        // Generate HTML report
        generateHtmlReport(totalRequests.get(), failedRequests, successRate, totalElapsedSeconds, 
                          requestsPerSecond, avgResponseTime, numUsers);
    }

    /**
     * Executes a single scenario using Apache HttpClient
     */
    private void executeScenarioWithHttpClient(int userId, ScenarioDefinition scenario, 
                                               AtomicInteger totalRequests, AtomicInteger totalErrors,
                                               AtomicLong totalResponseTime) {
        HttpClient client = HttpClients.createDefault();
        
        for (Step step : scenario.getSteps()) {
            String url = config.getTargetUrl() + step.getPath();
            
            try {
                long requestStartTime = System.currentTimeMillis();
                
                // Create HTTP request
                ClassicHttpRequest request = createHttpRequest(step, url);
                
                // Execute request
                int statusCode = client.execute(request, response -> {
                    totalRequests.incrementAndGet();
                    return response.getCode();
                });
                
                long responseTime = System.currentTimeMillis() - requestStartTime;
                totalResponseTime.addAndGet(responseTime);
                
                // Validate response
                boolean assertionPassed = validateAssertions(step, statusCode);
                if (!assertionPassed) {
                    totalErrors.incrementAndGet();
                    logger.warn("User {} - Assertion failed for {} {}: got status code {}", 
                               userId, step.getMethod(), step.getPath(), statusCode);
                }
                
                logger.debug("User {} - {} {} -> {} ({}ms)", userId, step.getMethod(), step.getPath(), statusCode, responseTime);
                
                // Think time between requests
                if (config.getThinkTime() > 0) {
                    Thread.sleep(config.getThinkTime());
                }
                
            } catch (Exception e) {
                totalErrors.incrementAndGet();
                logger.warn("User {} - Error executing {} {}: {}", userId, step.getMethod(), step.getPath(), e.getMessage());
            }
        }
    }

    /**
     * Creates an HTTP request based on step configuration
     */
    private org.apache.hc.core5.http.ClassicHttpRequest createHttpRequest(Step step, String url) {
        org.apache.hc.core5.http.ClassicHttpRequest request;
        
        switch (step.getMethod().toUpperCase()) {
            case "GET":
                request = new HttpGet(url);
                break;
            case "POST":
                request = new HttpPost(url);
                if (step.getBody() != null && !step.getBody().isEmpty()) {
                    ((HttpPost) request).setEntity(new StringEntity(step.getBody(), ContentType.APPLICATION_JSON));
                }
                break;
            case "PUT":
                request = new HttpPut(url);
                if (step.getBody() != null && !step.getBody().isEmpty()) {
                    ((HttpPut) request).setEntity(new StringEntity(step.getBody(), ContentType.APPLICATION_JSON));
                }
                break;
            case "DELETE":
                request = new HttpDelete(url);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + step.getMethod());
        }
        
        // Add headers
        if (step.getHeaders() != null && !step.getHeaders().isEmpty()) {
            for (String headerName : step.getHeaders().keySet()) {
                request.addHeader(new BasicHeader(headerName, step.getHeaders().get(headerName)));
            }
        }
        
        return request;
    }

    /**
     * Validates response against assertions
     */
    private boolean validateAssertions(Step step, int statusCode) {
        if (step.getAssertions() == null || step.getAssertions().isEmpty()) {
            return true;
        }
        
        for (Assertion assertion : step.getAssertions()) {
            if (assertion.getStatusCode() != null) {
                if (statusCode != assertion.getStatusCode()) {
                    return false;
                }
            }
        }
        
        return true;
    }

    /**
     * Generates an HTML report of the load test results
     */
    private void generateHtmlReport(int totalRequests, int failedRequests, double successRate, 
                                   long totalElapsedSeconds, double requestsPerSecond, 
                                   double avgResponseTime, int numUsers) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String reportPath = "target/load-test-report-" + timestamp + ".html";
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"en\">\n");
            html.append("<head>\n");
            html.append("  <meta charset=\"UTF-8\">\n");
            html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("  <title>Load Test Report</title>\n");
            html.append("  <style>\n");
            html.append("    body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
            html.append("    .container { max-width: 900px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
            html.append("    h1 { color: #333; border-bottom: 3px solid #007bff; padding-bottom: 10px; }\n");
            html.append("    .metrics { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 20px 0; }\n");
            html.append("    .metric { background-color: #f9f9f9; padding: 15px; border-radius: 5px; border-left: 4px solid #007bff; }\n");
            html.append("    .metric-label { font-weight: bold; color: #666; font-size: 12px; text-transform: uppercase; }\n");
            html.append("    .metric-value { font-size: 28px; color: #007bff; font-weight: bold; margin-top: 5px; }\n");
            html.append("    .metric-unit { font-size: 14px; color: #999; margin-left: 5px; }\n");
            html.append("    .success { border-left-color: #28a745; }\n");
            html.append("    .success .metric-value { color: #28a745; }\n");
            html.append("    .warning { border-left-color: #ffc107; }\n");
            html.append("    .warning .metric-value { color: #ffc107; }\n");
            html.append("    .error { border-left-color: #dc3545; }\n");
            html.append("    .error .metric-value { color: #dc3545; }\n");
            html.append("    .info-section { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; }\n");
            html.append("    table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n");
            html.append("    th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }\n");
            html.append("    th { background-color: #f0f0f0; font-weight: bold; }\n");
            html.append("    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; font-size: 12px; color: #999; text-align: center; }\n");
            html.append("  </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("  <div class=\"container\">\n");
            html.append("    <h1>Load Test Report</h1>\n");
            html.append("    \n");
            html.append("    <div class=\"metrics\">\n");
            html.append("      <div class=\"metric success\">\n");
            html.append("        <div class=\"metric-label\">Total Requests</div>\n");
            html.append("        <div class=\"metric-value\">").append(totalRequests).append("</div>\n");
            html.append("      </div>\n");
            html.append("      <div class=\"metric error\">\n");
            html.append("        <div class=\"metric-label\">Failed Requests</div>\n");
            html.append("        <div class=\"metric-value\">").append(failedRequests).append("</div>\n");
            html.append("      </div>\n");
            html.append("      <div class=\"metric success\">\n");
            html.append("        <div class=\"metric-label\">Success Rate</div>\n");
            html.append("        <div class=\"metric-value\">").append(String.format("%.1f", successRate)).append("<span class=\"metric-unit\">%</span></div>\n");
            html.append("      </div>\n");
            html.append("      <div class=\"metric warning\">\n");
            html.append("        <div class=\"metric-label\">Avg Response Time</div>\n");
            html.append("        <div class=\"metric-value\">").append(String.format("%.0f", avgResponseTime)).append("<span class=\"metric-unit\">ms</span></div>\n");
            html.append("      </div>\n");
            html.append("      <div class=\"metric\">\n");
            html.append("        <div class=\"metric-label\">Requests/Second</div>\n");
            html.append("        <div class=\"metric-value\">").append(String.format("%.2f", requestsPerSecond)).append("<span class=\"metric-unit\">req/s</span></div>\n");
            html.append("      </div>\n");
            html.append("      <div class=\"metric\">\n");
            html.append("        <div class=\"metric-label\">Duration</div>\n");
            html.append("        <div class=\"metric-value\">").append(totalElapsedSeconds).append("<span class=\"metric-unit\">s</span></div>\n");
            html.append("      </div>\n");
            html.append("    </div>\n");
            html.append("    \n");
            html.append("    <div class=\"info-section\">\n");
            html.append("      <h2>Test Configuration</h2>\n");
            html.append("      <table>\n");
            html.append("        <tr><th>Configuration</th><th>Value</th></tr>\n");
            html.append("        <tr><td>Virtual Users</td><td>").append(numUsers).append("</td></tr>\n");
            html.append("        <tr><td>Test Duration</td><td>").append(config.getDuration()).append(" seconds</td></tr>\n");
            html.append("        <tr><td>Ramp-up Time</td><td>").append(config.getRampUp()).append(" seconds</td></tr>\n");
            html.append("        <tr><td>Target URL</td><td>").append(config.getTargetUrl()).append("</td></tr>\n");
            html.append("        <tr><td>Think Time</td><td>").append(config.getThinkTime()).append(" ms</td></tr>\n");
            html.append("        <tr><td>Scenarios</td><td>").append(scenarios.size()).append("</td></tr>\n");
            html.append("      </table>\n");
            html.append("    </div>\n");
            html.append("    \n");
            html.append("    <div class=\"info-section\">\n");
            html.append("      <h2>Scenarios Executed</h2>\n");
            html.append("      <table>\n");
            html.append("        <tr><th>Scenario</th><th>Steps</th><th>Endpoints</th></tr>\n");
            
            for (ScenarioDefinition scenario : scenarios) {
                StringBuilder endpoints = new StringBuilder();
                for (Step step : scenario.getSteps()) {
                    if (endpoints.length() > 0) endpoints.append(", ");
                    endpoints.append(step.getMethod()).append(" ").append(step.getPath());
                }
                html.append("        <tr><td>").append(scenario.getName()).append("</td><td>")
                    .append(scenario.getSteps().size()).append("</td><td>").append(endpoints).append("</td></tr>\n");
            }
            
            html.append("      </table>\n");
            html.append("    </div>\n");
            html.append("    \n");
            html.append("    <div class=\"footer\">\n");
            html.append("      <p>Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>\n");
            html.append("    </div>\n");
            html.append("  </div>\n");
            html.append("</body>\n");
            html.append("</html>\n");
            
            Files.write(Paths.get(reportPath), html.toString().getBytes());
            logger.info("HTML report generated: {}", reportPath);
            
        } catch (Exception e) {
            logger.warn("Failed to generate HTML report: {}", e.getMessage());
        }
    }
}
