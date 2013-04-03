package org.mccaughey.geotools.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.CRS;
import org.mccaughey.util.TemporaryFileManager;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Point;

public class ShapeFile {
	static final Logger LOGGER = LoggerFactory.getLogger(ShapeFile.class);

	public static File createShapeFileAndReturnAsZipFile(File geoJsonFile,
			HttpSession session) throws IOException {

		SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) readFeatures(geoJsonFile);
		return createShapeFileAndReturnAsZipFile(geoJsonFile.getName(),
				featureCollection, session);
	}

	public static File createShapeFileAndReturnAsZipFile(String shapefileName,
			SimpleFeatureCollection featureCollection, HttpSession session)
			throws IOException {

		//
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		File zipfolder = Files.createTempDirectory(tempDir.toPath(),
				"walkability_zipfolder_").toFile();
		//
		if (shapefileName.indexOf(".") != -1)
			shapefileName = shapefileName.substring(0,
					shapefileName.lastIndexOf("."));
		File newFile = File.createTempFile(shapefileName, ".shp", zipfolder);
		//
		SimpleFeatureType sft = featureCollection.getSchema();
		SimpleFeatureTypeBuilder stb = new SimpleFeatureTypeBuilder();
		stb.setName("Output");
		for (AttributeDescriptor attDisc : sft.getAttributeDescriptors()) {
			String name = attDisc.getLocalName();
			Class type = attDisc.getType().getBinding();
			if (attDisc instanceof GeometryDescriptor) {
				stb.add(name, Point.class);
				stb.setDefaultGeometry(name);
			} else {
				stb.add(name, type);
			}
		}
		SimpleFeatureType newFeatureType = stb.buildFeatureType();
		//
		try {
			featuresExportToShapeFile(newFeatureType, featureCollection,
					newFile, true);
		} catch (NoSuchAuthorityCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		File zipfile = TemporaryFileManager.getNew(session, shapefileName,
				".zip", true);

		Zip zip = new Zip(zipfile, newFile.getParentFile().getAbsolutePath());
		zip.createZip();
		//
		FileUtils.deleteDirectory(zipfolder);
		//
		return zipfile;

	}

	private static void featuresExportToShapeFile(SimpleFeatureType type,
			SimpleFeatureCollection simpleFeatureCollection, File newFile,
			boolean createSchema) throws IOException,
			NoSuchAuthorityCodeException, FactoryException {

		if (!newFile.exists()) {
			newFile.createNewFile();
		}
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put("url", newFile.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);

		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory
				.createNewDataStore(params);
		if (createSchema) {
			newDataStore.createSchema(type);
		}
		newDataStore.forceSchemaCRS(CRS.decode("EPSG:28355"));
		Transaction transaction = new DefaultTransaction("create");
		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore
				.getFeatureSource(typeName);

		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			featureStore.setTransaction(transaction);
			try {
				featureStore.addFeatures(simpleFeatureCollection);
				transaction.commit();
				LOGGER.info("commited to feature store");

			} catch (Exception problem) {
				problem.printStackTrace();
				LOGGER.error("exception, rolling back transaction in feature store");
				transaction.rollback();
			} finally {
				transaction.close();
			}
		} else {
			LOGGER.info(typeName + " does not support read/write access");
		}

	}

	private static FeatureCollection readFeatures(File file) {
		FeatureCollection features = null;
		FeatureJSON fjson = new FeatureJSON();
		InputStream is;
		try {
			is = new FileInputStream(file);
			features = fjson.readFeatureCollection(is);
			try {
				if (features.getSchema().getCoordinateReferenceSystem() != null) {
					LOGGER.info("Encoding CRS?");
					fjson.setEncodeFeatureCollectionBounds(true);
					fjson.setEncodeFeatureCollectionCRS(true);
					// fjson.writeCRS(features.getSchema().getCoordinateReferenceSystem(),
					// os);
				} else {
					LOGGER.info("CRS is null");
				}
			} finally {
				is.close();
			}
		} catch (FileNotFoundException e1) {
			LOGGER.error("Failed to write feature collection "
					+ e1.getMessage());
		} catch (IOException e) {
			LOGGER.error("Failed to write feature collection " + e.getMessage());
		}
		return features;
	}
}
