package org.mccaughey.pathGenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.graph.build.line.LineStringGraphGenerator;
import org.geotools.graph.path.AStarShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.AStarIterator.AStarFunctions;
import org.geotools.graph.traverse.standard.AStarIterator.AStarNode;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

public class PathGenerator {

  private static final int GEOMETRY_PRECISION = 100;
  private static final int INTERSECTION_THRESHOLD = 3;
  static final Logger LOGGER = LoggerFactory.getLogger(PathGenerator.class);
  private static final Double MAX_SNAP_DISTANCE = 50.0;
  private static PrecisionModel precision = new PrecisionModel(
      GEOMETRY_PRECISION);

  public Path shortestPath(SimpleFeatureSource networkSource, Point start,
      Point end) throws Exception {
    List<LineString> lines = nodeIntersections(networkSource.getFeatures());

    // Graph graph = buildGraph(lines);
    LineStringGraphGenerator lineStringGen = createGraphWithAdditionalNodes(
        lines, start, end);
    Node startNode = lineStringGen.getNode(start.getCoordinate());
    Node endNode = lineStringGen.getNode(end.getCoordinate());
    LOGGER.info("Start Node: " + startNode.toString());
    LOGGER.info("End Node: " + endNode.toString());
    Graph graph = lineStringGen.getGraph();
    LOGGER.info("GRAPH: " + graph);

    Path shortest = findAStarShortestPath(graph, startNode, endNode);
    writePathFromEdges(shortest.getEdges(), networkSource.getSchema());
    CoordinateReferenceSystem crs = new CoordinateReferenceSystem();
    
    writePathAsNodes(shortest.getEdges(), networkSource.getSchema().getCoordinateReferenceSystem());
    return shortest;
  }

  private static LineStringGraphGenerator createGraphWithAdditionalNodes(
      List<LineString> lines, Point startingPoint, Point endPoint)
      throws IOException {

    LocationIndexedLine startConnectedLine = findNearestEdgeLine(lines,
        MAX_SNAP_DISTANCE, startingPoint);
    LocationIndexedLine endConnectedLine = findNearestEdgeLine(lines,
        MAX_SNAP_DISTANCE, endPoint);
    // create a linear graph generator
    LineStringGraphGenerator lineStringGen = new LineStringGraphGenerator();

    LOGGER.info("Lines Count: " + lines.size());

    lines = addConnectingLine(startingPoint, lines, startConnectedLine);
    lines = addConnectingLine(endPoint, lines, endConnectedLine);
    lines = nodeIntersections(lines);
    for (LineString line : lines) {
      lineStringGen.add(line);
    }
    LOGGER.info("Lines Count: " + lines.size());

    return lineStringGen;

  }

  private static List<LineString> addConnectingLine(Point newPoint,
      List<LineString> lines, LocationIndexedLine connectedLine) {
    Coordinate pt = newPoint.getCoordinate();
    LinearLocation here = connectedLine.project(pt);
    Coordinate minDistPoint = connectedLine.extractPoint(here);
    LineString lineA = (LineString) connectedLine.extractLine(
        connectedLine.getStartIndex(), connectedLine.project(minDistPoint));
    LineString lineB = (LineString) connectedLine.extractLine(
        connectedLine.project(minDistPoint), connectedLine.getEndIndex());
    LineString originalLine = (LineString) connectedLine.extractLine(
        connectedLine.getStartIndex(), connectedLine.getEndIndex());

    GeometryFactory geometryFactory = new GeometryFactory(precision);
    LineString newConnectingLine = geometryFactory
        .createLineString(new Coordinate[] { pt, minDistPoint });

    // lineStringGen.add(newConnectingLine);
    lines.add(newConnectingLine);
    if (!((lineB.getLength() == 0.0) || (lineA.getLength() == 0.0))) {
      removeLine(lines, originalLine);

      lineA = geometryFactory.createLineString(lineA.getCoordinates());
      lineB = geometryFactory.createLineString(lineB.getCoordinates());
      lines.add(lineA);
      lines.add(lineB);
      // lineStringGen.add(lineA);
      // lineStringGen.add(lineB);
    }
    // return lineStringGen;
    return lines;
  }

  private static LocationIndexedLine findNearestEdgeLine(
      List<LineString> lines, Double maxDistance, Point pointOfInterest)
      throws IOException {
    // Build network Graph - within bounds
    SpatialIndex index = createLineStringIndex(lines);

    Coordinate pt = pointOfInterest.getCoordinate();
    Envelope search = new Envelope(pt);
    search.expandBy(maxDistance);

    /*
     * Query the spatial index for objects within the search envelope. Note that
     * this just compares the point envelope to the line envelopes so it is
     * possible that the point is actually more distant than MAX_SEARCH_DISTANCE
     * from a line.
     */
    List<LocationIndexedLine> linesIndexed = index.query(search);

    // Initialize the minimum distance found to our maximum acceptable
    // distance plus a little bit
    double minDist = maxDistance;// + 1.0e-6;
    Coordinate minDistPoint = null;
    LocationIndexedLine connectedLine = null;

    for (LocationIndexedLine line : linesIndexed) {

      LinearLocation here = line.project(pt);
      Coordinate point = line.extractPoint(here);
      double dist = point.distance(pt);
      if (dist <= minDist) {
        minDist = dist;
        minDistPoint = point;
        connectedLine = line;
      }
    }

    if (minDistPoint != null) {
      LOGGER.debug("{} - snapped by moving {}\n", pt.toString(), minDist);
      return connectedLine;
    }
    return null;
  }

  private static void removeLine(List<LineString> lines, Geometry originalLine) {

    for (LineString line : lines) {
      if ((line.equals(originalLine))) {
        lines.remove(line);
        return;
      }
    }
  }

  private static void writePathFromEdges(List<Edge> edges,
      SimpleFeatureType featureType) {
    List<SimpleFeature> featuresList = new ArrayList();
    for (Edge edge : edges) {
      SimpleFeature feature = buildFeatureFromGeometry(featureType,
          (Geometry) edge.getObject());
      featuresList.add(feature);
    }
    File file = new File("path.json");
    writeFeatures(DataUtilities.collection(featuresList), file);
  }

  private static void writePathAsNodes(List<Edge> edges,
      CoordinateReferenceSystem crs) {
    SimpleFeatureType featureType = createPathFeatureType(crs);
    GeometryFactory geometryFactory = new GeometryFactory(precision);
    List<SimpleFeature> featuresList = new ArrayList();
    int unix_time = (int)(System.currentTimeMillis() / 1000L);
    for (Edge edge : edges) {
      LineString line = (LineString) edge.getObject();
      for (Coordinate coordinate : line.getCoordinates()) {
        Point point = geometryFactory.createPoint(coordinate);
        SimpleFeature feature = buildTimeFeatureFromGeometry(featureType,point,unix_time);
        unix_time+=10;
        featuresList.add(feature);
      }
    }

    File file = new File("path_nodes.json");
    writeFeatures(DataUtilities.collection(featuresList), file);
  }

  private static SimpleFeature buildTimeFeatureFromGeometry(
      SimpleFeatureType featureType, Geometry geom, int unix_time) {
    SimpleFeatureTypeBuilder stb = new SimpleFeatureTypeBuilder();
    stb.init(featureType);
    SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(featureType);
    sfb.add(geom);
    sfb.add(unix_time);
    return sfb.buildFeature(null);
  }
  
  private static SimpleFeatureType createPathFeatureType(
      CoordinateReferenceSystem crs) {

    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
    builder.setName("Buffer");
    if (crs != null) {
      builder.setCRS(crs); // <- Coordinate reference system
    }

    // add attributes in order
    builder.add("geometry", Point.class);
    builder.add("when", String.class);

    // build the type
    SimpleFeatureType pathType = builder.buildFeatureType();

    return pathType;
  }

  private static void writeNetworkFromEdges(HashSet<Edge> edges,
      SimpleFeatureType featureType) {
    List<SimpleFeature> featuresList = new ArrayList();
   
   
    for (Edge edge : edges) {
      SimpleFeature feature = buildFeatureFromGeometry(featureType,
          (Geometry) edge.getObject());
      featuresList.add(feature);
    }
    File file = new File("graph.json");
    writeFeatures(DataUtilities.collection(featuresList), file);
  }


  
  private static SimpleFeature buildFeatureFromGeometry(
      SimpleFeatureType featureType, Geometry geom) {
    SimpleFeatureTypeBuilder stb = new SimpleFeatureTypeBuilder();
    stb.init(featureType);
    SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(featureType);
    sfb.add(geom);
    return sfb.buildFeature(null);
  }

  private static void writeFeatures(SimpleFeatureCollection features, File file) {
    FeatureJSON fjson = new FeatureJSON();
    OutputStream os;
    try {
      os = new FileOutputStream(file);
      try {
        if (features.getSchema().getCoordinateReferenceSystem() != null) {
          fjson.setEncodeFeatureCollectionBounds(true);
          fjson.setEncodeFeatureCollectionCRS(true);
        } else {
          LOGGER.info("CRS is null");
        }
        fjson.writeFeatureCollection(features, os);
      } finally {
        os.close();
      }
    } catch (FileNotFoundException e1) {
      LOGGER.error("Failed to write feature collection " + e1.getMessage());
    } catch (IOException e) {
      LOGGER.error("Failed to write feature collection " + e.getMessage());
    }
  }

  private static SpatialIndex createLineStringIndex(List<LineString> lines)
      throws IOException {
    SpatialIndex index = new STRtree();

    // Create line string index
    // Just in case: check for null or empty geometry
    for (LineString line : lines) {
      if (line != null) {
        Envelope env = line.getEnvelopeInternal();
        if (!env.isNull()) {
          index.insert(env, new LocationIndexedLine(line));
        }
      }
    }

    return index;
  }

  private List<LineString> nodeIntersections(
      SimpleFeatureCollection nonNodedNetwork) {
    SimpleFeatureIterator networkIterator = nonNodedNetwork.features();

    List<LineString> lines = new ArrayList<LineString>();

    while (networkIterator.hasNext()) {
      SimpleFeature edge = networkIterator.next();
      Geometry line = (Geometry) edge.getDefaultGeometry();
      for (int i = 0; i < line.getNumGeometries(); i++) {
        lines.add((LineString) line.getGeometryN(i));
      }
    }

    return nodeIntersections(lines);

  }

  private static List<LineString> nodeIntersections(List<LineString> rawLines) {
    List<LineString> lines = new ArrayList<LineString>();

    for (LineString line : rawLines) {
      for (int i = 0; i < line.getNumGeometries(); i++) {
        lines.add((LineString) line.getGeometryN(i));
      }
    }

    GeometryFactory geomFactory = new GeometryFactory(precision);
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

  private Graph buildGraph(List<LineString> lines) throws IOException {
    // create a linear graph generate
    LineStringGraphGenerator lineStringGen = new LineStringGraphGenerator();

    for (LineString line : lines) {
      lineStringGen.add(line);
    }

    return lineStringGen.getGraph();
  }

  private Path findAStarShortestPath(Graph graph, Node start, Node destination)
      throws Exception { // WrongPathException { <--- FIXME: WrongPathException
                         // is not visible
    // create a cost function and heuristic for A-Star
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

    AStarShortestPathFinder pf = new AStarShortestPathFinder(graph, start,
        destination, asf);
    pf.calculate();
    pf.finish();
    return pf.getPath();
  }
}
