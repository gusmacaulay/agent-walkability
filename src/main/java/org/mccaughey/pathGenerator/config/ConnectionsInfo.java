package org.mccaughey.pathGenerator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConnectionsInfo {
	
	@Value("${geoserverRESTURL}") 
	private String RESTURL; 
	

	@Value("${geoserverRESTUSER}") 
	private String RESTUSER; 
	
	@Value("${geoserverRESTPW}") 
	private String RESTPW; 
	
	public String getRESTURL() {
		return RESTURL;
	}
	
	public String getRESTUSER() {
		return RESTUSER;
	}
	
	public String getRESTPW() {
		return RESTPW;
	}

}
