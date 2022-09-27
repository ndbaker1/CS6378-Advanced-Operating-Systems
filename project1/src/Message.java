import java.io.Serializable;
import java.util.List;


public class Message implements Serializable {
  private int source;

  private Message(final int source) {
    this.source = source;
  }

  public int getSource() {
    return source;
  }

  static class Finish extends Message {
    public Finish(final int source) {
      super(source);
    }
  }

  static class Snapshot extends Message {
    LocalState localState;
    List<ChannelState> channelStates;

    public Snapshot(final int source, final LocalState localState, final List<ChannelState> channelStates) {
      super(source);
      this.localState = localState;
      this.channelStates = channelStates;
    }
  }

  static class Application extends Message {
    int[] vectorClock;
    
    public Application(final int source, final int[] vectorClock) {
      super(source);
      this.vectorClock = vectorClock.clone();
    }
  }

  static class Marker extends Message {
    public Marker(final int source) {
      super(source);
    }
  }
}
