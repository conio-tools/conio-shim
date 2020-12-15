package org.conio.container.k8s;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.ImmutableList;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class TestPod {
  @Test
  public void testParseFromFile() throws FileNotFoundException {
    Pod pod = Pod.parseFromFile("src/test/resources/sleep_pod.yaml");

    testMetadata(pod.getMetadata());

    // pod
    PodSpec podSpec = pod.getSpec();
    assertEquals("Never", podSpec.getRestartPolicy());

    // container
    assertEquals(1, podSpec.getContainers().size());
    testContainer(podSpec.getContainers().get(0));
  }

  private void testMetadata(Metadata meta) {
    assertNull(meta.getAnnotations());
    assertEquals("sleep-pod", meta.getName());
    assertNull(meta.getNamespace());
    assertEquals("default", meta.getExactNamespace());
    Map<String, String> labels = meta.getLabels();
    assertEquals("sleep", labels.get("app"));
  }

  private void testContainer(Container container) {
    testEnvVariables(container.getEnv());
    assertEquals(ImmutableList.of("sleep", "60"), container.getCommand());
    assertEquals("busybox", container.getImage());
    assertEquals("IfNotPresent", container.getImagePullPolicy());
    assertEquals("sleep-container", container.getName());

    testResources(container.getResources());
  }

  private void testEnvVariables(List<EnvVar> envVars) {
    assertNotNull(envVars);
    assertEquals(2, envVars.size());
    EnvVar envVar1 = envVars.get(0);
    assertEquals("SOME_KEY", envVar1.getName());
    assertEquals("some value", envVar1.getValue());
    EnvVar envVar2 = envVars.get(1);
    assertEquals("OTHER_KEY", envVar2.getName());
    assertEquals("some other value", envVar2.getValue());
  }

  private void testResources(ResourceRequirements resources) {
    Map<String, String> limits = new HashMap<>();
    limits.put("cpu", "1");
    limits.put("memory", "200Mi");
    Map<String, String> requests = new HashMap<>();
    requests.put("cpu", "500m");
    requests.put("memory", "100Mi");
    assertEquals(resources.getLimits(), limits);
    assertEquals(resources.getRequests(), requests);
  }
}
