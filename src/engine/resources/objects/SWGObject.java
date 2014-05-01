package engine.resources.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.Descriptor;

import main.NGECore;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.SyncMessageBus;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.listener.Listener;

import org.apache.commons.collections.CollectionUtils;
import org.apache.mina.core.buffer.IoBuffer;

import protocol.Message;
import protocol.swg.SceneCreateObjectByCrc;
import protocol.swg.SceneDestroyObject;
import protocol.swg.SceneEndBaselines;
import protocol.swg.UpdateContainmentMessage;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import resources.objects.player.PlayerObject;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.NotPersistent;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;

import engine.clientdata.ClientFileManager;
import engine.clientdata.visitors.AppearanceVisitor;
import engine.clientdata.visitors.ComponentAppearanceVisitor;
import engine.clientdata.visitors.LevelOfDetailVisitor;
import engine.clientdata.visitors.MeshVisitor;
import engine.clientdata.visitors.ObjectVisitor;
import engine.clientdata.visitors.PortalVisitor;
import engine.clientdata.visitors.SlotArrangementVisitor;
import engine.clientdata.visitors.SlotDefinitionVisitor;
import engine.clientdata.visitors.SlotDescriptorVisitor;
import engine.clientdata.visitors.SlotDefinitionVisitor.SlotDefinition;
import engine.clients.Client;
import engine.resources.common.CRC;
import engine.resources.common.Event;
import engine.resources.container.AbstractSlot;
import engine.resources.container.AllPermissions;
import engine.resources.container.ContainerPermissions;
import engine.resources.container.ContainerSlot;
import engine.resources.container.ExclusiveSlot;
import engine.resources.container.Traverser;
import engine.resources.scene.Planet;
import engine.resources.scene.Point3D;
import engine.resources.scene.Quaternion;

@SuppressWarnings("unused")
@Persistent(version=1)
public abstract class SWGObject implements ISWGObject, Serializable {
	
	private static final long serialVersionUID = 1L;
	@PrimaryKey
	private long objectID;
	private long objectId; // store a second id as objects which are not stored directly will have their primary key overwritten when their container gets saved to DB
	@NotPersistent
	private transient Planet planet;
	@NotPersistent
	private transient SWGObject parent;	// we store the id only because we only need this loaded from db if we have a creature inside a cell
	private int planetId;
	private long parentId;
	private int containmentType;
	private Point3D position;
	private Quaternion orientation;
	private String template;
	private boolean isInSnapshot = false;
	private boolean isPersistent;
	private String stfName;
	private String stfFilename;
	private String detailName;
	private String detailFilename;
	private float collisionLength;
	private float collisionHeight;
	private boolean collidable;
	@NotPersistent
	private transient ObjectVisitor templateData;
	@NotPersistent
	protected transient SlotDescriptorVisitor slotDescriptor;
	@NotPersistent
	protected transient SlotArrangementVisitor slotArrangement;
	@NotPersistent
	private transient Client client;
	@NotPersistent
	private transient Set<Client> observers = Collections.synchronizedSet(new HashSet<Client>());
	@NotPersistent
	private transient List<SWGObject> awareObjects = Collections.synchronizedList(new ArrayList<SWGObject>());
	@NotPersistent
	protected transient final Object objectMutex = new Object();
	private AbstractSlot[] slots;
	private volatile boolean fetchedChildren;
	private ContainerPermissions permissions = AllPermissions.ALL_PERMISSIONS;
	private int arrangementId;
	private float complexity = 1;
	private String customName;
	private int volume = 1;
	private boolean isInQuadtree;
	@NotPersistent
	private transient int movementCounter;
	@NotPersistent
	private transient MeshVisitor meshVisitor;
	@NotPersistent
	private transient PortalVisitor portalVisitor;
	private Map<String, String> attributes = new LinkedHashMap<String, String>();
	private Map<String, Object> attachments = new HashMap<String, Object>();
	@NotPersistent
	private transient SyncMessageBus<Event> eventBus = new SyncMessageBus<Event>(NGECore.getInstance().getEventBusConfig());


	public SWGObject() { 
		loadAppearanceData();
		if(meshVisitor != null)
			meshVisitor.getTriangles();
	}
	
	public SWGObject(long objectID, Planet planet, Point3D position, Quaternion orientation, String Template) {
		this.objectID = objectID;
		this.objectId = objectID;
		this.planet = planet;
		this.template = Template;
		setPosition(position);
		setOrientation(orientation);
		getContainerInfo(Template);
		loadSTFInfo();
		loadDetailedDescription();
		loadAppearanceData();
		if(meshVisitor != null)
			meshVisitor.getTriangles();
	}
	
	private void loadAppearanceData() {
		
		if(getTemplateData() != null) {
			if(getTemplateData().getAttribute("portalLayoutFilename") != null && ((String) getTemplateData().getAttribute("portalLayoutFilename")).contains(".pob")) {
				loadPortalData();
			} else {
				if(getTemplateData().getAttribute("appearanceFilename") != null) {
					
					if(((String) getTemplateData().getAttribute("appearanceFilename")).contains(".lod")) {
						try {
							LevelOfDetailVisitor lodVisitor = ClientFileManager.loadFile((String) getTemplateData().getAttribute("appearanceFilename"), LevelOfDetailVisitor.class);
							if(lodVisitor == null)
								return;
							if(lodVisitor.getFirstMesh() != null && lodVisitor.getFirstMesh().contains(".msh")) {
								meshVisitor = ClientFileManager.loadFile("appearance/" + lodVisitor.getFirstMesh(), MeshVisitor.class);
							} else {
								System.out.println("NULL first mesh in .lod for: " + getTemplate());
							}
						} catch (InstantiationException | IllegalAccessException e) {
							e.printStackTrace();
						}
					} else if(((String) getTemplateData().getAttribute("appearanceFilename")).contains(".apt")) {
						try {
							AppearanceVisitor appVisitor = ClientFileManager.loadFile((String) getTemplateData().getAttribute("appearanceFilename"), AppearanceVisitor.class);
							if(appVisitor == null || appVisitor.getChildFilename() == null)
								return;
							if(appVisitor.getChildFilename().contains(".lod")) {
								LevelOfDetailVisitor lodVisitor = ClientFileManager.loadFile(appVisitor.getChildFilename(), LevelOfDetailVisitor.class);
								if(lodVisitor == null)
									return;
								if(lodVisitor.getFirstMesh() != null && !lodVisitor.getFirstMesh().contains(".cmp")) {
									meshVisitor = ClientFileManager.loadFile("appearance/" + lodVisitor.getFirstMesh(), MeshVisitor.class);
								} else if(lodVisitor.getFirstMesh() != null && lodVisitor.getFirstMesh().contains(".cmp")) {
									ComponentAppearanceVisitor cmpVisitor = ClientFileManager.loadFile("appearance/" + lodVisitor.getFirstMesh(), ComponentAppearanceVisitor.class);
									if(cmpVisitor == null)
										return;
									if(cmpVisitor.getFirstMesh() != null && cmpVisitor.getFirstMesh().endsWith(".lod")) {
										LevelOfDetailVisitor lodVisitor2 = ClientFileManager.loadFile("appearance/" + cmpVisitor.getFirstMesh(), LevelOfDetailVisitor.class);
										if(lodVisitor2 == null)
											return;
										if(lodVisitor.getFirstMesh() != null && !lodVisitor.getFirstMesh().contains(".msh")) {
											meshVisitor = ClientFileManager.loadFile("appearance/" + lodVisitor2.getFirstMesh(), MeshVisitor.class);
										}
									}
								}
								return;
							}

							meshVisitor = ClientFileManager.loadFile(appVisitor.getChildFilename(), MeshVisitor.class);
							
						} catch (InstantiationException | IllegalAccessException e) {
							e.printStackTrace();
						}
					} else if(((String) getTemplateData().getAttribute("appearanceFilename")).contains(".msh")) {
						try {
							meshVisitor = ClientFileManager.loadFile((String) getTemplateData().getAttribute("appearanceFilename"), MeshVisitor.class);
						} catch (InstantiationException | IllegalAccessException e) {
							e.printStackTrace();
						}
					}
					
				}
			}
		}
		
	}
	
	private void loadPortalData() {
		
		try {
			portalVisitor = ClientFileManager.loadFile((String) getTemplateData().getAttribute("portalLayoutFilename"), PortalVisitor.class);
			if(portalVisitor.cells.isEmpty())
				return;
			String fileName = portalVisitor.cells.get(0).mesh;

			if(fileName.contains(".msh"))
				meshVisitor = ClientFileManager.loadFile(fileName, MeshVisitor.class);
			else if(fileName.contains(".lod")) {
				LevelOfDetailVisitor lodVisitor = ClientFileManager.loadFile(fileName, LevelOfDetailVisitor.class);
				if(lodVisitor == null)
					return;
				if(lodVisitor.getFirstMesh() != null && lodVisitor.getFirstMesh().contains(".msh")) {
					meshVisitor = ClientFileManager.loadFile("appearance/" + lodVisitor.getFirstMesh(), MeshVisitor.class);
				}
			} else if(fileName.contains(".apt")) {
				AppearanceVisitor appVisitor = ClientFileManager.loadFile(fileName, AppearanceVisitor.class);
				if(appVisitor == null || appVisitor.getChildFilename() == null)
					return;
				if(appVisitor.getChildFilename().contains(".lod")) {
					LevelOfDetailVisitor lodVisitor = ClientFileManager.loadFile(appVisitor.getChildFilename(), LevelOfDetailVisitor.class);
					if(lodVisitor == null)
						return;
					if(lodVisitor.getFirstMesh() != null && lodVisitor.getFirstMesh().contains(".msh")) {
						meshVisitor = ClientFileManager.loadFile("appearance/" + lodVisitor.getFirstMesh(), MeshVisitor.class);
					}
					return;
				}
				meshVisitor = ClientFileManager.loadFile(appVisitor.getChildFilename(), MeshVisitor.class);
			}
			
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}

	}

	private void loadSTFInfo() {

		if(getTemplateData() != null) {
			
			if(getTemplateData().getAttribute("stfFilename") == null)
				return;
			
			String stfFilename = (String) getTemplateData().getAttribute("stfFilename");
			String stfName = (String) getTemplateData().getAttribute("stfName");
			setStfFilename(stfFilename.replace("", ""));
			setStfName(stfName.replace("", ""));

		}
		
	}
	
	private void loadDetailedDescription() {
		
		if(getTemplateData() != null) {

			if(getTemplateData().getAttribute("detailFilename") == null)
				return;

			String detailFilename = (String) getTemplateData().getAttribute("detailFilename");
			String detailName = (String) getTemplateData().getAttribute("detailName");
			setDetailFilename(detailFilename.replace("", ""));
			setDetailName(detailName.replace("", ""));
			
		}
		
	}

	public void getContainerInfo(String template) {

		try {
			
			setTemplateData(ClientFileManager.loadFile(template, ObjectVisitor.class));
			getTemplateData().setName(template);
			if((String)getTemplateData().getAttribute("arrangementDescriptorFilename") == null)
				return;
			slotArrangement = ClientFileManager.loadFile((String)getTemplateData().getAttribute("arrangementDescriptorFilename"), SlotArrangementVisitor.class);

			// Now we need to check if we have a slotted container or not...
			
			String slotDescriptorFilename = (String)getTemplateData().getAttribute("slotDescriptorFilename");
			
			if(slotDescriptorFilename != null && slotDescriptorFilename.length() > 0) {
				// container with slots
				slotDescriptor = ClientFileManager.loadFile(slotDescriptorFilename, SlotDescriptorVisitor.class);
				SlotDefinitionVisitor slotDefinitions = ClientFileManager.loadFile("abstract/slot/slot_definition/slot_definitions.iff", SlotDefinitionVisitor.class);

				if(slots == null) {
					slots = new AbstractSlot[slotDescriptor.getAvailableSlots().size()];
					
					//Prep the array
					int i=0;
					for(String slotName : slotDescriptor.getAvailableSlots()) {
						SlotDefinition def = slotDefinitions.getDefinitions().get(slotName);
						slots[i] = new ExclusiveSlot(def.slotName);
						++i;
					}
				}
			} else {
				//NOT SLOTTED CONTAINER
				//Prep the children
				if(slots == null) {
					slots = new AbstractSlot[1];
					slots[0] = new ContainerSlot("default");
				}
			}


		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
	}
	
	public SlotArrangementVisitor getArrangement() { return slotArrangement; }

	public long getObjectID() {
		if(objectID == 0)
			return objectId;
		return objectID;
	}
	
	public long getObjectId() { return objectId; }
	
	public int getPlanetId() {
		synchronized(objectMutex) {
			return planetId;
		}
	}
	
	public void setPlanetId(int planetId) {
		synchronized(objectMutex) {
			this.planetId = planetId;
		}
	}
	
	public Planet getPlanet() {
		synchronized(objectMutex) {
			return planet;
		}
	}
	
	public void setPlanet(Planet planet) {
		synchronized(objectMutex) {
			this.planet = planet;
		}
	}
		
	public Set<Client> getObservers() {
		return observers;
	}
	
	public SWGObject getContainer() {
		synchronized(objectMutex) {
			return parent;
		}
	}
	
	public void setParent(SWGObject parent) {
		synchronized(objectMutex) {
			this.parent = parent;
		}
	}	
	
	public void setPosition(Point3D newPosition) {
		synchronized(objectMutex) {
			position = newPosition;
		}
	}

	public void setOrientation(Quaternion newOrientation) {
		synchronized(objectMutex) {
			orientation = newOrientation;
		}
	}
	
	public boolean hasObservers() { return !observers.isEmpty(); }
	
	public boolean isInQuadtree() { 
		return isInQuadtree; 
	}
	
	public void setIsInQuadtree(boolean isInQuadtree) {
		this.isInQuadtree = isInQuadtree;
	}

	
	public Point3D getPosition() { 
		synchronized(objectMutex) {
			return position; 
		}
	}
	
	public Quaternion getOrientation() { 
		synchronized(objectMutex) {
			return orientation; 
		}
	}
	
	public String getStfName() {
		synchronized(objectMutex) {
			return stfName; 
		}
	}
	
	public void setStfName(String stfName) {
		synchronized(objectMutex) {
			this.stfName = stfName; 
		}
	}
	
	public String getStfFilename() {
		synchronized(objectMutex) {
			return stfFilename; 
		}
	}
	
	public void setStfFilename(String stfFilename) {
		synchronized(objectMutex) {
			this.stfFilename = stfFilename; 
		}
	}
	
	public String getDetailName() {
		synchronized(objectMutex) {
			return detailName; 
		}
	}
	
	public void setDetailName(String detailName) {
		synchronized(objectMutex) {
			this.detailName = detailName; 
		}
	}
	
	public String getDetailFilename() {
		synchronized(objectMutex) {
			return detailFilename; 
		}
	}
	
	public void setDetailFilename(String detailFilename) {
		synchronized(objectMutex) {
			this.detailFilename = detailFilename; 
		}
	}
	
	public long getParentId() {
		synchronized(objectMutex) {
			return parentId; 
		}
	}
	
	public void setParentId(long parentId) {
		synchronized(objectMutex) {
			this.parentId = parentId; 
		}
	}
	
	public float getComplexity() {
		synchronized(objectMutex) {
			return complexity; 
		}
	}
	
	public void setParentId(float complexity) {
		synchronized(objectMutex) {
			this.complexity = complexity; 
		}
	}
	
	public String getCustomName() {
		synchronized(objectMutex) {
			return customName;
		}
	}

	public void setCustomName(String customName) {
		synchronized(objectMutex) {
			this.customName = customName;
		}
	}

	public int getVolume() {
		synchronized(objectMutex) {
			return volume;
		}
	}
	
	public void setVolume(int volume) {
		synchronized(objectMutex) {
			this.volume = volume;
		}
	}

	public int getMovementCounter() {
		synchronized(objectMutex) {
			return movementCounter; 
		}
	}
	
	public void setMovementCounter(int movementCounter) {
		synchronized(objectMutex) {
			this.movementCounter = movementCounter; 
		}
	}
	
	public String getTemplate() { return template; }

	public void setTemplate(String template) { this.template = template; }
	
	public Client getClient() { return client; }
	
	public void setClient(Client client) { this.client = client; }

	public boolean isPersistent() { return isPersistent; }
	
	public boolean isInSnapshot() { return isInSnapshot; }

	public void setPersistent(boolean isPersistent) { this.isPersistent = isPersistent; }

	public void setisInSnapshot(boolean isInSnapshot) { this.isInSnapshot = isInSnapshot; }
	
	public List<SWGObject> getAwareObjects() { return awareObjects; }

	public float getRadians() {
		
		Quaternion orientation = getOrientation().clone();
		float w = orientation.w;
		float y = orientation.y;
		float angle;
		
		if(w * w + y * y > 0) {
			if(w > 0 && y < 0)
				w *= -1;
			angle = (float) (2 * Math.acos(w));
		} else {
			angle = 0;
		}
			
		return angle;
	}
	
	public float getHeading() {
		
		Quaternion orientation = getOrientation().clone();
		float heading = 0.f;
		
		float invert = (float) Math.sqrt(1 - (orientation.w * orientation.w));
		float w = orientation.w;
		float y = orientation.y;

		if(invert != 0) {
			
			if(w > 0 && y < 0) {
				w *= -1;
				y *= -1;
			}

			float rad = (float) (2 * Math.acos(w));
			float t = rad /*/ 0.06283f*/;
			heading = (y / invert) * t;
		}
		
		return heading;
	}
	
	public boolean inRange(Point3D other, float range) {
		if(getWorldPosition().getDistance2D(other) <= range)
			return true;
			
		return false;
	}
	
	public void makeAware(final SWGObject obj) {
		
		if(awareObjects.contains(obj) || !obj.getPermissions().canView(this, obj)) {
			//System.out.println("Already aware of: " + obj.getTemplate());
			return;
		}
		
		if(this != obj) {
			awareObjects.add(obj);
			obj.addObserver(this);
		}
		if(!obj.isInSnapshot()) {
			obj.sendCreate(getClient());
			obj.sendBaselines(getClient());
			//obj.sendSceneEndBaselines(getClient());
		}

	//	if(obj.getSlottedObject("appearance_inventory") != null)
	//		makeAware(obj.getSlottedObject("appearance_inventory"));
	//	if(obj.getSlottedObject("ghost") != null)
	//		makeAware(obj.getSlottedObject("ghost"));
		//if(!obj.isInSnapshot() && !(obj instanceof BuildingObject))
		//	obj.sendSceneEndBaselines(getClient());
				
		obj.viewChildren(this, true, false, new Traverser() {

			@Override
			public void process(SWGObject object) {
				if(object == null)
					return;
				//if(object instanceof PlayerObject && obj != SWGObject.this)
				//	return;
				if(object.getClient() != null && object != SWGObject.this)
					object.makeAware(SWGObject.this);
				makeAware(object);
			}
				
		});

		if(!obj.isInSnapshot() /*&& obj instanceof BuildingObject*/)
			obj.sendSceneEndBaselines(getClient());

	}
	
	public void makeUnaware(final SWGObject obj) {
		
		if(!awareObjects.contains(obj) || obj == this || !obj.getObservers().contains(getClient()))
			return;
		
		/*if(getObjectID() == obj.getObjectID()) {
			try {
				throw new Exception("Trying to make an object unaware of itself");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}*/
		
		obj.viewChildren(this, false, false, new Traverser() {

			@Override
			public void process(SWGObject object) {
				if(object == null)
					return;
				if(object.getClient() != null)
					object.makeUnaware(SWGObject.this);
				makeUnaware(object);
			}
				
		});
		if(!obj.isInSnapshot())
			obj.sendDestroy(getClient());
		
		obj.removeObserver(this);
		awareObjects.remove(obj);

	}
	
	public void addObserver(SWGObject object) {
		
		if(object.getClient() != null) {
			observers.add(object.getClient());
		}
		
	}
	
	public void removeObserver(SWGObject object) {
		
		if(object.getClient() != null) {
			observers.remove(object.getClient());
		}
		
	}
	
	public void sendCreate(Client destination) {
		
		if(this instanceof BuildingObject && getAttachment("cellsSorted") == null && slots[0] != null && slots[0] instanceof ContainerSlot) {
			((ContainerSlot) slots[0]).sortCells();
			((ContainerSlot) slots[0]).inverse();
			setAttachment("cellsSorted", new Boolean(true));
		}
		
		if(destination == null || destination.getSession() == null)
			return;
		Quaternion quat = getOrientation();
		Point3D pos = getPosition();
		//if(destination != getClient()) {
			SceneCreateObjectByCrc create = new SceneCreateObjectByCrc(getObjectID(), quat.x, quat.y, quat.z, quat.w, pos.x, pos.y, pos.z, CRC.StringtoCRC(template), (byte) 0);
			destination.getSession().write(create.serialize());
		//}
		sendUpdateContainment(destination);
		
	}
	
	public void sendSceneEndBaselines(Client destination) {
		
		if(destination == null || destination.getSession() == null)
			return;
		SceneEndBaselines sceneEndBaselines = new SceneEndBaselines(getObjectID());
		destination.getSession().write(sceneEndBaselines.serialize());

	}
	
	public void sendDestroy(Client destination) {
				
		if(destination == null || destination.getSession() == null || destination == getClient())
			return;
		SceneDestroyObject sceneDestroy = new SceneDestroyObject(getObjectID());
		destination.getSession().write(sceneDestroy.serialize());

	}
	
	
	/**
	 * Notifies all observers of the packet.
	 * @param message The packet.
	 * @param updateSelf If true this object will also recieve the packet.
	 */
	public void notifyObservers(Message message, boolean updateSelf) {
		
		if(observers.isEmpty() && !updateSelf)
			return;
		
		IoBuffer data = message.serialize();
		
		if(updateSelf && client != null && client.getSession() != null)
			client.getSession().write(data);
		
		synchronized(objectMutex) {
			for(Client client : observers) {
				if(client != null && client.getSession() != null)
					client.getSession().write(data);
			}
		}
		
	}
	
	/**
	 * Notifies all observers of the packet.
	 * @param message The packet.
	 * @param updateSelf If true this object will also recieve the packet.
	 */
	public void notifyObservers(IoBuffer message, boolean updateSelf) {
		
		if(observers.isEmpty() && !updateSelf)
			return;
				
		if(updateSelf && client != null && client.getSession() != null)
			client.getSession().write(message);
		
		synchronized(objectMutex) {
			for(Client client : observers) {
				if(client != null && client.getSession() != null)
					client.getSession().write(message);
			}
		}
		
	}

	/**
	 * Notifies all observers of this object in range of the message passed in the arguments.
	 * @param message The message/packet that is being broadcasted.
	 * @param updateSelf If true this object will also recieve the packet.
	 * @param range The maximum range for the broadcast.
	 */
	public void notifyObserversInRange(Message message, boolean updateSelf, float range) {
		
		IoBuffer data = message.serialize();
		
		if(updateSelf && client != null && client.getSession() != null)
			client.getSession().write(data);
		
		if(observers.isEmpty() && !updateSelf)
			return;
		
		HashSet<Client> observers = new HashSet<Client>(this.observers);
		
		for(Client client : observers) {
			float distance = client.getParent().getPosition().getDistance2D(position);
			if(client != null && client.getSession() != null && distance <= range) {
				client.getSession().write(data);
			}
		}
		
	}

	
	// this is ANHs algorithm, might not work for new NGE arrangement Ids
	public int getCorrectArrangementId(SWGObject other) {
		
		if(slotDescriptor == null)
			return -1;
		
		int arrangementId = 4;
		int filledArrangementId = -1;
		
		for(List<Integer> arrangement : other.getArrangement().getArrangement()) {
			boolean passesCompletely = true;
			boolean isValid = true;
			for(Integer i : arrangement) {
				Integer slot = slotDescriptor.getIndexOf(i); 
				if(slot == null) {
					isValid = false;
					break;
				} else if(slots[slot].isFilled()) {
					passesCompletely = false;
				}
			}
			if(isValid && passesCompletely) {
				return arrangementId;
			} else if(isValid) {
				filledArrangementId = arrangementId;
			}
			++arrangementId;
		}
		return (filledArrangementId != -1) ? filledArrangementId : 0;
		
	}
	
	public boolean add(final SWGObject object) {
		fetchChildren();
			
		//Do the insert
		_add(object);
			
		//Notify observers
		for(Client c : observers) {
			object.makeAware(c.getParent());
			c.getParent().makeAware(object);
		}
		
		
		final Client client = object.getClient();
		if(client != null) {
			//Make sure the object has seen everything!!
			
			//This next part works because the top level container always returns
			//itself as its parent. This may seem kinda icky, but the other options are
			//worse.
			SWGObject topLevelParent = this;
			SWGObject lastButNotLeast = this;
			while(topLevelParent != null && topLevelParent.getObjectID() != 0) {
				lastButNotLeast = topLevelParent;
				topLevelParent = topLevelParent.getContainer();
			}
			
			if(topLevelParent == null) {
				return true;
			}
			
			//Now we work our way down from the top.
			topLevelParent.viewChildren((SWGObject)lastButNotLeast, true, true, new Traverser() {
				@Override
				public void process(SWGObject t) {
					object.makeAware(t);
					t.makeAware(client.parent);
				}
			});
		}
		
		return true;
	}
	
	public boolean remove(final SWGObject object) {
		fetchChildren();
			
		//Do the remove
		_remove(object);
			
		//Notify observers
		for(Client c : observers) {
			c.makeUnaware(object);
		}
		makeUnaware(object);
		
		/*final Client client = object.getClient();
		if(client != null) {
			//Make sure the object has seen everything!!
			
			//This next part works because the top level container always returns
			//itself as its parent. This may seem kinda icky, but the other options are
			//worse.
			SWGObject topLevelParent = this;
			SWGObject lastButNotLeast = this;
			while(topLevelParent != null && topLevelParent.getObjectID() != 0) {
				lastButNotLeast = topLevelParent;
				topLevelParent = topLevelParent.getContainer();
			}
			
			if(topLevelParent == null) {
				return true;
			}
			
			//Now we work our way down from the top.
			topLevelParent.traverseChildren((SWGObject)lastButNotLeast, true, true, new TreeVisitor() {
				@Override
				public void process(SWGObject t) {
					object.makeAware(t);
				}
			});
		}*/
		
		return true;
	}
	
	/**
	 * Transfers an object out of this container into another.
	 * @param requester The requester of the transfer.
	 * @param otherContainer The container which recieves the object.
	 * @param object The object being transfered.
	 */
	public boolean transferTo(SWGObject requester, SWGObject otherContainer, SWGObject object) {
		
		/*System.out.println("Transfer: " + object.getTemplate() + " Arrangement ID: " + object.getArrangementId() + " Correct Arrangement ID: " + otherContainer.getCorrectArrangementId(object)
				+ "Old Container: " + getTemplate() + " New Container: " + otherContainer.getTemplate()
				);*/
		
		if(!permissions.canRemove(requester, this) || !otherContainer.getPermissions().canInsert(requester, otherContainer))
			return false;
		
		fetchChildren();
		
		Collection<Client> oldObservers, updateObservers, newObservers;
		
		oldObservers = new HashSet<Client>(object.getObservers());
		if(getClient() != null)
			oldObservers.add(getClient());
		_remove(object);
		object.setParent(this); // hacky solution
		newObservers = new HashSet<Client>(otherContainer.getObservers());
		if(otherContainer.getClient() != null)
			newObservers.add(otherContainer.getClient());
		otherContainer._add(object);
		
		
		updateObservers = CollectionUtils.intersection(oldObservers, newObservers);
		
		if(requester.getClient() != null && !updateObservers.contains(requester.getClient()))
			updateObservers.add(requester.getClient());
		
		for(Client c : oldObservers) {
			if(!updateObservers.contains(c)) {
				c.makeUnaware(object);
			}
		}
		
		for(Client c : updateObservers) {
			object.sendUpdateContainment(c);
		}
		
		for(Client c : newObservers) {
			if(!updateObservers.contains(c)) {
				c.makeAware(object);
			}
		}
		//System.out.println("Transfer complete New Parent: " + object.getContainer().getTemplate() + "Arrangement ID: " + object.getArrangementId());
		return false;
	}

	public void sendUpdateContainment(Client client) {

		if(getParentId() == 0)
			return;
		
		if(client == null || client.getSession() == null)
			return;
		
		int arrangementId = getArrangementId();
		if(arrangementId == 0)
			arrangementId = 4;
	
		UpdateContainmentMessage ucm = new UpdateContainmentMessage(getObjectID(), getParentId(), arrangementId);
		
		client.getSession().write(ucm.serialize());
		//System.out.println("UCM sent for: " + getTemplate() + "Arrangement Id: " + arrangementId);

	}
	
	public int getArrangementId() {
		synchronized(objectMutex) {
			return arrangementId;
		}
	}
	
	public void setArrangementId(int id) { 
		synchronized(objectMutex) {
			this.arrangementId = id; 
		}
	} 

	public boolean _remove(SWGObject object) {
		fetchChildren();
		if((slotDescriptor == null || slotArrangement == null) && template != null)
			getContainerInfo(getTemplate());
		if(object.getArrangementId() == -1) {
			synchronized(objectMutex) {
				slots[0].remove(object);
			}
		} else {
			synchronized(objectMutex) {
				for(Integer i : object.slotArrangement.getArrangement().get(object.arrangementId - 4)) {
					slots[slotDescriptor.getIndexOf(i)].remove(object);
				}
			}
		}
		object.setArrangementId(-1);
		object.setParent(null);
		object.setParentId(0);
		
		return true;
	}
	
	public boolean _add(SWGObject object) {
		fetchChildren();
		if((slotDescriptor == null || slotArrangement == null) && template != null)
			getContainerInfo(getTemplate());

		//Get the proper arrangement
		int arrangementId = getCorrectArrangementId(object);
		SWGObject oldContainer = object.getContainer();
		if(arrangementId == -1) {
			synchronized(objectMutex) {
				slots[0].insert(object);
				object.setArrangementId(arrangementId);
				object.setParent(this);
				object.setParentId(getObjectID());
			}
		} else if(arrangementId > 0){	
			synchronized(objectMutex) {
				for(Integer i : object.slotArrangement.getArrangement().get(arrangementId - 4)) {
					Integer slotIndex = slotDescriptor.getIndexOf(i);
					if(this.slots[slotIndex].isFilled() && object.getContainer() != null) {
						if(oldContainer != slots[slotIndex].getObject().getContainer() && object.getContainer() != slots[slotIndex].getObject())
							transferTo(this, object.getContainer(), this.slots[slotIndex].getObject());
						/*this.slots[slotIndex].traverse(null, true, false, new TreeVisitor() {
							@Override
							public void process(SWGObject object) {
								_remove(object);
							}
						});*/
					}
					this.slots[slotIndex].insert(object);
					//System.out.println(slots[slotIndex].getName());
				}
			}
			
			object.setArrangementId(arrangementId);
			object.setParent(this);
			object.setParentId(getObjectID());
			
		} else {
			System.out.println("Found bad arrangement Id for: " + object.getTemplate() + " parent: " + getTemplate() + "arrangement Id: " + arrangementId);
		}
		
		return true;
	}
	
	public String getSlotNameForObject(SWGObject object) {
		if((slotDescriptor == null || slotArrangement == null) && template != null)
			getContainerInfo(getTemplate());
		int arrangementId = getCorrectArrangementId(object);
		synchronized(objectMutex) {
			for(Integer i : object.slotArrangement.getArrangement().get(arrangementId - 4)) {
				Integer slotIndex = slotDescriptor.getIndexOf(i);
				return slots[slotIndex].getName();
			}
		}
		return null;
	}
	
	public Vector<String> getSlotNamesForObject(SWGObject object) {
		if((slotDescriptor == null || slotArrangement == null) && template != null)
			getContainerInfo(getTemplate());
		Vector<String> slotNames = new Vector<String>();
		int arrangementId = getCorrectArrangementId(object);
		synchronized(objectMutex) {
			for(Integer i : object.slotArrangement.getArrangement().get(arrangementId - 4)) {
				Integer slotIndex = slotDescriptor.getIndexOf(i);
				slotNames.add(slots[slotIndex].getName());
			}
		}
		return slotNames;
	}
	

	/**
	 * Traverses through the children of these objects with by implementing the Traverser interface with an anonymous class.
	 * @param viewer The object that is viewing the children(important for permissions)
	 * @param topDown If true, objects that are at the top of the tree are viewed first.
	 * @param recursive If true, the child objects of every child objects will be traversed.
	 * @param tv The traverser interface.
	 */
	public void viewChildren(SWGObject viewer, boolean topDown, boolean recursive, Traverser tv) {
		if(getPermissions().canView(viewer, this)) {
			fetchChildren();
			
			for(AbstractSlot slot : slots) {
				slot.traverse(viewer, topDown, recursive, tv);
			}
		}
	}
	
	private void fetchChildren() {
		if(!fetchedChildren) {
			fetchedChildren = true;
		}
	}
	
	public ContainerPermissions getPermissions() { 
		synchronized(objectMutex) {
			return permissions;
		}
	}
	
	public void setContainerPermissions(ContainerPermissions permissions) {
		synchronized(objectMutex) {
			this.permissions = permissions;
		}
	}
	
	/**
	 * Gets the Mutex of this object for locking.
	 * @return
	 */
	public Object getMutex() { return objectMutex; }

	
	public abstract void sendBaselines(Client client);
	
	/**
	 * Gets a child object of this object based on the slot name, for example "inventory" for the inventory object.
	 * @param slotName The name of the slot defined by the client files.
	 * @return The Object occupying the slot, or null if no object occupies the slot.
	 */
	public SWGObject getSlottedObject(String slotName) {
		
		for(AbstractSlot slot : slots) {
			if(!(slot instanceof ExclusiveSlot))
				continue;
			ExclusiveSlot _slot = (ExclusiveSlot) slot;
			if(slot.getName().equals(slotName))
				return _slot.getObject();
		}
		
		return null;
	}
	/**
	 * Computes the World Position of an object that is located in a cell.
	 * @return The World Position of this object.
	 */
	public Point3D getWorldPosition() {
		if(parent == null || parent.getContainer() == null)
			return getPosition();
		else {
			Point3D cellPos = getPosition();
			Point3D buildingPos = parent.getContainer().getPosition();
			float length = (float) Math.sqrt(cellPos.x * cellPos.x + cellPos.z * cellPos.z);
			float angle = (float) (parent.getContainer().getRadians() + Math.atan2(cellPos.x, cellPos.z));
			return new Point3D(buildingPos.x + (float) (Math.sin(angle) * length), buildingPos.y + cellPos.y,  buildingPos.z + (float) (Math.cos(angle) * length));
		}
	}

	public ObjectVisitor getTemplateData() {
		return templateData;
	}

	public void setTemplateData(ObjectVisitor templateData) {
		this.templateData = templateData;
	}
	/**
	 * Checks if this object is a child of the object passed in the argument.
	 * @param object The parent object.
	 * @return true if this object is a child of the object passed in the argument, false if it isn't.
	 */
	public boolean isSubChildOf(SWGObject object) {
		
		final AtomicBoolean found = new AtomicBoolean(false);
		object.viewChildren(this, true, true, new Traverser() {

			@Override
			public void process(SWGObject obj) {
				
				if(obj == SWGObject.this)
					found.set(true);
				
			}
			
		});
		return found.get();
		
	}
	/**
	 * Equivalent to getContainer().getContainer() but with null checks to prevent errors
	 * @return getContainer().getContainer() or null if the parent does not have a parent itself 
	 */
	public SWGObject getGrandparent() {
		
		if(getContainer() != null && getContainer().getContainer() != null)
			return getContainer().getContainer();
		
		return null;
		
	}
	
	public MeshVisitor getMeshVisitor() { return meshVisitor; }
	
	public PortalVisitor getPortalVisitor() { return portalVisitor; }
	
	public String getStringAttribute(String attributeName) {
		synchronized(objectMutex) {
			return attributes.get(attributeName);
		}
	}
	
	public int getIntAttribute(String attributeName) {
		synchronized(objectMutex) {
			return Integer.parseInt(attributes.get(attributeName));
		}
	}

	public float getFloatAttribute(String attributeName) {
		synchronized(objectMutex) {
			return Float.parseFloat(attributes.get(attributeName));
		}
	}
	
	public void setStringAttribute(String attributeName, String value) {
		synchronized(objectMutex) {
			attributes.put(attributeName, value);
		}
	}
	
	public void setIntAttribute(String attributeName, int value) {
		synchronized(objectMutex) {
			attributes.put(attributeName, String.valueOf(value));
		}
	}

	public void setFloatAttribute(String attributeName, float value) {
		synchronized(objectMutex) {
			attributes.put(attributeName, String.valueOf(value));
		}
	}
	
	public Map<String, String> getAttributes() {
		return attributes;
	}

	public Object getAttachment(String attachmentName) {
		synchronized(objectMutex) {
			return attachments.get(attachmentName);
		}
	}
	
	public void setAttachment(String attachmentName, Object value) {
		synchronized(objectMutex) {
			attachments.put(attachmentName, value);
		}
	}
	
	public SyncMessageBus<Event> getEventBus() {
		return eventBus;
	}

}
