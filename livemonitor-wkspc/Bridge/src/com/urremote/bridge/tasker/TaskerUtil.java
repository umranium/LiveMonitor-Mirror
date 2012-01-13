package com.urremote.bridge.tasker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class TaskerUtil {
	
	private static final BroadcastReceiver br = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if ( intent.getBooleanExtra( TaskerIntent.EXTRA_SUCCESS_FLAG, false ) )
				Toast.makeText(context, "Start Task added to Tasker", Toast.LENGTH_LONG).show();
			else
				Toast.makeText(context, "Failure attempting to add 'Start' Task to Tasker", Toast.LENGTH_LONG).show();

			context.unregisterReceiver( br );
		}
	};
	
	public static void createStartTask(final Context context) {
		TaskerIntent task = new TaskerIntent("START_TASK");
		
		if (!TaskerIntent.taskerInstalled(context)) {
			throw new RuntimeException("Tasker is not installed.");
		}
		
		if (task.receiverExists(context)) {
			task.addAction(ActionCodes.SEND_ACTION_INTENT)
				.addArg("com.urremote.bridge.intent.START")
				.addArg(0)
				.addArg("")
				.addArg("param:true")
				.addArg("")
				.addArg(0)
				.setTaskPriority(9);
			
			// Setup a receiver to get a) when task finished b) success/failure
			// Note: you need to setup a new receiver for each task, because each
			// receiver is tuned to a particular broadcast task
		 
			// You probably want to unregister this if the user leaves your app e.g. in onPause
			// You may want to set a timeout in case e.g. Tasker's queue is full

			context.registerReceiver( br, task.getCompletionFilter() );

			// Start the task. This call exits immediately.

			context.sendBroadcast( task );
		} else {
			throw new RuntimeException("Tasker is not enabled.");
		}
	}

}
