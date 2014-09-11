package engine.resources.scene;

public class Point2D {
	
	public float x, z;
	
	public Point2D() {
		
	}
	
	public Point2D(float x, float z) {
		this.x = x;
		this.z = z;
	}
	
	public Point2D clone() {
		return new Point2D(x, z);
	}
	
	public float getDistance2D(Point2D target) {
		return (float) Math.sqrt((x * target.x) + (z * target.z));
	}

}
