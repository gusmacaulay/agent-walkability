package org.mccaughey.pathGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.mccaughey.pathGenerator.config.ConnectionsInfo;
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

@Controller
@RequestMapping("/agent-paths")
public class PathGeneratorResource {

	@Autowired
	public WFSDataStoreFactoryImpl wfsDataStoreFactoryImpl;

	static final Logger LOGGER = LoggerFactory
			.getLogger(PathGeneratorResource.class);

	@RequestMapping(value = "/{easting}/{northing}", method = RequestMethod.GET)
	public void getPaths(HttpServletRequest request,
			HttpServletResponse response, @PathVariable String easting,
			@PathVariable String northing) throws IOException,
			NoSuchAuthorityCodeException, FactoryException,
			MismatchedDimensionException, TransformException {
		ApplicationContext ctx = WebApplicationContextUtils
				.getWebApplicationContext(request.getSession()
						.getServletContext());
		ConnectionsInfo connectionsInfo = (ConnectionsInfo) ctx
				.getBean(ConnectionsInfo.class);

		;
		DataStore dataStore = wfsDataStoreFactoryImpl
				.getDataStore(LayerMapping.RANDOM_DESTINATION_LAYER);
		SimpleFeatureSource networkSource = dataStore
				.getFeatureSource(LayerMapping.ROAD_SAMPLE_LAYER);
		Query query = new DefaultQuery(LayerMapping.RANDOM_DESTINATION_LAYER);
		query.setCoordinateSystem(CRS.decode("EPSG:28355"));
		SimpleFeatureIterator features = dataStore
				.getFeatureSource(LayerMapping.RANDOM_DESTINATION_LAYER)
				.getFeatures(query).features();
		//
		List<Point> destinations = new ArrayList<Point>();
		while (features.hasNext()) {
			SimpleFeature feature = features.next();
			destinations.add((Point) feature.getDefaultGeometry());
		}

		GeometryFactory geometryFactory2 = new GeometryFactory(
				new PrecisionModel(0));
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
			try {
				CoordinateReferenceSystem crs = CRS.decode("EPSG:28355");
				SimpleFeatureCollection paths = PathWriter.writePathNodes(
						PathGenerator.shortestPaths(networkSource,
								targetGeometry, destinations), crs, file);
				//
				request.getSession().setAttribute("Generated_File_Location",
						file.getAbsolutePath());
				//
				File zipfile = ShapeFile.createShapeFileAndReturnAsZipFile(
						file.getName(), paths, request.getSession());
				request.getSession().setAttribute("Generated_ZipFile_Location",
						zipfile.getAbsolutePath());
				//
				LOGGER.info("Paths Generated");
				FileCopyUtils.copy(new FileInputStream(file),
						response.getOutputStream());
			} catch (GeneratedOutputEmptyException e) {
				TemporaryFileManager.deleteAll(request.getSession());
			}

		}
	}

	@RequestMapping(value = "downloadGeneratedOutput", method = RequestMethod.GET)
	public void downloadGeneratedOutput(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		synchronized (request.getSession()) {
			if (request.getSession().getAttribute("Generated_File_Location") == null)
				throw new IOException("No output is generated");
			File file = new File((String) request.getSession().getAttribute(
					"Generated_File_Location"));
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
			if (request.getSession().getAttribute("Generated_File_Location") == null)
				throw new IOException("No output is generated");
			File file = new File((String) request.getSession().getAttribute(
					"Generated_ZipFile_Location"));
			response.setContentType("application/x-download");
			response.setHeader("Content-disposition", "attachment; filename="
					+ "agent_walkability_output.zip");
			FileCopyUtils.copy(new FileInputStream(file),
					response.getOutputStream());
		}
	}
}
