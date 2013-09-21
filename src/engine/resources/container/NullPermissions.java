package engine.resources.container;

import com.sleepycat.persist.model.Persistent;

import engine.resources.objects.SWGObject;

@Persistent
public class NullPermissions implements ContainerPermissions {

	public final static NullPermissions NULL_PERMISSIONS = new NullPermissions();
	
	public NullPermissions() {}
	
	@Override
	public boolean canInsert(SWGObject actor, SWGObject actee) {
		return false;
	}

	@Override
	public boolean canRemove(SWGObject actor, SWGObject actee) {
		return false;
	}

	@Override
	public boolean canView(SWGObject actor, SWGObject actee) {
		return false;
	}

}
