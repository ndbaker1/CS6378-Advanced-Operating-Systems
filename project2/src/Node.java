import java.util.Random;

public class Node {
  private final MutualExclusionService meService;

  private final int id;
  private final Config config;
  private final Random random;

  public Node(
    final int id,
    final Config config,
    final MutualExclusionService meService
  ) {
    this.id = id;
    this.config = config;
    this.meService = meService;
    random = new Random();
  }

  public static void main(String[] args) throws Exception {
    // find out which node this instance is labelled
    final int id = Integer.parseInt(args[0]);
    final Config config = Config.fromString(args[2], args[1]);
    
    // start new node
    new Node(id, config, new MutualExclusionService(id, config)).run();
  }

  private void run() throws Exception {
    for (int i = 0; i < config.numRequestsToGenerate; i++) {
      log("sleeping until enter request.." + i);
      Thread.sleep(randomExponentialFromMean(config.interRequestDelay));
      log("requesting entrance to cs.." + i);
      meService.csEnter();
      log("running cs.." + i);
      Thread.sleep(randomExponentialFromMean(config.csExecutionTime));
      log("exiting cs.." + i);
      meService.csLeave(i + 1 == config.numRequestsToGenerate); // Tell ME if you are finished when leaving CS
    }
  }

  private int randomExponentialFromMean(final int mean) {
    return (int)(-Math.log(1 - random.nextDouble()) * mean); // make sample from an exponential distribution with mean of the input value using inverse transform sampling
  }

  private void err(final String message) {
    System.err.println("[" + id + "] " + message);
  }

  private void log(final String message) {
    System.out.println("[" + id + "] " + message);
  }
}