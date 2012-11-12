package org.mccaughey.pathGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.graph.build.feature.FeatureGraphGenerator;
import org.geotools.graph.build.line.LineStringGraphGenerator;
import org.geotools.graph.path.AStarShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.AStarIterator.AStarFunctions;
import org.geotools.graph.traverse.standard.AStarIterator.AStarNode;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class PathGenerator {

  public Path shortestPath(SimpleFeatureSource networkSource) throws Exception {
    List<LineString> lines = nodeIntersections(networkSource.getFeatures());
    
    Graph graph = buildGraph(lines);
    Path shortest = findAStarShortestPath(graph, (Node) graph.getNodes()
        .toArray()[1], (Node) graph.getNodes().toArray()[2]);
    return shortest;
  }

  private List<LineString> nodeIntersections(SimpleFeatureCollection nonNodedNetwork) {
    SimpleFeatureIterator networkIterator = nonNodedNetwork.features();

    GeometryFactory geomFactory = new GeometryFactory();
    List<LineString> lines = new ArrayList<LineString>();

    while (networkIterator.hasNext()) {
      SimpleFeature edge = networkIterator.next();
      Geometry line = (Geometry) edge.getDefaultGeometry();
      for (int i = 0; i < line.getNumGeometries(); i++) {
        lines.add((LineString) line.getGeometryN(i));
      }
    }

    Geometry grandMls = geomFactory.buildGeometry(lines);
    Point mlsPt = geomFactory.createPoint(grandMls.getCoordinate());
    Geometry nodedLines = grandMls.union(mlsPt);

    lines.clear();

    for (int i = 0, n = nodedLines.getNumGeometries(); i < n; i++) {
      Geometry g = nodedLines.getGeometryN(i);
      if (g instanceof LineString) {
        lines.add((LineString) g);
      }
    }

    return lines;
  }

  private Graph buildGraph(List<LineString> lines)
      throws IOException {
    // create a linear graph generate
    LineStringGraphGenerator lineStringGen = new LineStringGraphGenerator();

    for(LineString line : lines) {
      lineStringGen.add(line);
    }
    
    return lineStringGen.getGraph();
  }
  
  private Graph buildGraph(SimpleFeatureSource networkSource)
      throws IOException {
    // get a feature collection somehow
    SimpleFeatureCollection fCollection = networkSource.getFeatures();

    // create a linear graph generate
    LineStringGraphGenerator lineStringGen = new LineStringGraphGenerator();

    // wrap it in a feature graph generator
    FeatureGraphGenerator featureGen = new FeatureGraphGenerator(lineStringGen);

    // throw all the features into the graph generator
    SimpleFeatureIterator iter = fCollection.features();
    try {
      while (iter.hasNext()) {
        Feature feature = iter.next();
        featureGen.add(feature);
      }
    } finally {
      iter.close();
    }
    return featureGen.getGraph();
  }

  private Path findAStarShortestPath(Graph graph, Node start, Node destination)
      throws Exception { // WrongPathException { <--- FIXME: WrongPathException
                         // is not visible
    // create a strategy for weighting edges in the graph
    // in this case we are using geometry length
    AStarFunctions asf = new AStarFunctions(destination) {

      @Override
      public double cost(AStarNode n1, AStarNode n2) {
        Coordinate coordinate1 = ((Point) n1.getNode().getObject())
            .getCoordinate();
        Coordinate coordinate2 = ((Point) n2.getNode().getObject())
            .getCoordinate();
        return coordinate1.distance(coordinate2);
      }

      @Override
      public double h(Node n) {
        Coordinate coordinate1 = ((Point) n.getObject()).getCoordinate();
        Coordinate coordinate2 = ((Point) this.getDest().getObject())
            .getCoordinate();
        return coordinate1.distance(coordinate2);
      }
    };
    // Create GraphWalker - in this case DijkstraShortestPathFinder
    AStarShortestPathFinder pf = new AStarShortestPathFinder(graph, start,
        destination, asf);
    pf.calculate();
    pf.finish();
    return pf.getPath();
  }

}
