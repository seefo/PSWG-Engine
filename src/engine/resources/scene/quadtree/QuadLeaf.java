package engine.resources.scene.quadtree;

import java.util.Vector;

public class QuadLeaf<T> {
	public final float x;
	public final float y;
	public final Vector<T> values;

	public QuadLeaf(float x, float y, T value) {
		this.x = x;
		this.y = y;
		this.values = new Vector<T>(1);
		this.values.add(value);
	}
}