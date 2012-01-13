package com.urremote.extractor;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class TrackExtractorActivity extends Activity {
	
//	private static final DateFormat DATETIME_FMT = new SimpleDateFormat(Constants.STD_DATE_TIME_FORMAT);

	private Button btnExtract;
	private Extractor extractor;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		btnExtract = (Button) findViewById(R.id.btn_extract);
		
		extractor = new Extractor(this.getApplicationContext());

		btnExtract.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				btnExtract.setEnabled(false);
				Thread th = new Thread() {
					public void run() {
						extractor.extract();
						TrackExtractorActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								btnExtract.setEnabled(true);
							}
						});
						
					};
				};
				th.start();
			}
		});
	}
	
}