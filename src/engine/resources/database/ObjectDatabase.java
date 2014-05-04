package engine.resources.database;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import resources.objects.creature.CreatureObject;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.evolve.Conversion;
import com.sleepycat.persist.evolve.Converter;
import com.sleepycat.persist.evolve.Deleter;
import com.sleepycat.persist.evolve.Mutations;
import com.sleepycat.persist.model.AnnotationModel;
import com.sleepycat.persist.model.EntityModel;

import engine.resources.objects.SWGObject;

public class ObjectDatabase implements Runnable {
	
	private Environment environment;
	private EnvironmentConfig EnvConfig;
	private DatabaseConfig dbConfig;
	private EntityStore entityStore;
	private Thread checkpointThread;
	private CheckpointConfig checkpointConfig;
	private EntryBinding dataBinding;
	private StoredClassCatalog classCatalog;
	private Database db;
	private Database classCatalogDb;
	private Vector<Cursor> cursors = new Vector<Cursor>();
	
	public ObjectDatabase(String name, boolean allowCreate, boolean useCheckpointThread, boolean allowTransactional, Class targetClass) {
		
		EnvConfig = new EnvironmentConfig();
		EnvConfig.setAllowCreate(allowCreate);
		//EnvConfig.setTransactional(allowTransactional);
		
		/*EntityModel model = new AnnotationModel();
		model.registerClass(CopyOnWriteArrayListProxy.class);
		model.registerClass(MultimapProxy.class);
		model.registerClass(VectorProxy.class);

		Mutations mutation = new Mutations();
		mutation.addDeleter(new Deleter(CreatureObject.class.getName(), 0, "performanceAudience"));		
	    StoreConfig storeConfig = new StoreConfig();
	    storeConfig.setModel(model);
	    storeConfig.setAllowCreate(allowCreate);
	    storeConfig.setTransactional(allowTransactional);
	    storeConfig.setMutations(mutation);*/
	    
        environment = new Environment(new File(".", "odb/" + name), EnvConfig);
        //entityStore = new EntityStore(environment, "EntityStore." + name, storeConfig);
        
        dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
       // dbConfig.setTransactional(false);
        
		classCatalogDb =
        		environment.openDatabase(null,
                                   "ClassCatalogDB",
                                   dbConfig);

            // Create our class catalog
        classCatalog = new StoredClassCatalog(classCatalogDb);
        dataBinding = new SerialBinding(classCatalog, targetClass);
		db = environment.openDatabase(null, name, dbConfig);       
		if (useCheckpointThread) {
        	checkpointConfig = new CheckpointConfig();
        	checkpointThread = new Thread(this);       
        	checkpointThread.start();
        }


	}
		
	
	public void put(Long key, Object value) {
        DatabaseEntry theKey = new DatabaseEntry();    
        theKey.setData(ByteBuffer.allocate(8).putLong(key).array());
        DatabaseEntry theData = new DatabaseEntry();
        dataBinding.objectToEntry(value, theData);
		db.put(null, theKey, theData);
	}
	
	public void put(String key, Object value) {
        DatabaseEntry theKey = new DatabaseEntry();    
        theKey.setData(key.getBytes());
        DatabaseEntry theData = new DatabaseEntry();
        dataBinding.objectToEntry(value, theData);
		db.put(null, theKey, theData);
	}
	
	public Object get(Long key) {
		if(!contains(key))
			return null;
        DatabaseEntry theKey = new DatabaseEntry();    
        theKey.setData(ByteBuffer.allocate(8).putLong(key).array());
        DatabaseEntry theData = new DatabaseEntry();
        db.get(null, theKey, theData, LockMode.DEFAULT);
        // Recreate the object from the retrieved DatabaseEntry using the EntryBinding 
        Object obj = dataBinding.entryToObject(theData);
        if(obj instanceof SWGObject) {
        	((SWGObject) obj).initAfterDBLoad();
        	((SWGObject) obj).viewChildren((SWGObject) obj, true, true, child -> child.initAfterDBLoad());
        }
        return obj;
	}
	
	public Object get(String key) {
		if(!contains(key))
			return null;
        DatabaseEntry theKey = new DatabaseEntry();    
        theKey.setData(key.getBytes());
        DatabaseEntry theData = new DatabaseEntry();
        db.get(null, theKey, theData, LockMode.DEFAULT);
        // Recreate the object from the retrieved DatabaseEntry using the EntryBinding 
        return dataBinding.entryToObject(theData);
	}

	public void remove(Long key) {
        DatabaseEntry theKey = new DatabaseEntry();    
        theKey.setData(ByteBuffer.allocate(8).putLong(key).array());
		db.removeSequence(null, theKey);
	}
	
	public ODBCursor getCursor() {
		Cursor cursor = db.openCursor(null, null);
		cursors.add(cursor);
		return new ODBCursor(cursor, dataBinding, this);
	}
	
	public boolean contains(Long key) {
        DatabaseEntry theKey = new DatabaseEntry();    
        theKey.setData(ByteBuffer.allocate(8).putLong(key).array());
        DatabaseEntry theData = new DatabaseEntry();
        return db.get(null, theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS;
	}
	
	public boolean contains(String key) {
        DatabaseEntry theKey = new DatabaseEntry();    
        theKey.setData(key.getBytes());
        DatabaseEntry theData = new DatabaseEntry();
        return db.get(null, theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS;
	}

	
	public Environment getEnvironment() { return environment; }
		
	public void compress() {
		environment.compress();
	}
	
    public void close() {
        if (environment != null) {
            try {
            	if(checkpointThread != null) 
            		checkpointThread.interrupt();
            	cursors.forEach(Cursor::close);
				environment.flushLog(true);          	
            	db.close();
            	classCatalogDb.close();
            	environment.close();
            } catch(DatabaseException dbe) {
                System.err.println("Error closing environment" + 
                     dbe.toString());
            }
        }
    }
    
	@Override
	public void run() {
		while(environment != null && environment.isValid()) {
			try {
				Thread.sleep(300000);
				environment.flushLog(true);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public Vector<Cursor> getCursors() {
		return cursors;
	}
	
	
	
}
