package org.mccaughey;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import junit.framework.Assert;

import org.geotools.data.DataUtilities;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.jbehave.core.embedder.Embedder;
import org.mccaughey.pathGenerator.PathGenerator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

public class PathGeneratorSteps extends Embedder {

  PathGenerator pathGenerator;
  SimpleFeatureSource networkSource;
  Path path;
  Point start, end;
  GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(),28355);
  
  @Given("a path generator")
  public void aPathGenerator() {
    pathGenerator = new PathGenerator();
  }

  @Given("a footpath network")
  public void aFootpathNetwork() throws IOException {
//    File networkShapeFile = new File("/home/amacaulay/SRC/agent-walkability/"
//        + "non-free-data/Footpaths_simple.shp");
    File networkShapeFile = new File(getClass().getResource("/melton_roads_sample.shp").getFile());
//      URL footpathURL = getClass().getResource("/melton_road_sample.geojson");
//      System.out.println("URL:" + footpathURL);
//      networkSource = DataUtilities.source(readFeatures(footpathURL));
    FileDataStore networkDataStore = FileDataStoreFinder
        .getDataStore(networkShapeFile);
    // ShapefileDataStore store = new
    // ShapefileDataStore(networkShapeFile.toURI().toURL());
    // System.out.println("SHAPEFILE INFO: ");
    // System.out.println("PATH: " + networkShapeFile);
    System.out.println(networkDataStore.getInfo().toString());
    networkSource = networkDataStore.getFeatureSource();
  }

  @Given("a startpoint")
  public void aStartPoint() {
    double easting = 285752;
    double northing = 5824386;
    start = geometryFactory.createPoint(new Coordinate(easting, northing));
  }

  @Given("an endpoint")
  public void anEndPoint() {
    double easting = 285545;
    double northing = 5825011;
    end = geometryFactory.createPoint(new Coordinate(easting, northing));

  }
  
  @Given("a set of endpoints")
  public void aSetOfEndPoints() {
    double easting = 285545;
    double northing = 5825011;
    end = geometryFactory.createPoint(new Coordinate(easting, northing));

  }

  @When("the path generator is asked for the shortest path/s")
  public void requestShortestPath() throws Exception {
    path = pathGenerator.shortestPath(networkSource, start, end);
    // Assert.assertTrue(true);
  }

  @Then("the correct shortest path/s will be provided")
  public void shortestPathIsCorrect() {
    Assert.assertTrue(path.isValid());
  }
  
  @Then("the path/s will have timestamps")
  public void shortestPathHasTimeStamps() {
    System.out.println("EDGES: " + path.getEdges().size());
    for (Edge edge : (List<Edge>) path.getEdges()) {
      LineString line = (LineString) edge.getObject();
      System.out.println(line.toText());
    }
    // Assert.assertTrue(path != null);
  }
  
  private static SimpleFeatureCollection readFeatures(URL url)
      throws IOException {
    FeatureJSON io = new FeatureJSON();
    // io.setEncodeFeatureCollectionCRS(true);

    // io.readCRS(url.openConnection().getInputStream()));
    FeatureIterator<SimpleFeature> features = io.streamFeatureCollection(url
        .openConnection().getInputStream());
    SimpleFeatureCollection collection = FeatureCollections.newCollection();

    while (features.hasNext()) {
      SimpleFeature feature = (SimpleFeature) features.next();
      collection.add(feature);
    }

    return collection;
  }

}
