package engine.clients;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketAddress;
import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;


import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;

import resources.common.Console;
import engine.clients.connection.Connection;
import engine.resources.common.StringUtilities;
import engine.resources.objects.SWGObject;

public class Client {

	protected IoSession session;
	protected long accountId;
	protected String accountName;
	protected String password;
	protected String email;
	protected byte[] sessionKey;
	protected long lastPacket;
	protected boolean disconnected = false;
	public boolean ready = false;
	private boolean isGM;
	private boolean debugPackets = false;
	
	public Connection connection;
	public SWGObject parent;
	
	public Client(SocketAddress endPoint) {
		this.connection = new Connection(endPoint);
	}
	
	public boolean isGM() { return isGM; }
	public void setGM(boolean isGM) { this.isGM = isGM; }
	
	public IoSession getSession() { return session; }
	public void setSession(IoSession s) { this.session = ((!debugPackets) ? s : new DebugSession(s)); }
	
	public long getAccountId() { return accountId; }
	public void setAccountId(long accountID) { this.accountId = accountID; }
	
	public String getAccountName() { return accountName; }
	public void setAccountName(String accountName) { this.accountName = accountName; }
	
	public String getAccountEmail() { return email; }
	public void setAccountEmail(String email) { this.email = email; }

	public Connection getConnection() { return connection; }
	public int getConnectionId() { return connection.getConnectionId(); }
	public String getPassword() { return password; }
	
	public byte[] getSessionKey() {
		return sessionKey;
	}
	
	public byte[] setSessionKey(byte[] sessionKey) {
		return this.sessionKey = sessionKey;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setDisconnected(boolean disconnected) {
		this.disconnected = disconnected;
	}
	
	public boolean isDisconnected() {
		return disconnected;
	}

	public void makeUnaware(SWGObject object) {
		parent.makeUnaware(object);
	}
	public void makeAware(SWGObject object) {
		parent.makeAware(object);
	}

	public SWGObject getParent() {
		return parent;
	}

	public void setParent(SWGObject parent) {
		this.parent = parent;	
	}
	
	public class DebugSession implements IoSession {
		IoSession session;
		
		public WriteFuture write(Object arg0) {
			String line, pidInfo ="";
			Process p;
			
			try {
				p = Runtime.getRuntime().exec(System.getenv("windir") + "\\system32\\" + "tasklist.exe");
				BufferedReader input =  new BufferedReader(new InputStreamReader(p.getInputStream()));
				while ((line = input.readLine()) != null) pidInfo += line; input.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (!pidInfo.contains("SwgClient_r.exe")) {
				Console.print("Warning: Client is no longer running.");
			} else {
				StringUtilities.printBytes(((IoBuffer) arg0).array());
			}
			
			return session.write(arg0);
		}
		
		public DebugSession(IoSession session) { this.session = session; }
		public DebugSession() {}
		public CloseFuture close() { return session.close(); }
		public CloseFuture close(boolean arg0) { return session.close(arg0); }
		public boolean containsAttribute(Object arg0) { return session.containsAttribute(arg0); }
		public Object getAttachment() { return session.getAttachment(); }
		public Object getAttribute(Object arg0) { return session.getAttribute(arg0); }
		public Object getAttribute(Object arg0, Object arg1) { return session.getAttribute(arg0, arg1); }
		public Set<Object> getAttributeKeys() { return session.getAttributeKeys(); }
		public int getBothIdleCount() { return session.getBothIdleCount(); }
		public CloseFuture getCloseFuture() { return session.getCloseFuture(); }
		public IoSessionConfig getConfig() { return session.getConfig(); }
		public long getCreationTime() { return session.getCreationTime(); }
		public Object getCurrentWriteMessage() { return session.getCurrentWriteMessage(); }
		public WriteRequest getCurrentWriteRequest() { return session.getCurrentWriteRequest(); }
		public IoFilterChain getFilterChain() { return session.getFilterChain(); }
		public IoHandler getHandler() { return session.getHandler(); }
		public long getId() { return session.getId(); }
		public int getIdleCount(IdleStatus arg0) { return session.getIdleCount(arg0); }
		public long getLastBothIdleTime() { return session.getLastBothIdleTime(); }
		public long getLastIdleTime(IdleStatus arg0) { return session.getLastIdleTime(arg0); }
		public long getLastIoTime() { return session.getLastIoTime(); }
		public long getLastReadTime() { return session.getLastReadTime(); }
		public long getLastReaderIdleTime() { return session.getLastReaderIdleTime(); }
		public long getLastWriteTime() { return session.getLastWriteTime(); }
		public long getLastWriterIdleTime() { return session.getLastWriterIdleTime(); }
		public SocketAddress getLocalAddress() { return session.getLocalAddress(); }
		public long getReadBytes() { return session.getReadBytes(); }
		public double getReadBytesThroughput() { return session.getReadBytesThroughput(); }
		public long getReadMessages() { return session.getReadMessages(); }
		public double getReadMessagesThroughput() { return session.getReadMessagesThroughput(); }
		public int getReaderIdleCount() { return session.getReaderIdleCount(); }
		public SocketAddress getRemoteAddress() { return session.getRemoteAddress(); }
		public long getScheduledWriteBytes() { return session.getScheduledWriteBytes(); }
		public int getScheduledWriteMessages() { return session.getScheduledWriteMessages(); }
		public IoService getService() { return session.getService(); }
		public SocketAddress getServiceAddress() { return session.getServiceAddress(); }
		public TransportMetadata getTransportMetadata() { return session.getTransportMetadata(); }
		public WriteRequestQueue getWriteRequestQueue() { return session.getWriteRequestQueue(); }
		public int getWriterIdleCount() { return session.getWriterIdleCount(); }
		public long getWrittenBytes() { return session.getWrittenBytes(); }
		public double getWrittenBytesThroughput() { return session.getWrittenBytesThroughput(); }
		public long getWrittenMessages() { return session.getWrittenMessages(); }
		public double getWrittenMessagesThroughput() { return session.getWrittenMessagesThroughput(); }
		public boolean isBothIdle() { return session.isBothIdle(); }
		public boolean isClosing() { return session.isClosing(); }
		public boolean isConnected() { return session.isConnected(); }
		public boolean isIdle(IdleStatus arg0) { return session.isIdle(arg0); }
		public boolean isReadSuspended() { return session.isReadSuspended(); }
		public boolean isReaderIdle() { return session.isReaderIdle(); }
		public boolean isWriteSuspended() { return session.isWriteSuspended(); }
		public boolean isWriterIdle() { return session.isWriterIdle(); }
		public ReadFuture read() { return session.read(); }
		public Object removeAttribute(Object arg0) { return session.removeAttribute(arg0); }
		public boolean removeAttribute(Object arg0, Object arg1) { return session.removeAttribute(arg0, arg1); }
		public boolean replaceAttribute(Object arg0, Object arg1, Object arg2) { return session.replaceAttribute(arg0, arg1, arg2); }
		public void resumeRead() { session.resumeRead(); }
		public void resumeWrite() { session.resumeWrite(); }
		public Object setAttachment(Object arg0) { return session.getAttachment(); }
		public Object setAttribute(Object arg0) { return session.setAttribute(arg0); }
		public Object setAttribute(Object arg0, Object arg1) { return session.setAttribute(arg0, arg1); }
		public Object setAttributeIfAbsent(Object arg0) {return session.setAttributeIfAbsent(arg0); }
		public Object setAttributeIfAbsent(Object arg0, Object arg1) { return session.setAttributeIfAbsent(arg0, arg1); }
		public void setCurrentWriteRequest(WriteRequest arg0) { session.setCurrentWriteRequest(arg0); }
		public void suspendRead() { session.suspendRead(); }
		public void suspendWrite() { session.suspendWrite(); }
		public void updateThroughput(long arg0, boolean arg1) { session.updateThroughput(arg0, arg1); }
		public WriteFuture write(Object arg0, SocketAddress arg1) { return session.write(arg0, arg1); }
	}
	
}
