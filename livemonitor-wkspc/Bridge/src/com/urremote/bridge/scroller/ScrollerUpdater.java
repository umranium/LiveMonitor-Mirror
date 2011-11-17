package com.urremote.bridge.scroller;

import java.util.ArrayList;
import java.util.List;

import com.urremote.bridge.common.Constants;

import android.util.Log;
import android.widget.ArrayAdapter;

public class ScrollerUpdater {
	
	private static final ScrollerMessage NOT_CONNECTED_MESSAGE = new ScrollerMessage(0) {
		@Override
		public String toString() {
			return "PLEASE PRESS START TO BEGIN";
		}
	};
	
	private List<? extends ScrollerMessage> messageList;
	private ArrayAdapter<ScrollerMessage> lstAdapter;
	private long lastUpdateSystemMessage = 0;
	private ArrayList<ScrollerMessage> deleted = new ArrayList<ScrollerMessage>();
	
	public ScrollerUpdater(ArrayAdapter<ScrollerMessage> lstAdapter) {
		this.lstAdapter = lstAdapter;
		
		this.lstAdapter.clear();
		this.lstAdapter.add(NOT_CONNECTED_MESSAGE);
	}
	
	public ArrayAdapter<ScrollerMessage> getLstAdapter() {
		return lstAdapter;
	}
	
	public void setLstAdapter(ArrayAdapter<ScrollerMessage> lstAdapter) {
		this.lstAdapter = lstAdapter;
	}
	
	public List<? extends ScrollerMessage> getMessageList() {
		return messageList;
	}
	
	public void setMessageList(List<? extends ScrollerMessage> messageList) {
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":setMessageList()");
		this.messageList = messageList;
	}
	
	synchronized
	public void update()
	{
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":update()");
		
		if (messageList==null) return;
		
		long earliestMsg = Long.MAX_VALUE;
		if (!messageList.isEmpty())
			earliestMsg = messageList.get(0).timeStamp;
		
		deleted.clear();
		for (int i=0; i<lstAdapter.getCount(); ++i) {
			ScrollerMessage msg = lstAdapter.getItem(i); 
			if (msg.timeStamp<earliestMsg) {
				deleted.add(msg);
			} else {
				break;
			}
		}
		
		for (ScrollerMessage msg:deleted)
			lstAdapter.remove(msg);
		
		long newLastUpdate = lastUpdateSystemMessage;
		for (int i=0; i<messageList.size(); ++i) {
			ScrollerMessage msg = messageList.get(i);
			if (msg.timeStamp>lastUpdateSystemMessage) {
				lstAdapter.add(msg);
				if (msg.timeStamp>newLastUpdate) {
					newLastUpdate = msg.timeStamp;
				}
			}
		}
		lastUpdateSystemMessage = newLastUpdate;
	}

}
