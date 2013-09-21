package engine.resources.objects;

import com.sleepycat.je.Environment;
import com.sleepycat.je.Transaction;

public interface IPersistent {
	
	public Transaction getTransaction();
	public void createTransaction(Environment environment);

}
