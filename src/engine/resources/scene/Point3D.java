package engine.resources.scene;

import com.sleepycat.persist.model.Persistent;

@Persistent
public class Point3D {
	
	public float z;
	public float x;
	public float y;
	
	public Point3D() { }

	public Point3D(float x, float y, float z) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		
	}
	
	public Point3D clone() {
		
		return new Point3D(x, y, z);
		
	}
	
	public float getDistance(Point3D target) {
		
		return (float)Math.sqrt(
			Math.pow(x - target.x, 2) + 
			Math.pow(y - target.y, 2) +
			Math.pow(z - target.z, 2));
		
	}

	public float getDistance2D(Point3D target) {
		
		return (float)Math.sqrt(
				Math.pow(x - target.x, 2) + 
				Math.pow(z - target.z, 2));
		
	}


}
