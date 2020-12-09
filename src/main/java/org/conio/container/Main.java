package org.conio.container;

import org.conio.container.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Client.class);

  public static void main(String[] args) {
    try {
      Client client = new Client();
      client.init(args);
      client.run();
    } catch (Exception e) {
      LOG.error("Fatal exception, exiting.", e);
      System.exit(1);
    }
  }
}
