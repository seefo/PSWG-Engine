/*******************************************************************************
 * Copyright (c) 2013 <Project SWG>
 * 
 * This File is part of NGECore2.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Using NGEngine to work with NGECore2 is making a combined work based on NGEngine. 
 * Therefore all terms and conditions of the GNU Lesser General Public License cover the combination.
 ******************************************************************************/
package engine.resources.common;

import java.io.Serializable;

import org.apache.mina.core.buffer.IoBuffer;

import engine.clientdata.StfTable;
import engine.resources.objects.Delta;
import engine.resources.objects.SWGObject;

public class Stf extends Delta implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private AString stfFilename = new AString("");
	private int spacer = 0;
	private AString stfName = new AString("");
	
	public Stf(String stfFilename, int spacer, String stfName) {
		this.stfFilename = new AString(stfFilename);
		this.spacer = spacer;
		this.stfName = new AString(stfName);
	}
	
	public Stf(String stf) {
		setString(stf);
	}
	
	public Stf() {
		
	}
	
	public void init(SWGObject object) {
		super.init(object);
		stfFilename.init(object);
		stfName.init(object);
	}
	
	public String getStfFilename() {
		synchronized(objectMutex) {
			return stfFilename.get();
		}
	}
	
	public void setStfFilename(String stfFilename) {
		synchronized(objectMutex) {
			this.stfFilename = new AString(stfFilename);
		}
	}
	
	public int getSpacer() {
		synchronized(objectMutex) {
			return spacer;
		}
	}
	
	public void setSpacer(int spacer) {
		synchronized(objectMutex) {
			this.spacer = spacer;
		}
	}
	
	public String getStfName() {
		synchronized(objectMutex) {
			return stfName.get();
		}
	}
	
	public void setStfName(String stfName) {
		synchronized(objectMutex) {
			this.stfName = new AString(stfName);
		}
	}
	
	public String getStfValue() {
		synchronized(objectMutex) {
			try {
				StfTable stf = new StfTable("clientdata/string/en/" + stfFilename.get() + ".stf");
				
				for (int s = 1; s < stf.getRowCount(); s++) {
					if (stf.getStringById(s).getKey() != null && stf.getStringById(s).getKey().equals(stfName.get())) {
						if (stf.getStringById(s).getValue() != null) {
							return stf.getStringById(s).getValue();
						}
					}
				}
				
				return "";
	        } catch (Exception e) {
	        	return "";
	        }
		}
	}
	
	public String getString() {
		synchronized(objectMutex) {
			return ("@" + stfFilename.get() + ":" + stfName.get());
		}
	}
	
	public void setString(String stf) {
		synchronized(objectMutex) {
			if (stf == null || stf.equals("")) {
				stfFilename.set("");
				stfName.set("");
			} else if (stf.contains(":")) {
				stf = stf.replace("@", "");
				stfFilename.set(stf.split(":")[0]);
				stfName.set(stf.split(":")[1]);
			}
		}
	}
	
	public byte[] getBytes() {
		synchronized(objectMutex) {
			int size = stfFilename.getBytes().length + 4 + stfName.getBytes().length;
			
			IoBuffer buffer = createBuffer(size);
			buffer.put(stfFilename.getBytes());
			buffer.putInt(spacer);
			buffer.put(stfName.getBytes());
			buffer.flip();
			
			return buffer.array();
		}
	}
	
}
