package org.conio.container.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.RejectedSchedulingRequest;
import org.conio.container.k8s.Container;
import org.conio.container.k8s.RestartPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerTracker {
  private static final Logger LOG =
      LoggerFactory.getLogger(ContainerTracker.class);

  // input parameters
  private final List<Container> containers;
  private final RestartPolicy restartPolicy;

  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private final Map<String, List<ContainerEvent>> containerStatuses;
  private final Map<ContainerId, String> yarnContainerToK8sContainerName;
  private final Map<Long, String> containerMapping;

  private final AtomicLong requestId = new AtomicLong(1);

  ContainerTracker(List<Container> containers, RestartPolicy restartPolicy) {
    this.containers = containers;
    this.restartPolicy = restartPolicy;
    this.containerStatuses = new HashMap<>();
    this.containerMapping = new HashMap<>();
    this.yarnContainerToK8sContainerName = new HashMap<>();
  }

  synchronized long getNextRequestIdForContainer(Container container) {
    lock.writeLock();
    long id = requestId.incrementAndGet();
    containerMapping.put(id, container.getName());
    return id;
  }

  synchronized Container containerAllocated(
      org.apache.hadoop.yarn.api.records.Container yarnContainer) {
    long id = yarnContainer.getAllocationRequestId();
    if (!containerMapping.containsKey(id)) {
      throw new RuntimeException("Received container with unexpected allocatedRequestId: " + id);
    }
    String containerName = containerMapping.remove(id);
    yarnContainerToK8sContainerName.put(yarnContainer.getId(), containerName);
    for (Container k8sContainer : containers) {
      if (containerName.equals(k8sContainer.getName())) {
        registerEvent(containerName, ContainerEventType.STARTED);
        return k8sContainer;
      }
    }
    throw new RuntimeException("Unknown container name: " + containerName);
  }

  private synchronized void registerEvent(String containerName, ContainerEventType type) {
    // report STARTED event
    if (!containerStatuses.containsKey(containerName)) {
      containerStatuses.put(containerName, new ArrayList<>());
    }
    ContainerEvent event = new ContainerEvent(type);
    containerStatuses.get(containerName).add(event);
  }

  synchronized void containerCompleted(ContainerStatus status) {
    boolean success = status.getExitStatus() == 0;
    String containerName = yarnContainerToK8sContainerName.remove(status.getContainerId());
    List<ContainerEvent> events = containerStatuses.get(containerName);
    events.add(new ContainerEvent(
        success ? ContainerEventType.SUCCEEDED : ContainerEventType.FAILED));
  }

  synchronized void requestRejected(RejectedSchedulingRequest rejectedRequest) {
    long id = rejectedRequest.getRequest().getAllocationRequestId();
    String containerName = containerMapping.remove(id);
    LOG.info("Request rejected for container {}", containerName);
  }

  synchronized List<Container> getUnlaunchedContainers() {
    List<Container> unlaunchedContainers = new LinkedList<>();
    for (Container container : containers) {
      if (!containerStatuses.containsKey(container.getName())) {
        if (!requestHasAlreadySent(container)) {
          unlaunchedContainers.add(container);
        }
      } else {
        ContainerEventType type = getLastEventType(container);
        if (type.isTerminated()) {
          switch (restartPolicy) {
            case ALWAYS:
              unlaunchedContainers.add(container);
              continue;
            case ON_FAILURE:
              if (type == ContainerEventType.FAILED) {
                unlaunchedContainers.add(container);
                continue;
              }
              break;
            case NEVER:
              // do nothing
              break;
            default:
              throw new RuntimeException("Unexpected restart policy");
          }
        }
      }
    }
    return unlaunchedContainers;
  }

  private synchronized boolean requestHasAlreadySent(Container container) {
    for (Map.Entry<Long, String> containerReqIdToName: containerMapping.entrySet()) {
      String containerName = containerReqIdToName.getValue();
      if (container.getName().equals(containerName)) {
        return true;
      }
    }
    return false;
  }

  boolean hasFinished() {
    if (getUnlaunchedContainers().size() == 0) {
      return containers.stream().allMatch(x -> getLastEventType(x).isTerminated());
    }
    return false;
  }

  private synchronized ContainerEventType getLastEventType(Container container) {
    List<ContainerEvent> events = containerStatuses.get(container.getName());
    if (events == null) {
      return ContainerEventType.NONE;
    }
    ContainerEvent event = events.get(events.size() - 1);
    return event.getType();
  }
}
