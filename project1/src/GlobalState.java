import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;


public class GlobalState {
    List<LocalState> localStates = new ArrayList<LocalState>();
    List<ChannelState> channelStates = new ArrayList<ChannelState>();

    public GlobalState() { }
}