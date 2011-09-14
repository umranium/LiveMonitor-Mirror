/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.csiro.mapmymaps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

/**
 *
 * @author abd01c
 */
public class MapMyTracksInterfaceApi {
	
	private static final String MY_MAP_WEBSITE_URL = "www.mapmytracks.com";
	
	public static final URI URI_POST = URI.create("http://"+MY_MAP_WEBSITE_URL+"/api/");
	
	public static final String APP_NAME = "CSIRO_ANDROID_APP";
	
	private Pattern SERVER_TIME_PAT = Pattern.compile("<server_time>\\s*(\\d+)\\s*</server_time>");
	private Pattern ACTIVITIES_PAT = Pattern.compile("<id>\\s*(\\d+)\\s*</id>\\s*<title>(.+)</title>\\s*<date>\\s*(\\d+)\\s*</date>");
	private Pattern ACTIVITY_PAT = Pattern.compile("<complete>\\s*(\\w+)\\s*</complete>\\s*<points>(.*)</points>");
	private Pattern START_ACTIVITY_PAT = Pattern.compile("<type>\\s*(\\w+)\\s*</type>\\s*<activity_id>\\s*(\\d+)\\s*</activity_id>");
	private Pattern UPDATE_ACTIVITY_PAT = Pattern.compile("<type>\\s*(\\w+)\\s*</type>");
	private Pattern STOP_ACTIVITY_PAT = Pattern.compile("<type>\\s*(\\w+)\\s*</type>");
	
	private DefaultHttpClient client;
	private HttpPost httpPost;
	
	private ReusableNameValuePairMap serverTimeParamsMap;
	private ReusableNameValuePairMap activitiesParamsMap;
	private ReusableNameValuePairMap startActivityParamsMap;
	private ReusableNameValuePairMap updateActivityParamsMap;
	private ReusableNameValuePairMap stopActivityParamsMap;
	
	public MapMyTracksInterfaceApi(String username, String password) {
		client = new DefaultHttpClient();
		client.getCredentialsProvider().setCredentials(
				new AuthScope(MY_MAP_WEBSITE_URL, 80),
				new UsernamePasswordCredentials("umranium", "password123"));
		httpPost = new HttpPost(URI_POST);
		
		serverTimeParamsMap = new ReusableNameValuePairMap();
		serverTimeParamsMap.add("request", "get_time");
		
		activitiesParamsMap = new ReusableNameValuePairMap();
		activitiesParamsMap.add("request", "get_activities");
		activitiesParamsMap.add("author", "");
		
		startActivityParamsMap = new ReusableNameValuePairMap();
		startActivityParamsMap.add("request", "start_activity");
		startActivityParamsMap.add("title", "");
		startActivityParamsMap.add("tags", "");
		startActivityParamsMap.add("privacy", "");
		startActivityParamsMap.add("activity", "");
		startActivityParamsMap.add("source", APP_NAME);
		startActivityParamsMap.add("points", "");
		
		updateActivityParamsMap = new ReusableNameValuePairMap();
		updateActivityParamsMap.add("request", "update_activity");
		updateActivityParamsMap.add("activity_id", "");
		updateActivityParamsMap.add("points", "");
		updateActivityParamsMap.add("hr", "");
		updateActivityParamsMap.add("cad", "");
		updateActivityParamsMap.add("pwr", "");
		
		stopActivityParamsMap = new ReusableNameValuePairMap();
		stopActivityParamsMap.add("request", "stop_activity");
	}
	
	public void shutdown() {
		this.client.getConnectionManager().shutdown();
	}
	
	private String sendPost(ReusableNameValuePairMap nameValuePairMap) throws IOException
	{
		System.out.println("executing request:\n" + httpPost.getRequestLine());
		HttpEntity requestEntity = new UrlEncodedFormEntity(nameValuePairMap.getPairs());
		httpPost.setEntity(requestEntity);
		HttpResponse response = client.execute(httpPost);
		HttpEntity responseEntity = response.getEntity();

		System.out.println("----------------------------------------");
		System.out.println(response.getStatusLine());
		if (responseEntity != null) {
			InputStream content = responseEntity.getContent();
			System.out.println(responseEntity.getContentType());
			System.out.println("length="+responseEntity.getContentLength());
			BufferedReader rd = new BufferedReader(new InputStreamReader(content));
			StringBuilder buffer = new StringBuilder((int)responseEntity.getContentLength());
			String line;
			while ((line = rd.readLine()) != null) {
				buffer.append(line).append("\n");
			}
			String msg = buffer.toString();
			System.out.println(msg);
			return msg;
		} else {
			return null;
		}
	}
	
	public Long getServerTime() throws IOException
	{
		String reply = sendPost(serverTimeParamsMap);
		if (reply!=null) {
			Matcher matcher = SERVER_TIME_PAT.matcher(reply);
			if (matcher.find() && matcher.groupCount()==1) {
				String time = matcher.group(1);
				System.out.println("time="+time);
				return Long.parseLong(time)*1000;
			}
		}
		return null;
	}
	
	public Long startActivity(
			String title,
			String tags,
			boolean isPublic,
			ActivityType activityType,
			List<LocationPoint> points) throws IOException
	{
		startActivityParamsMap.update("title", title);
		
//		if (tags!=null && !tags.isEmpty()) {
//			StringBuilder tagBuilder = new StringBuilder();
//			boolean first = true;
//			for (String tag:tags) {
//				if (first)
//					first = false;
//				else
//					tagBuilder.append(",");
//				tagBuilder.append(tag);
//			}
//			startActivityParamsMap.update("tags", tagBuilder.toString());
//		} else {
//			startActivityParamsMap.update("tags", "");
//		}
		startActivityParamsMap.update("tags", tags);
		
		startActivityParamsMap.update("privacy", isPublic?"public":"private");
		startActivityParamsMap.update("activity", activityType.toString());
		
		StringBuilder locBuilder = new StringBuilder();
		for (LocationPoint lp:points) {
			locBuilder.append(lp.latitude).append(" ");
			locBuilder.append(lp.longitude).append(" ");
			locBuilder.append(lp.altitude).append(" ");
			locBuilder.append(lp.timeStamp/1000).append(" ");
		}
		startActivityParamsMap.update("points", locBuilder.toString());
		
		String reply = sendPost(startActivityParamsMap);
		if (reply!=null) {
			Matcher matcher = START_ACTIVITY_PAT.matcher(reply);
			if (matcher.find() && matcher.groupCount()==2) {
				String typeStr = matcher.group(1);
				String idStr = matcher.group(2);

				if (typeStr.equalsIgnoreCase("activity_started")) {
					return Long.parseLong(idStr);
				}
			}
		}

		return null;
	}
	
	public Boolean updateActivity(
			long activityId,
			List<LocationPoint> points,
			List<SensorData> sensorDatas
			) throws IOException
	{
		updateActivityParamsMap.update("request", "update_activity");
		updateActivityParamsMap.update("activity_id", Long.toString(activityId));
		
		StringBuilder locBuilder = new StringBuilder();
		for (LocationPoint lp:points) {
			locBuilder.append(lp.latitude).append(" ");
			locBuilder.append(lp.longitude).append(" ");
			locBuilder.append(lp.altitude).append(" ");
			locBuilder.append(lp.timeStamp/1000).append(" ");
		}
		updateActivityParamsMap.update("points", locBuilder.toString());
		
		if (sensorDatas!=null && !sensorDatas.isEmpty()) {
			StringBuilder hrBuilder = new StringBuilder();
			StringBuilder cadBuilder = new StringBuilder();
			StringBuilder pwrBuilder = new StringBuilder();
			for (SensorData sd:sensorDatas) {
				long tm = sd.timeStamp/1000;
				if (sd.heartRate!=null) {
					hrBuilder.append(sd.heartRate).append(" ")
							.append(tm).append(" ");
				}
				if (sd.cadence!=null) {
					cadBuilder.append(sd.cadence).append(" ")
							.append(tm).append(" ");
				}
				if (sd.power!=null) {
					pwrBuilder.append(sd.power).append(" ")
							.append(tm).append(" ");
				}
			}
			
			updateActivityParamsMap.update("hr", hrBuilder.toString());
			updateActivityParamsMap.update("cad", cadBuilder.toString());
			updateActivityParamsMap.update("pwr", pwrBuilder.toString());
		} else {
			updateActivityParamsMap.update("hr", "");
			updateActivityParamsMap.update("cad", "");
			updateActivityParamsMap.update("pwr", "");
		}
		
		String reply = sendPost(updateActivityParamsMap);
		if (reply!=null) {
			Matcher matcher = UPDATE_ACTIVITY_PAT.matcher(reply);
			if (matcher.find() && matcher.groupCount()==1) {
				String typeStr = matcher.group(1);

				if (typeStr.equalsIgnoreCase("activity_updated")) {
					return true;
				}
			}
		}
		return null;
	}
	
	
	public Boolean stopActivity() throws IOException
	{
		String reply = sendPost(stopActivityParamsMap);
		if (reply!=null) {
			Matcher matcher = UPDATE_ACTIVITY_PAT.matcher(reply);
			if (matcher.find() && matcher.groupCount()==1) {
				String typeStr = matcher.group(1);
				if (typeStr.equalsIgnoreCase("activity_stopped")) {
					return true;
				}
			}
		}
		return null;
	}
	
}

