package engine.servers;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;

public class PingServer {
	
	private int port;
	NioDatagramAcceptor acceptor;
	
	public PingServer(int port) {
		
		this.port = port;
		
		acceptor = new NioDatagramAcceptor();
		acceptor.setHandler(new IoHandlerAdapter() {
			
			@Override
			public void messageReceived(IoSession session, Object message) throws Exception {
				session.write(message);
			}
			
		});
		
		DatagramSessionConfig dcfg = acceptor.getSessionConfig();
        dcfg.setReuseAddress(true);
		
	}
	
	public void bind() throws IOException {
		acceptor.bind(new InetSocketAddress(port));
	}


}
