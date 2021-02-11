package org.conio.client.command;

import org.apache.commons.cli.Options;
import org.conio.client.command.option.CLIOptionProvider;

public interface Command {
  Options collectOptions();
  void init(String[] args) throws Exception;
  void run() throws Exception;
  void cleanup();
}
