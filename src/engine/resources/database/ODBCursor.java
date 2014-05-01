package engine.resources.database;

import java.nio.ByteBuffer;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

public class ODBCursor {
	
	public Cursor cursor;
	public EntryBinding dataBinding;
	
	public ODBCursor(Cursor cursor, EntryBinding dataBinding) {
		this.cursor = cursor;
		this.dataBinding = dataBinding;
	}
	
	public Object next() {
        DatabaseEntry theKey = new DatabaseEntry();    
        DatabaseEntry theData = new DatabaseEntry();
		if(cursor.getNext(theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS)
			return dataBinding.entryToObject(theData);
		else 
			return null;
		
	}
	
	public void close() {
		cursor.close();
	}
	
	public boolean hasNext() {
        DatabaseEntry theKey = new DatabaseEntry();    
        DatabaseEntry theData = new DatabaseEntry();
		if(cursor.getNext(theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
			cursor.skipPrev(1, theKey, theData, LockMode.DEFAULT);
			return true;
		} else {
			return false;
		}

	}

}
