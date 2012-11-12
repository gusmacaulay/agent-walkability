package org.mccaughey;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.geotools.data.shapefile.*;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.graph.path.Path;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.jbehave.core.embedder.Embedder;
import org.mccaughey.pathGenerator.PathGenerator;

public class PathGeneratorSteps extends Embedder {

  PathGenerator pathGenerator;
  SimpleFeatureSource networkSource;
  Path path;

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
   // ShapefileDataStore store = new ShapefileDataStore(networkShapeFile.toURI().toURL());
   // System.out.println("SHAPEFILE INFO: ");
   // System.out.println("PATH: " + networkShapeFile);
    System.out.println(networkDataStore.getInfo().toString());
    networkSource = networkDataStore.getFeatureSource();
  }

  @Given("a startpoint")
  public void aStartPoint() {

  }

  @Given("an endpoint")
  public void anEndPoint() {

  }

  @When("the path generator is asked for a shortest path")
  public void requestShortestPath() throws Exception {
    path = pathGenerator.shortestPath(networkSource);
    // Assert.assertTrue(true);
  }

  @Then("the correct shortest path will be provided")
  public void shortestPathIsCorrect() {
    Assert.assertTrue(path.isValid());
    System.out.println("EDGES: " + path.getEdges().size());
    //Assert.assertTrue(path != null);
  }

}
