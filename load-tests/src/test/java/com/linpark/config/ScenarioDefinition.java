package com.linpark.config;

import java.util.List;

/**
 * Represents a complete test scenario (workflow).
 */
public class ScenarioDefinition {
    private String name;
    private List<Step> steps;

    public ScenarioDefinition() {
    }

    public ScenarioDefinition(String name, List<Step> steps) {
        this.name = name;
        this.steps = steps;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }
}
