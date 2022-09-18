import java.util.Scanner;
import java.io.File;


public class Config {
  public final int nodes;
  public final int minPerActive;
  public final int maxPerActive;
  public final int minSendDelay;
  public final int snapshotDelay;
  public final int maxNumber;

  public final Node[] nodeConfigs;

  public Config(
    final int nodes,
    final int minPerActive,
    final int maxPerActive,
    final int minSendDelay,
    final int snapshotDelay,
    final int maxNumber,
    final Node[] nodeConfigs
  ) {
    this.nodes = nodes;
    this.minPerActive = minPerActive;
    this.maxPerActive = maxPerActive;
    this.minSendDelay = minSendDelay;
    this.snapshotDelay = snapshotDelay;
    this.maxNumber = maxNumber;
    this.nodeConfigs = nodeConfigs;
  }

  public static Config fromFile(String configFilepath) throws Exception {
    final Scanner configReader = new Scanner(new File(configFilepath));

    clearEmptyLines(configReader);

    // read first line with 6 main tokens,
    final int nodes = configReader.nextInt();
    final int minPerActive = configReader.nextInt();
    final int maxPerActive = configReader.nextInt();
    final int minSendDelay = configReader.nextInt();
    final int snapshotDelay = configReader.nextInt();
    final int maxNumber = configReader.nextInt();

    final Node[] nodeConfigs = new Node[nodes];

    // read node descriptions
    for (int i = 0; i < nodes; i++) {
      clearEmptyLines(configReader);

      final int nodeid = configReader.nextInt();
      final String hostName = configReader.next();
      final int listenPort = configReader.nextInt();

      nodeConfigs[i] = new Node(
        nodeid,
        hostName,
        listenPort
      );

      // clear existing buffer
      configReader.nextLine();
    }
    
    // read neighbors for nodes
    for (int i = 0; i < nodes; i++) {
      clearEmptyLines(configReader);

      final Scanner neighborReader = new Scanner(configReader.nextLine());

      while (neighborReader.hasNextInt()) {
        final int neighbor = neighborReader.nextInt();
        nodeConfigs[i].getNeighbors().add(neighbor);
      }

      // close reader
      neighborReader.close();
    }

    // close reader
    configReader.close();

    return new Config(
      nodes,
      minPerActive,
      maxPerActive,
      minSendDelay,
      snapshotDelay,
      maxNumber,
      nodeConfigs
    );
  }

  private static void clearEmptyLines(final Scanner scanner) {
    while (!scanner.hasNextInt()) scanner.nextLine();
  }
}
