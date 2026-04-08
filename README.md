# LinPark Load Testing Project

A Java Maven multi-module project for load testing using **JMeter Java DSL**. This project provides a configuration-driven framework for defining test scenarios in YAML and executing them with parameterized load patterns.

## Features

- 🎯 **YAML-based scenario definitions** - Define workflows as data, not code
- ⚙️ **CLI parameter overrides** - Configure tests from command line without code changes
- 🔄 **Multi-scenario execution** - Run multiple workflows in a single test
- 📊 **JUnit integration** - Execute as standard JUnit tests, compatible with CI/CD
- 🚀 **JMeter Java DSL** - Programmatic fluent API for building load tests
- 📝 **Auto-discovery** - Add new scenarios by dropping YAML files

## Project Structure

```
linpark-demo/
├── pom.xml                                      # Parent POM (Java 21, dependencyManagement)
├── load-tests/                                  # Load testing module
│   ├── pom.xml                                  # Module POM
│   ├── src/test/java/
│   │   ├── LoadTest.java                        # Main JUnit test class
│   │   └── config/
│   │       ├── LoadTestConfig.java              # Runtime parameters POJO
│   │       ├── ScenarioDefinition.java          # Scenario workflow POJO
│   │       ├── Step.java                        # HTTP request step POJO
│   │       ├── Assertion.java                   # Response assertion POJO
│   │       └── ConfigLoader.java                # YAML loader + CLI override logic
│   ├── src/test/resources/
│   │   ├── log4j2.xml                           # Logging configuration
│   │   └── scenarios/
│   │       ├── scenarios.yaml                   # Default config & scenario list
│   │       ├── login-browse-checkout.yaml       # Example E2E scenario
│   │       └── api-read-only.yaml               # Example API scenario
├── spec.md                                      # Technical specification
├── README.md                                    # This file
└── .gitignore                                   # Git ignore rules
```

## Getting Started

### Prerequisites

- Java 21 or later
- Maven 3.6.0 or later

### Build

```bash
mvn clean install
```

### Run All Scenarios (with Default Configuration)

```bash
mvn test
```

This executes all scenarios defined in `load-tests/src/test/resources/scenarios/` with default parameters from `scenarios.yaml`:

- Virtual Users: 10
- Duration: 60 seconds
- Ramp-up: 5 seconds
- Target URL: http://httpbin.org
- Think Time: 500ms

## Configuration

All configuration parameters can be overridden via Maven properties. Configuration can be specified via:

1. Default values in `scenarios.yaml`
2. CLI parameter overrides (takes precedence)

### CLI Parameters

- **`-Dusers=N`** — Number of virtual users (default: from YAML)
- **`-Dduration=SECONDS`** — Test duration in seconds (default: from YAML)
- **`-DrampUp=SECONDS`** — Ramp-up time in seconds (default: from YAML)
- **`-DtargetUrl=URL`** — Base target URL (default: from YAML)
- **`-DthinkTime=MS`** — Think time between requests in milliseconds (default: from YAML)
- **`-Dscenario=NAME`** — Run only this scenario (optional; if not set, all scenarios run)

### Usage Examples

#### Run with custom user count and duration

```bash
mvn test -Dusers=50 -Dduration=120
```

#### Run with custom target URL

```bash
mvn test -DtargetUrl=http://localhost:8080 -Dusers=100
```

#### Run specific scenario only

```bash
mvn test -Dscenario=login-browse-checkout
```

#### All parameters overridden

```bash
mvn test \
  -Dusers=200 \
  -Dduration=300 \
  -DrampUp=30 \
  -DtargetUrl=http://myapp.local \
  -DthinkTime=1000
```

#### Run with verbose/debug output

```bash
mvn test -X
```

## Scenario Definition Format

Scenarios are defined in YAML files in the `load-tests/src/test/resources/scenarios/` directory.

### Basic Scenario Structure

```yaml
name: "Scenario Name"
steps:
  - method: "HTTP_METHOD"
    path: "/endpoint/path"
    body: "optional request body"
    headers:
      Header-Name: "header value"
    assertions:
      - statusCode: 200
      # Additional assertions can be added
```

### Example: E2E Workflow (login-browse-checkout.yaml)

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

### Example: API Read-Only Scenario (api-read-only.yaml)

```yaml
name: "API Read-Only"
steps:
  - method: "GET"
    path: "/api/users"
    assertions:
      - statusCode: 200

  - method: "GET"
    path: "/api/products"
    assertions:
      - statusCode: 200

  - method: "GET"
    path: "/api/orders"
    assertions:
      - statusCode: 200
```

### Default Configuration (scenarios.yaml)

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

## Adding New Scenarios

1. Create a new YAML file in `load-tests/src/test/resources/scenarios/`
2. Define scenario structure following the format above
3. (Optional) Add scenario name to `scenarios.yaml` for documentation
4. Run tests - the new scenario will be auto-discovered and loaded

**No code changes required!**

## Test Execution

### How It Works

1. **ConfigLoader** loads `scenarios.yaml` to get default configuration
2. **CLI parameters** override default configuration (if provided)
3. **All YAML scenario files** are auto-discovered and loaded
4. **Scenario filter** applied if `-Dscenario=name` is specified
5. **JMeter test plan** is dynamically built from scenario definitions
6. **Thread group** is configured with virtual users, duration, ramp-up
7. **HTTP samplers** are created for each step with assertions
8. **Think time** is added between requests
9. **Test plan executed** and results reported via JUnit

### Test Output

Tests are executed via Maven Surefire plugin. Results are available in:

- **Console output** - Real-time test progress and metrics
- **target/surefire-reports/** - JUnit XML reports
- **target/load-tests.log** - Detailed execution logs

## Configuration Classes

### LoadTestConfig

Runtime parameters for the load test (virtual users, duration, ramp-up, target URL, think time).

### ScenarioDefinition

Represents a complete test scenario (workflow) with a name and list of steps.

### Step

Represents an individual HTTP request step (method, path, body, headers, assertions).

### Assertion

Represents a response validation (status code, regex pattern, body content assertion).

### ConfigLoader

Utility class that:

- Loads scenario definitions from YAML files
- Loads default configuration from `scenarios.yaml`
- Applies CLI parameter overrides via `System.getProperty()`
- Supports scenario filtering by name
- Auto-discovers YAML files in scenarios directory

## Logging

Logging is configured via `log4j2.xml`:

- **Console output** - INFO level logs for monitoring
- **File output** - DEBUG level logs to `target/load-tests.log`
- **Component-specific levels** - Higher verbosity for application code, standard for JMeter

Adjust logging levels in `log4j2.xml` as needed:

```xml
<Logger name="com.linpark" level="DEBUG" />
```

## Dependencies

### Core JMeter Dependencies

- **jmeter-java-dsl** - Fluent API for building JMeter tests
- **ApacheJMeter_core** - Core JMeter engine

### Test & HTTP

- **JUnit 5** - Test framework
- **Apache HttpClient 5** - HTTP client (native to JMeter DSL)

### Configuration & Logging

- **Jackson (YAML)** - YAML parsing and object mapping
- **SLF4J** - Logging facade
- **Log4j2** - High-performance logging implementation

### Development

- **Lombok** - Reduce boilerplate in POJOs (annotations for getters, setters, constructors)

## Verification Checklist

- [x] Build: `mvn clean install` completes without errors
- [x] Execute: `mvn test` runs all scenarios successfully
- [x] CLI override: `mvn test -Dusers=50` applies parameter correctly
- [x] Scenario filter: `mvn test -Dscenario=api-read-only` runs only that scenario
- [x] Custom URL: `mvn test -DtargetUrl=http://localhost:8080` connects to custom endpoint
- [x] Results: `target/surefire-reports/` contains JUnit results
- [x] Logs: `target/load-tests.log` contains detailed execution logs
- [x] Extensibility: Add new YAML scenario and verify auto-loading

## Future Enhancements

- Conditional logic in scenarios (if-then-else workflows)
- Detailed metrics export (CSV, JMeter JMX format)
- Result aggregation and reporting dashboard
- Performance assertions (e.g., 99th percentile response time)
- Custom headers/correlation IDs injection
- Authentication support (Basic, OAuth, etc.)
- Data parameterization (CSV files, functions)
- Distributed load testing (multiple JMeter nodes)

## Troubleshooting

### Build Fails: Missing Dependencies

Ensure you're using Java 21+:

```bash
javac -version  # Should show version 21 or later
maven --version # Should show 3.6.0+
```

### Tests Don't Find Scenarios

Verify scenario YAML files are in `load-tests/src/test/resources/scenarios/` and are valid YAML format.

### Configuration Not Overriding

Ensure parameter names match exactly (case-sensitive):

```bash
mvn test -Dusers=100  # Correct
mvn test -DUsers=100  # Wrong - will be ignored
```

### Connection Refused

Check the `-DtargetUrl` parameter points to a running service:

```bash
mvn test -DtargetUrl=http://localhost:8080
```

## Contributing

To add new test scenarios:

1. Create scenario YAML file in `load-tests/src/test/resources/scenarios/`
2. Follow the YAML format from existing examples
3. Run `mvn test` to automatically discover and execute
4. (Optional) Document scenario in `scenarios.yaml`

## License

This project is part of the LinPark initiative.

## Support

For issues, questions, or suggestions, please refer to the specification in `spec.md`.
