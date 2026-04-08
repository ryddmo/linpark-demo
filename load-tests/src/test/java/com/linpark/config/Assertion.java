package com.linpark.config;

/**
 * Represents an HTTP assertion for validating response.
 */
public class Assertion {
    private Integer statusCode;
    private String regexPattern;
    private String bodyContains;

    public Assertion() {
    }

    public Assertion(Integer statusCode, String regexPattern, String bodyContains) {
        this.statusCode = statusCode;
        this.regexPattern = regexPattern;
        this.bodyContains = bodyContains;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getRegexPattern() {
        return regexPattern;
    }

    public void setRegexPattern(String regexPattern) {
        this.regexPattern = regexPattern;
    }

    public String getBodyContains() {
        return bodyContains;
    }

    public void setBodyContains(String bodyContains) {
        this.bodyContains = bodyContains;
    }
}
