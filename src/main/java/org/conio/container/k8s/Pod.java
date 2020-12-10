package org.conio.container.k8s;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.yaml.snakeyaml.Yaml;

public class Pod {
  private String apiVersion;
  private String kind;
  private Metadata metadata;
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

  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  /**
   * Returns the metadata of the pod.
   * Throws RuntimeException if the metadata is null.
   */
  public Metadata getMetadata() {
    if (metadata == null) {
      throw new RuntimeException("Metadata field is null");
    }
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  public PodSpec getSpec() {
    return spec;
  }

  public void setSpec(PodSpec spec) {
    this.spec = spec;
  }
}
