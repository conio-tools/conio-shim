package org.conio.container.k8s;

// TODO
// https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/

import java.util.List;
import java.util.Map;

public class Container {
    private Map<String, String> env;
    private List<String> command;
    private String image;
    private String imagePullPolicy;
    private String name;
    private ResourceRequirements resources;

    /*public static class Builder {
        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Container build() {
            return new Container(this);
        }
    }

    private Container (Builder builder) {
    }*/

    public Container(Map<String, String> env, List<String> command, String image, String imagePullPolicy, String name, ResourceRequirements resources) {
        this.env = env;
        this.command = command;
        this.image = image;
        this.imagePullPolicy = imagePullPolicy;
        this.name = name;
        this.resources = resources;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public List<String> getCommand() {
        return command;
    }

    public String getImage() {
        return image;
    }

    public String getImagePullPolicy() {
        return imagePullPolicy;
    }

    public String getName() {
        return name;
    }

    public ResourceRequirements getResources() {
        return resources;
    }
}
