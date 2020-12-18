package org.conio.container.engine.util;

import org.apache.hadoop.yarn.api.ApplicationConstants;

public class Util {
  private Util() {
  }

  /**
   * Retrieves an environment variable from the system.
   * Throws RuntimeException if not found.
   */
  public static String secureGetEnv(String env) {
    String value = System.getenv(env);
    if (value.isEmpty()) {
      throw new RuntimeException(String.format("Expected %s environment variable not found", env));
    }
    return value;
  }
}
