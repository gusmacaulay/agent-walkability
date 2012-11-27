package org.mccaughey.pathGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

// The Java class will be hosted at the URI path "/helloworld"
@Path("/agent-paths")
public class PathGeneratorResource {
  static final Logger LOGGER = LoggerFactory.getLogger(PathGeneratorResource.class);
  // The Java method will process HTTP GET requests
  @GET
  // The Java method will produce content identified by the MIME Media
  // type "text/plain"
  @Produces("application/json")
  public Response getPaths() throws Exception {
    // Hardcoded inputs for now
    File networkShapeFile = new File("src/test/resources/graph.shp");
    FileDataStore networkDataStore = FileDataStoreFinder
        .getDataStore(networkShapeFile);
    //System.out.println(networkDataStore.getInfo().toString());
    SimpleFeatureSource networkSource = networkDataStore.getFeatureSource();
    //
    File destinationsFile = new File("src/test/resources/random_destinations.shp");
    FileDataStore destinationsDataStore = FileDataStoreFinder
        .getDataStore(destinationsFile);
    SimpleFeatureIterator features = destinationsDataStore.getFeatureSource()
        .getFeatures().features();
    List<Point> destinations = new ArrayList();
    while (features.hasNext()) {
      SimpleFeature feature = features.next();
      destinations.add((Point) feature.getDefaultGeometry());
    }
    
    //
    double easting = 285752.0;
    double northing = 5824386.0;
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(),
        28355);
    Point start = geometryFactory.createPoint(new Coordinate(easting, northing));
    
    File file = new File("all_path_nodes.json");
    List<org.geotools.graph.path.Path> paths = PathGenerator.shortestPaths(networkSource, start, destinations,file);
    LOGGER.info("Paths Generated");
    return Response.ok().entity(new FileInputStream(file)).build();
  }
}