package au.csiro.livemonitor.service.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import au.csiro.livemonitor.common.Constants;

/**
 * 
 * A utility class that helps to set the service
 * from background to foreground and vice versa
 * 
 * @author abd01c
 *
 */
public class ServiceForegroundUtil {
    
    private NotificationManager notificationManager;
	private int notificationId;
	private Service service;
	private Class<? extends Activity> mainActivity;
	private Notification notification;
	private PendingIntent notificationContentIntent;
	private boolean isForeground;
	
	public ServiceForegroundUtil(Service service, Class<? extends Activity> mainActivity, int notificationId) {
		this.notificationManager = (NotificationManager)service.getSystemService(Context.NOTIFICATION_SERVICE);
		this.notificationId = notificationId;
		this.service = service;
		this.mainActivity = mainActivity;
		this.isForeground = isServiceForeground(service);
	}
	
	private void createNotification(int icon, String ticker) {
		this.notification = new Notification(icon, ticker, System.currentTimeMillis());		
		this.notification.defaults = 0;
		this.notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
	}
	
	public void updateNotification(String title, String text) {
		Context context = service.getApplicationContext();
		Intent notificationIntent = new Intent(context, mainActivity);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
				//| Intent.FLAG_ACTIVITY_CLEAR_TOP
				);
		this.notificationContentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);		
		this.notification.setLatestEventInfo(context, title, text, this.notificationContentIntent);
		if (this.isForeground) {
			notificationManager.notify(notificationId, notification);
		}
	}
	
	public void setToForeground(int icon, String ticker, String title, String text)
	{
		if (this.isForeground) {
			updateNotification(title, text);
		} else {
			createNotification(icon, ticker);
			updateNotification(title, text);
			service.startForeground(notificationId, notification);
			this.isForeground = true;
			Log.d(Constants.TAG, "Service now in foreground mode");
		}
	}
	
	public void cancelForeground()
	{
		service.stopForeground(true);
		this.isForeground = false;
	}
	
	public boolean isForeground()
	{
		return this.isForeground;
	}

	private static boolean isServiceForeground(Service service){
        boolean serviceForeground = false;
        ActivityManager am = (ActivityManager)service.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = am.getRunningServices(50);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : serviceList) {
            if(runningServiceInfo.service.getClassName().equals(service.getClass().getName())){
            	Log.d(Constants.TAG, "running:"+runningServiceInfo.service.getClassName()+" fg="+runningServiceInfo.foreground);
                serviceForeground = runningServiceInfo.foreground;
                break;
            }
        }
        return serviceForeground;
    }	
}
