package org.conio.container.k8s;

// TODO
// https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/

import java.util.Map;

public class Container {
    private Map<String, String> env;
    private String image;
    private String imagePullPolicy;
    private String name;
    private ResourceRequirements resources;

    public static class Builder {
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
    }
}
