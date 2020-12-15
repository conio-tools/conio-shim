package org.conio.container.engine.util;

import org.apache.hadoop.yarn.api.ApplicationConstants;

public class Util {
  private Util() {
  }

  public static String secureGetEnv(String env) {
    String value = System.getenv(ApplicationConstants.Environment.LOCAL_DIRS.key());
    if (value.isEmpty()) {
      throw new RuntimeException(String.format("Expected %s environment variable not found", env));
    }
    return value;
  }
}
