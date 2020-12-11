package org.conio.container.k8s;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.yaml.snakeyaml.Yaml;

public class Pod extends Object {
  private PodSpec spec;

  public Pod() {
  }

  /**
   * Parses the input string as file and loads it as a yaml file.
   */
  public static Pod parseFromFile(String yamlFile) throws FileNotFoundException {
    if (yamlFile == null || yamlFile.isEmpty()) {
      throw new IllegalArgumentException("Empty yaml file provided as input");
    }
    File file = new File(yamlFile);
    if (!file.exists()) {
      throw new IllegalArgumentException("Yaml file does not exist");
    }

    Yaml yaml = new Yaml();
    InputStream inputStream = new FileInputStream(yamlFile);

    return yaml.loadAs(inputStream, Pod.class);
  }

  public PodSpec getSpec() {
    return spec;
  }

  public void setSpec(PodSpec spec) {
    this.spec = spec;
  }
}
