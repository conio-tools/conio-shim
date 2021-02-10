package org.conio.container.k8s;

// TODO
// https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/

import java.util.List;

public class Container {
  private List<EnvVar> env;
  private List<String> command;
  private String image;
  private String imagePullPolicy;
  private String name;
  private ResourceRequirements resources;

  public Container() {
  }

  public List<EnvVar> getEnv() {
    return env;
  }

  public void setEnv(List<EnvVar> env) {
    this.env = env;
  }

  public List<String> getCommand() {
    return command;
  }

  public void setCommand(List<String> command) {
    this.command = command;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public String getImagePullPolicy() {
    return imagePullPolicy;
  }

  public void setImagePullPolicy(String imagePullPolicy) {
    this.imagePullPolicy = imagePullPolicy;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ResourceRequirements getResources() {
    return resources;
  }

  public void setResources(ResourceRequirements resources) {
    this.resources = resources;
  }
}
