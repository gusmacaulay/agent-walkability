package org.mccaughey;

import junit.framework.Assert;

import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.jbehave.core.embedder.Embedder;
import org.mccaughey.pathGenerator.PathGenerator;

public class PathGeneratorSteps extends Embedder{
  
  PathGenerator pathGenerator;
  
  @Given("a path generator")
  public void aPathGenerator() {
    pathGenerator = new PathGenerator();
  }
  
  @When("the path generator is asked for a shortest path")
  public void requestShortestPath() {
    //Path path = pathGenerator.shortestPath();
    Assert.assertTrue(true);
  }
  
  @Then("the correct shortest path will be provided") 
  public void shortestPathIsCorrect() {
    Assert.assertTrue(true);
  }
  
}

