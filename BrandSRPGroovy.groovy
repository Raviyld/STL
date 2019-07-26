package com.common.groovy

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.Calendar

import javax.sql.DataSource

import com.elitecore.core.commons.drivers.DriverInitializationFailedException
import com.elitecore.core.commons.drivers.DriverProcessFailedException
import com.elitecore.corewebin.constants.WebinKeyConstants
import com.elitecore.webin.core.databasemanagement.datasource.JNDIDSManager
import com.elitecore.webin.core.spr.script.GroovySPR
import com.elitecore.webin.core.util.WebinPacketUtil
import com.elitecore.webin.server.WebinServerContext
import com.elitecore.webin.server.service.WebinRequest
import com.elitecore.webin.server.service.WebinResponse
import com.elitecore.webin.sm.web.sprinstances.groovysprinstance.data.GroovySPRInstanceData
import com.elitecore.commons.logging.LogManager

class BrandSRPGroovy extends GroovySPR{
	private static final String MODULE = "BRAND-SPR-GROOVY";
	DataSource ds = null;

	public BrandSRPGroovy(WebinServerContext serverContext, GroovySPRInstanceData groovySPRInstance) {
		super(serverContext, groovySPRInstance);
	}



	@Override
	public void initInternal() throws DriverInitializationFailedException {
		/*
		 * Put code here to initialize SPR.
		 */
		//Code to generate alert for groovy spr up event
		getServerContext().generateSystemAlert(com.elitecore.webin.constants.Alerts.SPR_SUCCESS.getSeverity(), com.elitecore.webin.constants.Alerts.SPR_SUCCESS, MODULE, "Groovy SPR started successfully, Name :" + getName());

		//code to get datasource
		ds=JNDIDSManager.getJDBCDataSource("eliteaaa");
	}

	/** code to get the loc, addloc, calledStationId, callingStationId in request and response  **/

	@Override
	protected boolean fetchAccountData(WebinRequest request, WebinResponse response, String userIdentity) throws DriverProcessFailedException {


		/*
		 * Put code here to fetch profile from SPR
		 */

		/*
		 * return false, if Profile not found in SPR
		 *
		 * if(profile == null){
		 * 		return false;
		 * }
		 */

		// Put code here to put all required profile attributes in response
		try{
			if(ds!=null){
				LogManager.getLogger().error(MODULE, "DataSource available.");
				//getting database connection
				Connection conn =ds.getConnection();
				PreparedStatement stmt = null;
				ResultSet rs = null;
				if(conn != null){
					LogManager.getLogger().error(MODULE, "Connection available.");
					//create prepared statement
					stmt = conn.prepareStatement("SELECT  CALLING_STATION_ID,CALLED_STATION_ID,NAS_IP_ADDRESS,NAS_IDENTIFIER FROM TBLMCONCURRENTUSERS WHERE FRAMED_IP_ADDRESS=?");
					if(stmt != null){
						LogManager.getLogger().error(MODULE, "Statement Available");
						stmt.setString(1, (String)request.getAttribute("FramedIPAddress"));
						//executing the query
						rs = stmt.executeQuery();
						if(rs != null){
							while (rs.next()){
								// code to set the information in request and response object
								String callingStationId = rs.getString("CALLING_STATION_ID");
								String calledStationId =  rs.getString("CALLED_STATION_ID");
								String nasIdentifier = rs.getString("NAS_IDENTIFIER");
								
								if(callingStationId != null){
									String strClientMac = callingStationId.replaceAll(":", "");
									request.setAttribute("client-mac",strClientMac.toLowerCase());
									response.setAttribute("client-mac",strClientMac.toLowerCase());
								}
								if(calledStationId != null){
									
									String strAPIIdentity = calledStationId.substring(0,calledStationId.indexOf(":"));
								
									String strAPIdentifier = strAPIIdentity.substring(0, strAPIIdentity.lastIndexOf(".", strAPIIdentity.lastIndexOf(".")-1));
									String strHTTPKey = calledStationId.substring(calledStationId.indexOf(":")+1,calledStationId.length());
									
									response.setAttribute("vnpt-ssid", strHTTPKey.toLowerCase());
									request.setAttribute("vnpt-ssid",strHTTPKey.toLowerCase());
									response.setAttribute("AP.Identifier", strAPIdentifier.toLowerCase());
									request.setAttribute("AP.Identifier",strAPIdentifier.toLowerCase());
								}
								if(nasIdentifier != null){
									request.setAttribute("NASIPAddress",nasIdentifier);
									response.setAttribute("NASIPAddress",nasIdentifier);
								}
							}
							WebinPacketUtil.buildWebinResponse(request, response);
						}
					}else{
						LogManager.getLogger().error(MODULE, "Statement not Available");
					}
				}else{
					LogManager.getLogger().error(MODULE, "Connection Not available.");
				}
				if(rs != null){
						rs.close();
				}

				if(stmt != null){
						stmt.close();
				}

				if(conn != null){
						conn.close();
				}
			}else{
				LogManager.getLogger().error(MODULE, "DataSource not available.");
			}
			
			if(response.getAttribute("Hotspot") != null){
				String strHotspot = (String)response.getAttribute("Hotspot");
				String strDisplayOption= strHotspot.substring(0,strHotspot.indexOf("."));
				String strAdLoc = strHotspot.substring(strHotspot.indexOf(".")+1,strHotspot.length());
			}else{
				LogManager.getLogger().error(MODULE, "Hotspot Not not available.");
			}
		}
		catch(Exception e){
				LogManager.getLogger().trace(MODULE,e);
		}
		return true;
	}
}