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
import org.mccaughey.pathGenerator.config.ConnectionsInfo;
import org.mccaughey.service.DataStoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public class WFSDataStoreFactoryImpl implements DataStoreFactory {

	@Autowired
	ConnectionsInfo connectionsInfo;

	private DataStore dataStoreDSE = null;
	private DataStore dataStoreCSDILA = null;

	@PreDestroy
	public void dipose() {
		dataStoreDSE.dispose();
		dataStoreCSDILA.dispose();
	}

	public SimpleFeatureSource getFeatureSource(String layerName)
			throws IOException {
		return getDataStore(layerName).getFeatureSource(layerName);
	}

	public DataStore getDataStore(String layername) throws IOException {

		return getCSDILADataStore();

	}

	public DataStore getCSDILADataStore() throws IOException,
			DataSourceException {

		if (this.dataStoreCSDILA == null) {
			Map<String, Object> dataStoreParams = new HashMap<String, Object>();
			String getCapabilities = connectionsInfo.getGeoserverURL()
					+ "/ows?service=wfs&version=1.1.0&request=GetCapabilities";
			dataStoreParams.put("WFSDataStoreFactory:GET_CAPABILITIES_URL",
					getCapabilities);
			dataStoreParams.put("WFSDataStoreFactory:USERNAME",
					connectionsInfo.getGeoserverUser());
			dataStoreParams.put("WFSDataStoreFactory:PASSWORD",
					connectionsInfo.getGeoserverPassword());
			dataStoreParams.put(WFSDataStoreFactory.TIMEOUT.key, new Integer(
					18000000));
			dataStoreCSDILA = DataStoreFinder.getDataStore(dataStoreParams);
		}
		return dataStoreCSDILA;
	}

	public DataStore getExportableDataStore() throws Exception {
		throw new Exception(
				"Datastore is not exportable, use other datastore type like postgis datastore");
	}

}
