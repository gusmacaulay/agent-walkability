package org.mccaughey.pathGenerator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConnectionsInfo {
	
	@Value("${geoserverRESTURL}") 
	private String geoserverURL; 
	

	@Value("${geoserverRESTUSER}") 
	private String geoserverUSER; 
	
	@Value("${geoserverRESTPW}") 
	private String geoserverPassword; 
	
	public String getGeoserverURL() {
		return geoserverURL;
	}
	
	public String getGeoserverUser() {
		return geoserverUSER;
	}
	
	public String getGeoserverPassword() {
		return geoserverPassword;
	}

}
