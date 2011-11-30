/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.urremote.bridge.mapmymaps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.location.Location;
import android.util.Log;

import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.urremote.bridge.common.Constants;

/**
 *
 * @author abd01c
 */
public class MapMyTracksInterfaceApi {
	
	private static final String MY_MAP_WEBSITE_URL = "www.mapmytracks.com";
	
	public static final URI URI_POST = URI.create("http://"+MY_MAP_WEBSITE_URL+"/api/");
	
	public static final String APP_NAME = "get Avocado Activity Classifier";
	
	public static final boolean DEBUG = false;//Constants.IS_TESTING; 
	public static final String TAG = "livemonitor-html";
	
	private static final Pattern SERVER_TIME_PAT = Pattern.compile("<server_time>\\s*(\\d+)\\s*</server_time>");
	private static final Pattern REPLY_TYPE_PAT = Pattern.compile("<type>\\s*(\\w+)\\s*</type>");
	private static final Pattern ACTIVITY_ID_PAT = Pattern.compile("<activity_id>\\s*(\\d+)\\s*</activity_id>");
	private static final Pattern REASON_PAT = Pattern.compile("<reason>(.*)</reason>");
	private static final Pattern COMPLETE_PAT = Pattern.compile("<complete>(.*)</complete>");
	
	private DefaultHttpClient client;
	private HttpPost httpPost;
	
	private ReusableNameValuePairMap serverTimeParamsMap;
	private ReusableNameValuePairMap activitiesParamsMap;
	private ReusableNameValuePairMap startActivityParamsMap;
	private ReusableNameValuePairMap updateActivityParamsMap;
	private ReusableNameValuePairMap stopActivityParamsMap;
	private ReusableNameValuePairMap queryActivityParamsMap;
	
	private int instanceId;
	
	public MapMyTracksInterfaceApi(int instanceId, String username, String password) {
		this.instanceId = instanceId;
		
		client = new DefaultHttpClient();
		client.getCredentialsProvider().setCredentials(
				new AuthScope(MY_MAP_WEBSITE_URL, 80),
				new UsernamePasswordCredentials(username, password));
		HttpParams params = client.getParams();
		HttpConnectionParams.setConnectionTimeout(params, 30000);
		HttpConnectionParams.setSoTimeout(params, 30000);
		
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

		queryActivityParamsMap = new ReusableNameValuePairMap();
		queryActivityParamsMap.add("request", "get_activity");
		queryActivityParamsMap.add("activity_id", "");
		queryActivityParamsMap.add("from_time", "");
		
	}
	
	public void shutdown() {
		this.client.getConnectionManager().shutdown();
	}
	
	private String sendPost(ReusableNameValuePairMap nameValuePairMap) throws IOException
	{
		if (DEBUG) {
			Log.d(TAG, instanceId+": executing request:\n" + httpPost.getRequestLine());
			
			for (ReusableBasicNameValuePair pair:nameValuePairMap.getPairs()) {
				Log.d(TAG, instanceId+": \t"+pair.getName()+"="+pair.getValue());
			}
		}
		
		HttpEntity requestEntity = new UrlEncodedFormEntity(nameValuePairMap.getPairs());
		httpPost.setEntity(requestEntity);
		HttpResponse response = client.execute(httpPost);
		HttpEntity responseEntity = response.getEntity();

		if (DEBUG) {
			Log.d(TAG, instanceId+": ----------------------------------------");
			Log.d(TAG, instanceId+": "+response.getStatusLine().toString());
		}
		if (responseEntity != null) {
			InputStream content = responseEntity.getContent();
			if (DEBUG) {
				Log.d(TAG, instanceId+": "+responseEntity.getContentType().toString());
				Log.d(TAG, instanceId+": length="+responseEntity.getContentLength());
			}
			BufferedReader rd = new BufferedReader(new InputStreamReader(content));
			StringBuilder buffer = new StringBuilder((int)responseEntity.getContentLength());
			String line;
			while ((line = rd.readLine()) != null) {
				buffer.append(line).append("\n");
			}
			String msg = buffer.toString();
			if (DEBUG)
				Log.d(TAG, instanceId+": "+msg);
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
				if (DEBUG)
					Log.i(TAG, "server-time="+time);
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
			List<Location> points) throws MapMyMapsException, IOException
	{
		startActivityParamsMap.update("title", title);
		
		startActivityParamsMap.update("tags", tags);
		startActivityParamsMap.update("privacy", isPublic?"public":"private");
		startActivityParamsMap.update("activity", activityType.toString());
		
		StringBuilder locBuilder = new StringBuilder();
		for (Location lp:points) {
			double timeSecs = (double)lp.getTime() / 1000.0;
			long timeStamp = Math.round(timeSecs);
			
			locBuilder.append(lp.getLatitude()).append(" ");
			locBuilder.append(lp.getLongitude()).append(" ");
			locBuilder.append(lp.getAltitude()).append(" ");
			locBuilder.append(timeStamp).append(" ");
		}
		startActivityParamsMap.update("points", locBuilder.toString());
		
		String reply = sendPost(startActivityParamsMap);
		if (reply!=null) {
			String[] type = matchOne(REPLY_TYPE_PAT, reply);
			if (type==null) {
				throw new MapMyMapsException("Unparsable reply: "+reply);
			}
			
			if (type[0].equalsIgnoreCase("activity_started")) {
				String[] idStr = matchOne(ACTIVITY_ID_PAT, reply);
				if (idStr==null) {
					throw new MapMyMapsException("Invalid format for 'activity_started' reply type: "+reply);
				}
				return Long.parseLong(idStr[0]);
			} else {
				parseError(type, reply);
				throw new MapMyMapsException("Unexpected server reply type: '"+type[0]+"'");
			}
		} else
			throw new MapMyMapsException("Server did not reply. Service appears to be down.");
	}
	
	public Boolean updateActivity(
			long activityId,
			List<Location> points,
			List<SensorDataSet> sensorDatas
			) throws MapMyMapsException, IOException
	{
		updateActivityParamsMap.update("request", "update_activity");
		updateActivityParamsMap.update("activity_id", Long.toString(activityId));
		
		StringBuilder locBuilder = new StringBuilder();
		for (Location lp:points) {
			double timeSecs = (double)lp.getTime() / 1000.0;
			long timeStamp = Math.round(timeSecs);
			
			locBuilder.append(lp.getLatitude()).append(" ");
			locBuilder.append(lp.getLongitude()).append(" ");
			locBuilder.append(lp.getAltitude()).append(" ");
			locBuilder.append(timeStamp).append(" ");
		}
		updateActivityParamsMap.update("points", locBuilder.toString());
		
		if (sensorDatas!=null && !sensorDatas.isEmpty()) {
			StringBuilder hrBuilder = new StringBuilder();
			StringBuilder cadBuilder = new StringBuilder();
			StringBuilder pwrBuilder = new StringBuilder();
			for (SensorDataSet sd:sensorDatas)
			if (sd.hasCreationTime()) {
				double timeSecs = (double)sd.getCreationTime() / 1000.0;
				long timeStamp = Math.round(timeSecs);
				
				if (sd.hasHeartRate()) {
					hrBuilder.append(sd.getHeartRate().getValue()).append(" ")
							.append(timeStamp).append(" ");
				}
				if (sd.hasCadence()) {
					cadBuilder.append(sd.getCadence().getValue()).append(" ")
							.append(timeStamp).append(" ");
				}
				if (sd.hasPower()) {
					pwrBuilder.append(sd.getPower().getValue()).append(" ")
							.append(timeStamp).append(" ");
				}
			}
			
			updateActivityParamsMap.update("hr", hrBuilder.toString());
			updateActivityParamsMap.update("cad", cadBuilder.toString());
			if (!Constants.IS_CRIPLED) {
				updateActivityParamsMap.update("pwr", pwrBuilder.toString());
			}
		} else {
			updateActivityParamsMap.update("hr", "");
			updateActivityParamsMap.update("cad", "");
			if (!Constants.IS_CRIPLED) {
				updateActivityParamsMap.update("pwr", "");
			}
		}
		
		String reply = sendPost(updateActivityParamsMap);
		if (reply!=null) {
			String[] type = matchOne(REPLY_TYPE_PAT, reply);
			if (type==null) {
				throw new MapMyMapsException("Unparsable reply: "+reply);
			}
			
			if (type[0].equalsIgnoreCase("activity_updated")) {
				return Boolean.TRUE;
			} else {
				parseError(type, reply);
				throw new MapMyMapsException("Unexpected server reply type: '"+type[0]+"'");
			}
		} else
			throw new MapMyMapsException("Server did not reply. Service appears to be down.");
	}
	
	
	public Boolean stopActivity() throws MapMyMapsException, IOException
	{
		String reply = sendPost(stopActivityParamsMap);
		if (reply!=null) {
			String[] type = matchOne(REPLY_TYPE_PAT, reply);
			if (type==null) {
				throw new MapMyMapsException("Unparsable reply: "+reply);
			}
			
			if (type[0].equalsIgnoreCase("activity_stopped")) {
				return Boolean.TRUE;
			} else {
				parseError(type, reply);
				throw new MapMyMapsException("Unexpected server reply type: '"+type[0]+"'");
			}
		} else
			throw new MapMyMapsException("Server did not reply. Service appears to be down.");
	}
	
	public Boolean isActivityRecording(long activityId) throws IOException, MapMyMapsException
	{
		long timeSecs = System.currentTimeMillis() / 1000;
		queryActivityParamsMap.update("activity_id", Long.toString(activityId));
		queryActivityParamsMap.update("from_time", Long.toString(timeSecs));
		String reply = sendPost(queryActivityParamsMap);
		if (reply!=null) {
			String[] type = matchOne(COMPLETE_PAT, reply);
			if (type==null) {
				throw new MapMyMapsException("Unparsable reply: "+reply);
			}
			
			if (type[0].equalsIgnoreCase("yes")) {
				return Boolean.FALSE; // not complete, hence recording
			} if (type[0].equalsIgnoreCase("no")) {
				return Boolean.TRUE; // complete, hence not recording anymore
			} else {
				parseError(type, reply);
				throw new MapMyMapsException("Unexpected server reply type: '"+type[0]+"'");
			}
		} else
			throw new MapMyMapsException("Server did not reply. Service appears to be down.");
	}
	
	private static String[] matchOne(Pattern pattern, String input)
	{
		Matcher matcher;
		
		matcher = pattern.matcher(input);
		if (matcher.find()) {
			String[] result = new String[matcher.groupCount()];
			
			for (int i=0; i<matcher.groupCount(); ++i) {
				result[i] = matcher.group(i+1);
			}
			
			return result;
		} else {
			return null;
		}
	}
	
	private static void parseError(String[] type, String reply) throws MapMyMapsException
	{
		if (type[0].equalsIgnoreCase("error")) {
			String[] reason = matchOne(REASON_PAT, reply);
			if (reason==null) {
				throw new MapMyMapsException("Invalid format for 'error' reply type: "+reply);
			} else
				throw new MapMyMapsException(reason[0]);
		}
	}
	
}

