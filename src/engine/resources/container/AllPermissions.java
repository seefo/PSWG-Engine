package engine.resources.container;

import com.sleepycat.persist.model.Persistent;

import engine.resources.objects.SWGObject;


@Persistent
public class AllPermissions implements ContainerPermissions {

	public final static AllPermissions ALL_PERMISSIONS = new AllPermissions();
	
	public AllPermissions() {}
	
	@Override
	public boolean canInsert(SWGObject actor, SWGObject actee) {
		return true;
	}

	@Override
	public boolean canRemove(SWGObject actor, SWGObject actee) {
		return true;
	}

	@Override
	public boolean canView(SWGObject actor, SWGObject actee) {
		return true;
	}

}
