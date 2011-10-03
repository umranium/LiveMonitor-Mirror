package au.urremote.bridge.common;

import android.content.SharedPreferences;
import au.urremote.bridge.mapmymaps.ActivityType;

public class DefSettings {
	
	public static String getUsername(SharedPreferences state) {
		return state.getString(Constants.KEY_USERNAME, "");
	}

	public static String getPassword(SharedPreferences state) {
		return state.getString(Constants.KEY_PASSWORD, "");
	}

	public static String getActivityTitle(SharedPreferences state) {
		return state.getString(Constants.KEY_ACTIVITY_TITLE, "activity");
	}

	public static boolean isPublic(SharedPreferences state) {
		return state.getBoolean(Constants.KEY_IS_PUBLIC, false);
	}
	
	public static ActivityType getActivityType(SharedPreferences state) {
		int ord = state.getInt(Constants.KEY_ACTIVITY_TYPE,ActivityType.CYCLING.ordinal());
		return ActivityType.values()[ord];
	}
	
	public static String getTags(SharedPreferences state) {
		return state.getString(Constants.KEY_TAGS, "");
	}
	
}
