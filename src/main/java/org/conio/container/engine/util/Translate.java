package org.conio.container.engine.util;

import java.util.Map;
import org.apache.hadoop.yarn.api.records.Resource;
import org.conio.container.k8s.Pod;
import org.conio.container.k8s.ResourceRequirements;

public class Translate {
  private Translate() {
  }

  /**
   * Translates a Kubernetes ResourceRequirements to a Hadoop Resource object.
   * Currently only supports cpu and memory resources.
   */
  public static Resource translateResourceRequirements(Pod pod) {
    ResourceRequirements res = pod.getSpec().getContainers().get(0).getResources();
    Map<String, String> limits = res.getLimits();
    String cpu = limits.get("cpu");
    String memory = limits.get("memory");
    return Resource.newInstance(translateMemory(memory), translateCpu(cpu));
  }

  private static int translateCpu(String cpu) {
    cpu = cpu.trim();
    try {
      return Integer.parseInt(cpu);
    } catch (NumberFormatException nfe) {
      double cpuDouble = (Integer.parseInt(cpu.replace("m", "")) * 1.0f) / 1000;
      return (int)Math.ceil(cpuDouble);
    }
  }

  private static int translateMemory(String memory) {
    memory = memory.trim();
    try {
      return Integer.parseInt(memory);
    } catch (NumberFormatException nfe) {
      if (memory.contains("Mi")) {
        return Integer.parseInt(memory.replace("Mi", ""));
      } else if (memory.contains("Gi")) {
        return Integer.parseInt(memory.replace("Gi", "")) * 1024;
      } else {
        throw new IllegalArgumentException(String.format("Not parsable memory value: %s", memory));
      }
    }
  }
}
