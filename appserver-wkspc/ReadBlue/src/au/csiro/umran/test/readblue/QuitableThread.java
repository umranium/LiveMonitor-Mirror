package au.csiro.umran.test.readblue;

import android.content.Context;
import android.util.Log;

/**
 * 
 * @author Umran
 */
public abstract class QuitableThread extends Thread {
	
	private Context context;
	private boolean quit;
	
	public QuitableThread(Context context, String name) {
		super(name);
		this.context = context;
		this.quit = false;
	}
	
	/**
	 * Call to stop the thread
	 * If called from another thread, blocks until this thread is finished (not alive anymore)
	 * but if called from the same thread, then returns immediately.
	 */
	public void quit() {
		Log.d(Constants.TAG, this.getName()+" thread stopping.");
		this.quit = true;
		doQuit();
		if (!Thread.currentThread().equals(this)) {
			Log.d(Constants.TAG, this.getName()+" waiting for thread to finish.");
			while (this.isAlive()) {
				Thread.yield();
			}
		}
	}
	
	public boolean isQuiting() {
		return quit;
	}
	
	@Override
	public void run() {
		CustomUncaughtExceptionHandler.setInterceptHandler(context, this);
		
		try {
			Log.d(Constants.TAG, this.getName()+" thread started.");
			while (!this.quit) {
				doAction();
				Thread.yield();
			}
		} finally {
			doFinalize();
		}
		
		Log.d(Constants.TAG, this.getName()+" thread finished.");
	}
	
	/**
	 * Called either from within the thread, or from another thread, when
	 * requesting the thread to stop
	 */
	public abstract void doQuit();
	
	/**
	 * Called from within the thread to do a small section of the work
	 */
	public abstract void doAction();
	
	/**
	 * Called when the thread is about to exit from within the thread
	 */
	public abstract void doFinalize();
}