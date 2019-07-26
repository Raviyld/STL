package com.common.groovy

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.logging.LogManager

import javax.sql.DataSource

import com.elitecore.coreradius.client.base.IRadiusClient
import com.elitecore.coreradius.client.base.RadiusClientFactory
import com.elitecore.corewebin.constants.WebinKeyConstants
import com.elitecore.webin.core.databasemanagement.datasource.JNDIDSManager
import com.elitecore.webin.core.servicepolicy.script.ServicePolicyGroovyScript
import com.elitecore.webin.server.WebinServerContext
import com.elitecore.webin.server.service.WebinRequest
import com.elitecore.webin.server.service.WebinResponse

import com.elitecore.commons.logging.LogManager


public class UpdateSessionGroovy  extends ServicePolicyGroovyScript{

	private static final String MODULE = "UPDATE-SESSION-GROOVY";
	private DataSource ds=null;

	public UpdateSessionGroovy(WebinServerContext serverContext) {
		super(serverContext);
		// TODO Auto-generated constructor stub
	}

	public void init(String groovyArgs) {
		LogManager.getLogger().info(MODULE, "Init for Service Policy.");
		//code to get datasource
		ds=JNDIDSManager.getJDBCDataSource("eliteaaa");

	}

	public void preHandleRequest(WebinRequest request, WebinResponse response) {
		LogManager.getLogger().info(MODULE, "Pre-request handle for Service Policy.");
	}



	public void postHandleRequest(WebinRequest request, WebinResponse response) {
		LogManager.getLogger().info(MODULE, "Post-request handle for Service Policy. "+response);

		String resultCode = response.getAttribute(WebinKeyConstants.RESULT_CODE.val);
		LogManager.getLogger().info(MODULE, "Result Code>>>>>>>>> "+resultCode);

		if(resultCode.equals("200")){
			try{
				if(ds!=null){
					LogManager.getLogger().error(MODULE, "DataSource available.");
					updateTblmConcurrentUsers(ds, request, response);
				}else {
					LogManager.getLogger().error(MODULE, "DataSource not available.");

				    if(!resultCode.equals("200") && !resultCode.equals("307")){
						LogManager.getLogger().info(MODULE, "Result Code "+resultCode+" not for Acct Packet");
						response.setFurtherProcessingRequired(false);
						response.setAttribute("ResultCode",resultCode);
					} else if(framedIpAddress==null){
						LogManager.getLogger().info(MODULE, "Framed IP is Null");
					} else if(subScriberIdentity == null){
						LogManager.getLogger().info(MODULE, "SubScriberIdentity is Null");
			     	}

				}
			}
			catch(Exception e){
				LogManager.getLogger().trace(MODULE,e);
			}

		}
	}

	private updateTblmConcurrentUsers(ds, WebinRequest request, WebinResponse response) {
		Connection conn =ds.getConnection();
		PreparedStatement stmt = null;
		int rs = 0;
		if(conn != null){
			LogManager.getLogger().error(MODULE, "Connection available.");
			//create prepared statement
			String sql = " UPDATE TBLMCONCURRENTUSERS SET PARAM_STR1=?, PARAM_STR2=?, PARAM_STR3=?, PARAM_STR4=? WHERE FRAMED_IP_ADDRESS=?";


			String subScriberIdentity = request.getAttribute("Sub.SubscriberIdentity");
			String servicetype = request.getAttribute("ServiceType");
			String location = request.getAttribute("Location");
			String framedIpAddress = request.getAttribute("Sub.FramedIPAddress");

			LogManager.getLogger().info(MODULE, "sql>>>>>>>>> "+sql);

			stmt = conn.prepareStatement(sql);

			if(stmt != null){
				stmt.setString(1, subScriberIdentity);
				stmt.setString(2, servicetype);
				stmt.setString(3, location);
				stmt.setString(4, "success");
				stmt.setString(5, framedIpAddress);
			
				LogManager.getLogger().error(MODULE, "Statement Available");
				//executing the query
				rs = stmt.executeUpdate();
				if(rs > 0){
					response.setAttribute("ResultCode","200");
				}				
			}else{
				LogManager.getLogger().error(MODULE, "Statement not Available");
			}
		}else{
			LogManager.getLogger().error(MODULE, "Connection Not available.");
		}
		if(stmt != null){
			stmt.close();
		}
		if(conn != null){
			conn.close();
		}
	}
}