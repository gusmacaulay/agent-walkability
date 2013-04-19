package org.mccaughey.pathGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geotools.data.DataStore;
import org.geotools.data.DefaultQuery;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.mccaughey.geotools.util.ShapeFile;
import org.mccaughey.pathGenerator.config.LayerMapping;
import org.mccaughey.service.impl.WFSDataStoreFactoryImpl;
import org.mccaughey.util.TemporaryFileManager;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.support.WebApplicationContextUtils;

import au.com.bytecode.opencsv.CSVWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

@Controller
@RequestMapping("/agent-paths")
public class AgentModelResource {

	private static final int MILLISECONDS = 1000;
	private static final String ZIP_FILE_LOCATION_ATTRIBUTE = "Generated_ZipFile_Location";
	private static final String SHAPEFILE_LOCATION_ATTRIBUTE = "Generated_File_Location";
	private static final String METRICS_LOCATION_ATTRIBUTE = "Metrics_File_Location";
	private static final int STEP_DISTANCE = 25;

	@Autowired
	private WFSDataStoreFactoryImpl wfsDataStoreFactoryImpl;

	static final Logger LOGGER = LoggerFactory
			.getLogger(AgentModelResource.class);

	@RequestMapping(value = "/{easting}/{northing}/{maxTime}/{intersectionWait}/{walkSpeed}", method = RequestMethod.GET)
	public void getPaths(HttpServletRequest request,
			HttpServletResponse response, @PathVariable String easting,
			@PathVariable String northing, @PathVariable int maxTime,
			@PathVariable int intersectionWait, @PathVariable double walkSpeed)
			throws IOException, NoSuchAuthorityCodeException, FactoryException,
			MismatchedDimensionException, TransformException {

		double stepTime = (STEP_DISTANCE / walkSpeed) * MILLISECONDS;
		double maxDistance = (maxTime * walkSpeed) / MILLISECONDS;

		ApplicationContext ctx = WebApplicationContextUtils
				.getWebApplicationContext(request.getSession()
						.getServletContext());

		DataStore dataStore = wfsDataStoreFactoryImpl
				.getDataStore(LayerMapping.randomDestinationLayer);
		SimpleFeatureSource networkSource = dataStore
				.getFeatureSource(LayerMapping.roadSampleLayer);
		Query query = new DefaultQuery(LayerMapping.randomDestinationLayer);
		query.setCoordinateSystem(CRS.decode("EPSG:28355"));
		SimpleFeatureIterator features = dataStore
				.getFeatureSource(LayerMapping.randomDestinationLayer)
				.getFeatures(query).features();
		//
		List<Point> destinations = new ArrayList<Point>();
		while (features.hasNext()) {
			SimpleFeature feature = features.next();
			destinations.add((Point) feature.getDefaultGeometry());
		}

		GeometryFactory geometryFactory = new GeometryFactory(
				new PrecisionModel(0));
		Point start = geometryFactory.createPoint(new Coordinate(Double
				.parseDouble(easting), Double.parseDouble(northing)));
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:3857"); // same
																		// as
																		// 900913
		CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:28355");

		MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
		Point targetGeometry = (Point) JTS.transform(start, transform);

		LOGGER.info("Converted geom: " + targetGeometry.toString());

		LOGGER.info("sessionid:" + request.getSession().getId());

		synchronized (request.getSession()) {
			TemporaryFileManager.deleteAll(request.getSession());
			File file = TemporaryFileManager.getNew(request.getSession(),
					"all_path_nodes_", ".json");
			File metricsFile = TemporaryFileManager.getNew(
					request.getSession(), "metrics", ".csv");
			try {

				CoordinateReferenceSystem crs = CRS.decode("EPSG:28355");

				SimpleFeatureCollection paths = PathProcessor.processPathNodes(
						PathGenerator.shortestPaths(networkSource,
								targetGeometry, destinations,maxDistance), stepTime,
						maxTime, intersectionWait, STEP_DISTANCE, crs);

				Map<String, String> metrics = MetricAnalyser.calculateMetrics(
						paths, maxDistance);
				LOGGER.info("AVERAGE CROSSINGS: {}",
						metrics.get("meanCrossings"));
				LOGGER.info("Writing metrics to file {}",
						metricsFile.getAbsoluteFile());
				CSVWriter writer = new CSVWriter(new FileWriter(metricsFile.getAbsolutePath()), ',');
				for (String key : metrics.keySet()) {
					String[] keyValue = new String[2];
					keyValue[0] = key;
					keyValue[1] = metrics.get(key);
					writer.writeNext(keyValue);
				}
				writer.close();
				request.getSession().setAttribute(METRICS_LOCATION_ATTRIBUTE,
						metricsFile.getAbsolutePath());
			
				//
				PathWriter.writePathNodes(paths, crs, file);
				request.getSession().setAttribute(SHAPEFILE_LOCATION_ATTRIBUTE,
						file.getAbsolutePath());
				LOGGER.info("Writing shp to file {}", file.getAbsoluteFile());
				File zipfile = ShapeFile.createShapeFileAndReturnAsZipFile(
						file.getName(),metricsFile, paths, request.getSession());
				request.getSession().setAttribute(ZIP_FILE_LOCATION_ATTRIBUTE,
						zipfile.getAbsolutePath());
				//
				LOGGER.info("Writing zip to file {}", zipfile.getAbsoluteFile());
				LOGGER.info("Paths Generated");
				FileCopyUtils.copy(new FileInputStream(file),
						response.getOutputStream());
			} catch (GeneratedOutputEmptyException e) {
				LOGGER.error("Empty Output!!");
				TemporaryFileManager.deleteAll(request.getSession());
			}

		}
	}

	@RequestMapping(value = "downloadGeneratedOutput", method = RequestMethod.GET)
	public void downloadGeneratedOutput(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		synchronized (request.getSession()) {
			if (request.getSession().getAttribute(SHAPEFILE_LOCATION_ATTRIBUTE) == null) {
				throw new IOException("No output is generated");
			}
			File file = new File((String) request.getSession().getAttribute(
					SHAPEFILE_LOCATION_ATTRIBUTE));
			response.setContentType("application/x-download");
			response.setHeader("Content-disposition", "attachment; filename="
					+ "agent_walkability_output.geojson");
			FileCopyUtils.copy(new FileInputStream(file),
					response.getOutputStream());
		}
	}

	@RequestMapping(value = "downloadGeneratedOutputAzShpZip", method = RequestMethod.GET)
	public void downloadGeneratedOutputAzShpZip(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		synchronized (request.getSession()) {
			if (request.getSession().getAttribute(SHAPEFILE_LOCATION_ATTRIBUTE) == null) {
				throw new IOException("No output is generated");
			}
			File file = new File((String) request.getSession().getAttribute(
					ZIP_FILE_LOCATION_ATTRIBUTE));
			response.setContentType("application/x-download");
			response.setHeader("Content-disposition", "attachment; filename="
					+ "agent_walkability_output.zip");
			FileCopyUtils.copy(new FileInputStream(file),
					response.getOutputStream());
		}
	}
}
