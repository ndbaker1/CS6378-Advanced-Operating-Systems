public class ChannelState {
    private final int destination;
    private final int source;
    private final int[] clock;

    public ChannelState(final int source, final int destination, final int[] clock) {
        this.source = source;
        this.destination = destination;
        this.clock = clock.clone();
    }

    public int getDestination() {
        return destination;
    }

    public int getSource() {
        return source;
    }

    public int[] getClock() {
        return clock;
    }
}