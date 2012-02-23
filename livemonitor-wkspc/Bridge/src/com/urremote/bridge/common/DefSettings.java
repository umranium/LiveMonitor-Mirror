package com.urremote.bridge.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.urremote.bridge.mapmymaps.ActivityType;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class DefSettings {
	
	private static final String JSON_KEY_TAG = "tag";
	private static final String JSON_KEY_TIMESTAMP = "timestamp";
	
	public static class TagOptions {
		public String tag;
		public boolean timestamp;
		
		public TagOptions(String tag, boolean timestamp) {
			super();
			this.tag = tag;
			this.timestamp = timestamp;
		}
		
	}
	
	public static String getUsername(SharedPreferences state) {
		return state.getString(Constants.KEY_USERNAME, "");
	}

	public static String getPassword(SharedPreferences state) {
		return state.getString(Constants.KEY_PASSWORD, "");
	}
	
	public static boolean isActivityTitleTimestamped(SharedPreferences state) {
		return state.getBoolean(Constants.KEY_ACTIVITY_TITLE_TIMESTAMP, true);
	}

	public static String getActivityTitle(SharedPreferences state) {
		return state.getString(Constants.KEY_ACTIVITY_TITLE, "activity");
	}
	
	public static String compileActivityTitle(SharedPreferences state) {
		String title = state.getString(Constants.KEY_ACTIVITY_TITLE, "activity");
		if (isActivityTitleTimestamped(state)) {
			String timestamp = Constants.FMT_TITLE_TIMESTAMP.format(new Date(System.currentTimeMillis())).replace('/', '-');
			return title + "-" + timestamp.replaceAll("\\s+", "_");
		} else {
			return title;
		}
	}
	

	public static boolean isPublic(SharedPreferences state) {
		return state.getBoolean(Constants.KEY_IS_PUBLIC, true);
	}
	
	public static ActivityType getActivityType(SharedPreferences state) {
		int ord = state.getInt(Constants.KEY_ACTIVITY_TYPE,ActivityType.CYCLING.ordinal());
		return ActivityType.values()[ord];
	}
	
	public static String compileTags(SharedPreferences state) {
		
		String timestamp = Constants.FMT_TAG_TIMESTAMP.format(new Date(System.currentTimeMillis())).replace('/', '-');
		
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (TagOptions tag:getTagOptions(state))
			if (!tag.tag.isEmpty()) {
				if (first)
					first = false;
				else
					builder.append(",");
				
				builder.append(tag.tag);
				
				if (tag.timestamp) {
					builder.append("-").append(timestamp);
				}
			}
		
		return builder.toString();
	}
	
	public static List<TagOptions> getTagOptions(SharedPreferences state) {
		String tagString = state.getString(Constants.KEY_TIMESTAMPED_TAGS, "");
		if (tagString.length()>0) {
			try {
				JSONArray array = new JSONArray(tagString);
				ArrayList<TagOptions> tagOptions = new ArrayList<TagOptions>();
				for (int i=0; i<array.length(); ++i) {
					JSONObject jsonObject = array.getJSONObject(i);
					tagOptions.add(new TagOptions(
							jsonObject.getString(JSON_KEY_TAG),
							jsonObject.getBoolean(JSON_KEY_TIMESTAMP)));
				}
				return tagOptions;
			} catch (JSONException e) {
				Log.e(Constants.TAG, "Error while loading tags", e);
				return Collections.emptyList();
			}
		} else {
			return Collections.emptyList();
		}
	}
	
	public static void saveTagOptions(Editor editor, List<TagOptions> tagOptions) throws JSONException {
		JSONArray array = new JSONArray();
		for (TagOptions tag:tagOptions) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(JSON_KEY_TAG, tag.tag);
			jsonObject.put(JSON_KEY_TIMESTAMP, tag.timestamp);
			array.put(jsonObject);
		}
		editor.putString(Constants.KEY_TIMESTAMPED_TAGS, array.toString());
	}
	
}
