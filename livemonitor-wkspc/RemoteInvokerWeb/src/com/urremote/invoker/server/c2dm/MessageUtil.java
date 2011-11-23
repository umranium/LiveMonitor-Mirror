package com.urremote.invoker.server.c2dm;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import java.net.URLConnection;
import java.util.Map;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
//import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class MessageUtil {
	private final static String AUTH = "authentication";

	private static final String UPDATE_CLIENT_AUTH = "Update-Client-Auth";

	public static final String PARAM_REGISTRATION_ID = "registration_id";

	public static final String PARAM_DELAY_WHILE_IDLE = "delay_while_idle";

	public static final String PARAM_COLLAPSE_KEY = "collapse_key";

	private static final String UTF8 = "UTF-8";
	
	private static Logger log = Logger.getLogger(MessageUtil.class.getName());

	public static void sendMessage(String deviceName, String registrationId, String collapseKey, Map<String,String> messages) throws IOException {
		
		String authToken = AuthenticationUtil.INSTANCE.getToken();
		
		StringBuilder postDataBuilder = new StringBuilder();
		postDataBuilder.append(PARAM_REGISTRATION_ID).append("=")
				.append(registrationId);
		postDataBuilder.append("&").append(PARAM_COLLAPSE_KEY).append("=")
				.append(collapseKey);
		for (Map.Entry<String, String> message:messages.entrySet()) {
			postDataBuilder.append("&").append("data."+message.getKey()).append("=").append(URLEncoder.encode(message.getValue(), UTF8));
		}

		byte[] postData = postDataBuilder.toString().getBytes(UTF8);

		// Hit the dm URL.

		URL url = new URL("https://android.clients.google.com/c2dm/send");
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setDoOutput(true);
		conn.setUseCaches(false);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded;charset=UTF-8");
		conn.setRequestProperty("Content-Length",
				Integer.toString(postData.length));
		conn.setRequestProperty("Authorization", "GoogleLogin auth="
				+ authToken);

		OutputStream out = conn.getOutputStream();
		out.write(postData);
		out.close();

        int responseCode = conn.getResponseCode();
        
        if (responseCode == 401 ||
                responseCode == 403) {
            // The token is too old - return false to retry later, will fetch the token 
            // from DB. This happens if the password is changed or token expires. Either admin
            // is updating the token, or Update-Client-Auth was received by another server, 
            // and next retry will get the good one from database.
            log.warning("Unauthorized - need token");
            return;
        }
        
        // Check for updated token header
        String updatedAuthToken = conn.getHeaderField(UPDATE_CLIENT_AUTH);
        if (updatedAuthToken != null && !authToken.equals(updatedAuthToken)) {
            log.info("Got updated auth token from C2DM servers: " +
                    updatedAuthToken);
            AuthenticationUtil.INSTANCE.setToken(updatedAuthToken);
        }
            
        String responseLine = new BufferedReader(new InputStreamReader(conn.getInputStream()))
            .readLine();
            
        // NOTE: You *MUST* use exponential backoff if you receive a 503 response code.
        // Since App Engine's task queue mechanism automatically does this for tasks that
        // return non-success error codes, this is not explicitly implemented here.
        // If we weren't using App Engine, we'd need to manually implement this.
        log.info("Got " + responseCode + " response from Google C2DM endpoint.");
        
        log.info("Response = "+responseLine);
        
        if (responseLine == null || responseLine.equals("")) {
            throw new IOException("Got empty response from Google C2DM endpoint.");
        }

        String[] responseParts = responseLine.split("=", 2);
        if (responseParts.length != 2) {
            log.warning("Invalid message from google: " + 
                    responseCode + " " + responseLine);
            throw new IOException("Invalid response from Google " + 
                    responseCode + " " + responseLine);
        }

        if (responseParts[0].equals("id")) {
            log.info("Successfully sent data message to device "+deviceName+": " + responseLine);
        }
        else
        if (responseParts[0].equals("Error")) {
            String err = responseParts[1];
            log.severe("Got error response from Google C2DM endpoint: " + err);
        } else {
            // 500 or unparseable response - server error, needs to retry
            log.severe("Invalid response from google " + responseLine + " " + 
                    responseCode);
        }
	}

}
