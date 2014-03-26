package engine.resources.container;

import com.sleepycat.persist.model.Persistent;

import engine.resources.objects.SWGObject;

@Persistent
public class WorldCellPermissions implements ContainerPermissions {
	
	public final static WorldCellPermissions WORLD_CELL_PERMISSIONS = new WorldCellPermissions();
	
	public WorldCellPermissions() {}

	@Override
	public boolean canInsert(SWGObject requester, SWGObject container) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canRemove(SWGObject requester, SWGObject container) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canView(SWGObject requester, SWGObject container) {
		// TODO Auto-generated method stub
		return true;
	}

}
