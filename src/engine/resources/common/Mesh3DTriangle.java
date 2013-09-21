package engine.resources.common;

import engine.resources.scene.Point3D;

public class Mesh3DTriangle {
	
    Point3D[] points = new Point3D[3];
    
    public Mesh3DTriangle(Point3D point1, Point3D point2, Point3D point3) {
        points[0] = point1;
        points[1] = point2;
        points[2] = point3;
    }
    
    public Point3D getPointOne() {
        return points[0];
    }
    
    public Point3D getPointTwo() {
        return points[1];
    }
    
    public Point3D getPointThree() {
        return points[2];
    }
    
}
