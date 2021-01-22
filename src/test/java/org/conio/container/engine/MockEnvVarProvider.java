package org.conio.container.engine;

import org.conio.container.engine.util.EnvVarProvider;

import java.util.HashMap;
import java.util.Map;

public class MockEnvVarProvider extends EnvVarProvider {
  private final Map<String, String> envs = new HashMap<>();

  public void put(String key, String value) {
    envs.put(key, value);
  }

  @Override
  public String get(String env) {
    return envs.get(env);
  }
}
