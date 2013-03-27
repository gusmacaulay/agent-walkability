package org.mccaughey.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.geotools.data.DataSourceException;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.data.wfs.v1_1_0.WFS_1_1_0_DataStore;
import org.mccaughey.service.DataStoreFactory;

public class WFSDataStoreFactoryImpl implements DataStoreFactory {


	
	private static final String USERNAME = "USER";
	private static final String PASSWORD = "PASS";
	private DataStore DSEDataStore = null;
	private DataStore CSDILADataStore = null;
	private DataStore NewCastleDataStore = null;
	private WFS_1_1_0_DataStore wFS_1_1_0_DataStore = null;

	@PreDestroy
	public void dispose(){
		DSEDataStore.dispose();
		CSDILADataStore.dispose();
	}
	
	public SimpleFeatureSource getFeatureSource(String layerName) throws IOException {
		return getDataStore(layerName).getFeatureSource(layerName);
	}	

	public DataStore getDataStore(String layername) throws IOException {
	
			return getCSDILADataStore();
		
	}

	

	public DataStore getCSDILADataStore() throws IOException, DataSourceException{
		
		if (this.CSDILADataStore == null) {
			Map<String, Object> dataStoreParams = new HashMap<String, Object>();
			String getCapabilities = "http://192.43.209.39:8080/geoserver/ows?service=wfs&version=1.1.0&request=GetCapabilities";
			dataStoreParams.put("WFSDataStoreFactory:GET_CAPABILITIES_URL",getCapabilities);
			dataStoreParams.put("WFSDataStoreFactory:USERNAME", USERNAME);
			dataStoreParams.put("WFSDataStoreFactory:PASSWORD",PASSWORD );
			dataStoreParams.put(WFSDataStoreFactory.TIMEOUT.key, new Integer(18000000)); 			

//			wFS_1_1_0_DataStore = (WFS_1_1_0_DataStore) DataStoreFinder.getDataStore(dataStoreParams);
			CSDILADataStore =  DataStoreFinder.getDataStore(dataStoreParams);
//			CSDILADataStore).setMaxFeatures(10000);
		}
//		return (WFS_1_1_0_DataStore)this.wFS_1_1_0_DataStore;
		return CSDILADataStore;
	}

	


	public DataStore getExportableDataStore() throws Exception{		
		throw new Exception("Datastore is not exportable, use other datastore type like postgis datastore");
	}

	

}
