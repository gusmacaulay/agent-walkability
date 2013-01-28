package org.mccaughey.pathGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
//import org.springframework.core.env.Environment;



@Controller
@RequestMapping("/agent-paths")
public class PathGeneratorResource {
  static final Logger LOGGER = LoggerFactory.getLogger(PathGeneratorResource.class);
  
//  @Autowired
//  private Environment env;
  // The Java method will process HTTP GET requests
  //@GET
  // The Java method will produce content identified by the MIME Media
  // type "text/plain"
  //@Produces("application/json")
  
  @RequestMapping(value = "/{easting}/{northing}", method = RequestMethod.GET)
  public void getPaths(HttpServletResponse response, @PathVariable String easting, @PathVariable String northing) throws Exception {
    // Hardcoded inputs for now
//    File networkShapeFile = new File("src/test/resources/graph.shp");
//    FileDataStore networkDataStore = FileDataStoreFinder
//        .getDataStore(networkShapeFile);
//    SimpleFeatureSource networkSource = networkDataStore.getFeatureSource();
    
    SimpleFeatureSource networkSource = getDataSource("walkability:melton_roads_sample");
    //
    File destinationsFile = new File("src/test/resources/random_destinations.shp");
    FileDataStore destinationsDataStore = FileDataStoreFinder
        .getDataStore(destinationsFile);
    SimpleFeatureIterator features = destinationsDataStore.getFeatureSource()
        .getFeatures().features();
    List<Point> destinations = new ArrayList<Point>();
    while (features.hasNext()) {
      SimpleFeature feature = features.next();
      destinations.add((Point) feature.getDefaultGeometry());
    }
    
    //
   // double eastingD= 285752.0;
   // double northingD = 5824386.0;
    GeometryFactory geometryFactory2 = new GeometryFactory(new PrecisionModel(0));
    //283308.0178542186 5902355.348786879
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(0));
    Point start = geometryFactory.createPoint(new Coordinate(Double.parseDouble(easting), Double.parseDouble(northing)));
   // Point comparison = geometryFactory2.createPoint(new Coordinate(eastingD, northingD));
   // LOGGER.info("Comparising Geom" + comparison.toString());
    CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:3857"); //same as 900913
    CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:28355");

    MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
    Point targetGeometry = (Point) JTS.transform( start, transform);
    
    LOGGER.info("Converted geom: " + targetGeometry.toString());
    
    File file = new File("all_path_nodes.json"); //TODO: create in memory
    List<org.geotools.graph.path.Path> paths = PathGenerator.shortestPaths(networkSource, targetGeometry, destinations,file);
    LOGGER.info("Paths Generated");
    FileCopyUtils.copy(new FileInputStream(file), response.getOutputStream());
    return;
  }
  
  private SimpleFeatureSource getDataSource(String typeName) throws IOException {
      String getCapabilities = "http://localhost:8080/geoserver/ows?service=wfs&version=1.0.0&request=GetCapabilities";
//    String getCapabilitiesWFS = env.getProperty("wfs.url");
    //	"getfeature&typename=shireofmelton:footpaths_meltons";

    Map<String, String> connectionParameters = new HashMap<String, String>();
    connectionParameters.put("WFSDataStoreFactory:GET_CAPABILITIES_URL", getCapabilities);

    // Step 2 - connection
    DataStore wfsStore = DataStoreFinder.getDataStore( connectionParameters );
    if (wfsStore == null)
	LOGGER.info("Null Data Store");
    String typeNames[] = wfsStore.getTypeNames();
    LOGGER.info(typeNames[0]);
    //typeName = typeNames[0];
    SimpleFeatureType schema = wfsStore.getSchema( typeName );

    // Step 4 - target
    SimpleFeatureSource source = wfsStore.getFeatureSource( typeName );
    System.out.println( "Metadata Bounds:"+ source.getBounds() );
    LOGGER.debug(wfsStore.toString());
  
    return source;
    //return source;

  }
}
