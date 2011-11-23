package com.urremote.invoker.client;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.urremote.invoker.channel.Channel;
import com.urremote.invoker.channel.ChannelFactory;
import com.urremote.invoker.channel.SocketListener;
import com.urremote.invoker.client.utils.SimpleDateFormat;
import com.urremote.invoker.shared.DeviceChannelMessage;
import com.urremote.invoker.shared.MessageParsingException;

public class DeviceDetailsEntryPoint implements EntryPoint {

	/**
	 * Create a remote service proxy to talk to the server-side service.
	 */
	private final DeviceServiceAsync deviceService = GWT.create(DeviceService.class);

	private static final String LAST_UPDATE_TIME_CONTAINER = "lastUpdatTimeContainer";
	private static final String IS_RECORDING_STATE_CONTAINER = "isRecordingStateContainer";
	private static final String START_RECORDING_BTN_CONTAINER = "startRecordingBtnContainer";
	private static final String STOP_RECORDING_BTN_CONTAINER = "stopRecordingBtnContainer";
	
	private native String getDeviceId() /*-{
		return $wnd.deviceId;
	}-*/;
	private native String getChannelToken() /*-{
		return $wnd.channelToken;
	}-*/;
	
	private boolean hasSetServerDeviceInterest = false;
	
	private SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss a dd MMM yyyy");

	@Override
	public void onModuleLoad() {

		String deviceId = getDeviceId();
		if (deviceId==null || deviceId.isEmpty()) {
			GWT.log("no device id found");
			return;
		}
		
		String channelToken = getChannelToken();
		if (channelToken==null || channelToken.isEmpty()) {
			GWT.log("no channel token found");
			return;
		}
		
		GWT.log("DeviceDetailsEntryPoint: channelToken="+channelToken);
		
		GWT.log("loading module");
		
		final Button startBtn = new Button("start");
		final Button stopBtn = new Button("stop");
		
		startBtn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				//	disable multiple clicks
				startBtn.setEnabled(false);
				stopBtn.setEnabled(false);
				deviceService.startDeviceRecording(getDeviceId(), new AsyncCallback<Void>() {
					@Override
					public void onSuccess(Void result) {
						startBtn.setEnabled(true);
						stopBtn.setEnabled(true);
					}
					
					@Override
					public void onFailure(Throwable caught) {
						onError("Error while sending device start request", caught);
						startBtn.setEnabled(true);
						stopBtn.setEnabled(true);
					}
				});
			}
		});
		
		stopBtn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				//	disable multiple clicks
				startBtn.setEnabled(false);
				stopBtn.setEnabled(false);
				deviceService.stopDeviceRecording(getDeviceId(), new AsyncCallback<Void>() {
					@Override
					public void onSuccess(Void result) {
						startBtn.setEnabled(true);
						stopBtn.setEnabled(true);
					}
					
					@Override
					public void onFailure(Throwable caught) {
						onError("Error while sending device stop request", caught);
						startBtn.setEnabled(true);
						stopBtn.setEnabled(true);
					}
				});
			}
		});
		
		RootPanel.get("startRecordingBtnContainer").add(startBtn);
		RootPanel.get("stopRecordingBtnContainer").add(stopBtn);
		
		setLastUpdateTime(null);
		setIsRecordingState(null);
		initializeChannel();
	}

	private void initializeChannel() {
		Channel channel = ChannelFactory.createChannel(getChannelToken());
		channel.open(new SocketListener() {
			@Override
			public void onMessage(String message) {
				try {
					if (message.endsWith("\r\n")) {
						message = message.substring(0, message.length()-2);
					}
					
					GWT.log("DeviceDetailsEntryPoint: Received Message: " + message);
					
					String device = getDeviceId();
					
					List<DeviceChannelMessage> channelMessages = DeviceChannelMessage.decode(message);
					
					for (DeviceChannelMessage channelMessage:channelMessages) {
						GWT.log("DeviceDetailsEntryPoint: Received message: "+channelMessage);
						
						if (channelMessage.getDevice().equals(device)) {
							switch (channelMessage.getMessageType()) {
							case UpdateTimeChanged: {
								setLastUpdateTime(channelMessage.getNewUpdateTime());
								break;
							}
							case IsRecordingStateChanged: {
								setIsRecordingState(channelMessage.getNewIsRecordingState());
								break;
							}
							}
						}
					}
				} catch (Exception e) {
					byte[] bytes = message.getBytes();
					GWT.log("Error while processing channel message: "+Arrays.toString(bytes), e);
				}
			}

			@Override
			public void onOpen() {
				GWT.log("DeviceDetailsEntryPoint: Channel open");
				fetchLastUpdateTime();
				fetchIsRecordingState();
			}
		});
	}
	
	private void fetchLastUpdateTime() {
		GWT.log("fetching device last update time");
		deviceService.getDeviceLastUpdate(getDeviceId(), new AsyncCallback<Long>() {
			@Override
			public void onSuccess(Long result) {
				GWT.log("device last update time received");
				setLastUpdateTime(result);
				if (!hasSetServerDeviceInterest) {
					hasSetServerDeviceInterest = true;
					setServerDeviceInterest();
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				onError("Error while fetching last device update time", caught);
			}
		});
	}
	
	private void fetchIsRecordingState() {
		GWT.log("fetching device recording state");
		deviceService.isDeviceRecordingState(getDeviceId(), new AsyncCallback<Boolean>() {
			
			@Override
			public void onSuccess(Boolean result) {
				GWT.log("device recording state received");
				setIsRecordingState(result);
				if (!hasSetServerDeviceInterest) {
					hasSetServerDeviceInterest = true;
					setServerDeviceInterest();
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				onError("Error while fetching device recording state", caught);
			}
		});
	}
	
	private void setServerDeviceInterest() {
		GWT.log("setting server device interest");
		deviceService.setChannelDeviceFilter(getChannelToken(), getDeviceId(),
				new AsyncCallback<Void>() {
					@Override
					public void onSuccess(Void result) {
					}

					@Override
					public void onFailure(Throwable caught) {
						onError("Error while setting interested device", caught);
					}
				});
	}
	
	private void setLastUpdateTime(Long time) {
		if (time==null)
			set(LAST_UPDATE_TIME_CONTAINER, new HTML("<i>Unknown</i>"));
		else
			set(LAST_UPDATE_TIME_CONTAINER, new HTML(dateFormat.format(new Date(time))));
	}

	private void setIsRecordingState(Boolean state) {
		if (state==null)
			set(IS_RECORDING_STATE_CONTAINER, new HTML("<i>Unknown</i>"));
		else
			set(IS_RECORDING_STATE_CONTAINER, new HTML(state.toString()));
	}
	
	private void onError(String msg, Throwable caught) {
		GWT.log(msg, caught);
		set("errorLabelContainer", new HTML(caught.getMessage()));
	}

	private void set(String container, Widget widget) {
		RootPanel rootPanel = RootPanel.get(container);
		if (rootPanel != null) {
			rootPanel.clear();
			rootPanel.add(widget);
		}
	}

	private void clear(String container) {
		RootPanel rootPanel = RootPanel.get(container);
		if (rootPanel != null) {
			rootPanel.clear();
		}
	}

}
