package engine.clientdata.visitors.terrainDetail.layers;

public abstract class FilterLayer extends Layer {

	protected int feather_type;
	protected float feather_amount;
	
	public int getFeatherType() {
		return feather_type;
	}
	
	public float getFeatherAmount() {
		return feather_amount;
	}
	
	
	
}
