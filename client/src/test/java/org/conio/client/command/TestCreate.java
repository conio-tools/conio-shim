package org.conio.client.command;

import java.util.Map;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.conio.client.zookeeper.ClientWrapperWithOptions;
import org.conio.client.zookeeper.MockClientWrapperWithOptions;
import org.junit.Test;

public class TestCreate {
  @Test(timeout = 1000)
  public void test() throws Exception {
    MockYarnClient yarnClient = new MockYarnClient();
    MockCreate create = new MockCreate(yarnClient);
    String[] args = {"--yaml", "src/test/resources/pod.yaml"};

    create.init(args);
    create.run();
    create.cleanup();

    ApplicationSubmissionContext context = yarnClient.getLatestApplication();
  }

  private static class MockCreate extends Create {
    private final YarnClient client;

    MockCreate(YarnClient client) {
      this.client = client;
    }

    @Override
    YarnClient createYarnClient() {
      return client;
    }

    @Override
    ClientWrapperWithOptions createClientWrapper() {
      return new MockClientWrapperWithOptions();
    }

    @Override
    void addToLocalResources(
        FileSystem fs, String fileSrcPath, String fileDstPath,
        String appId, Map<String, LocalResource> localResources, String resources) {
    }
  }
}
