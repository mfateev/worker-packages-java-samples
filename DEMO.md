# Interactive upgrade demo

A three-terminal walkthrough of the restart demo using the sample worker and the [Temporal CLI](https://docs.temporal.io/cli). Workflow A starts on version 1 and blocks; the worker is replaced with one that also has version 2; workflow B starts on version 2. Both complete on the version they started with.

Prerequisites: build the samples as described in the [README](README.md) and install the `temporal` CLI. Run every command from this repository's root.

```bash
./gradlew packageExamples :worker:installDist
```

Use `temporal server start-dev` rather than the library's bundled `server` command. The bundled Java test service never expires sticky task queues, so a workflow that was running before the worker restart would not resume.

The sample worker connects to `127.0.0.1:7233`, namespace `default`, polls task queue `workflow-packages` and caches packages in `.package-cache`. Its only argument is the package directory. `TEMPORAL_PACKAGE_VERSION` sets the current version for new workflows; it may be omitted while only one version is installed.

## 1. Start the service

Terminal 1:

```bash
temporal server start-dev
```

The Web UI is at http://localhost:8233.

## 2. Deploy version 1

Terminal 2:

```bash
rm -rf build/demo-packages .package-cache && mkdir -p build/demo-packages
cp build/examples/greetings-1.twp build/demo-packages/
worker/build/install/worker/bin/worker build/demo-packages
```

Startup prints `version=1 (current)` and its exported types.

## 3. Start workflow A

Terminal 3:

```bash
temporal workflow start --type GreetingWorkflow --task-queue workflow-packages --workflow-id order-A
```

A runs on version 1 and waits for the `proceed` signal.

## 4. Upgrade the worker

In terminal 2, stop the worker with Ctrl-C, then add version 2 and restart with it as the current version:

```bash
cp build/examples/greetings-2.twp build/demo-packages/
TEMPORAL_PACKAGE_VERSION=2 worker/build/install/worker/bin/worker build/demo-packages
```

Startup lists both versions, marks version 2 `(current)` and shows its added `GetName` activity.

## 5. Start workflow B and release both

Terminal 3:

```bash
temporal workflow start --type GreetingWorkflow --task-queue workflow-packages --workflow-id order-B

temporal workflow signal --workflow-id order-A --name proceed
temporal workflow signal --workflow-id order-B --name proceed

temporal workflow result --workflow-id order-A    # "Hello, World!"
temporal workflow result --workflow-id order-B    # "Hi, multi-versioned worker!"
```

A finishes on version 1 although the worker's current version is now 2. `temporal workflow show --workflow-id order-B` or the Web UI shows the extra `GetName` activity that only version 2 calls.

## 6. Request a version from a client

Terminal 3:

```bash
./gradlew :worker:greetingStarter --args="1"   # Hello, World!
./gradlew :worker:greetingStarter              # Hi, multi-versioned worker!
```

The starter sends a version header when given an argument; otherwise the worker assigns its current version. See [Start a greeting from Java](README.md#start-a-greeting-from-java).

## Clean up

Stop terminals 1 and 2 with Ctrl-C, then:

```bash
rm -rf build/demo-packages .package-cache
```

Restarting `start-dev` discards all workflows. To repeat the demo against the same service, use new workflow IDs.
