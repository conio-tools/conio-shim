package org.conio.client.command.option;

import org.apache.commons.cli.Option;

public enum CLIOption {
  QUEUE("q", "queue", true, "the queue this application will be submitted"),
  WATCH("w", "watch", false, "watches the application until termination"),
  YAML("y", "yaml", true, "the yaml file containing the description of the Kubernetes object"),
  ZK_CLIENT("zk", "zookeeper", true, "the address of the zookeeper instance to connect to"),
  ZK_ROOT_NODE("zn", "znode", true, "the root znode conio uses for storing data in Zookeeper"),

  TEST("t", "test", true, "long test description"),
  TEST2("t2", "test2", false, "long test description");

  private final String shortOpt;
  private final String longOpt;
  private final boolean hasArg;
  private final String desc;

  CLIOption(String shortOpt, String longOpt, boolean hasArg, String desc) {
    this.shortOpt = shortOpt;
    this.longOpt = longOpt;
    this.hasArg = hasArg;
    this.desc = desc;
  }

  public String option() {
    return longOpt;
  }

  Option toOption() {
    return new Option(shortOpt, longOpt, hasArg, desc);
  }
}
