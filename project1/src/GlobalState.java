import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;


public class GlobalState {
    private final List<LocalState> localStates = new ArrayList<LocalState>();
    private final List<ChannelState> channelStates = new ArrayList<ChannelState>();

    public GlobalState() { }

    public List<LocalState> getLocalStates() {
        return localStates;
    }

    public List<ChannelState> getChannelStates() {
        return channelStates;
    }

    public void reset() {
        localStates.clear();
        channelStates.clear();
    }
}
