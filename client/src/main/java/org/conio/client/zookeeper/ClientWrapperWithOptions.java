package org.conio.client.zookeeper;

import org.conio.client.command.option.CLIOption;
import org.conio.client.command.option.CLIOptionProvider;
import org.conio.zookeeper.ClientWrapper;

import java.util.Arrays;
import java.util.Collection;

import static org.conio.client.command.option.CLIOption.ZK_CLIENT;
import static org.conio.client.command.option.CLIOption.ZK_ROOT_NODE;

public class ClientWrapperWithOptions extends ClientWrapper implements CLIOptionProvider {
  @Override
  public Collection<CLIOption> collectCLIOptions() {
    return Arrays.asList(ZK_CLIENT, ZK_ROOT_NODE);
  }
}
