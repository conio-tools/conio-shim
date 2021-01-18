package org.conio.container;

public class Constants {
  public static final String DEFAULT_QUEUE_NAME = "root.default";
  public static final String DEFAULT_ZK_ROOT_NODE = "/conio";

  public static final String APP_NAME = "conio";
  public static final String TYPE_POD = "pod";
  public static final String APP_MASTER_JAR = "AppMaster.jar";
  public static final int DEFAULT_AM_MEMORY = 100;
  public static final String POD_ZK_PATH_TEMPLATE = "/namespace/%s/pod/%s";

  public static final String ENV_ZK_ADDRESS = "CONIO_ZK_ADDRESS";
  public static final String ENV_ZK_ROOT_NODE = "CONIO_ZK_ROOT_NODE";
  public static final String ENV_NAMESPACE = "CONIO_NAMESPACE";
  public static final String ENV_POD_NAME = "CONIO_POD_NAME";

  private Constants() {
  }
}
