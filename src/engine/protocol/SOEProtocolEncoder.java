package engine.protocol;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.mina.core.buffer.CachedBufferAllocator;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import engine.clients.Client;
import engine.protocol.packager.MessageCRC;
import engine.protocol.packager.MessageCompression;
import engine.protocol.packager.MessageEncryption;
import engine.protocol.packager.MessagePackager;
import engine.protocol.soe.DataChannelA;
import engine.protocol.soe.FragmentedChannelA;
import engine.resources.common.CRC;
import engine.resources.common.Utilities;
import engine.resources.service.*;



public class SOEProtocolEncoder implements ProtocolEncoder {

	private MessageCompression messageCompression;
	private MessageEncryption messageEncryption;
	private MessageCRC messageCRC;
	private Map<IoSession, Vector<IoBuffer>> queue;
	private CachedBufferAllocator bufferPool;


	SOEProtocolEncoder() {
		this.messageCompression = new MessageCompression();
		this.messageEncryption = new MessageEncryption();
		this.messageCRC = new MessageCRC();
		bufferPool = new CachedBufferAllocator();
	}
	
	@Override
	public void dispose(IoSession arg0) throws Exception {}

	@Override
	public void encode(IoSession session, Object input, ProtocolEncoderOutput output) throws Exception {
		if(input instanceof IoBuffer) {
			((NetworkDispatch) session.getHandler()).queueMessage(session, (IoBuffer) input);
		}
		if(input instanceof SOEPacket) {
			if(((SOEPacket) input).getData().array()[1] == 2 || ((SOEPacket) input).getData().array()[1] == 8) {
				
				if(((SOEPacket) input).getData().array()[1] == 8) {
					byte[] data = ((SOEPacket) input).getData().array();
					data = messageCompression.compress(data);
					IoBuffer buffer = ((SOEPacket) input).getData();
					buffer.clear();
					buffer.setAutoExpand(true);
					buffer.put(data);
					buffer.flip();
				}
				output.write(((SOEPacket) input).getData());
				return;
			}
			assembleMessage(session, ((SOEPacket) input).getData(), output);
		}
		if(input instanceof byte[]) {
			IoBuffer packet = IoBuffer.allocate(((byte[])input).length).put((byte[]) input);
			packet.flip();
			session.setAttribute("sent", (Long) session.getAttribute("sent") + 1);
			output.write(packet);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void assembleMessage(IoSession session, IoBuffer buffer, ProtocolEncoderOutput encoderOutput) {
		
		if(Utilities.IsSOETypeMessage(buffer.array())) {

			byte[] data = buffer.array();
			if(buffer.get(1) == 8)
				data = messageCRC.append(
						messageEncryption.encrypt(messageCompression.compress(data), 0xDEADBABE), 0xDEADBABE);
			else
				data = messageCRC.append(
						messageEncryption.encrypt(data, 0xDEADBABE), 0xDEADBABE);
			buffer.clear();
			buffer.setAutoExpand(true);
			buffer.put(data);
			buffer.flip();
			if(data.length > 0) {
				session.setAttribute("sent", (Long) session.getAttribute("sent") + 1);
				encoderOutput.write(buffer);
			}
			
		} else {
						
			if(buffer.array().length < 6) return;
			
			if(buffer.array().length > 487) {
				
				
				FragmentedChannelA fragChanA = new FragmentedChannelA();
				for (FragmentedChannelA fragChanASection : fragChanA.create(buffer.array())) {
					fragChanASection.setSequence(((Integer) session.getAttribute("nextOutValue")).shortValue());
					session.setAttribute("nextOutValue", (Integer) session.getAttribute("nextOutValue") + 1);
					byte[] data = messageCRC.append(
							messageEncryption.encrypt(messageCompression.compress(fragChanASection.serialize().array()), 0xDEADBABE), 0xDEADBABE);
					buffer.clear();
					buffer.setAutoExpand(true);
					//buffer = IoBuffer.allocate(data.length);
					buffer.put(data);
					buffer.flip();
					if(data.length > 0) {
						encoderOutput.write(buffer);
					}	
					
					((TreeMap<Short, byte[]>) session.getAttribute("sentPackets")).put(((Integer) session.getAttribute("nextOutValue")).shortValue(), buffer.array());

				}
			} else {
				

				/*DataChannelA dataA = new DataChannelA();
				dataA.addMessage(buffer);
				
				dataA.setSequence(((Integer) session.getAttribute("nextOutValue")).shortValue());
				session.setAttribute("nextOutValue", (Integer) session.getAttribute("nextOutValue") + 1);
				byte[] data = messageCRC.append(
						messageEncryption.encrypt(messageCompression.compress(dataA.serialize().array()), 0xDEADBABE), 0xDEADBABE);
				buffer.clear();
				buffer = IoBuffer.allocate(data.length);
				buffer.put(data);
				buffer.flip();
				if(data.length > 0) {
					encoderOutput.write(buffer);
				}
				//TreeMap<Short, IoBuffer> sent = (TreeMap<Short, IoBuffer>) session.getAttribute("sentPackets");


				((TreeMap<Short, byte[]>) session.getAttribute("sentPackets")).put(((Integer) session.getAttribute("nextOutValue")).shortValue(), buffer.array());*/
				
			}
		}
	}
	
	
	
}
