public class ChannelState {
    final int destination;
    final int source;
    final int[] clock;

    public ChannelState(final int source, final int destination, final int[] clock) {
        this.source = source;
        this.destination = destination;
        this.clock = clock;
    }
}