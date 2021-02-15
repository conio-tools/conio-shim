package org.conio.client.zookeeper;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;

public class MockClientWrapperWithOptions extends ClientWrapperWithOptions {
  @Override
  public void init(CommandLine cliParser) {
  }

  @Override
  public void start() {
  }

  @Override
  public byte[] downloadPod(String namespace, String name) throws Exception {
    File file = new File("src/test/resources/sleep_pod.yaml");
    return FileUtils.readFileToByteArray(file);
  }

  @Override
  public boolean zkPathExists(String path) {
    return true;
  }

  @Override
  public String getZkPathData(String path) {
    return "";
  }

  @Override
  public void uploadPod(String namespace, String name, String yamlFile) {
  }

  @Override
  public void close() {
  }
}
