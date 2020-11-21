package org.conio.container.k8s;

import java.util.Map;

public class ResourceRequirements {
    private Map<String, String> limits;
    private Map<String, String> requests;

    public ResourceRequirements() {
    }

    public Map<String, String> getLimits() {
        return limits;
    }

    public void setLimits(Map<String, String> limits) {
        this.limits = limits;
    }

    public Map<String, String> getRequests() {
        return requests;
    }

    public void setRequests(Map<String, String> requests) {
        this.requests = requests;
    }
}
