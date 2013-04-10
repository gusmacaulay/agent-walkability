package org.mccaughey.pathGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;

public final class MetricAnalyser {

	static final Logger LOGGER = LoggerFactory.getLogger(MetricAnalyser.class);

	private MetricAnalyser() {
	}

	public static Map<String, Double> calculateMetrics(
			SimpleFeatureCollection agentPaths, int maxDistance) {
		Map<String, Double> metrics = new HashMap<String, Double>();
		SimpleFeatureCollection destinationFeatures = getDestinationFeatures(agentPaths);

		metrics.put("ratioOfAreas", ratioOfAreas(destinationFeatures,maxDistance));
		metrics.put("meanCrossings", meanCrossings(destinationFeatures));
		metrics.put("meanDistanceTravelled", meanDistanceTravelled(destinationFeatures));
		return metrics;

	}

	private static SimpleFeatureCollection getDestinationFeatures(
			SimpleFeatureCollection agentPaths) {
		SimpleFeatureIterator pathsIter = agentPaths.features();
		Map<String, SimpleFeature> destinationFeatures = new HashMap<String, SimpleFeature>();
		try {
			while (pathsIter.hasNext()) {
				SimpleFeature feature = pathsIter.next();
				String pathID = (String) feature.getAttribute("path_id");
				if (destinationFeatures.containsKey(pathID)) {
					int time = (int) feature.getAttribute("when");
					if (time > ((int) (destinationFeatures.get(pathID)
							.getAttribute("when")))) {
						destinationFeatures.put(pathID, feature);
					}
				} else {
					destinationFeatures.put(pathID, feature);
				}
			}
			List<SimpleFeature> destinationFeaturesList = new ArrayList();
			destinationFeaturesList.addAll(destinationFeatures.values());
			return DataUtilities.collection(destinationFeaturesList);
		} finally {
			pathsIter.close();
		}
	}

	private static Double meanDistanceTravelled(
			SimpleFeatureCollection agentDestinations) {
		int totalDistance = 0;
		SimpleFeatureIterator iter = agentDestinations.features();
		try {
			while (iter.hasNext()) {
				SimpleFeature feature = iter.next();
				totalDistance += (int) feature.getAttribute("walk_dist");
			}
		} finally {
			iter.close();
		}
		return (double) (totalDistance / agentDestinations.size());
	}

	private static Double meanCrossings(
			SimpleFeatureCollection agentDestinations) {
		int totalCrossings = 0;
		SimpleFeatureIterator iter = agentDestinations.features();
		try {
			while (iter.hasNext()) {
				SimpleFeature feature = iter.next();
				totalCrossings += (int) feature.getAttribute("crossings");
			}
		} finally {
			iter.close();
		}
		return (double) (totalCrossings / agentDestinations.size());
	}

	private static Double ratioOfAreas(SimpleFeatureCollection agentDestinations, int maxDistance) {
		SimpleFeatureIterator iter = agentDestinations.features();
		Geometry points = null;
		Geometry convexHull = null;
		double radialArea = Math.PI*maxDistance*maxDistance;
		LOGGER.info("Radial Area {}",radialArea);
		try {
			while (iter.hasNext()) {
				SimpleFeature feature = iter.next();
				if(points == null) {
					points = (Geometry) feature.getDefaultGeometry();
					
				} else {
					points = points.union((Geometry) feature.getDefaultGeometry());
				}
			}
			LOGGER.info("Points geomtries {}", points.getNumGeometries());	
			//ConvexHull ch = new ConvexHull(points);
			convexHull = points.convexHull();
			LOGGER.info("Convex Hull area: {}", convexHull.getArea());
		} finally {
			iter.close();
		}
		Double ratioOfAreas = (convexHull.getArea()/radialArea);
		LOGGER.info("Ratio of Areas {}", ratioOfAreas);
		return ratioOfAreas;
	}

}
