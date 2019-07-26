
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

import javax.sql.DataSource

import com.elitecore.webin.core.databasemanagement.datasource.JNDIDSManager
import com.elitecore.webin.core.servicepolicy.script.ServicePolicyGroovyScript
import com.elitecore.webin.server.WebinServerContext
import com.elitecore.webin.server.service.WebinRequest
import com.elitecore.webin.server.service.WebinResponse
import com.elitecore.commons.logging.LogManager

public class ConcurrencyCheckGroovy extends ServicePolicyGroovyScript {
	private static final String MODULE = "SMP-USER-GROOVY";
	private DataSource ds=null;
	/*
	* Configuration for Max Login allow a time for each user.
	*/
	private static final int CONCURRENCY_ALLOW = 1;
	
	public ConcurrencyCheckGroovy(WebinServerContext serverContext) {
		super(serverContext);
	}

	public void init(String groovyArgs) {
		LogManager.getLogger().info(MODULE, "Init for SMP User Policy.");
		ds=JNDIDSManager.getJDBCDataSource("webin");
	}

	public void preHandleRequest(WebinRequest request, WebinResponse response) {
		LogManager.getLogger().info(MODULE, "Pre-request Groovy handle for SMP User Policy.");
		int intLoginCount=0;
		try{
			if(ds!=null){
				LogManager.getLogger().error(MODULE, "DataSource available.");
				
				Connection conn =ds.getConnection();
				
				PreparedStatement stmt = null;
				ResultSet rs = null;
				/*
				 * Fetch currently active sessions from EliteAAA
				 */
				if(conn != null){
				
					LogManager.getLogger().error(MODULE, "Connection available.");
					
					stmt = conn.prepareStatement("SELECT SUBSCRIBERIDENTITY,ACCOUNTNUMBER,SERVICETYPE,LOCATION FROM TBLWEBINSPR WHERE ACCOUNTNUMBER=?");
					if(stmt != null){
					
						LogManager.getLogger().error(MODULE, "Statement Available"+request.getAttribute("Sub.UserName"));
						stmt.setString(1, (String)request.getAttribute("Sub.UserName"));
						rs = stmt.executeQuery();
						if(rs != null){
							while (rs.next()){
								/*
								 * Check if current Active Session is not reached to allow limit. If its reached allow limit it reject login.
								*/
								LogManager.getLogger().error(MODULE, "Statement Available"+rs.getString("SUBSCRIBERIDENTITY"));
								response.setAttribute("Sub.UserName",rs.getString("SUBSCRIBERIDENTITY"));
								response.setAttribute("ServiceType",rs.getString("SERVICETYPE"));
								response.setAttribute("Location",rs.getString("LOCATION"));
								
								request.setAttribute("Sub.UserName",rs.getString("SUBSCRIBERIDENTITY"));
								request.setAttribute("ServiceType",rs.getString("SERVICETYPE"));
								request.setAttribute("Location",rs.getString("LOCATION"));
							}
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
		}
		catch(Exception e){
			    LogManager.getLogger().trace(MODULE,e);
		}
	}

	public void postHandleRequest(WebinRequest request, WebinResponse response) {
		LogManager.getLogger().info(MODULE, "Post-request handle for SMP User Policy.");
	}
}
