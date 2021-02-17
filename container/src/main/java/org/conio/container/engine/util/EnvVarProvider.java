package org.conio.container.engine.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvVarProvider {
  private static final Logger LOG = LoggerFactory.getLogger(EnvVarProvider.class);

  /**
   * Retrieves an environment variable from the system.
   * Throws RuntimeException if not found.
   */
  public String get(String env) {
    String value = System.getenv(env);
    if (value == null || value.isEmpty()) {
      LOG.info("Env variable {} is not found", env);
      throw new RuntimeException(String.format("Expected %s environment variable not found", env));
    }
    return value;
  }
}
