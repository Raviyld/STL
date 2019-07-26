import com.elitecore.commons.logging.LogManager;
import com.elitecore.core.commons.InitializationFailedException;
import com.elitecore.corewebin.constants.WebinKeyConstants;
import com.elitecore.webin.core.esi.paymentgateway.script.PaymentGatewayGroovy;
import com.elitecore.webin.server.WebinServerContext;
import com.elitecore.webin.server.service.WebinRequest;
import com.elitecore.webin.server.service.WebinResponse;
import com.elitecore.webin.sm.web.externalsystem.paymentgateway.data.PaymentGateway;

import freemarker.core.ReturnInstruction.Return;
import groovy.json.StringEscapeUtils;
import groovy.xml.StreamingDOMBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class OnlinePaymentGatewayGroovy extends PaymentGatewayGroovy{

	private static final String MODULE = "PAY-GW-GRV";
	private String groovyArgument;

	public OnlinePaymentGatewayGroovy(WebinServerContext serverContext, PaymentGateway paymentGatewayConfiguration) {
		super(serverContext, paymentGatewayConfiguration);
		this.groovyArgument = paymentGatewayConfiguration.getGroovyArgument();
	}

	@Override
	public void init() throws InitializationFailedException {
		LogManager.getLogger().info(MODULE, "Initializing payment gateway groovy");
		// Initialization Code goes here.
		//Read output in string format
		// Use Below method to get Payment Gateway Configuration
		// getPaymentGatewayConfiguration();
	}

	@Override
	public void processPaymentRequest(WebinRequest request, WebinResponse response) {
		LogManager.getLogger().debug(MODULE, "Processing payment gateway groovy request");
			try {
				SSLContext sslContext = SSLContext.getInstance("SSL");
				def trustAllCerts = [
					new X509TrustManager() {
						public X509Certificate[] getAcceptedIssuers() {
							return null
						}
				
						public void checkServerTrusted(X509Certificate[] certs, String authType)  {
						}
				
						public void checkClientTrusted(X509Certificate[] certs, String authType)  {
						}
					}
				] as TrustManager[]
				
			  /* for java*/
			  /* TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return null;
						}
	
						public void checkClientTrusted(X509Certificate[] certs, String authType) {
						}
	
						public void checkServerTrusted(X509Certificate[] certs, String authType) {
						}
					} };*/
	
				sslContext.init(null, trustAllCerts, new SecureRandom());
	
				SSLSocketFactory socketFactory = new SSLSocketFactory(sslContext, new X509HostnameVerifier() {
					@Override
					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
	
					@Override
					public void verify(String arg0, String[] arg1, String[] arg2) throws SSLException {
					}
	
					@Override
					public void verify(String arg0, X509Certificate arg1) throws SSLException {
					}
	
					@Override
					public void verify(String arg0, SSLSocket arg1) throws IOException {
					}
				});
	
				Scheme httpsschema = new Scheme("https", 443, socketFactory);
				Scheme httpschema = new Scheme("https", 80, PlainSocketFactory.getSocketFactory());
	
				SchemeRegistry schemeRegistry = new SchemeRegistry();
				schemeRegistry.register(httpschema);
				schemeRegistry.register(httpsschema);
	
				PoolingClientConnectionManager cm = new PoolingClientConnectionManager(schemeRegistry);
				cm.setMaxTotal(10);
				cm.setDefaultMaxPerRoute(200);
	
				HttpClient httpClient = new DefaultHttpClient(cm);
				httpClient.getParams().setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 3000);
	
				HttpPost postRequest = new HttpPost("https://sandbox.vnptpay.vn/rest/payment/v1.0.5/init");
				postRequest.setHeader("accept", ContentType.APPLICATION_JSON.getMimeType());
				postRequest.setHeader("Authorization", "Bearer acf392daf78c3cbeeb9705dab9a63212");
				postRequest.setHeader("content-type", ContentType.APPLICATION_JSON.getMimeType());
	
				
				JSONObject requestJSON = getJsonData(request, response);
			
				StringEntity strEntity = new StringEntity(requestJSON.toString(), ContentType.APPLICATION_JSON);
				postRequest.setEntity(strEntity);
	
				HttpResponse httpResponse = httpClient.execute(postRequest);
				StatusLine statusLine = httpResponse.getStatusLine();
				HttpEntity entity = httpResponse.getEntity();
				if (entity == null) {
					EntityUtils.consume(entity);
					throw new HttpResponseException(statusLine.getStatusCode(),
					"Error while parsing Web-Service response, Reason: response Body is null");
				}
	
				if (statusLine.getStatusCode() >= 300 || statusLine.getStatusCode() < 200) {
					EntityUtils.consume(entity);
					LogManager.getLogger().info(MODULE , "Code : " + statusLine.getStatusCode());
					LogManager.getLogger().info(MODULE,  "Reason : " + statusLine.getReasonPhrase());
					throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
				}
				
				String urlSrc = EntityUtils.toString(entity);
				LogManager.getLogger().info(MODULE ,"urlSrc : " + urlSrc);
				// parsing JSON
				JSONObject result = new JSONObject(urlSrc); // Convert String to
															// JSON Object
				String strDesc = result.get("DESCRIPTION").toString();
				String strRedirectUrl = result.get("REDIRECT_URL").toString();
	
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		// Put code here to process Payment Request
	}

	@Override
	public void processPaymentResponse(WebinRequest request, WebinResponse response) {
			LogManager.getLogger().debug(MODULE, "Processing payment gateway groovy response");
		// Put code here to process Payment Response
	}
	
	
	

	private JSONObject getJsonData(WebinRequest request, WebinResponse response) {
		JSONObject requestJSON = new JSONObject();
		   try {
			   String strAction = "INIT";
			   String strVersion = "1.0.5";
			   String strMerchantServiceId = "636";
			   String strMerchantOrderId =   getRandomMerchantOrderId();
			   String strPrice = request.getAttribute(WebinKeyConstants.PRICE);
			   String strRandomServiceId = getRandomServiceId();
			   String strDevice = "1";
			   String strlocal = "vi-VN";
			   String strCurrencyCode = "VND";
			   String strPaymentMethod = "VNPTPAY";
			   String strDescription = "Thanh toan don hang";
			   String strTimeVal = getCurrentTime();
			   String strFrameIp = getFramedIp(request);
			   String strSecretkey = "36e3c5b5a6d53fce2a2cc23d46ee5987";
			   String strPipe ="|";
			   
			   requestJSON.put("ACTION", strAction);
			   requestJSON.put("VERSION", strVersion);
			   requestJSON.put("MERCHANT_SERVICE_ID", strMerchantServiceId);
			   requestJSON.put("MERCHANT_ORDER_ID", strMerchantOrderId);
			   requestJSON.put("AMOUNT", strPrice);
			   requestJSON.put("AMOUNT_DETAIL", strPrice);
			   requestJSON.put("SERVICE_ID", strRandomServiceId);
			   requestJSON.put("DEVICE", strDevice);
			   requestJSON.put("LOCALE", strlocal);
			   requestJSON.put("CURRENCY_CODE", strCurrencyCode);
			   requestJSON.put("PAYMENT_METHOD", strPaymentMethod);
			   requestJSON.put("DESCRIPTION", strDescription);
			   requestJSON.put("CREATE_DATE", strTimeVal);
			   requestJSON.put("CLIENT_IP", strFrameIp);
			   
			   StringBuilder strBuilder = new StringBuilder();
			   strBuilder.append(strAction).append(strPipe).append(strVersion).append(strPipe).append(strMerchantServiceId).append(strPipe)
			   .append(strMerchantOrderId).append(strPipe).append(strPrice).append(strPipe).append(strPrice).append(strPipe).append(strRandomServiceId)
			   .append(strPipe).append(strDevice).append(strPipe).append(strlocal).append(strPipe).append(strCurrencyCode).append(strPipe)
			   .append(strPaymentMethod).append(strPipe).append(strDescription).append(strPipe).append(strTimeVal).append(strPipe).append(strFrameIp)
			   .append(strPipe).append(strSecretkey);
			   
			   LogManager.getLogger().debug(MODULE,"Parameters :- "+strBuilder.toString() );
			   String strSHA256Params = getSha_256Code(strBuilder.toString());
			   LogManager.getLogger().debug(MODULE,"Parameters Encoded:- "+strSHA256Params );
			   requestJSON.put("SECURE_CODE", strSHA256Params);
			   return requestJSON;
		   } catch (JSONException e) {
			   // TODO Auto-generated catch block
			   e.printStackTrace();
		   }
		   return requestJSON;
	   }
	
	private static String getRandomMerchantOrderId() {
		Random rand = new Random();
		String merchantOrderIdStr = "VNP";
		int merchantOrderId = rand.nextInt(99999999);
		// this will convert any number sequence into 8 character.
		String randomValue = String.format("%8d", merchantOrderId);
		merchantOrderIdStr = merchantOrderIdStr.concat(randomValue);
		if (merchantOrderIdStr.contains(" ")) {
			merchantOrderIdStr = merchantOrderIdStr.replace(" ", "T");
		}
		return merchantOrderIdStr;
	}
	
	
	private  String getRandomServiceId() {
		Random rand = new Random();
		String serviceIdStr = "SER";
		int serviceId = rand.nextInt(99999999);
		// this will convert any number sequence into 8 character.
		String randomValue = String.format("%8d", serviceId);
		return serviceIdStr.concat(randomValue);
	}
		
	private String getFramedIp(WebinRequest request) {
		String framedIpAddress = request.getAttribute("FramedIPAddress");
	}
	
	private String getCurrentTime()
	{
		// (1) get today's date
		Date today = Calendar.getInstance().getTime();
		// (2) create a date "formatter" (the date format we want)
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
		// (3) create a new String using the date format we want
		// (4) this prints "Folder Name = 20090906082323"
		return formatter.format(today);
	}
	
	private static String getSha_256Code(String value)
	{
		String strtest = "";
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
			return DatatypeConverter.printHexBinary(digest).toLowerCase();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return strtest;
	}
	
}
