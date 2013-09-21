package engine.protocol;

import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class SOEProtocolCodecFactory implements ProtocolCodecFactory {

	private boolean isZone;
	private ProtocolEncoder encoder;
	private ProtocolDecoder decoder;

	public SOEProtocolCodecFactory() {
		decoder = new SOEProtocolDecoder();
		encoder = new SOEProtocolEncoder();
	}
	
	
	public ProtocolDecoder getDecoder(IoSession arg0) throws Exception {
		return new SOEProtocolDecoder();
	}

	
	public ProtocolEncoder getEncoder(IoSession arg0) throws Exception {
		return new SOEProtocolEncoder();
	}
	
}
