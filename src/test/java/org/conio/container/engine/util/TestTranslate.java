package org.conio.container.engine.util;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import java.io.FileNotFoundException;
import java.util.Map;
import org.apache.hadoop.yarn.api.records.Resource;
import org.conio.container.k8s.Container;
import org.conio.container.k8s.Pod;
import org.conio.container.k8s.ResourceRequirements;
import org.junit.Test;

public class TestTranslate {
  @Test
  public void testTranslateResourceRequirementsFromPod() throws FileNotFoundException {
    Pod pod = Pod.parseFromFile("src/test/resources/sleep_pod.yaml");
    Resource res = Translate.translateResourceRequirements(pod.getSpec().getContainers().get(0));
    assertEquals(200L, res.getMemorySize());
    assertEquals(1, res.getVirtualCores());
  }

  @Test
  public void testTranslateResourceRequirements() {
    Container container = new Container();
    ResourceRequirements resreq = new ResourceRequirements();
    Map<String, String> resources = ImmutableMap.of("cpu", "100m", "memory", "1Gi");
    resreq.setLimits(resources);
    container.setResources(resreq);
    Resource res = Translate.translateResourceRequirements(container);
    assertEquals(1024L, res.getMemorySize());
    assertEquals(1, res.getVirtualCores());
  }
}
