package org.mccaughey.pathGenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
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

public class PathProcessor {

	static final Logger LOGGER = LoggerFactory.getLogger(PathProcessor.class);

	private static final double GEOMETRY_PRECISION = 100;
	private static PrecisionModel precision = new PrecisionModel(
			GEOMETRY_PRECISION);

	private PathProcessor() {
	}

	public static SimpleFeatureCollection processPathNodes(List<Path> paths,
			int stepTime, CoordinateReferenceSystem crs)
			throws GeneratedOutputEmptyException {

		SimpleFeatureType featureType = createPathFeatureType(crs);
		GeometryFactory geometryFactory = new GeometryFactory(precision);
		List<SimpleFeature> featuresList = new ArrayList<SimpleFeature>();
		int pathID = 0;
		for (Path path : paths) {
			pathID++;
			Node currentNode = path.getFirst();

			long walkTime = 0; // The epoch in unix time

			List<String> unvisited = new ArrayList<String>();
			for (Edge edge : (List<Edge>) path.getEdges()) {
				unvisited.add(String.valueOf(edge.getID()));
			}

			featuresList = processEdges(unvisited, currentNode,
					geometryFactory, featureType, walkTime, pathID,
					featuresList, stepTime);

		}
		SimpleFeatureCollection pathFeatures = DataUtilities
				.collection(featuresList);
		return pathFeatures;
	}

	private static List<SimpleFeature> processEdges(List<String> unvisited,
			Node startNode, GeometryFactory geometryFactory,
			SimpleFeatureType featureType, long walkTime, int pathID,
			List<SimpleFeature> featuresList, int stepTime) {

		int maxTime = 1200000;
		
		LineString line;
		Node currentNode = startNode;
		int crossings = 0;
		int walkDistance = 0;
		while ((unvisited.size() > 0)&&(walkTime <= maxTime)) {
			if (currentNode.getEdges().size() > 0) {
				int intersectionWait = 0;
				if (currentNode.getEdges().size() > 2) {
					// This is an intersection - therefore delay for crossing
					intersectionWait = 30000;
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
							for (int index = 0; index < lil.getEndIndex(); index += 25) {
								Coordinate coordinate = lil.extractPoint(index);
								Point point = geometryFactory
										.createPoint(coordinate);
								SimpleFeature feature = buildTimeFeatureFromGeometry(
										featureType, point, walkTime,crossings,walkDistance,
										String.valueOf(pathID));
								walkDistance += 25; 
								walkTime += stepTime + intersectionWait;
								intersectionWait = 0; // only perform wait once
								featuresList.add(feature);
							}
						} else if (lil.project(pt) == lil.getEndIndex()) {
							// start coordinate is at the end of the line
							for (int index = (int) lil.getEndIndex(); index >= 0; index -= 25) {
								Coordinate coordinate = lil.extractPoint(index);
								Point point = geometryFactory
										.createPoint(coordinate);
								SimpleFeature feature = buildTimeFeatureFromGeometry(
										featureType, point, walkTime,crossings,walkDistance,
										String.valueOf(pathID));
								walkDistance += 25; 
								walkTime += stepTime + intersectionWait;
								intersectionWait = 0; // only perform wait once
								featuresList.add(feature);
							}
						} else {
							LOGGER.error("Start coordinate did not match with Index!");
						}
						unvisited.remove(String.valueOf(edge.getID()));

						Node nextNode = edge.getOtherNode(currentNode);
						if (nextNode != null)
							currentNode = nextNode;
						else {
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
