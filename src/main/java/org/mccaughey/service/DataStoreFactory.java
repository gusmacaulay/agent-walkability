package org.mccaughey.service;

import java.io.IOException;

import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureSource;

public interface DataStoreFactory {
	
	public DataStore getDataStore(String layername)  throws IOException;

	public DataStore getExportableDataStore () throws Exception;
	
	public SimpleFeatureSource getFeatureSource(String layername) throws IOException;

}
