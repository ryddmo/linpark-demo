# Load Testing Project Specification

## Overview

This is a Java Maven multi-module project for load testing using JMeter Java DSL. It provides a configuration-driven framework for defining test scenarios in YAML and executing them with parameterized load patterns.

## Architecture

- **Parent POM**: Centralizes dependency versions and plugin configuration
- **load-tests Module**: Contains JUnit-executable load tests powered by JMeter Java DSL
- **Configuration Framework**: YAML-based scenario definitions with CLI parameter overrides
- **Target Java Version**: Java 21 (LTS)

## Project Structure

```
linpark-demo/
├── pom.xml                                 # Parent POM
├── load-tests/                             # Load testing module
│   ├── pom.xml                             # Module POM
│   ├── src/
│   │   ├── test/java/
│   │   │   ├── LoadTest.java               # Main JUnit test class
│   │   │   └── config/
│   │   │       ├── LoadTestConfig.java     # Runtime parameters POJO
│   │   │       ├── ScenarioDefinition.java # Scenario workflow POJO
│   │   │       ├── Step.java               # HTTP request step POJO
│   │   │       ├── Assertion.java          # Response assertion POJO
│   │   │       └── ConfigLoader.java       # YAML loader + CLI override logic
│   │   └── test/resources/
│   │       ├── log4j2.xml                  # Logging configuration
│   │       └── scenarios/
│   │           ├── scenarios.yaml          # Default config & scenario list
│   │           ├── login-browse-checkout.yaml
│   │           └── api-read-only.yaml
├── README.md                               # User-facing documentation
├── spec.md                                 # This file
└── .gitignore                              # Exclude build artifacts
```

## Configuration

### Scenario YAML Format

Scenarios are defined in `load-tests/src/test/resources/scenarios/` as YAML files.

**Default Config** (`scenarios.yaml`):

```yaml
defaults:
  virtualUsers: 10
  duration: 60
  rampUp: 5
  targetUrl: "http://httpbin.org"
  thinkTime: 500

scenarios:
  - login-browse-checkout
  - api-read-only
```

**Example Scenario** (`login-browse-checkout.yaml`):

```yaml
name: "Login → Browse → Checkout"
steps:
  - method: "POST"
    path: "/login"
    body: '{"username":"user","password":"pass"}'
    headers:
      Content-Type: "application/json"
    assertions:
      - statusCode: 200
  - method: "GET"
    path: "/products"
    assertions:
      - statusCode: 200
  - method: "POST"
    path: "/cart/add"
    body: '{"productId":"123","quantity":1}'
    headers:
      Content-Type: "application/json"
    assertions:
      - statusCode: 200
  - method: "POST"
    path: "/checkout"
    body: '{"paymentMethod":"card"}'
    headers:
      Content-Type: "application/json"
    assertions:
      - statusCode: 200
```

### CLI Parameters

All configuration parameters can be overridden via Maven CLI properties:

- **`-Dusers=N`** — Number of virtual users (default: from YAML)
- **`-Dduration=SECONDS`** — Test duration in seconds (default: from YAML)
- **`-DrampUp=SECONDS`** — Ramp-up time in seconds (default: from YAML)
- **`-DtargetUrl=URL`** — Base target URL (default: from YAML)
- **`-DthinkTime=MS`** — Think time between requests in milliseconds (default: from YAML)
- **`-Dscenario=NAME`** — Run only this scenario; if not set, all scenarios run (optional)

## Usage

### Build the Project

```bash
mvn clean install
```

### Run All Scenarios with Defaults

```bash
mvn test
```

### Run with Custom Parameters

```bash
# 50 users, 120 seconds duration
mvn test -Dusers=50 -Dduration=120

# Custom target URL
mvn test -DtargetUrl=http://localhost:8080 -Dusers=100

# All parameters overridden
mvn test -Dusers=200 -Dduration=300 -DrampUp=30 -DtargetUrl=http://myapp.local -DthinkTime=1000
```

### Run Specific Scenario Only

```bash
mvn test -Dscenario=login-browse-checkout
```

### Run Tests with Verbose Output

```bash
mvn test -X
```

## Implementation Phases

### Phase 1: Project Structure & Configuration Framework ✅

- [x] Create parent pom.xml with Java 21, dependencyManagement, Surefire plugin
- [x] Create load-tests module pom.xml with Jackson-YAML dependency
- [x] Create config POJOs: LoadTestConfig, ScenarioDefinition, Step, Assertion
- [x] Create ConfigLoader utility class

### Phase 2: Scenario Definitions ✅

- [x] Create scenarios.yaml with defaults and scenario list
- [x] Create login-browse-checkout.yaml example scenario
- [x] Create api-read-only.yaml example scenario

### Phase 3: Test Implementation & CLI Integration ✅

- [x] Create LoadTest.java JUnit test class
- [x] Implement scenario loading via ConfigLoader
- [x] Implement CLI parameter override logic
- [x] Build dynamic JMeter test plans from scenario definitions
- [x] Add assertions and result collection

### Phase 4: Configuration & Execution ✅

- [x] Configure Surefire plugin for \*LoadTest.java naming
- [x] Create log4j2.xml logging configuration
- [x] Document CLI usage examples

### Phase 5: Documentation & Git ✅

- [x] Create README.md with usage instructions
- [x] Create spec.md with technical specification
- [x] Create or update .gitignore

## Verification Steps

1. **Build**: Run `mvn clean install` — should complete without errors
2. **Load all scenarios**: Run `mvn test` — should execute LoadTest and run all scenarios
3. **CLI override**: Run `mvn test -Dusers=50 -Dduration=120` — verify parameters override YAML
4. **Filter scenario**: Run `mvn test -Dscenario=login-browse-checkout` — run only specified scenario
5. **Custom URL**: Run `mvn test -DtargetUrl=http://localhost:8080 -Dusers=20` — verify URL and user override
6. **Results**: Check `target/surefire-reports/` for JUnit results and console for JMeter metrics
7. **Extensibility**: Add a new scenario YAML file; verify it loads and executes without code changes

## Dependencies

### Parent POM - dependencyManagement

- **jmeter-java-dsl** (0.65) - Fluent JMeter API
- **junit-jupiter** (5.9.3) - Modern JUnit framework
- **httpclient5** (5.2.1) - Apache HTTP client
- **slf4j-api** (2.0.9) - Logging facade
- **log4j-core** (2.21.1) - Logging implementation
- **jackson-dataformat-yaml** (2.15.3) - YAML parsing
- **lombok** (1.18.30) - Reduce boilerplate

### load-tests Module - Direct Dependencies

- All above (inherited from parent dependencyManagement)

## Configuration Classes Details

### LoadTestConfig

```java
@Data
public class LoadTestConfig {
    private int virtualUsers;
    private int duration;
    private int rampUp;
    private String targetUrl;
    private int thinkTime;
}
```

Holds runtime parameters for load testing. Can be overridden via CLI properties.

### ScenarioDefinition

```java
@Data
public class ScenarioDefinition {
    private String name;
    private List<Step> steps;
}
```

Represents a complete test scenario (workflow) with a name and list of HTTP request steps.

### Step

```java
@Data
public class Step {
    private String method;      // GET, POST, etc.
    private String path;        // Relative path
    private String body;        // Optional request body
    private Map<String, String> headers; // Optional headers
    private List<Assertion> assertions; // Response validations
}
```

Represents an individual HTTP request step with optional body, headers, and assertions.

### Assertion

```java
@Data
public class Assertion {
    private Integer statusCode;
    private String regexPattern;
    private String bodyContains;
}
```

Represents a response validation check (status code, regex pattern, or body content).

### ConfigLoader

Loads scenarios from YAML, applies System property overrides, returns (LoadTestConfig, List<ScenarioDefinition>).

**Key Methods:**

- `loadConfig()` - Main entry point; returns pair of config and scenarios
- `loadDefaultConfig()` - Loads defaults from scenarios.yaml
- `applyCliOverrides(LoadTestConfig)` - Applies System.getProperty() overrides
- `loadScenarios(LoadTestConfig)` - Auto-discovers and loads all scenario YAML files

## Key Design Decisions

1. **YAML-based configuration**: Readable, version-controllable; scenarios are data, not code
2. **CLI parameter overrides**: Configure tests without code changes; supports CI/CD parameterization
3. **Auto-discovery**: Add scenarios by dropping YAML files; no code modifications needed
4. **Multi-scenario execution**: Run all scenarios in a single test, or filter by name
5. **JUnit integration**: Tests are JUnit tests, compatible with standard test runners and CI/CD
6. **Apache HttpClient**: Native HTTP client in JMeter DSL; no additional setup
7. **SLF4J + Log4j2**: Standard logging facade with high-performance implementation
8. **Lombok**: Reduce POJO boilerplate with annotations

## Test Execution Flow

1. **JUnit discovers** `LoadTest.java` via Surefire plugin (matches `*LoadTest.java` pattern)
2. **@BeforeAll** calls `ConfigLoader.loadConfig()` to:
   - Load defaults from `scenarios.yaml`
   - Apply CLI parameter overrides from `System.getProperty()`
   - Auto-discover and load all scenario YAML files
   - Filter scenarios if `-Dscenario=name` is provided
3. **@Test** method `runLoadTest()` executes:
   - Builds HTTP samplers for each scenario step
   - Configures JMeter thread group with users, duration, ramp-up
   - Adds think time between requests
   - Creates assertions for response validation
   - Executes the test plan via JMeter DSL
4. **Results** reported via:
   - Console output (real-time metrics)
   - JUnit XML reports in `target/surefire-reports/`
   - Detailed logs in `target/load-tests.log`

## Logging Configuration

`log4j2.xml` provides:

- **Console appender** - Real-time test progress output (INFO level)
- **File appender** - Detailed logs to `target/load-tests.log` (DEBUG level)
- **Component-specific levels**:
  - `com.linpark` - DEBUG (application code)
  - `org.apache.jmeter` - INFO (JMeter core)
  - `us.abstracta.jmeter` - INFO (JMeter DSL)

Adjust levels as needed for different scenarios (verbose debugging vs. minimal output).

## Extensibility

### Adding New Scenarios

1. Create `scenarios/my-scenario.yaml` with step definitions
2. Run `mvn test` - automatically discovered and executed
3. (Optional) Add reference to `scenarios.yaml` for documentation

### CLI Scenario Selection

```bash
mvn test -Dscenario=my-scenario
```

### Adding New Assertions

Extend `Assertion` class with additional fields:

- `String regexPattern` - Already defined for regex matching
- `String bodyContains` - Already defined for body content check
- Custom assertion types can be added and handled in `LoadTest.buildScenarioSamplers()`

### Parameterized Tests

Current implementation loads all scenarios in a single test. For parameterized execution:

- Use `@ParameterizedTest` with `@MethodSource` returning different LoadTestConfig instances
- Or use separate test methods for different load profiles

## Future Enhancements

- Conditional logic in scenarios (if-then-else, variable extraction)
- Detailed metrics export (CSV, JMeter JMX format for GUI analysis)
- Result aggregation and reporting dashboard
- Performance assertions (e.g., 99th percentile response time < 1000ms)
- Custom headers/correlation ID injection and extraction
- Authentication support (Basic, OAuth2, etc.)
- Data parameterization (CSV data set, dynamic variables)
- Distributed load testing (remote JMeter nodes)
- Custom variable functions (random data generation, etc.)
- Response payload verification and validation
- Custom samplers for non-HTTP protocols

## Build & Test Commands

### Build Only

```bash
mvn clean compile
```

### Build & Run Tests

```bash
mvn clean test
```

### Full Build (including integration tests)

```bash
mvn clean install
```

### Run Specific Test

```bash
mvn test -Dtest=LoadTest
```

### Clean Build Artifacts

```bash
mvn clean
```

### View Dependency Tree

```bash
mvn dependency:tree
```

## Troubleshooting Guide

| Issue                                | Cause                         | Solution                                                   |
| ------------------------------------ | ----------------------------- | ---------------------------------------------------------- |
| Build fails with "Java 21 not found" | JDK version mismatch          | Install Java 21+: `java -version`                          |
| Tests don't find scenarios           | YAML files in wrong location  | Verify files in `load-tests/src/test/resources/scenarios/` |
| CLI parameters ignored               | Wrong property name or format | Use exact names: `-Dusers=50` (not `-DUsers=50`)           |
| Connection refused                   | Target URL unreachable        | Verify URL with `-DtargetUrl=http://localhost:8080`        |
| Low throughput                       | Ramp-up time too long         | Reduce ramp-up: `-DrampUp=2`                               |
| High memory usage                    | Too many virtual users        | Reduce users: `-Dusers=50`                                 |

## Notes for Operators

- Default target is `http://httpbin.org` (public test service) - modify via `-DtargetUrl`
- Think time (500ms default) increases realism; set to 0 for max throughput: `-DthinkTime=0`
- Results are JUnit-compatible and can be parsed by CI/CD systems
- Logs are stored in `target/load-tests.log` for post-execution analysis
- Scenario execution is sequential; all steps in a scenario execute before next scenario starts
- Virtual users distribute load across all scenarios equally

## Related Documentation

- **README.md** - User guide with examples and troubleshooting
- **spec.md** - This technical specification document
- **JMeter Java DSL** - https://abstracta.us/jmeter/
- **Apache JMeter** - https://jmeter.apache.org/
- **Maven** - https://maven.apache.org/
