package com.linpark.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Loads scenario definitions from YAML files and applies CLI parameter overrides.
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * Loads all scenarios and configuration.
     * @return Pair of (LoadTestConfig, List of ScenarioDefinitions)
     */
    public static Pair<LoadTestConfig, List<ScenarioDefinition>> loadConfig() throws IOException {
        logger.info("Loading scenario configuration...");
        
        // Load default config from scenarios.yaml
        LoadTestConfig defaultConfig = loadDefaultConfig();
        
        // Apply CLI parameter overrides
        LoadTestConfig finalConfig = applyCliOverrides(defaultConfig);
        
        // Load all scenarios
        List<ScenarioDefinition> scenarios = loadScenarios(finalConfig);
        
        // Filter by scenario name if specified
        String scenarioFilter = System.getProperty("scenario");
        if (scenarioFilter != null && !scenarioFilter.isEmpty()) {
            logger.info("Filtering scenarios to: {}", scenarioFilter);
            scenarios = scenarios.stream()
                    .filter(s -> s.getName().equalsIgnoreCase(scenarioFilter))
                    .toList();
        }
        
        logger.info("Loaded {} scenario(s)", scenarios.size());
        logger.info("Configuration: users={}, duration={}s, rampUp={}s, targetUrl={}, thinkTime={}ms",
                finalConfig.getVirtualUsers(),
                finalConfig.getDuration(),
                finalConfig.getRampUp(),
                finalConfig.getTargetUrl(),
                finalConfig.getThinkTime());
        
        return new Pair<>(finalConfig, scenarios);
    }

    /**
     * Loads default configuration from scenarios.yaml
     */
    private static LoadTestConfig loadDefaultConfig() throws IOException {
        InputStream is = ConfigLoader.class.getResourceAsStream("/scenarios/scenarios.yaml");
        if (is == null) {
            throw new IOException("Cannot find scenarios.yaml in classpath");
        }
        
        Map<String, Object> yamlContent = yamlMapper.readValue(is, Map.class);
        Map<String, Object> defaults = (Map<String, Object>) yamlContent.get("defaults");
        
        if (defaults == null) {
            throw new IOException("No 'defaults' section found in scenarios.yaml");
        }
        
        return new LoadTestConfig(
                ((Number) defaults.getOrDefault("virtualUsers", 10)).intValue(),
                ((Number) defaults.getOrDefault("duration", 60)).intValue(),
                ((Number) defaults.getOrDefault("rampUp", 5)).intValue(),
                (String) defaults.getOrDefault("targetUrl", "http://httpbin.org"),
                ((Number) defaults.getOrDefault("thinkTime", 500)).intValue()
        );
    }

    /**
     * Applies CLI parameter overrides to configuration
     */
    private static LoadTestConfig applyCliOverrides(LoadTestConfig config) {
        String users = System.getProperty("users");
        if (users != null) {
            config.setVirtualUsers(Integer.parseInt(users));
            logger.info("Overriding virtualUsers to: {}", users);
        }
        
        String duration = System.getProperty("duration");
        if (duration != null) {
            config.setDuration(Integer.parseInt(duration));
            logger.info("Overriding duration to: {}s", duration);
        }
        
        String rampUp = System.getProperty("rampUp");
        if (rampUp != null) {
            config.setRampUp(Integer.parseInt(rampUp));
            logger.info("Overriding rampUp to: {}s", rampUp);
        }
        
        String targetUrl = System.getProperty("targetUrl");
        if (targetUrl != null) {
            config.setTargetUrl(targetUrl);
            logger.info("Overriding targetUrl to: {}", targetUrl);
        }
        
        String thinkTime = System.getProperty("thinkTime");
        if (thinkTime != null) {
            config.setThinkTime(Integer.parseInt(thinkTime));
            logger.info("Overriding thinkTime to: {}ms", thinkTime);
        }
        
        return config;
    }

    /**
     * Loads all scenario definitions from YAML files in scenarios directory
     */
    private static List<ScenarioDefinition> loadScenarios(LoadTestConfig config) throws IOException {
        List<ScenarioDefinition> scenarios = new ArrayList<>();
        
        // Get the scenarios directory from classpath
        URL scenariosUrl = ConfigLoader.class.getResource("/scenarios");
        if (scenariosUrl == null) {
            logger.warn("Scenarios directory not found in classpath");
            return scenarios;
        }
        
        Path scenariosPath;
        try {
            scenariosPath = Paths.get(scenariosUrl.toURI());
        } catch (java.net.URISyntaxException e) {
            logger.error("Invalid scenario path URI: {}", e.getMessage());
            return scenarios;
        }
        
        if (!Files.isDirectory(scenariosPath)) {
            logger.warn("Scenarios path is not a directory: {}", scenariosPath);
            return scenarios;
        }
        
        // Load all YAML files (except scenarios.yaml)
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(scenariosPath, "*.yaml")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                if ("scenarios.yaml".equals(fileName)) {
                    continue; // Skip the main config file
                }
                
                try {
                    logger.debug("Loading scenario from: {}", fileName);
                    ScenarioDefinition scenario = yamlMapper.readValue(
                            Files.newInputStream(file),
                            ScenarioDefinition.class
                    );
                    scenarios.add(scenario);
                    logger.info("Loaded scenario: {} with {} steps", scenario.getName(), scenario.getSteps().size());
                } catch (Exception e) {
                    logger.error("Failed to load scenario from {}: {}", fileName, e.getMessage());
                    throw new IOException("Error loading scenario: " + fileName, e);
                }
            }
        }
        
        return scenarios;
    }

    /**
     * Simple Pair class to return two values
     */
    public static class Pair<A, B> {
        public final A first;
        public final B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }
    }
}
