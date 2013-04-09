package org.mccaughey;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.referencing.CRS;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.jbehave.core.embedder.Embedder;
import org.mccaughey.pathGenerator.GeneratedOutputEmptyException;
import org.mccaughey.pathGenerator.PathGenerator;
import org.mccaughey.pathGenerator.PathWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

public class PathGeneratorSteps extends Embedder {

	PathGenerator pathGenerator;
	SimpleFeatureSource networkSource;
	List<Path> paths;
	Point start, end;
	List<Point> destinations = new ArrayList();
	GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(),
			28355);

	@Given("a path generator")
	public void aPathGenerator() {
		// pathGenerator = new PathGenerator();
	}

	@Given("a footpath network")
	public void aFootpathNetwork() throws IOException {

		// File networkShapeFile = new
		// File(getClass().getResource("/melton_roads_sample.shp").getFile());
		File networkShapeFile = new File(getClass().getResource("/graph.shp")
				.getFile());
		FileDataStore networkDataStore = FileDataStoreFinder
				.getDataStore(networkShapeFile);

		System.out.println(networkDataStore.getInfo().toString());
		networkSource = networkDataStore.getFeatureSource();
	}

	@Given("a startpoint")
	public void aStartPoint() {
		double easting = 285752.0;
		double northing = 5824386.0;
		start = geometryFactory.createPoint(new Coordinate(easting, northing));
	}

	@Given("an endpoint")
	public void anEndPoint() throws IOException {
		double easting = 286022.0;
		double northing = 5824941.0;
		// double easting = 285579.0;
		// double northing = 5824385.0;
		end = geometryFactory.createPoint(new Coordinate(easting, northing));
		destinations.add(end);
		// aSetOfEndPoints();
	}

	@Given("a set of endpoints")
	public void aSetOfEndPoints() throws IOException {
		File networkShapeFile = new File(getClass().getResource(
				"/random_destinations.shp").getFile());
		FileDataStore networkDataStore = FileDataStoreFinder
				.getDataStore(networkShapeFile);
		SimpleFeatureIterator features = networkDataStore.getFeatureSource()
				.getFeatures().features();
		while (features.hasNext()) {
			SimpleFeature feature = features.next();
			destinations.add((Point) feature.getDefaultGeometry());
		}
		// anEndPoint();
	}

	@When("the path generator is asked for the shortest path/s")
	public void requestShortestPath() throws Exception {
		paths = PathGenerator.shortestPaths(networkSource, start, destinations);// ,
																				// new
																				// File("all_path_nodes_test.json"));
		// paths =p.getOne();

		Assert.assertTrue(paths.size() > 0);
	}

	@Then("the correct shortest path/s will be provided")
	public void shortestPathIsCorrect() {
		for (Path path : paths) {
			Assert.assertTrue(path.isValid());
		}
	}

	@Then("the path/s will have timestamps")
	public void shortestPathHasTimeStamps() throws NoSuchAuthorityCodeException, FactoryException, GeneratedOutputEmptyException {
		// System.out.println("Paths: " + paths.size());
		// for (Path path : paths) {
		// for (Edge edge : (List<Edge>) path.getEdges()) {
		// LineString line = (LineString) edge.getObject();
		// // System.out.println(line.toText());
		// }
		// }
		// // Assert.assertTrue(path != null);n isntall
		CoordinateReferenceSystem crs = CRS.decode("EPSG:28355");
		int stepTime = 9000;
		SimpleFeatureCollection pathFeatures = PathWriter.writePathNodes(paths,
				stepTime, crs, new File("output.geojson"));
	}

	private static SimpleFeatureCollection readFeatures(URL url)
			throws IOException {
		FeatureJSON io = new FeatureJSON();
		// io.setEncodeFeatureCollectionCRS(true);

		// io.readCRS(url.openConnection().getInputStream()));
		FeatureIterator<SimpleFeature> features = io
				.streamFeatureCollection(url.openConnection().getInputStream());
		SimpleFeatureCollection collection = FeatureCollections.newCollection();

		while (features.hasNext()) {
			SimpleFeature feature = (SimpleFeature) features.next();
			collection.add(feature);
		}

		return collection;
	}

}
