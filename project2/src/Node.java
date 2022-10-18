import java.net.Socket;
import java.net.ServerSocket;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.File;
import java.io.EOFException;


public class Node {
  private final MutualExclusionService meService;

  private final int id;
  private final Config config;

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
    final Config config = Config.fromString(args[1]);
    // start new node
    new Node(id, config, new MutualExclusionService(id, config)).run();
  }

  private void run() throws Exception {
    for (int i = 0; i < config.numRequestsToGenerate; i++) {
      log("sleeping until enter request..");
      Thread.sleep(randomExponentialFromMean(config.interRequestDelay));
      log("requesting entrance to cs..");
      meService.csEnter();
      log("running cs..");
      Thread.sleep(randomExponentialFromMean(config.csExecutionTime));
      log("exiting cs..");
      meService.csLeave();
    }
  }

  private int randomExponentialFromMean(final int mean) {
    return mean; // make sample from an exponential distribution with mean of the input value
  }

  private void err(final String message) {
    System.err.println("[" + id + "] " + message);
  }

  private void log(final String message) {
    System.out.println("[" + id + "] " + message);
  }
}
