package org.mccaughey.pathGenerator;

import java.util.HashMap;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricAnalyser {
	
	static final Logger LOGGER = LoggerFactory.getLogger(MetricAnalyser.class);
	
	private MetricAnalyser() {}
	
	public Map<String,Double> calculateMetrics(SimpleFeatureCollection agentPaths) {
		Map<String,Double> metrics = new HashMap<String,Double>();
		metrics.put("ratioOfAreas", ratioOfAreas(agentPaths));
		metrics.put("ratioOfAreas", meanCrossings(agentPaths));
		metrics.put("ratioOfAreas", meanDistanceTravelled(agentPaths));
		return metrics;
		
	}

	private Double meanDistanceTravelled(SimpleFeatureCollection agentPaths) {
		// TODO Auto-generated method stub
		return null;
	}

	private Double meanCrossings(SimpleFeatureCollection agentPaths) {
		// TODO Auto-generated method stub
		return null;
	}

	private Double ratioOfAreas(SimpleFeatureCollection agentPaths) {
		// TODO Auto-generated method stub
		return null;
	}  

}
