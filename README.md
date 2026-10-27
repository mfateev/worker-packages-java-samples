# Temporal Worker Packages Java samples

Runnable greeting packages, a worker host, integration tests and a restart demonstration for [Temporal Worker Packages](https://github.com/mfateev/worker-packages-java/blob/main/README.md). This is a separate Git repository; the library builds and tests independently of it.

Use this default workspace layout:

```text
workspace/
  sdk-java/
  worker-packages-java/
  worker-packages-java-samples/    run the commands below here
```

Build with JDK 21. Sample application code and tests target Java 8; installed workers and the CLI run on Java 8 or later. Prepare SDK main through the library's bootstrap helper, then build the samples and install the library CLI:

```bash
../worker-packages-java/scripts/bootstrap-sdk.sh
./gradlew build installCli
```

The samples' settings include the local library, its packaging Gradle plugin and SDK main as composite builds. Override the library checkout with `-PworkerPackagesDir=/absolute/path/to/worker-packages-java` or `WORKER_PACKAGES_DIR`; override the SDK with `-PsdkDir=/absolute/path/to/sdk-java` or `TEMPORAL_SDK_DIR`. Use absolute paths for overrides. Set `WORKER_PACKAGES_DIR` when using the helper scripts with a nondefault library location; they do not read Gradle `-P` properties.

```bash
WORKER_PACKAGES_DIR=/path/to/worker-packages-java \
  TEMPORAL_SDK_DIR=/path/to/sdk-java ./gradlew build installCli
```

## Evolving an activity API

Each application version has one workflow, one activity interface and their implementations. Both versions deliberately use identical Java package and class names.

| | Version 1 | Version 2 |
| --- | --- | --- |
| Workflow | `GreetingWorkflow.run()` | Same interface |
| Original activity | `greet(String name)` | `greet(String greeting, String name)` |
| Added activity | — | `getName()` |
| Result | `Hello, World!` | `Hi, multi-versioned worker!` |

Version 1 waits for `proceed()` before calling `greet("World")`. Version 2 first calls `getName()`, waits for the same signal, then calls `greet("Hi", name)`. The activity type remains `Greet` across the signature change; `GetName` is exported only by version 2. See [v1](greeting-v1/src/main/java/example/greeting/GreetingWorkflowImpl.java) and [v2](greeting-v2/src/main/java/example/greeting/GreetingWorkflowImpl.java).

Run the restart demo:

```bash
./scripts/demo.sh
# Or, once the SDK is prepared:
./gradlew demo
```

It starts a local Temporal test service, installs v1 and starts workflow A, observes its committed history, adds v2 and replaces the worker JVM. It then starts B, signals both runs and verifies A returns `Hello, World!` while B returns `Hi, multi-versioned worker!`. Histories and subprocess logs are saved under `build/restart-demo-*`. The samples need no readiness query method.

## Build the workflow packages

Each greeting application applies the library's `io.temporal.worker-packages` Gradle plugin. Its `workerPackage` task produces a validated `.twp` and participates in `assemble` and `build`.

Version 1 declares its exports in [greeting-v1/build.gradle](greeting-v1/build.gradle):

```groovy
plugins { id 'io.temporal.worker-packages' }
version = '1'

workerPackage {
    packageId = 'greetings'
    workflows = ['GreetingWorkflow': 'example.greeting.GreetingWorkflowImpl']
    activities = ['Greet': 'example.greeting.GreetingActivitiesImpl']
}
```

Version 2 sets `version = '2'` and adds `GetName` to its activities map. The package version defaults to the project's version as a string; Git SHAs and semantic versions are supported. Set `workerPackage.packageVersion` explicitly to override it. Versions have no implicit deployment ordering. Archives may share a version when their workflow/activity types do not overlap.

For a package version based on the application's Git commit:

```groovy
version = providers.exec {
    commandLine 'git', 'rev-parse', 'HEAD'
}.standardOutput.asText.get().trim()
```

These two variants intentionally retain versions `1` and `2` because they share this repository and Git SHA.

```bash
./gradlew :greeting-v1:build
./gradlew :greeting-v2:build
./gradlew packageExamples
```

The application archives are `greeting-v1/build/libs/greetings-1.twp` and `greeting-v2/build/libs/greetings-2.twp`. `packageExamples` collects them into `build/examples/`. Each contains a manifest and application JAR under `app/`; runtime dependencies are bundled under `lib/`, and `compileOnly` dependencies such as the host-provided SDK are excluded. Packages and workers do not embed or compare SDK revisions.

For a replay-compatible patch, set `workerPackage.patchesVersion` to the target and choose a distinct package version. The output is `greetings-<target>-<patch>.twp`. These greeting variants change activity signatures and demonstrate independent versions rather than patches of each other. See the library's [patch contract](https://github.com/mfateev/worker-packages-java/blob/main/README.md#patched-packages).

## Worker implementation

[worker/PackageWorker.java](worker/src/main/java/example/worker/PackageWorker.java) builds `WorkerPackagePlugin` with `setPackagesDirectory` and `setCacheDirectory`, installs it through `WorkerFactoryOptions.setPlugins`, creates a worker and starts polling. Its single command-line argument is the package directory.

```bash
./gradlew packageExamples :worker:installDist
TEMPORAL_PACKAGE_VERSION=2 worker/build/install/worker/bin/worker build/examples
```

The worker uses `ClientConfigProfile.load()` from SDK main's environment-config module to configure service stubs and the client. With no connection configuration, it connects to `127.0.0.1:7233`, namespace `default`. Use `TEMPORAL_ADDRESS`, `TEMPORAL_NAMESPACE`, `TEMPORAL_PROFILE` and `TEMPORAL_CONFIG_FILE` for standard Temporal configuration:

```bash
TEMPORAL_ADDRESS=127.0.0.1:7233 TEMPORAL_NAMESPACE=default \
  TEMPORAL_PACKAGE_VERSION=2 worker/build/install/worker/bin/worker build/examples
```

`TEMPORAL_PACKAGE_VERSION` configures the worker’s current version; use `2` when both packages are installed. Without a selector, selection succeeds only with one installed version. The task queue defaults to `workflow-packages` and the cache to `.package-cache`; override them with `TEMPORAL_TASK_QUEUE` and `TEMPORAL_PACKAGE_CACHE_DIR`.

Startup prints the selector policy and exported types for each version. The sample leaves the worker polling and can be stopped with Ctrl-C. When an embedding host calls `factory.shutdown()`, the plugin closes package loaders after actual termination. Copy the worker distribution's entire `bin/` and `lib/` directories plus the archives to deploy elsewhere.

## Start a greeting from Java

[DoGreetingStarter.java](worker/src/main/java/example/worker/DoGreetingStarter.java) uses the same `ClientConfigProfile` connection setup as the worker. With a version argument, it configures `WorkerPackagePlugin.newBuilder().setClientVersion(args[0]).build()` through `WorkflowClientOptions.setPlugins`, starts `GreetingWorkflow`, sends `proceed`, and prints the result. The requested version overrides the worker’s current version. With no arguments, it uses a plain SDK client with no package plugin and no version header; the worker assigns its current version. The client needs no package files or package ID.

With the Temporal service running, start a worker with both archives and V2 as its current version:

```bash
./gradlew packageExamples :worker:installDist
TEMPORAL_PACKAGE_VERSION=2 worker/build/install/worker/bin/worker build/examples
```

In another terminal, run the starter with the worker’s current version or request V2 explicitly:

```bash
./gradlew :worker:greetingStarter
./gradlew :worker:greetingStarter --args="2"
```

Or run directly with Java 8 or later, using the installed worker distribution:

```bash
java -cp 'worker/build/install/worker/lib/*' example.worker.DoGreetingStarter
java -cp 'worker/build/install/worker/lib/*' example.worker.DoGreetingStarter 2
```

Version `2` prints `Hi, multi-versioned worker!`, regardless of the worker’s current version. Set `TEMPORAL_TASK_QUEUE` and the standard Temporal connection variables on both processes when using a different queue or service. Pass `1` to run V1 and print `Hello, World!`. Each invocation creates a new workflow ID.

## Run the library CLI directly

The CLI belongs to the library repository. From this samples root, install it through the composite build:

```bash
./gradlew build installCli
```

All commands below use the default sibling library path and require only Java 8 or later at runtime. Adjust that path if using a different checkout. In one terminal, use an existing Temporal service or start the bundled ephemeral test service:

```bash
../worker-packages-java/build/install/temporal-worker-packages/bin/temporal-worker-packages server --port 7233
```

Keep the service running. In another terminal, start the CLI worker (or use the sample worker above):

```bash
../worker-packages-java/build/install/temporal-worker-packages/bin/temporal-worker-packages worker \
  --packages build/examples --package-version 2 --cache .package-cache \
  --endpoint 127.0.0.1:7233 --namespace default --task-queue workflow-packages
```

In a third terminal, start and release a workflow:

```bash
../worker-packages-java/build/install/temporal-worker-packages/bin/temporal-worker-packages start \
  --type GreetingWorkflow --workflow-id greeting-direct-1 --input '[]' \
  --endpoint 127.0.0.1:7233 --namespace default --task-queue workflow-packages
../worker-packages-java/build/install/temporal-worker-packages/bin/temporal-worker-packages signal \
  --workflow-id greeting-direct-1 --name proceed --endpoint 127.0.0.1:7233 --namespace default
../worker-packages-java/build/install/temporal-worker-packages/bin/temporal-worker-packages result \
  --workflow-id greeting-direct-1 --timeout-seconds 60 --endpoint 127.0.0.1:7233 --namespace default
```

The result is `"Hi, multi-versioned worker!"`. Use a new workflow ID for another run. Select `--package-version 1` to get `"Hello, World!"`. Restart workers after changing installed archives. For another machine, copy the library's full `build/install/temporal-worker-packages/` distribution and the `.twp` files; neither Gradle nor source checkouts are needed at runtime. The bundled test service does not persist histories across service restarts.

## Validate the samples

```bash
./gradlew build installCli
./gradlew test -PtestJavaVersion=8
./gradlew demo -PtestJavaVersion=8
./scripts/validate-cli.py
```

The two integration tests under `src/test/java/example/samples` cover greeting version isolation/replay and the actual worker process with profile configuration. The demo replaces worker JVMs against a persistent test-service process. The CLI smoke check uses the library's installed launcher and writes logs under this repository's `build/verification/cli-smoke/`; it also exercises patch-only chain deployment. Set `JAVA_HOME` to Java 8 to run that script against Java 8. Library unit, runtime, stress-fixture and Gradle plugin tests remain in the separate library repository.

Clients need only the workflow type and task queue. Without a version header, the worker assigns `TEMPORAL_PACKAGE_VERSION`. To request a version explicitly, use the CLI start option `--package-version VERSION` or Java `WorkerPackagePlugin.newBuilder().setClientVersion("VERSION").build()`; clients need no package files or package ID.
