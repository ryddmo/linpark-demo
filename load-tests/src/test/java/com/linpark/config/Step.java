package com.linpark.config;

import java.util.List;
import java.util.Map;

/**
 * Represents an individual HTTP request step in a scenario.
 */
public class Step {
    private String method;
    private String path;
    private String body;
    private Map<String, String> headers;
    private List<Assertion> assertions;

    public Step() {
    }

    public Step(String method, String path, String body, Map<String, String> headers, List<Assertion> assertions) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.headers = headers;
        this.assertions = assertions;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public List<Assertion> getAssertions() {
        return assertions;
    }

    public void setAssertions(List<Assertion> assertions) {
        this.assertions = assertions;
    }
}
