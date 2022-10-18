import java.util.Scanner;
import java.io.File;


public class Config {
  public final int nodes;
  public final int interRequestDelay;
  public final int csExecutionTime;
  public final int numRequestsToGenerate;
  public final NodeConfig[] nodeConfigs;

  public Config(
    final int nodes,
    final int interRequestDelay,
    final int csExecutionTime,
    final int numRequestsToGenerate,
    final NodeConfig[] nodeConfigs
  ) {
    this.nodes = nodes;
    this.interRequestDelay = interRequestDelay;
    this.csExecutionTime = csExecutionTime;
    this.numRequestsToGenerate = numRequestsToGenerate;
    this.nodeConfigs = nodeConfigs;
  }

  /**
   * Entrypoint to run in order to output parsed Config
   */
  public static void main(final String args[]) throws Exception {
    final String configPath = args[0];
    System.out.println(Config.fromFile(new File(configPath)));
  }

  @Override
  public String toString() {
    String configString = String.join(":",
      String.valueOf(nodes),
      String.valueOf(interRequestDelay),
      String.valueOf(csExecutionTime),
      String.valueOf(numRequestsToGenerate)
    );

    for (NodeConfig nodeConfig : nodeConfigs) {
      configString = String.join(":",
        configString,
        String.valueOf(nodeConfig.id),
        nodeConfig.hostName,
        String.valueOf(nodeConfig.listenPort)
      );
    }

    return configString;
  }

  public static Config fromString(final String configString) throws Exception {
    final String[] args = configString.split(":");

    final int nodes = Integer.parseInt(args[0]);
    final int interRequestDelay = Integer.parseInt(args[1]);
    final int csExecutionTime = Integer.parseInt(args[2]);
    final int numRequestsToGenerate = Integer.parseInt(args[3]);
    final NodeConfig[] nodeConfigs = new NodeConfig[nodes];

    for (int ni = 0; ni < nodes; ni++) {
      final int starti = 3 + 3 * ni;
      nodeConfigs[ni] = new NodeConfig(
        Integer.parseInt(args[starti + 1]),
        args[starti + 2],
        Integer.parseInt(args[starti + 3])
      );
    }

    return new Config(
      nodes,
      interRequestDelay,
      csExecutionTime,
      numRequestsToGenerate,
      nodeConfigs
    );
  }

  public static Config fromFile(final File configFile) throws Exception {
    final Scanner configReader = new Scanner(configFile);

    clearEmptyLines(configReader);

    // read first line with 4 main tokens,
    final int nodes = configReader.nextInt();
    final int interRequestDelay = configReader.nextInt();
    final int csExecutionTime = configReader.nextInt();
    final int numRequestsToGenerate = configReader.nextInt();

    final NodeConfig[] nodeConfigs = new NodeConfig[nodes];

    // read node descriptions
    for (int i = 0; i < nodes; i++) {
      clearEmptyLines(configReader);

      final int nodeid = configReader.nextInt();
      final String hostName = configReader.next();
      final int listenPort = configReader.nextInt();

      nodeConfigs[i] = new NodeConfig(nodeid, hostName, listenPort);

      // clear existing buffer
      configReader.nextLine();
    }

    // close reader
    configReader.close();

    return new Config(
      nodes,
      interRequestDelay,
      csExecutionTime,
      numRequestsToGenerate,
      nodeConfigs
    );
  }

  private static void clearEmptyLines(final Scanner scanner) {
    while (!scanner.hasNextInt()) scanner.nextLine();
  }

  /**
   * Data Class for Node specific configuration
   */
  static class NodeConfig {
    public final int id;
    public final String hostName;
    public final int listenPort;

    public NodeConfig(
      final int id,
      final String hostName,
      final int listenPort
    ) {
      this.id = id;
      this.hostName = hostName;
      this.listenPort = listenPort;
    }
  }
}
