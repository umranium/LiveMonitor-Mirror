package au.csiro.umran.test.testupload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.BasicManagedEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import au.csiro.mapmymaps.LocationPoint;
import au.csiro.mapmymaps.SensorData;
import au.csiro.umran.test.testupload.R;

public class Main extends Activity {
	
	private static final String TAG = "TestUpload";
	private static final int MAX_MSG_COUNT = 30;
	private static final int NUM_OF_UPLOADERS = 5;
	public static final URI URI_DATA_UPLOAD = URI.create("http://testing-umranium.appspot.com/receive");
	
	private Button btnStartScan;
	private Button btnStopScan;
	private ListView lstMessages;
	private ArrayAdapter<String> lstMessagesAdapter;

	private Looper mainLooper;
	private Thread uiThread;
	private Handler uiHandler;
	private Producer producer;
	private Uploader[] uploaders = null;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		this.mainLooper = this.getMainLooper();
		this.uiThread = this.mainLooper.getThread();
		this.uiHandler = new Handler(mainLooper);
		
		btnStartScan = (Button) findViewById(R.id.btn_start_scan);
		btnStartScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startUpload();
			}
		});

		btnStopScan = (Button) findViewById(R.id.btn_stop_scan);
		btnStopScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopUpload();
			}
		});
		
		lstMessages = (ListView) findViewById(R.id.lst_messages);
		lstMessagesAdapter = new ArrayAdapter<String>(this, R.layout.msg_item);
		lstMessages.setAdapter(lstMessagesAdapter);
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		startUpload();
	}

	@Override
	protected void onStop() {
		super.onStop();

		stopUpload();
	}
	
	private void startUpload() {
		if (producer!=null) {
			producer.quit();
			producer = null;
		}
		if (uploaders!=null) {
			for (int i=0; i<NUM_OF_UPLOADERS; ++i)
				this.uploaders[i].quit();
			
			uploaders = null;
		}
		
		uploaders = new Uploader[NUM_OF_UPLOADERS];
		for (int i=0; i<NUM_OF_UPLOADERS; ++i) {
			uploaders[i] = new Uploader();
			uploaders[i].start();
		}
		
		producer = new Producer();
		producer.start();
		
		btnStartScan.setEnabled(false);
		btnStopScan.setEnabled(true);
	}
	
	private void stopUpload() {
		if (producer!=null) {
			producer.quit();
			producer = null;
		}
		if (uploaders!=null) {
			for (int i=0; i<NUM_OF_UPLOADERS; ++i)
				this.uploaders[i].quit();
			
			uploaders = null;
		}

		btnStartScan.setEnabled(true);
		btnStopScan.setEnabled(false);
	}
	

	private void displayMsg(final String msg) {
		if (!Thread.currentThread().equals(uiThread)) {
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					Main.this.displayMsg(msg);
				}
			});
			return;
		}
		
		int count = lstMessagesAdapter.getCount(); 
		for (int i=MAX_MSG_COUNT; i<count; ++i)
			lstMessagesAdapter.remove(lstMessagesAdapter.getItem(0));
		lstMessagesAdapter.add(msg);
	}
	
	private static class MyNameValuePair implements NameValuePair {
		
		public final String name;
		public String value;
		
		public MyNameValuePair(String name) {
			this.name = name;
			this.value = "";
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getValue() {
			return value;
		}
		
	}
	
	private class Consumable {
		long msgId;
	}
	
	private TwoWayBlockingQueue<Consumable> processingQueue =new TwoWayBlockingQueue<Main.Consumable>(100) {
		@Override
		protected Consumable getNewInstance() {
			return new Consumable();
		}
	};
	
	private long consumed = 0;
	
	private class Producer extends Thread {
		
		private boolean isQuiting = false;
		private long msgId = 0;
		
		private long timeTaken = 0;
		private long numOfRequests = 0;
		
		public void quit() {
			isQuiting = true;
			this.interrupt();
		}
		
		@Override
		public void run() {
			try {
				long lastCheckTime = System.currentTimeMillis();
				long lastConsumed = 0;
				
				while (!isQuiting) {
					Consumable consumable = processingQueue.takeEmptyInstance();
					/*
					consumable.msgId = msgId;
					++msgId;
					*/
					double startLat = -35.281974;
					double startLon = 149.128629;
					double stopLat = -35.248213;
					double stopLon = 149.13399;
					
					int steps = 100;
					
					LocationPoint point = new LocationPoint();
					point.latitude = startLat;
					point.longitude = startLon;
					point.altitude = 100;
					
					SensorData sensorData = new SensorData();
					
					
					point.timeStamp = System.currentTimeMillis();
					List<LocationPoint> points = Collections.singletonList(point);
					
					Long id = 
							api.startActivity(
								"myfirst",
								"apitest",
								false,
								ActivityType.CYCLING, 
								points);
					
					System.out.println("id = " + id);
					
					for (int i=0; i<steps; ++i) {
						
						System.out.println("\n"+(i+1)+"/"+steps);
						
						long now = System.currentTimeMillis();
						
						point.latitude = startLat + ((stopLat-startLat)*(i+1)/steps);
						point.longitude = startLon + ((stopLon-startLon)*(i+1)/steps);
						point.altitude = 100;
						point.timeStamp = now;
						
						sensorData.cadence = (int)(10+Math.sin(10*i/steps)*20);
						sensorData.heartRate = (int)(70+Math.sin(10*i/steps)*70);
						sensorData.power = (int)(50+Math.sin(10*i/steps)*75);
						sensorData.timeStamp = now;
					
						long start = System.currentTimeMillis();
						api.updateActivity(id,
								Collections.singletonList(point),
								Collections.singletonList(sensorData)
								);
						long stop = System.currentTimeMillis();
						System.out.println("update took: "+(stop-start)+"ms");
						
						Thread.sleep(1000);
					}
					
					processingQueue.returnFilledInstance(consumable);
					
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
					
					long currentConsumed = consumed;
					long currentCheckTime = System.currentTimeMillis();
					
					timeTaken += currentCheckTime - lastCheckTime;
					numOfRequests += currentConsumed - lastConsumed;
					
					Log.i(TAG, "Avg. Request Time: "+((double)timeTaken/(double)numOfRequests)+"ms");
					
					lastCheckTime = currentCheckTime;
					lastConsumed = currentConsumed;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
		}
	}
	
	private class Uploader extends Thread {
		
		private boolean isQuiting = false;
		
		public void quit() {
			isQuiting = true;
			this.interrupt();
		}
		
		@Override
		public void run() {
			DefaultHttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(URI_DATA_UPLOAD);
			MyNameValuePair msgIdValuePair = new MyNameValuePair("msgId"); 
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(msgIdValuePair);
			
			try {
				while (!isQuiting) {
					Consumable consumable = processingQueue.takeFilledInstance();
					long msgId = consumable.msgId;
					processingQueue.returnEmptyInstance(consumable);
					
					msgIdValuePair.value = Long.toString(msgId);
					post.setEntity(new UrlEncodedFormEntity(nameValuePairs));					
					
					Log.d(TAG, post.getRequestLine().toString());
					
					HttpResponse response = client.execute(post);

					int code = 0;
					response.getStatusLine().getStatusCode();
					String responseText = "";
					
					if (response.getStatusLine()!=null) {
						code = response.getStatusLine().getStatusCode();
						responseText = response.getStatusLine().getReasonPhrase();
					}
					
					if (response.getEntity() instanceof BasicManagedEntity) {
						BasicManagedEntity managedEntity = (BasicManagedEntity)response.getEntity();
						long contentLength = managedEntity.getContentLength();
						if (contentLength>2048)
							contentLength = 2048;
						if (contentLength>0) {
							InputStream inputStream = managedEntity.getContent();
							byte[] content = new byte[(int)contentLength];
							inputStream.read(content);
							responseText = new String(content);
						}
					} else {
						responseText = response.getEntity().toString();
					}
					
					Log.i(TAG, "Post Result: "+code+": "+responseText);
					displayMsg("Uploaded: "+msgId);
					++msgId;
					++consumed;
				}
			} catch (NoSuchElementException ex) {
				//	ignore
				Log.d(TAG, "No Message Found!");
			} catch (Exception ex) {
				Log.e(TAG, "Uploader Error", ex);
			} finally {
				client.getConnectionManager().shutdown();
			}
		}
	}


}