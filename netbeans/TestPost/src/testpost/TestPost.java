/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package testpost;

import au.csiro.mapmymaps.LocationPoint;
import au.csiro.mapmymaps.SensorData;
import au.csiro.mapmymaps.ActivityType;
import au.csiro.mapmymaps.MapMyTracksInterfaceApi;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 *
 * @author abd01c
 */
public class TestPost {

	//from: -35.281974,149.128629
	//to: -35.248213,149.13399
	
	public static void main(String[] args) {
		MapMyTracksInterfaceApi api = new MapMyTracksInterfaceApi("umranium", "password123");
		try {
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
			
			Boolean stopped = api.stopActivity();
			System.out.println("stopped = " + stopped);
			
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
		} finally {
			api.shutdown();
		}
	}
	
}
