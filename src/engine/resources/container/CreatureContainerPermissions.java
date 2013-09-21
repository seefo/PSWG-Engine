package engine.resources.container;

import com.sleepycat.persist.model.Persistent;

import engine.resources.objects.SWGObject;

@Persistent
public class CreatureContainerPermissions implements ContainerPermissions {
	
	public final static CreatureContainerPermissions CREATURE_CONTAINER_PERMISSIONS = new CreatureContainerPermissions();
	
	public CreatureContainerPermissions() {}
	
	@Override
	public boolean canInsert(SWGObject requester, SWGObject container) {
		
		if(container.getContainer() == requester || container.getGrandparent() == requester || requester == container) 
			return true;
		if(container.getGrandparent() != null && container.getGrandparent().getContainer() == requester)
			return true;
		return false;
	}

	@Override
	public boolean canRemove(SWGObject requester, SWGObject container) {
		if(container.getContainer() == requester || container.getGrandparent() == requester || requester == container) 
			return true;
		if(container.getGrandparent() != null && container.getGrandparent().getContainer() == requester)
			return true;
		return false;
	}

	@Override
	public boolean canView(SWGObject requester, SWGObject container) {
		if(container.getContainer() == requester || container.getGrandparent() == requester || requester == container) 
			return true;
		if(container.getGrandparent() != null && container.getGrandparent().getContainer() == requester)
			return true;
		return false;
	}


}
