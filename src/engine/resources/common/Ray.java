package engine.resources.common;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import wblut.geom.WB_Distance;
import wblut.geom.WB_ExplicitTriangle;
import wblut.geom.WB_Intersection;
import wblut.geom.WB_Point3d;

import engine.resources.scene.Point3D;

public class Ray {
	
	Point3D origin = null;
    Vector3D dir = null;
    public static final float SMALL_NUM = 0.00000001f;
    
    public Ray(Point3D origin, Vector3D dir) {
        this.origin = origin;
        this.dir = dir;
    }
    
    public Point3D getOrigin() {
        return origin;
    }
    
    public Vector3D getDirection() {
        return dir;
    }

    public Point3D intersectsTriangle(Mesh3DTriangle triangle) {
    	
    	Point3D I = new Point3D();
        Vector3D    u, v, n;
        Vector3D    dir, w0, w, p1;
        float     r, a, b;
        
        p1 = new Vector3D(triangle.getPointOne().x, triangle.getPointOne().y, triangle.getPointOne().z);
        u = new Vector3D(triangle.getPointTwo().x, triangle.getPointTwo().y, triangle.getPointTwo().z);
        u = u.subtract(p1);
        v = new Vector3D(triangle.getPointThree().x, triangle.getPointThree().y, triangle.getPointThree().z);
        v = v.subtract(p1);
        n = Vector3D.crossProduct(u, v);
        
        if (n.getNorm() == 0) {
            return null;
        }
        
        dir = getDirection();
        w0 = new Vector3D(getOrigin().x, getOrigin().y, getOrigin().z);
        w0 = w0.subtract(p1);
        a = (float) -(new Vector3D(n.getX(), n.getY(), n.getZ()).dotProduct(w0));
        b = (float) new Vector3D(n.getX(), n.getY(), n.getZ()).dotProduct(dir);
        
        if ((float)Math.abs(b) < SMALL_NUM) {
            return null;
        }
        
        r = a / b;
        if (r < 0.0) {
            return null;
        }
        
        I = getOrigin().clone();
        I.x += r * dir.getX();
        I.y += r * dir.getY();
        I.z += r * dir.getZ();
        
        float    uu, uv, vv, wu, wv, D;
        
        uu = (float) Vector3D.dotProduct(u,u);
        uv = (float) Vector3D.dotProduct(u,v);
        vv = (float) Vector3D.dotProduct(v,v);
        w = new Vector3D(I.x - triangle.getPointOne().x, I.y - triangle.getPointOne().y, I.z - triangle.getPointOne().z);
        wu = (float) Vector3D.dotProduct(w,u);
        wv = (float) Vector3D.dotProduct(w,v);
        D = uv * uv - uu * vv;
        
        // get and test parametric coords
        float s, t;
        s = (uv * wv - vv * wu) / D;
        if (s < 0.0 || s > 1.0)         // I is outside T
            return null;
        t = (uv * wu - uu * wv) / D;
        if (t < 0.0 || (s + t) > 1.0)  // I is outside T
            return null;
        
        return I;          // I is in T

    	
    }
    
    public Point3D intersectsTriangle(Mesh3DTriangle triangle, float distance) {
    	
    	WB_Point3d closestPoint = WB_Intersection.closestPointToTriangle(new WB_Point3d(getOrigin().x, getOrigin().y, getOrigin().z), new WB_Point3d(triangle.getPointOne().x, triangle.getPointOne().y, triangle.getPointOne().z), new WB_Point3d(triangle.getPointTwo().x, triangle.getPointTwo().y, triangle.getPointTwo().z), new WB_Point3d(triangle.getPointThree().x, triangle.getPointThree().y, triangle.getPointThree().z));
    	if(new Point3D(getOrigin().x, getOrigin().y, getOrigin().z).getDistance(new Point3D((float) closestPoint.x, (float) closestPoint.y, (float) closestPoint.z)) > distance)
    		return null;
    	
    	Point3D I = new Point3D();
        Vector3D    u, v, n;
        Vector3D    dir, w0, w, p1;
        float     r, a, b;
        
        p1 = new Vector3D(triangle.getPointOne().x, triangle.getPointOne().y, triangle.getPointOne().z);
        u = new Vector3D(triangle.getPointTwo().x, triangle.getPointTwo().y, triangle.getPointTwo().z);
        u = u.subtract(p1);
        v = new Vector3D(triangle.getPointThree().x, triangle.getPointThree().y, triangle.getPointThree().z);
        v = v.subtract(p1);
        n = Vector3D.crossProduct(u, v);
        
        if (n.getNorm() == 0) {
            return null;
        }
        
        dir = getDirection();
        w0 = new Vector3D(getOrigin().x, getOrigin().y, getOrigin().z);
        w0 = w0.subtract(p1);
        a = (float) -(new Vector3D(n.getX(), n.getY(), n.getZ()).dotProduct(w0));
        b = (float) new Vector3D(n.getX(), n.getY(), n.getZ()).dotProduct(dir);
        
        if ((float)Math.abs(b) < SMALL_NUM) {
            return null;
        }
        
        r = a / b;
        if (r < 0.0) {
            return null;
        }
        
        I = getOrigin().clone();
        I.x += r * dir.getX();
        I.y += r * dir.getY();
        I.z += r * dir.getZ();
        
        float    uu, uv, vv, wu, wv, D;
        
        uu = (float) Vector3D.dotProduct(u,u);
        uv = (float) Vector3D.dotProduct(u,v);
        vv = (float) Vector3D.dotProduct(v,v);
        w = new Vector3D(I.x - triangle.getPointOne().x, I.y - triangle.getPointOne().y, I.z - triangle.getPointOne().z);
        wu = (float) Vector3D.dotProduct(w,u);
        wv = (float) Vector3D.dotProduct(w,v);
        D = uv * uv - uu * vv;
        
        // get and test parametric coords
        float s, t;
        s = (uv * wv - vv * wu) / D;
        if (s < 0.0 || s > 1.0)         // I is outside T
            return null;
        t = (uv * wu - uu * wv) / D;
        if (t < 0.0 || (s + t) > 1.0)  // I is outside T
            return null;
        
        return I;          // I is in T

    	
    }

}
