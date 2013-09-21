package engine.resources.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sleepycat.persist.model.Persistent;

import engine.resources.objects.SWGObject;


@Persistent
public class ContainerSlot extends AbstractSlot {

		List<SWGObject> objects;
		public ContainerSlot(String name) {
			super(name);
			objects = new ArrayList<SWGObject>();
		}
		
		public ContainerSlot() { }
		
		@Override
		public boolean isFilled() {
			return false;
		}
		
		@Override
		public void insert(SWGObject swgObj) {
			objects.add(swgObj);
		}

		@Override
		public void traverse(SWGObject viewer, boolean topDown, boolean recursive, Traverser traverser) {
			for(SWGObject obj : objects) {
				
				if(recursive && !topDown) {
					obj.viewChildren(viewer, topDown, recursive, traverser);
				}
				
				traverser.process(obj);
				
				if(recursive && topDown) {
					obj.viewChildren(viewer, topDown, recursive, traverser);
				}
			}
		}

		@Override
		public void remove(SWGObject swgObj) {
			objects.remove(swgObj);
		}

		@Override
		public void clear() {
			objects.clear();
		}

		@Override
		public SWGObject getObject() {
			return null;
		}
		
	}
