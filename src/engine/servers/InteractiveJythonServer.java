package engine.servers;

import org.apache.mina.core.service.IoHandler;

public class InteractiveJythonServer extends TcpServer {
	
	public InteractiveJythonServer(IoHandler iohandler, int port) {
		this.port = port;
		this.iohandler = iohandler;
	}
	
}
