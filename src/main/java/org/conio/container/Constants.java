package org.conio.container;

public class Constants {
  public static final String DEFAULT_QUEUE_NAME = "root.default";
  public static final String APP_NAME = "conio";
  public static final String TYPE_POD = "pod";
  public static final String APP_MASTER_JAR = "AppMaster.jar";
  public static final int DEFAULT_AM_MEMORY = 100;
  public static final String CONIO_HDFS_ROOT = "/conio";

  // environment variables
  public static final String ENV_YAML_HDFS_PATH = "CONIO_YAML_HDFS_PATH";

  private Constants() {
  }
}
