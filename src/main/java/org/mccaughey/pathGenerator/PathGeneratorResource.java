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
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
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
  
  @RequestMapping(method = RequestMethod.GET)
  public void getPaths(HttpServletResponse response) throws Exception {
    // Hardcoded inputs for now
//    File networkShapeFile = new File("src/test/resources/graph.shp");
//    FileDataStore networkDataStore = FileDataStoreFinder
//        .getDataStore(networkShapeFile);
//    //System.out.println(networkDataStore.getInfo().toString());
//    SimpleFeatureSource networkSource = networkDataStore.getFeatureSource();
//    
    SimpleFeatureSource networkSource = getDataSource("shireofmelton:footpaths_meltons");
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
    
    File file = new File("all_path_nodes.json"); //TODO: create in memory
    List<org.geotools.graph.path.Path> paths = PathGenerator.shortestPaths(networkSource, start, destinations,file);
    LOGGER.info("Paths Generated");
    FileCopyUtils.copy(new FileInputStream(file), response.getOutputStream());
    return;
  }
  
  private SimpleFeatureSource getDataSource(String typeName) throws IOException {
    String getCapabilitiesWFS = "http://192.43.209.39:8080/geoserver/ows?service=wfs&version=1.0.0&request=GetCapabilities";
//    String getCapabilitiesWFS = env.getProperty("wfs.url");
    //	"getfeature&typename=shireofmelton:footpaths_meltons";

    Map connectionParameters = new HashMap();
    connectionParameters.put("WFSDataStoreFactory:GET_CAPABILITIES_URL", getCapabilitiesWFS);

    // Step 2 - connection
    DataStore data = DataStoreFinder.getDataStore( connectionParameters );

    // Step 3 - discouvery
    String typeNames[] = data.getTypeNames();
    
    SimpleFeatureType schema = data.getSchema( typeNames[0] );

    // Step 4 - target
    SimpleFeatureSource source = data.getFeatureSource( typeNames[0] );
   
    return source;

  }
}
