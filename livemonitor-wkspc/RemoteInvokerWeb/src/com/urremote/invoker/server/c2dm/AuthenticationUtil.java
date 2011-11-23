package com.urremote.invoker.server.c2dm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.urremote.invoker.server.common.Constants;

public class AuthenticationUtil {
	
	public static final AuthenticationUtil INSTANCE = new AuthenticationUtil(); 
	
	private static final Logger log = Logger.getLogger(AuthenticationUtil.class.getName());
	
	private final Object tokenSetMutex = new Object();
	private String token;
	private static long timeTokenSet = 0;
	
	private AuthenticationUtil() {
		try {
			this.token = fetchToken(Constants.C2DM_ACC_EMAIL, Constants.C2DM_ACC_PASSWORD);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error while attempting to obtain token", e);
		}
	}
	
	public void setToken(String newToken) {
		long currentTime = System.currentTimeMillis();
		synchronized (tokenSetMutex) {
			// if it has been set recently, return
			if (currentTime<timeTokenSet) {
				return;
			}
			
			token = newToken;
			timeTokenSet = currentTime;
		}
	}
	
	public String getToken() {
		long currentTime = System.currentTimeMillis();
		if (token!=null && currentTime-timeTokenSet<Constants.DURATION_UPDATE_C2DM_TOKEN) {
			return token;
		}

		synchronized (tokenSetMutex) {
			// check again
			currentTime = System.currentTimeMillis();
			if (token!=null && currentTime-timeTokenSet<Constants.DURATION_UPDATE_C2DM_TOKEN) {
				return token;
			}
			
			try {
				token = fetchToken(Constants.C2DM_ACC_EMAIL, Constants.C2DM_ACC_PASSWORD);
				timeTokenSet = currentTime;
				return token;
			} catch (IOException e) {
				log.log(Level.SEVERE, "Error while attempting to obtain token", e);
				return null;
			} 
		}
	}
	

	private static String fetchToken(String email, String password)
			throws IOException {
		// Create the post data
		// Requires a field with the email and the password
		StringBuilder builder = new StringBuilder();
		builder.append("Email=").append(email);
		builder.append("&Passwd=").append(password);
		builder.append("&accountType=GOOGLE");
		builder.append("&source=MyLittleExample");
		builder.append("&service=ac2dm");

		// Setup the Http Post
		byte[] data = builder.toString().getBytes();
		URL url = new URL("https://www.google.com/accounts/ClientLogin");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setUseCaches(false);
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
		con.setRequestProperty("Content-Length", Integer.toString(data.length));

		// Issue the HTTP POST request
		OutputStream output = con.getOutputStream();
		output.write(data);
		output.close();

		// Read the response
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				con.getInputStream()));
		String line = null;
		String auth_key = null;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("Auth=")) {
				auth_key = line.substring(5);
			}
		}

		// Finally get the authentication token
		// To something useful with it
		return auth_key;
	}	
	
}
