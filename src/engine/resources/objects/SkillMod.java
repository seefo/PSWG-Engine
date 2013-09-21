package engine.resources.objects;

import com.sleepycat.persist.model.Persistent;

@Persistent
public class SkillMod {
	
	private String skillModString;
	private int base;
	private int modifier;

	
	public SkillMod() { }


	public String getSkillModString() {
		return skillModString;
	}


	public void setSkillModString(String skillModString) {
		this.skillModString = skillModString;
	}


	public int getBase() {
		return base;
	}


	public void setBase(int base) {
		this.base = base;
	}


	public int getModifier() {
		return modifier;
	}


	public void setModifier(int modifier) {
		this.modifier = modifier;
	}

}
