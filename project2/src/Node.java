import java.io.FileWriter;
import java.util.Random;

public class Node {

  private final int id;
  private final Config config;
  private final MutualExclusionService meService;

  private final Random random = new Random();

  private int responseTimeAccumulator = 0;

  public Node(
    final int id,
    final Config config,
    final MutualExclusionService meService
  ) {
    this.id = id;
    this.config = config;
    this.meService = meService;
  }

  public static void main(String[] args) throws Exception {
    // find out which node this instance is labelled
    final int id = Integer.parseInt(args[0]);
    final Config config = Config.fromString(args[2], args[1]);
    // initialize the metrics base
    MetricLogger.init(config);
    
    // start new node
    new Node(id, config, new MutualExclusionService(id, config)).run();
  }

  private void run() throws Exception {
    long systemStartTime = System.currentTimeMillis();

    for (int i = 0; i < config.numRequestsToGenerate; i++) {
      log("sleeping until enter request.." + i);
      Thread.sleep(randomExponentialFromMean(config.interRequestDelay));
      log("requesting entrance to cs.." + i);
      long requestStartTime = System.currentTimeMillis();
      meService.csEnter();
      log("running cs.." + i);
      Thread.sleep(randomExponentialFromMean(config.csExecutionTime));
      log("exiting cs.." + i);

      // add to the total response time accumulator
      responseTimeAccumulator += System.currentTimeMillis() - requestStartTime;

      final boolean finished = i + 1 == config.numRequestsToGenerate;
      // NOTE: important to save to file before the csLeave, because the csLeave
      // may call System.exit() before the final node can finish writing its data.
      if (finished) {
        // save the average response time to a file
        MetricLogger.record(id, config, "responseTime", (double) responseTimeAccumulator / config.numRequestsToGenerate);
        MetricLogger.record(id, config, "systemThroughput", (double) (1000 * config.nodes * config.numRequestsToGenerate) / (System.currentTimeMillis() - systemStartTime));
      }

      meService.csLeave(finished); // Tell ME if you are finished when leaving CS
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
