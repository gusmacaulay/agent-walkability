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
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.geotools.referencing.crs.DefaultGeographicCRS;
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

public class PathWriter {
	
	static final Logger LOGGER = LoggerFactory.getLogger(PathWriter.class);
	private static final double GEOMETRY_PRECISION = 100;
	private static PrecisionModel precision = new PrecisionModel(
			GEOMETRY_PRECISION);
	
	public static SimpleFeatureCollection writePathNodes(List<Path> paths,
			CoordinateReferenceSystem crs, File file)
			throws GeneratedOutputEmptyException {
		SimpleFeatureType featureType = createPathFeatureType(crs);
		GeometryFactory geometryFactory = new GeometryFactory(precision);
		List<SimpleFeature> featuresList = new ArrayList<SimpleFeature>();
		int pathID = 0;
		for (Path path : paths) {
			pathID++;
			Node currentNode = path.getFirst();

			long unixTime = 0; // The epoch

			List<String> unvisited = new ArrayList<String>();
			for (Edge edge : (List<Edge>) path.getEdges()) {
				unvisited.add(String.valueOf(edge.getID()));
			}
			while (unvisited.size() > 0) {
				if (currentNode.getEdges().size() > 0) {
					featuresList = processEdges(unvisited, currentNode,
							geometryFactory, featureType, unixTime, pathID,
							featuresList);
				}
			}
		}
		SimpleFeatureCollection pathFeatures = DataUtilities
				.collection(featuresList);
		CoordinateReferenceSystem crs_target = DefaultGeographicCRS.WGS84;
		LOGGER.info(crs.toWKT());

		boolean outputisNotEmpty = pathFeatures.size() > 0;
		if (outputisNotEmpty) {
			ReprojectingFeatureCollection rfc = new ReprojectingFeatureCollection(
					pathFeatures, crs_target);

			writeFeatures(rfc, file);
			LOGGER.info("GeoJSON writing complete");
			return rfc;
		} else {
			LOGGER.info("Generated output is empty");
			throw new GeneratedOutputEmptyException();

		}
	}
	
	private static List<SimpleFeature> processEdges(List<String> unvisited,
			Node currentNode, GeometryFactory geometryFactory,
			SimpleFeatureType featureType, long unixTime, int pathID,
			List<SimpleFeature> featuresList) {
		
		LineString line;
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
						Point point = geometryFactory.createPoint(coordinate);
						SimpleFeature feature = buildTimeFeatureFromGeometry(
								featureType, point, unixTime,
								String.valueOf(pathID));
						unixTime += 9000;
						featuresList.add(feature);
					}
				} else if (lil.project(pt) == lil.getEndIndex()) {
					// start coordinate is at the end of the line
					for (int index = (int) lil.getEndIndex(); index >= 0; index -= 25) {
						Coordinate coordinate = lil.extractPoint(index);
						Point point = geometryFactory.createPoint(coordinate);
						SimpleFeature feature = buildTimeFeatureFromGeometry(
								featureType, point, unixTime,
								String.valueOf(pathID));
						unixTime += 9000;
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
		return featuresList;
	}
	
	private static SimpleFeature buildTimeFeatureFromGeometry(
			SimpleFeatureType featureType, Geometry geom, long unix_time,
			String path_id) {
		SimpleFeatureTypeBuilder stb = new SimpleFeatureTypeBuilder();
		stb.init(featureType);
		SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(featureType);
		sfb.add(geom);
		sfb.add(unix_time);
		sfb.add(path_id);
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
		builder.add("path_id", String.class);

		// build the type
		SimpleFeatureType pathType = builder.buildFeatureType();

		return pathType;
	}

	private static void writeFeatures(SimpleFeatureCollection features,
			File file) {
		FeatureJSON fjson = new FeatureJSON();
		OutputStream os;
		try {
			os = new FileOutputStream(file);
			try {
				if (features.getSchema().getCoordinateReferenceSystem() != null) {
					LOGGER.info("Encoding CRS?");
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
			LOGGER.error("Failed to write feature collection "
					+ e1.getMessage());
		} catch (IOException e) {
			LOGGER.error("Failed to write feature collection " + e.getMessage());
		}
	}

}
