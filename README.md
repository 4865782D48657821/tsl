# Gateway Provider Directory Synchronizer

[![Build](https://github.com/4865782D48657821/tsl/actions/workflows/ci-release.yml/badge.svg)](https://github.com/4865782D48657821/tsl/actions/workflows/ci-release.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=4865782D48657821_tsl&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=4865782D48657821_tsl)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=4865782D48657821_tsl&metric=coverage)](https://sonarcloud.io/summary/new_code?id=4865782D48657821_tsl)
[![Latest Release](https://img.shields.io/github/v/release/4865782D48657821/tsl)](https://github.com/4865782D48657821/tsl/releases)

Spring Boot application for synchronizing gateway providers from a trusted list into the existing `ti_gateway_provider` table.

## What it does

- downloads the configured trusted list XML
- parses the document with a JAXB-based trusted-list adapter and filters `TSPService` entries with
  - `ServiceTypeIdentifier == http://uri.etsi.org/TrstSvc/Svctype/unspecified`
  - an `ExtensionValue == oid_tigw_zugm` on the same service
- extracts provider data and uses `service_name` as the business identity
- fetches provider metadata from `/.well-known/openid-configuration` for every current XML entry before any DB write
- writes inserts, updates, and deletions atomically with batch SQL

Missing providers are physically deleted during the same atomic synchronization transaction.

## Local development

Use the pinned toolchain first:

```bash
nix develop
gradle lint
gradle test
gradle check
```

The repository also contains a GitHub Actions pipeline whose `Build` job runs `lint -> test ->
package` sequentially in one runner. A GitHub release is created only for tag pushes such as
`v1.0.0`, and only after that build job has succeeded.

On pushes to `main`, the same workflow also generates JavaDoc and deploys it to GitHub Pages.
The test phase also produces JaCoCo coverage reports in the standard Gradle locations.

Start dependencies if you want a local MySQL:

```bash
docker compose up -d
```

Run the application:

```bash
gradle bootRun
```

## Docker Compose

Start MySQL and the application together:

```bash
docker compose up --build
```

Run only MySQL if you want to start the app locally with Gradle:

```bash
docker compose up -d mysql
```

Useful commands:

```bash
docker compose logs -f app
docker compose down
```

The compose setup uses the same environment variables as `application.yaml`. Override them in your shell or a `.env` file before starting compose if needed.

## Configuration

The application is configured through `application.yaml` and environment variables.

Important values:

- `TRUSTED_LIST_URL`: source trusted-list URL
- `GATEWAY_PROVIDER_SYNCHRONIZATION_CRON`: scheduler cron expression
- `GATEWAY_PROVIDER_SYNCHRONIZATION_ZONE`: scheduler timezone
- `GATEWAY_PROVIDER_SYNCHRONIZATION_CONNECT_TIMEOUT`: outbound connect timeout
- `GATEWAY_PROVIDER_SYNCHRONIZATION_READ_TIMEOUT`: outbound read timeout
- `GATEWAY_PROVIDER_METADATA_FETCH_CONCURRENCY`: parallel provider-metadata fetch limit
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: database connection

Default trusted-list source:

```text
http://download-testref.crl.ti-dienste.de/TSL-ECC-test/ECC-RSA_TSL-test.xml
```

## Testing

The test suite covers:

- unit tests for directory reconciliation, metadata enrichment orchestration, and scheduler delegation
- JAXB-based XML parsing and filtering tests using deterministic TSL fixtures
- HTTPS provider-metadata fetch tests using an in-memory TLS server and generated certificates
- JDBC batch writer tests covering insert, update, and delete semantics
- Checkstyle-based linting for main and test sources via `gradle lint`
- JaCoCo coverage reports in XML and HTML format

Current limitation:

- XML signature validation is currently not implemented.

Run all tests with:

```bash
gradle test
```

Generate the coverage reports explicitly with:

```bash
gradle jacocoTestReport
```

Run the full local verification path with:

```bash
gradle lint
gradle test
gradle jacocoTestReport
gradle bootJar
```

Generate JavaDoc locally with:

```bash
gradle javadoc
```

## Release

Create and push a version tag to trigger packaging and release publication:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Docs

- [Architecture Notes](docs/architecture.md)
