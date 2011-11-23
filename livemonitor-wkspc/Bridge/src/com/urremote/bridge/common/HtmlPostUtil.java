package com.urremote.bridge.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

public class HtmlPostUtil {
	
	private static final List<HtmlPostUtil> CURRENT_POSTS = new ArrayList<HtmlPostUtil>();
	
	public interface PostResultListener {
		void OnError(Throwable e);
		void OnResult(String result);
	}
	
	private URI uri;
	private List<NameValuePair> nameValuePairs;
	private PostResultListener postResultListener;
	
	private Runnable post = new Runnable() {

		@Override
		public void run() {
			try {
				
				HttpClient client = new DefaultHttpClient();
				HttpPost post = new HttpPost(uri);
				post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				HttpResponse response = client.execute(post);
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				String line = "";
				StringBuilder respBuilder = new StringBuilder();
				while ((line = rd.readLine()) != null) {
					respBuilder.append(line).append("\n");
				}
				postResultListener.OnResult(respBuilder.toString());
			} catch (UnsupportedEncodingException e) {
				postResultListener.OnError(e);
			} catch (ClientProtocolException e) {
				postResultListener.OnError(e);
			} catch (IOException e) {
				postResultListener.OnError(e);
			}
			
			synchronized (CURRENT_POSTS) {
				CURRENT_POSTS.remove(HtmlPostUtil.this);
			}
		}
	};
	
	private UncaughtExceptionHandler uncaughtExceptionHandler = new UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(Thread thread, Throwable ex) {
			postResultListener.OnError(ex);
		}
	};
	
	private Thread thread;
	
	private HtmlPostUtil(PostResultListener postResultListener, URI uri, List<NameValuePair> nameValuePairs) {
		this.postResultListener = postResultListener;
		this.uri = uri;
		this.nameValuePairs = nameValuePairs;
		this.thread = new Thread(post);
		this.thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
		this.thread.start();
	}
	
	public void cancel() {
		thread.interrupt();
	}
	
	public static HtmlPostUtil asyncPost(PostResultListener postResultListener, URI uri, List<NameValuePair> nameValuePairs) {
		HtmlPostUtil post = new HtmlPostUtil(postResultListener, uri, nameValuePairs);
		synchronized (CURRENT_POSTS) {
			CURRENT_POSTS.add(post);
		}
		return post;
	}
	
	public static void cancelAllPosts() {
		synchronized (CURRENT_POSTS) {
			for (HtmlPostUtil post:CURRENT_POSTS) {
				post.cancel();
			}
		}
	}
	
	
	
}
