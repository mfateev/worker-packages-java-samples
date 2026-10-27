package example.worker;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.packages.runtime.WorkerPackagePlugin;
import java.util.UUID;

/**
 * Starts the greeting workflow with an optional version override, without loading package files.
 */
public class DoGreetingStarter {
  public static void main(String[] args) throws Exception {
    if (args.length > 1) throw new IllegalArgumentException("Usage: DoGreetingStarter [version]");

    ClientConfigProfile profile = ClientConfigProfile.load();
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    try {
      WorkflowClientOptions.Builder clientOptions = profile.toWorkflowClientOptions().toBuilder();
      if (args.length == 1)
        clientOptions.setPlugins(
            WorkerPackagePlugin.newBuilder().setClientVersion(args[0]).build());
      WorkflowClient client = WorkflowClient.newInstance(service, clientOptions.build());
      WorkflowStub greeting =
          client.newUntypedWorkflowStub(
              "GreetingWorkflow",
              WorkflowOptions.newBuilder()
                  .setWorkflowId("greeting-" + UUID.randomUUID())
                  .setTaskQueue(
                      System.getenv().getOrDefault("TEMPORAL_TASK_QUEUE", "workflow-packages"))
                  .build());
      greeting.start();
      greeting.signal("proceed");
      System.out.println(greeting.getResult(String.class));
    } finally {
      service.shutdown();
    }
  }
}
