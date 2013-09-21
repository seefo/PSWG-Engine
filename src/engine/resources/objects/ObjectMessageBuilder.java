package engine.resources.objects;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.mina.core.buffer.CachedBufferAllocator;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;

import engine.resources.common.Utilities;

import protocol.swg.SceneEndBaselines;

public abstract class ObjectMessageBuilder {
	
	public SWGObject object;
	public SimpleBufferAllocator bufferPool = new SimpleBufferAllocator();
	
	public IoBuffer createBaseline(String objectType, byte viewType, IoBuffer data, int size) {
		
		IoBuffer buf = bufferPool.allocate(23 + size, false).order(ByteOrder.LITTLE_ENDIAN);
		buf.putShort((short) 5);
		buf.putInt(0x68A75F0C);
		buf.putLong(object.getObjectID());
		try {
			buf.put(reverse(objectType).getBytes("US-ASCII"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		buf.put(viewType);
		buf.putInt(size);	// size
		buf.put(data);
		buf.flip();
		return buf;
	}
	
	public IoBuffer createDelta(String objectType, byte viewType, short updateCount, short updateType, IoBuffer data, int size) {
		
		IoBuffer buf = bufferPool.allocate(27 + size, false).order(ByteOrder.LITTLE_ENDIAN);
		buf.putShort((short) 5);
		buf.putInt(0x12862153);
		buf.putLong(object.getObjectID());
		try {
			buf.put(reverse(objectType).getBytes("US-ASCII"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		buf.put(viewType);
		buf.putInt(size);	// size
		buf.putShort(updateCount);
		buf.putShort(updateType);
		buf.put(data);

		buf.flip();
		return buf;

		
	}
	
	public SWGObject getObject() { return object; }
	public void setObject(SWGObject object) { this.object = object; }
	
	public abstract void sendBaselines();
	
	private String reverse(String reverseString) {
		
	    if (reverseString.length() <= 1) return reverseString;
	    return reverse(reverseString.substring(1, reverseString.length())) + reverseString.charAt(0);
	    
	}

	protected String getAsciiString(ByteBuffer buffer) { return getString(buffer, "US-ASCII"); }
	protected String getUnicodeString(ByteBuffer buffer) { return getString(buffer, "UTF-16LE"); }
	protected byte[] getAsciiString(String string) { return getString(string, "US-ASCII"); }
	protected byte[] getUnicodeString(String string) { return getString(string, "UTF-16LE"); }
	
	private String getString(ByteBuffer buffer, String charFormat) {
		String result;
		int length;
		if (charFormat == "UTF-16LE")
			length = buffer.order(ByteOrder.LITTLE_ENDIAN).getInt();
		else
			length = buffer.order(ByteOrder.LITTLE_ENDIAN).getShort();
		
		int bufferPosition = buffer.position();
		try {
			result = new String(buffer.array(), bufferPosition, length, charFormat);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
		buffer.position(bufferPosition + length);
		
		return result;
	}
	private byte[] getString(String string, String charFormat) {
		ByteBuffer result;
		int length = 2 + string.length();
		if (charFormat == "UTF-16LE") {
			result = ByteBuffer.allocate(length * 2).order(ByteOrder.LITTLE_ENDIAN);
			result.putInt(string.length());
		}
		else {
			result = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
			result.putShort((short)string.length());
		}
		try {
			result.put(string.getBytes(charFormat));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return new byte[] { };
		}
		return result.array();		
	}


}
