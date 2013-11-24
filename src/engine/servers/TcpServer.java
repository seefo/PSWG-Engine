package engine.servers;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import engine.resources.service.ISocketAcceptor;

public abstract class TcpServer {

	private NioSocketAcceptor nioacceptor;
	protected int port;
	protected IoHandler iohandler;

	private void createDependencies() throws Exception {
		nioacceptor = new NioSocketAcceptor();
		nioacceptor.setHandler(iohandler);
		nioacceptor.bind(new InetSocketAddress("127.0.0.1", port));
		if(iohandler instanceof ISocketAcceptor) {
			((ISocketAcceptor) iohandler).setServer(this);
		}
	}

	public void start() {
		try {
			createDependencies();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		nioacceptor.dispose();
	}
	
	public NioSocketAcceptor getNioacceptor() {
		return nioacceptor;
	}

	public void setNioacceptor(NioSocketAcceptor nioacceptor) {
		this.nioacceptor = nioacceptor;
	}
	
	
}
