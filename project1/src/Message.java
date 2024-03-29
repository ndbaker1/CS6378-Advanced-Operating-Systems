import java.io.Serializable;
import java.util.List;


public abstract class Message implements Serializable {
  private int source;

  private Message(final int source) {
    this.source = source;
  }

  public int getSource() {
    return source;
  }

  static class Application extends Message {
    private final int[] vectorClock;
    
    public Application(final int source, final int[] vectorClock) {
      super(source);
      this.vectorClock = vectorClock.clone();
    }

    public int[] getVectorClock() {
      return vectorClock;
    }
  }

  static class Marker extends Message {
    public Marker(final int source) {
      super(source);
    }
  }

 static class Snapshot extends Message {
    private final LocalState localState;
    private final List<ChannelState> channelStates;

    public Snapshot(final int source, final LocalState localState, final List<ChannelState> channelStates) {
      super(source);
      this.localState = localState;
      this.channelStates = channelStates;
    }

    public LocalState getLocalState() {
      return localState;
    }

    public List<ChannelState> getChannelStates() {
      return channelStates;
    }
  }

  static class Halt extends Message {
    public Halt(final int source) {
      super(source);
    }
  }
}
