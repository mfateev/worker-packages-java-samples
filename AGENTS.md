# Samples contributor rules

Follow [CONTRIBUTING.md](CONTRIBUTING.md) and the library's Java 8 conventions.

- Keep runnable applications and their tests in this repository; the library must build without it.
- Use Java 8 source and APIs, JUnit 4 tests, complete comments and public SDK APIs.
- Build with JDK 21 because SDK main includes multi-release classes.
- Resolve the library and SDK through local Gradle composites; do not introduce SDK SHA pins or runtime revision checks.
- Keep the two greeting versions simple and preserve their intentionally identical Java class names.
- Update the README for launch/configuration changes. Record actual validation commands and results.
