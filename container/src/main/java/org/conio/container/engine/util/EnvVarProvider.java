package org.conio.container.engine.util;

public class EnvVarProvider {
  /**
   * Retrieves an environment variable from the system.
   * Throws RuntimeException if not found.
   */
  public String get(String env) {
    String value = System.getenv(env);
    if (value.isEmpty()) {
      throw new RuntimeException(String.format("Expected %s environment variable not found", env));
    }
    return value;
  }
}
