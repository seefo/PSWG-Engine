package engine.resources.database;

import java.io.File;

import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.AnnotationModel;
import com.sleepycat.persist.model.EntityModel;

public class ObjectDatabase implements Runnable {
	
	private Environment environment;
	private EnvironmentConfig EnvConfig;
	//private DatabaseConfig dbConfig;
	private EntityStore entityStore;
	private Thread checkpointThread;
	private CheckpointConfig checkpointConfig;
	
	public ObjectDatabase(String name, boolean allowCreate, boolean useCheckpointThread, boolean allowTransactional) {
		
		EnvConfig = new EnvironmentConfig();
		EnvConfig.setAllowCreate(allowCreate);
		EnvConfig.setTransactional(allowTransactional);
		
		EntityModel model = new AnnotationModel();
		model.registerClass(CopyOnWriteArrayListProxy.class);
		model.registerClass(MultimapProxy.class);
		model.registerClass(VectorProxy.class);
		
	    StoreConfig storeConfig = new StoreConfig();
	    storeConfig.setModel(model);
	    storeConfig.setAllowCreate(allowCreate);
	    storeConfig.setTransactional(allowTransactional);
	    
        environment = new Environment(new File(".", "odb/" + name), EnvConfig);
        entityStore = new EntityStore(environment, "EntityStore." + name, storeConfig);

        if (useCheckpointThread) {
        	checkpointConfig = new CheckpointConfig();
        	checkpointThread = new Thread(this);
        }
        
        /*dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(false);*/

	}
	
	/**
	 * Puts an object into the EntityStore(non-transactional)
	 * @param value The Object that gets stored.
	 * @param keyClass The Class of the PrimaryKey
	 * @param valueClass The Class of the object that gets stored.
	 */
	@SuppressWarnings("unchecked")
	public <K, V> void put(Object value, Class<K> keyClass, Class<V> valueClass) {
		entityStore.getPrimaryIndex(keyClass, valueClass).put((V) value);
	}
	
	/**
	 * Puts an object into the EntityStore(transactional)
	 * @param value The Object that gets stored.
	 * @param keyClass The Class of the PrimaryKey
	 * @param valueClass The Class of the object that gets stored.
	 */
	@SuppressWarnings("unchecked")
	public <K, V> void put(Object value, Class<K> keyClass, Class<V> valueClass, Transaction txn) {
		entityStore.getPrimaryIndex(keyClass, valueClass).put(txn, (V) value);
	}
	
	/**
	 * Gets an object by its PrimaryKey(usually objectId).
	 * @param key The Primary Key of the object that you want to get.
	 * @param keyClass The Class of the Primary Key.
	 * @param valueClass The Class of the Object that you want to get.
	 * @return The Object that is matching to the given PrimaryKey.
	 */
	@SuppressWarnings("unchecked")
	public <K, V> V get(Object key, Class<K> keyClass, Class<V> valueClass) {
		return entityStore.getPrimaryIndex(keyClass, valueClass).get((K) key);
	}
	
	/**
	 * Deletes an object by its PrimaryKey(usually objectId).
	 * (Non-transactional)
	 * @param key The Primary Key of the object that gets deleted.
	 * @param keyClass The Class of the Primary Key.
	 * @param valueClass The Class of the Object that gets deleted.
	 */
	@SuppressWarnings("unchecked")
	public <K, V> void delete(Object key, Class<K> keyClass, Class<V> valueClass) {
		entityStore.getPrimaryIndex(keyClass, valueClass).delete((K) key);
	}
	
	/**
	 * Deletes an object by its PrimaryKey(usually objectId).
	 * (Transactional)
	 * @param key The Primary Key of the object that gets deleted.
	 * @param keyClass The Class of the Primary Key.
	 * @param valueClass The Class of the Object that gets deleted.
	 */
	@SuppressWarnings("unchecked")
	public <K, V> void delete(Object key, Class<K> keyClass, Class<V> valueClass, Transaction txn) {
		entityStore.getPrimaryIndex(keyClass, valueClass).delete(txn, (K) key);
	}
	
	/**
	 * Gets a Cursor for iterating through the DB records. Useful for spawning objects like buildings at server start.
	 * @param keyClass The Class of the Primary Key.
	 * @param valueClass The Class of the Values that are stored.
	 * @return The EntityCursor of this EntityStore.
	 */
	public <K, V> EntityCursor<V> getCursor(Class<K> keyClass, Class<V> valueClass) {
		return entityStore.getPrimaryIndex(keyClass, valueClass).entities();
	}
	
	/**
	 * Checks if the Database contains a value associated to the given key.
	 * @param key The Primary Key.
	 * @param keyClass The class of the Primary Key.
	 * @param valueClass The class of the Value.
	 */
	@SuppressWarnings("unchecked")
	public <K, V> boolean contains(Object key, Class<K> keyClass, Class<V> valueClass) {
		return entityStore.getPrimaryIndex(keyClass, valueClass).contains((K) key);
	}
	
	// not needed, we will store stuff in our EntityStore and have an enviroment for each type of object
	/*public Database openDatabase(String dbName) {
		
		Database db = environment.openDatabase(null, dbName, dbConfig);

		return db;
		
	}*/
	
	public Environment getEnvironment() { return environment; }
	
	public EntityStore getEntityStore() { return entityStore; }
	
	public void compress() {
		environment.compress();
	}
	
    public void close() {
        if (environment != null && entityStore != null) {
            try {
            	if(checkpointThread != null) 
            		checkpointThread.wait();
            	environment.close();
            	entityStore.close();
            } catch(DatabaseException | InterruptedException dbe) {
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
				environment.checkpoint(checkpointConfig);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
