package engine.resources.service;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;


public interface INetworkDispatch {

	public void insertOpcodes(Map<Integer,INetworkRemoteEvent> swgOpcodes, Map<Integer,INetworkRemoteEvent> objControllerOpcodes);
	
	public void shutdown();

}
