# Contributing

Build on JDK 21 and keep sample application and test code compatible with Java 8. The library and SDK are sibling checkouts by default; use `WORKER_PACKAGES_DIR`/`-PworkerPackagesDir` and `TEMPORAL_SDK_DIR`/`-PsdkDir` with absolute paths to override them. This repository has its own Git history and build; it is not a subproject of the library.

```bash
../worker-packages-java/scripts/bootstrap-sdk.sh
./gradlew build installCli
./gradlew test -PtestJavaVersion=8
./gradlew demo -PtestJavaVersion=8
./scripts/validate-cli.py
```

Use JUnit 4 for the two runnable-example integration tests. Keep application classes in their package archives rather than the host test classpath. Preserve the demo's durable wait and explicit version policy so old/new version behavior is reproducible. Keep broader isolation, archive-validation and lifecycle stress cases in the library's test suite.

The CLI smoke script uses the installed library CLI and writes only ignored build evidence. Run it with `JAVA_HOME` pointing to the desired runtime. No remote publishing or CI credentials are required for this local composite build. Do not add SDK source pins or embed SDK revision metadata in packages.
