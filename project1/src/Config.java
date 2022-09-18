import java.util.Scanner;


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

  public static Config fromFile(String configFilepath) {
    final Scanner configReader = new Scanner(configFilepath);

    // read first line with 6 main tokens,

    // read node descriptions
    
    // read neighbors for nodes

    return new Config(0,0,0,0,0,0, new Node[]{});
  }
}
