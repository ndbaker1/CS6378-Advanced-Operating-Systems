import java.net.Socket;
import java.net.ServerSocket;


public class Node {
  private State state;

  public static void main(String[] args) throws Exception {
    /**
      Pseudocode

      1. read the config file to get the parameters
      2. spawn the TCP or SCTP server connection handler in the background
      3. establish connections to other nodes based on configuration file parameters
      4. handle incoming messages from peers in a state-machine style
     */

    // open a listening socket for the server
    ServerSocket welcomeSocket = new ServerSocket(9999);
    boolean done = false;
    while (!done) {
      // accept incoming socket connection requests
      Socket connectionSocket = welcomeSocket.accept();
      // spawn a new handler for the accepted
      new ClientHandler(connectionSocket).start();
    }
  }

  public State getState() {
    return this.state;
  }

  public void setState(State state) {
    this.state = state;
  }

  private enum State { ACTIVE, PASSIVE }
}

/**
 * Thread for handling parallel client connections
 */
class ClientHandler extends Thread {
  private Socket clientSocket;

  public ClientHandler(Socket socket) {
    this.clientSocket = socket;
  }

  public void run() {

  }
}