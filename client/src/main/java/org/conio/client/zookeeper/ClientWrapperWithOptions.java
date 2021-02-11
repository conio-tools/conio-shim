package org.conio.client.zookeeper;

import static org.conio.client.command.option.CLIOption.ZK_CLIENT;
import static org.conio.client.command.option.CLIOption.ZK_ROOT_NODE;

import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.cli.CommandLine;
import org.conio.Constants;
import org.conio.client.command.option.CLIOption;
import org.conio.client.command.option.CLIOptionProvider;
import org.conio.zookeeper.ClientWrapper;

public class ClientWrapperWithOptions extends ClientWrapper implements CLIOptionProvider {
  /**
   * Precomputes the parameter from the command line arguments
   * to initialize the Zookeeper client.
   */
  public void init(CommandLine cliParser) {
    String zkConnectionString = cliParser.getOptionValue(ZK_CLIENT.option());
    String zkRoot = cliParser.getOptionValue(ZK_ROOT_NODE.option());
    if (zkRoot == null) {
      zkRoot = Constants.DEFAULT_ZK_ROOT_NODE;
    }
    super.init(zkRoot, zkConnectionString);
  }

  @Override
  public Collection<CLIOption> collectCLIOptions() {
    return Arrays.asList(ZK_CLIENT, ZK_ROOT_NODE);
  }
}
