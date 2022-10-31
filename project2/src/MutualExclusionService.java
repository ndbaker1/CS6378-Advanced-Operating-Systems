import java.util.concurrent.Semaphore;
import java.util.*;
import java.net.*;
import java.io.*;

public class MutualExclusionService {
  private final Map<Integer, Socket> sockets = new HashMap<>();
  private final Map<Integer, ObjectOutputStream> outputStreams = new HashMap<>();
  private final Map<Integer, Integer> timestamps = new HashMap<>(); // Latest timestamp received from each process
  private final Map<Integer, Boolean> finishedMap = new HashMap<>(); // Track if each process has finished 

  private final Semaphore csLock = new Semaphore(0, true);

  private final PriorityQueue<Message.Request> requestQueue = new PriorityQueue<Message.Request>();

  private final int nodeId;
  private final Config config;
  private final FileWriter outputWriter;
  
  // Scalar lamport clock
  private Integer lamportClock = 0;

  // Metric variables
  private int messagesSent = 0;

  public MutualExclusionService(
    final int nodeId,
    final Config config
  ) throws Exception {
    this.nodeId = nodeId;
    this.config = config;


    // Create logs directory in the project directory to write CS activity
    new File(config.project_path, "logs").mkdir();
    // Node 0 deletes log files that won't be overwritten
    if(nodeId == 0)
    {
      for (final File file : new File(config.project_path, "logs").listFiles()) {
        if (!file.isDirectory()) {
          try {
            // Only delete files that won't get overwritten by a node in this run
            String name = file.getName();
            if(config.nodes <= Integer.parseInt(name.substring(7, name.indexOf('.')))) {
              file.delete();
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
    outputWriter = new FileWriter(config.project_path + "/logs/output-" + nodeId + ".out");
    
    // spawn a thread to accept connections to this node
    new Thread(() -> setupListener()).start(); 

    log("sleeping for [5] seconds to allow peer server sockets to setup...");
    Thread.sleep(5000);

    finishedMap.put(nodeId, false); // Need entry to track if self has finished
    
    // create a socket connection to each node
    for (int node = 0; node < config.nodes; node++) {
      if(node == nodeId) // Don't connect to self
        continue;
        
      final Socket socket = new Socket(
        config.nodeConfigs[node].hostName,
        config.nodeConfigs[node].listenPort
      );

      log("connected to " + node);

      sockets.put(node, socket);
      outputStreams.put(node, new ObjectOutputStream(socket.getOutputStream()));

      timestamps.put(node, 0);
      finishedMap.put(node, false);
    }
    
    log("sleeping for [5] seconds to allow peer client sockets to setup...");
    Thread.sleep(5000);
  }

  /**
   * Critial Section Enter
   *
   * Send request messages to all nodes and wait for replies before allowing
   * entrance into the critical section
   */
  public void csEnter() {
    Message.Request requestMessage;
    synchronized(lamportClock) {
      lamportClock += 1;
      requestMessage = new Message.Request(nodeId, lamportClock);
    }
    synchronized(requestQueue) {
      requestQueue.add(requestMessage);
    }

    // Send out request messages to all outgoing channels
    for (int streamIndex : outputStreams.keySet()) {
      sendMessage(streamIndex, requestMessage);
    }

    // block until permission given
    try {
      csLock.acquire();
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      outputWriter.write(nodeId + " enter at " + lamportClock + "\n");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Critical Section Leave
   *
   * Send release messages notifying nodes in the system that the current
   * process has completed executing critical section
   */
  public void csLeave(boolean finished) {

    Message releaseMessage;
    synchronized(lamportClock) {
      lamportClock += 1;
      releaseMessage = new Message.Release(nodeId, lamportClock, finished);
    }
    
    try {
      outputWriter.write(nodeId + " leave at " + lamportClock + "\n");
    } catch (Exception e) {
      e.printStackTrace();
		}

    // Send out release messages to all outgoing channels
    for (int streamIndex : outputStreams.keySet()) {
      sendMessage(streamIndex, releaseMessage);
    }

    if (finished) {
      check_finished(nodeId);
    }
  }

  private void setupListener() {
    try {
      // open a listening socket for the server
      final ServerSocket welcomeSocket = new ServerSocket(config.nodeConfigs[nodeId].listenPort);
      while (true) {
        // accept incoming socket connection requests
        final Socket connectionSocket = welcomeSocket.accept();
        final ObjectInputStream inputStream = new ObjectInputStream(connectionSocket.getInputStream());
        // spawn a new handler for the accepted
        new Thread(() -> {
          while (true) {
            try {
              final Message message = (Message) inputStream.readObject();
              if (message instanceof Message.Request) {
                log("received a request message.");
                handleRequest((Message.Request) message);
              } else if (message instanceof Message.Reply) {
                log("received a reply message.");
                handleReply((Message.Reply) message);
              } else if (message instanceof Message.Release) {
                log("received a release message.");
                handleRelease((Message.Release) message);
              }
            } catch (EOFException e) {
              break; // this is fine to ignore
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }).start();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleRequest(final Message.Request requestMessage) {
    synchronized(requestQueue) {
      requestQueue.add(requestMessage);
    }

    // Update scalar clock and send reply
    synchronized(lamportClock) {
      lamportClock = Integer.max(lamportClock, requestMessage.getTime()) + 1; 
      sendMessage(requestMessage.getSource(), new Message.Reply(nodeId, lamportClock));
    }
    
    timestamps.put(requestMessage.getSource(), requestMessage.getTime());
  }

  private void handleReply(final Message.Reply replyMessage) {
    int source = replyMessage.getSource();
    int time = replyMessage.getTime();

    // Update scalar clock
    synchronized(lamportClock) {
      lamportClock = Integer.max(lamportClock, time) + 1; 
    }

    // Update the timestamp of a source. Don't need to synchronize since each source is only accessed by a single thread
    timestamps.put(source, time);

    testCriticalSection();
  }

  private void handleRelease(final Message.Release releaseMessage) {
    // Update scalar clock
    synchronized(lamportClock) {
      lamportClock = Integer.max(lamportClock, releaseMessage.getTime()) + 1; 
    }

    // Remove source's request from the queue
    synchronized(requestQueue) {
      requestQueue.removeIf((r) -> r.getSource() == releaseMessage.getSource());
    }

    timestamps.put(releaseMessage.getSource(), releaseMessage.getTime());

    if (releaseMessage.getFinished()) {
      check_finished(releaseMessage.getSource());
    }

    testCriticalSection();
  }
  
  /**
   * Send a message to a target node with a provided Message.
   * 
   * Note:
   *  important synchronized resource, only one thread
   *  should be using the output stream at once
   */
  private synchronized void sendMessage(final int targetNode, final Message message) {
    try {
      final ObjectOutputStream ostream = outputStreams.get(targetNode);
      ostream.writeObject(message);
      ostream.flush();
      // increment message counter
      messagesSent += 1;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // wait for reply from all or for timestamp to be lower than all other received times
  // wait for request to be at front of queue
  private void testCriticalSection() {
    synchronized(timestamps) {
      synchronized(requestQueue) {
        if ( // provide access to the critical section when request is at the front and all peering timestamps are lower
          requestQueue.peek() != null &&
          requestQueue.peek().getSource() == nodeId &&
          timestamps.entrySet().stream().allMatch(t -> t.getValue() > requestQueue.peek().getTime())
        ) {
          // Remove own request from the queue
          requestQueue.poll();
          csLock.release();
        }
      }
    }
  }

  private void err(final String message) {
    System.err.println("[" + nodeId + "-meService] " + message);
  }

  private void log(final String message) {
    System.out.println("[" + nodeId + "-meService] " + message);
  }

  // Update node_id to be finished. If all nodes are finished, terminate
  private void check_finished(int node_id) {
    finishedMap.put(node_id, true);
    if(!finishedMap.containsValue(false)) {
      log("terminating");
      try {
        outputWriter.close();
      } catch (Exception e) {
        e.printStackTrace();
      }

      // record the number of messages sent during the execution
      MetricLogger.record(nodeId, config, "messageComplexity", (double) messagesSent / config.numRequestsToGenerate);

      // Node 0 runs mutual exclusion check
      if (nodeId == 0) {
        log("running mutual exclusion check..");
        // Combine all log files into single list
        final List<String[]> entryList = new ArrayList<String[]>();
        for (final File file : new File(config.project_path, "logs").listFiles()) {
          if (!file.isDirectory()) {
            try {
              final Scanner fileScanner = new Scanner(file);
              while (fileScanner.hasNextLine()) {
                entryList.add(fileScanner.nextLine().split(" "));
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
        
        // sort the entries by their timestamp
        entryList.sort((String[] s1, String[] s2) -> Integer.parseInt(s1[3]) < Integer.parseInt(s2[3]) ? -1 : 1);

        // Ensure that enter/leave entries alternate, timestamps are strictly increasing, 
        // and a leave follows an enter from the same node
        String[] last = null;
        for (String[] cur : entryList) {
          if (
            last != null &&
              ( last[1].equals(cur[1]) ||
              Integer.parseInt(cur[3]) <= Integer.parseInt(last[3]) ||
              cur[1].equals("leave") && !last[0].equals(cur[0]) )
          ) {
            err("failed mutual exclusion for:");
            err("\t" + String.join(" ", cur));
            err("\t" + String.join(" ", last));
            System.exit(0);
          }
          last = cur;
        }
        log("passed mutual exclusion check!");
      }

      System.exit(0);
    }
  }
}
