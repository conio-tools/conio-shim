package org.conio.container.zookeeper;

import static org.conio.container.Constants.DEFAULT_ZK_ROOT_NODE;
import static org.conio.container.Constants.POD_ZK_PATH_TEMPLATE;

import java.io.File;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;

/**
 * A Wrapper class around the Zookeeper client.
 * Each node that is created/read through this wrapper class is done
 * under the root node given as parameter in construction time.
 */
public class ClientWrapper {
  private final CuratorFramework zkClient;
  private final String zkRoot;

  /**
   * Creates a ClientWrapper from the ZK connection string and the root znode.
   */
  public ClientWrapper(String zkConnectionString, String zkRoot) {
    RetryPolicy retryPolicy = new RetryNTimes(10, 1000);
    zkClient = CuratorFrameworkFactory.newClient(zkConnectionString, retryPolicy);
    this.zkRoot = zkRoot == null ? DEFAULT_ZK_ROOT_NODE : zkRoot;
  }

  private String prefixPath(String path) {
    return zkRoot + path;
  }

  /**
   * Starts the client and creates the root znode.
   */
  public void start() throws Exception {
    zkClient.start();
    if (!zkPathExists(zkRoot)) {
      createZkPath(zkRoot);
    }
  }

  public void close() {
    zkClient.close();
  }

  // low level calls

  public void createZkPath(String path) throws Exception {
    zkClient.create().forPath(prefixPath(path));
  }

  public void createZkPath(String path, String data) throws Exception {
    zkClient.create().forPath(prefixPath(path), data.getBytes());
  }

  public boolean zkPathExists(String path) throws Exception {
    return zkClient.checkExists().forPath(prefixPath(path)) != null;
  }

  private byte[] getZkPathDataAsBytes(String path) throws Exception {
    return zkClient.getData().forPath(prefixPath(path));
  }

  public String getZkPathData(String path) throws Exception {
    return new String(getZkPathDataAsBytes(path));
  }

  // high level calls

  /**
   * Saves the pod as bytes in Zookeeper.
   */
  public void uploadPod(String namespace, String name, String yamlFile) throws Exception {
    String yamlString =
        FileUtils.readFileToString(new File(yamlFile), Charset.defaultCharset());
    createZkPath(buildPodZkPath(namespace, name), yamlString);
  }

  public byte[] downloadPod(String namespace, String name) throws Exception {
    return getZkPathDataAsBytes(buildPodZkPath(namespace, name));
  }

  private String buildPodZkPath(String namespace, String name) {
    return String.format(POD_ZK_PATH_TEMPLATE, namespace, name);
  }
}
