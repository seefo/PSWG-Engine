package engine.resources.service;

import engine.servers.TcpServer;

public class InteractiveJythonAcceptor implements ISocketAcceptor {

	private TcpServer server;
	
	
	public void setServer(TcpServer server) {
		this.server =server;
	}

}
