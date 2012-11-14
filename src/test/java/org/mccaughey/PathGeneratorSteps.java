package org.mccaughey;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.jbehave.core.embedder.Embedder;
import org.mccaughey.pathGenerator.PathGenerator;

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
    File networkShapeFile = new File("/home/amacaulay/SRC/agent-walkability/"
        + "non-free-data/Footpaths_simple.shp");
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
    double easting = 286080;
    double northing = 5821102;
    start = geometryFactory.createPoint(new Coordinate(easting, northing));
  }

  @Given("an endpoint")
  public void anEndPoint() {
    double easting = 286074.21;
    double northing = 5821166.81;
    end = geometryFactory.createPoint(new Coordinate(easting, northing));

  }

  @When("the path generator is asked for a shortest path")
  public void requestShortestPath() throws Exception {
    path = pathGenerator.shortestPath(networkSource, start, end);
    // Assert.assertTrue(true);
  }

  @Then("the correct shortest path will be provided")
  public void shortestPathIsCorrect() {
    Assert.assertTrue(path.isValid());
    System.out.println("EDGES: " + path.getEdges().size());
    for (Edge edge : (List<Edge>) path.getEdges()) {
      LineString line = (LineString) edge.getObject();
      System.out.println(line.toText());
    }
    // Assert.assertTrue(path != null);
  }

}
