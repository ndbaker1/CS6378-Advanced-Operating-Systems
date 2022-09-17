import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;


public class Config {
  public final int nodes;
  public final int minPerActive;
  public final int maxPerActive;
  public final int minSendDelay;
  public final int snapshotDelay;
  public final int maxNumber;

  public final NodeConfig[] nodeConfigs;

  public Config(
    final int nodes,
    final int minPerActive,
    final int maxPerActive,
    final int minSendDelay,
    final int snapshotDelay,
    final int maxNumber,
    final NodeConfig[] nodeConfigs
  ) {
    this.nodes = nodes;
    this.minPerActive = minPerActive;
    this.maxPerActive = maxPerActive;
    this.minSendDelay = minSendDelay;
    this.snapshotDelay = snapshotDelay;
    this.maxNumber = maxNumber;
    this.nodeConfigs = nodeConfigs;
  }

  public static Config fromFile(String configFilepath) {
    final Scanner configReader = new Scanner(configFilepath);

    // read first line with 6 main tokens,

    // read node descriptions
    
    // read neighbors for nodes

    return new Config(0,0,0,0,0,0, new NodeConfig[]{});
  }
}

class NodeConfig {
  private final int id;
  private final String hostName;
  private final int listenPort;
  private final List<Integer> neighbors;

  public NodeConfig(
    final int id,
    final String hostName,
    final int listenPort
  ) {
    this.id = id;
    this.hostName = hostName;
    this.listenPort = listenPort;
    this.neighbors = new ArrayList<Integer>();
  }

  public int getId() {
    return id;
  }

  public String getHostName() {
    return hostName;
  }

  public int getListenPort() {
    return listenPort;
  }

  public List<Integer> getNeighbors() {
    return neighbors;
  }
}