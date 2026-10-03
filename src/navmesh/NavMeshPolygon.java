package navmesh;

import java.util.ArrayList;
import java.util.List;

public class NavMeshPolygon {

	 public final int id;

	    public final int vertexA;
	    public final int vertexB;
	    public final int vertexC;

	    public final float centerX;
	    public final float centerY;
	    public final float centerZ;

	    public final int area;

	    public final List<Integer> neighbors =
	            new ArrayList<>();

	    public NavMeshPolygon(
	            int id,
	            int vertexA,
	            int vertexB,
	            int vertexC,
	            float centerX,
	            float centerY,
	            float centerZ,
	            int area) {

	        this.id = id;

	        this.vertexA = vertexA;
	        this.vertexB = vertexB;
	        this.vertexC = vertexC;

	        this.centerX = centerX;
	        this.centerY = centerY;
	        this.centerZ = centerZ;

	        this.area = area;
	    }
	    
}
