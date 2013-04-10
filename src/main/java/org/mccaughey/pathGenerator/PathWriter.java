package org.mccaughey.pathGenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PathWriter {

	static final Logger LOGGER = LoggerFactory.getLogger(PathWriter.class);

	private PathWriter() {
	}

	public static void writePathNodes(
			SimpleFeatureCollection pathFeatures,
			CoordinateReferenceSystem crs, File file)
			throws GeneratedOutputEmptyException {

		CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
		LOGGER.info(crs.toWKT());

		if (pathFeatures.size() > 0) {
			ReprojectingFeatureCollection rfc = new ReprojectingFeatureCollection(
					pathFeatures, targetCRS);

			writeFeatures(rfc, file);
			LOGGER.info("GeoJSON writing complete");
		
		} else {
			LOGGER.info("Generated output is empty");
			throw new GeneratedOutputEmptyException();

		}
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
