package org.mccaughey.pathGenerator;

import java.util.ArrayList;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

public final class PathProcessor {

	static final Logger LOGGER = LoggerFactory.getLogger(PathProcessor.class);

	private static final double GEOMETRY_PRECISION = 100;
	private static PrecisionModel precision = new PrecisionModel(
			GEOMETRY_PRECISION);

	private PathProcessor() {
	}

	public static SimpleFeatureCollection processPathNodes(List<Path> paths,
			double stepTime, int maxTime, int intersectionWait, int stepDistance, CoordinateReferenceSystem crs)
			throws GeneratedOutputEmptyException {

		SimpleFeatureType featureType = createPathFeatureType(crs);
		GeometryFactory geometryFactory = new GeometryFactory(precision);
		List<SimpleFeature> featuresList = new ArrayList<SimpleFeature>();
		int pathID = 0;
		for (Path path : paths) {
			pathID++;
			Node currentNode = path.getFirst();

			//Walk time in milliseconds since the epoch (unix time)
			long walkTime = 0; 

			List<String> unvisited = new ArrayList<String>();
			for (Edge edge : (List<Edge>) path.getEdges()) {
				unvisited.add(String.valueOf(edge.getID()));
			}

			featuresList = processEdges(unvisited, currentNode,
					geometryFactory, featureType, walkTime, pathID,
					featuresList, stepTime, maxTime, intersectionWait, stepDistance);

		}
		SimpleFeatureCollection pathFeatures = DataUtilities
				.collection(featuresList);
		return pathFeatures;
	}

	private static List<SimpleFeature> processEdges(List<String> unvisited,
			Node startNode, GeometryFactory geometryFactory,
			SimpleFeatureType featureType, long startTime, int pathID,
			List<SimpleFeature> featuresList, double stepTime, int maxTime, int intersectionWait, int stepDistance) {

		
		
		LineString line;
		Node currentNode = startNode;
		int crossings = 0;
		int walkDistance = 0;
		long walkTime = startTime;
		while ((unvisited.size() > 0)&&(walkTime <= maxTime)) {
			if (currentNode.getEdges().size() > 0) {
				int crossingWait = 0;
				if (currentNode.getEdges().size() > 2) {
					// This is an intersection - therefore delay for crossing
					crossingWait = intersectionWait;
					crossings++;
				}
				for (Edge edge : (List<Edge>) currentNode.getEdges()) {
					if (unvisited.contains(String.valueOf(edge.getID()))) {
						line = (LineString) edge.getObject();

						Coordinate pt = ((Point) currentNode.getObject())
								.getCoordinate();

						LengthIndexedLine lil = new LengthIndexedLine(line);

						if (lil.project(pt) == lil.getStartIndex()) {
							// start coordinate is at start of line
							for (int index = 0; index < lil.getEndIndex(); index += stepDistance) {
								Coordinate coordinate = lil.extractPoint(index);
								Point point = geometryFactory
										.createPoint(coordinate);
								SimpleFeature feature = buildTimeFeatureFromGeometry(
										featureType, point, walkTime,crossings,walkDistance,
										String.valueOf(pathID));
								walkDistance += stepDistance; 
								walkTime += stepTime + crossingWait;
								crossingWait = 0; // only perform wait once
								featuresList.add(feature);
							}
						} else if (lil.project(pt) == lil.getEndIndex()) {
							// start coordinate is at the end of the line
							for (int index = (int) lil.getEndIndex(); index >= 0; index -= stepDistance) {
								Coordinate coordinate = lil.extractPoint(index);
								Point point = geometryFactory
										.createPoint(coordinate);
								SimpleFeature feature = buildTimeFeatureFromGeometry(
										featureType, point, walkTime,crossings,walkDistance,
										String.valueOf(pathID));
								walkDistance += stepDistance; 
								walkTime += stepTime + crossingWait;
								crossingWait = 0; // only perform wait once
								featuresList.add(feature);
							}
						} else {
							LOGGER.error("Start coordinate did not match with Index!");
						}
						unvisited.remove(String.valueOf(edge.getID()));

						Node nextNode = edge.getOtherNode(currentNode);
						if (nextNode != null) {
							currentNode = nextNode;
						} else {
							break;
						}

					}
				}
			}
		}
		return featuresList;
	}

	private static SimpleFeature buildTimeFeatureFromGeometry(
			SimpleFeatureType featureType, Geometry geom, long walkTime,
			int crossings,int walkDistance, String pathID) {
		SimpleFeatureTypeBuilder stb = new SimpleFeatureTypeBuilder();
		stb.init(featureType);
		SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(featureType);
		sfb.add(geom);
		sfb.add(walkTime);
		sfb.add(crossings);
		sfb.add(walkDistance);
		sfb.add(pathID);
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
		builder.add("when", Integer.class);
		builder.add("crossings", Integer.class);
		builder.add("walk_dist", Integer.class);
		builder.add("path_id", String.class);

		// build the type
		SimpleFeatureType pathType = builder.buildFeatureType();

		return pathType;
	}
}
