package com.urremote.bridge;

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

	private Button btnDone;

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
        btnDone = (Button)this.findViewById(R.id.btn_settings_done);
        
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
						addTag(input.getText().toString());
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
        
        btnDone.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		        SharedPreferences preferences = ActivitySettings.this.getSharedPreferences(Constants.SHARE_PREF, MODE_PRIVATE);
		    	saveState(preferences);
				ActivitySettings.this.setResult(RESULT_OK);
				ActivitySettings.this.finish();
			}
		});
        
        SharedPreferences preferences = this.getSharedPreferences(Constants.SHARE_PREF, MODE_PRIVATE);
    	loadState(preferences);
    	
    	setResult(RESULT_CANCELED);
    }
    
    private void loadState(SharedPreferences state)
    {
		txtActivityTitle.setText(DefSettings.getActivityTitle(state));    	
		chkIsPublic.setChecked(DefSettings.isPublic(state));
		spnActivityType.setSelection(spnActivityTypeAdapter.getPosition(DefSettings.getActivityType(state)));
    	String tags[] = DefSettings.getTags(state).split(",");
		lstTags.removeAllViews();
		for (String tag:tags) {
			if (tag.length()>0)
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
    	
    	StringBuilder builder = new StringBuilder();
    	for (int l=lstTags.getChildCount(), i=0; i<l; ++i) {
    		View v = lstTags.getChildAt(i);
    		TextView txt = (TextView) v.findViewById(R.id.txt_tag_list_item);
    		CharSequence tag = txt.getText();
    		if (i>0)
    			builder.append(",");
    		builder.append(tag);
    	}
    	editor.putString(Constants.KEY_TAGS, builder.toString());
    	
    	editor.commit();
    }
    
    private void addTag(String tag)
    {
        LayoutInflater vi = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.tag_lst_item, null);
        
        TextView txt = (TextView) v.findViewById(R.id.txt_tag_list_item);
        txt.setText(tag);
        View btn = v.findViewById(R.id.btn_del_tag);
        btn.setOnClickListener(new TagDelClickListener(v));
        
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
		}
    	
    }
        
}
