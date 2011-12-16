package com.urremote.bridge;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.urremote.bridge.R;

import com.urremote.bridge.common.Constants;
import com.urremote.bridge.common.DefSettings;
import com.urremote.bridge.mapmymaps.ActivityType;

public class ActivitySettings extends Activity {

	private TextView txtActivityTitle;
	private CheckBox chkIsPublic;
	private Spinner spnActivityType;
	private ArrayAdapter<ActivityType> spnActivityTypeAdapter;
	private Button btnAddTag;
	private LinearLayout lstTags;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        txtActivityTitle = (TextView)this.findViewById(R.id.txt_activity_title);
        chkIsPublic = (CheckBox)this.findViewById(R.id.chk_is_public);
        spnActivityType = (Spinner)this.findViewById(R.id.spn_activity_type);
        spnActivityTypeAdapter = new ArrayAdapter<ActivityType>(this, R.layout.activity_type_spn_item);
        for (ActivityType type:ActivityType.values())
        	spnActivityTypeAdapter.add(type);
        spnActivityType.setAdapter(spnActivityTypeAdapter);
        btnAddTag = (Button)this.findViewById(R.id.btn_add_tag);
        lstTags = (LinearLayout)this.findViewById(R.id.lst_tags);
        
        txtActivityTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					save();
				}
			}
		});
        
        chkIsPublic.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				save();
			}
		});
        
        btnAddTag.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(Constants.TAG, "btnAddTag clicked");
				
				AlertDialog.Builder alert = new AlertDialog.Builder(ActivitySettings.this);

				alert.setTitle("Add Tag");
				alert.setMessage("Please enter tag to add:");

				// Set an EditText view to get user input 
				final EditText input = new EditText(ActivitySettings.this);
				alert.setView(input);

				alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String tag = input.getText().toString();
						DefSettings.TagOptions options = new DefSettings.TagOptions(tag, false);
						addTag(options);
				        save();
						lstTags.requestFocus();
					}
				});
				
				alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						
					}
				});
				
				alert.show();
			}
		});
        
        SharedPreferences preferences = this.getSharedPreferences(Constants.SHARE_PREF, MODE_PRIVATE);
    	loadState(preferences);
    	
    	setResult(RESULT_CANCELED);
    }
    
    private void save() {
        SharedPreferences preferences = ActivitySettings.this.getSharedPreferences(Constants.SHARE_PREF, MODE_PRIVATE);
    	saveState(preferences);
    }
    
    @Override
    public void onBackPressed() {
    	save();
		setResult(RESULT_OK);
		finish();
    }
    
    private void loadState(SharedPreferences state)
    {
		txtActivityTitle.setText(DefSettings.getActivityTitle(state));    	
		chkIsPublic.setChecked(DefSettings.isPublic(state));
		spnActivityType.setSelection(spnActivityTypeAdapter.getPosition(DefSettings.getActivityType(state)));
		
		lstTags.removeAllViews();
		List<DefSettings.TagOptions> tags = DefSettings.getTagOptions(state);
		for (DefSettings.TagOptions tag:tags) {
			addTag(tag);
		}
    }
    
    private void saveState(SharedPreferences state)
    {
    	Editor editor = state.edit();
    	editor.putBoolean(Constants.KEY_ACTIVITY_SETTINGS_AVAILABLE, true);
    	editor.putString(Constants.KEY_ACTIVITY_TITLE, new String(new StringBuilder(txtActivityTitle.getText())));
    	editor.putBoolean(Constants.KEY_IS_PUBLIC, chkIsPublic.isChecked());
    	editor.putInt(Constants.KEY_ACTIVITY_TYPE, ((ActivityType)spnActivityType.getSelectedItem()).ordinal());
    	
    	List<DefSettings.TagOptions> options = new ArrayList<DefSettings.TagOptions>();
    	for (int l=lstTags.getChildCount(), i=0; i<l; ++i) {
    		View v = lstTags.getChildAt(i);
    		TagUi tagUi = new TagUi(v);
    		
    		String tag = (new StringBuilder(tagUi.txtTag.getText())).toString();
    		options.add(new DefSettings.TagOptions(tag, tagUi.chkTimestamp.isChecked()));
    	}
    	try {
			DefSettings.saveTagOptions(editor, options);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
    	
    	editor.commit();
    }
    
    private void addTag(DefSettings.TagOptions tagOptions)
    {
        LayoutInflater vi = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.tag_lst_item, null);
        
        TagUi tagUi = new TagUi(v);
        tagUi.txtTag.setText(tagOptions.tag);
        tagUi.chkTimestamp.setChecked(tagOptions.timestamp);
        tagUi.btnDel.setOnClickListener(new TagDelClickListener(v));
        tagUi.chkTimestamp.setOnClickListener(new TagTimestampClickListener(v));
        
        lstTags.addView(v);
    }
    
    public class TagDelClickListener implements View.OnClickListener {
    	
    	private View view;
    	
    	public TagDelClickListener(View view) {
    		this.view = view;
		}

		@Override
		public void onClick(View v) {
			lstTags.removeView(view);
			save();
		}
    	
    }
    
    public class TagTimestampClickListener implements View.OnClickListener {
    	
    	private View view;
    	
    	public TagTimestampClickListener(View view) {
    		this.view = view;
		}

		@Override
		public void onClick(View v) {
			final TagUi tagUi = new TagUi(view);
			final boolean wasChecked = !tagUi.chkTimestamp.isChecked();
			String tag = (new StringBuilder(tagUi.txtTag.getText())).toString();
			
			AlertDialog.Builder alert = new AlertDialog.Builder(ActivitySettings.this);

			if (wasChecked) {
				alert.setTitle("Remove Timestamp");
				alert.setMessage("Are you sure you DON'T want timestamps appended to the tag '"+tag+"' in the future?");
			} else {
				alert.setTitle("Append Timestamp");
				alert.setMessage("Are you sure you WANT timestamps appended to the tag '"+tag+"' in the future?");
			}

			alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					tagUi.chkTimestamp.setChecked(!wasChecked);
					save();
				}
			});
			
			alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					tagUi.chkTimestamp.setChecked(wasChecked);
					save();
				}
			});
			
			alert.show();
		}
    	
    }
    
    private class TagUi {
    	TextView txtTag;
    	Button btnDel;
    	CheckBox chkTimestamp;
    	
    	public TagUi(View parent) {
    		txtTag = (TextView)parent.findViewById(R.id.txt_tag_list_item);
    		btnDel = (Button)parent.findViewById(R.id.btn_del_tag);
    		chkTimestamp = (CheckBox)parent.findViewById(R.id.chk_timestamp);
		}
    }
        
}
