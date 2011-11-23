package com.urremote.invoker.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.urremote.invoker.channel.Channel;
import com.urremote.invoker.channel.ChannelFactory;
import com.urremote.invoker.channel.SocketListener;
import com.urremote.invoker.shared.DeviceChannelMessage;
import com.urremote.invoker.shared.MessageParsingException;

public class DeviceListEntryPoint implements EntryPoint {

	/**
	 * Create a remote service proxy to talk to the server-side service.
	 */
	private final DeviceServiceAsync deviceService = GWT.create(DeviceService.class);
	
	private static final String DEVICES_HEADER_CONTAINER = "devicesHeaderContainer";
	private static final String DEVICE_NAMES_CONTAINER = "deviceNamesContainer";
	
	private Set<String> displayedDevices = new TreeSet<String>(new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			return o1.compareToIgnoreCase(o2);
		}
	}); 
	
	private native String getChannelToken() /*-{
		return $wnd.channelToken;
	}-*/;
	
	
	@Override
	public void onModuleLoad() {
		
		String channelToken = getChannelToken();
		if (channelToken==null || channelToken.isEmpty()) {
			GWT.log("no channel token found");
			return;
		}

		GWT.log("DeviceListEntryPoint: channelToken="+channelToken);
		
		set(DEVICES_HEADER_CONTAINER, new HTML("<b>Establishing channel to server</b>"));
		initializeChannel();
	}
	
	private void initializeChannel() {
		Channel channel = ChannelFactory.createChannel(getChannelToken());
		channel.open(new SocketListener() {
		        @Override
		        public void onMessage(String msg) {
		        	
					try {
						if (msg.endsWith("\r\n")) {
							msg = msg.substring(0, msg.length()-2);
						}
						
			        	GWT.log("DeviceListEntryPoint: Received Message: "+msg);
			        	
						List<DeviceChannelMessage> channelMessages = DeviceChannelMessage.decode(msg);
						
						boolean changedList = false;
						
						for (DeviceChannelMessage channelMessage:channelMessages) {
							GWT.log("DeviceListEntryPoint: Received Message: "+channelMessage);
							
							switch (channelMessage.getMessageType()) {
							case DeviceAdded:
							{
								synchronized (displayedDevices) {
									displayedDevices.add(channelMessage.getDevice());
								}
								changedList = true;
								GWT.log(channelMessage.getDevice()+" added.");
								GWT.log(displayedDevices.toString());
								break;
							}
							case DeviceDeleted:
							{
								synchronized (displayedDevices) {
									displayedDevices.remove(channelMessage.getDevice());
								}
								changedList = true;
								GWT.log(channelMessage.getDevice()+" deleted.");
								GWT.log(displayedDevices.toString());
								break;
							}
							}
						}

						if (changedList) {
							List<String> devices = null;
							synchronized (displayedDevices) {
								devices = new ArrayList<String>(displayedDevices);
							}
							loadDevices(devices);
						}
					} catch (Exception e) {
						byte[] bytes = msg.getBytes();
						GWT.log("Error while processing channel message: "+Arrays.toString(bytes), e);
					}
					
					
		        }
		        @Override
		        public void onOpen() {
		        	GWT.log("DeviceListEntryPoint: Channel Open");
		    		set(DEVICES_HEADER_CONTAINER, new HTML("<b>Loading devices</b>"));
		        	initLoadDevices();
		        }
		});
	}

	private void initLoadDevices() {
		set(DEVICES_HEADER_CONTAINER, new HTML("<b>Loading Devices</b>"));
		set(DEVICE_NAMES_CONTAINER, new HTML("<b>loading..</b>"));

		deviceService.getDeviceList(new AsyncCallback<List<String>>() {

			@Override
			public void onSuccess(List<String> result) {
				loadDevices(result);
			}

			@Override
			public void onFailure(Throwable caught) {
				onError("Error while fetching device list", caught);
			}
		});
	}
	
	private void loadDevices(List<String> devices) {

		synchronized (displayedDevices) {
			displayedDevices.clear();
			displayedDevices.addAll(devices);
			
			devices.clear();
			devices.addAll(displayedDevices);
		}
		

		Map<String,Widget> widgetMap = new HashMap<String,Widget>();
		
		StringBuilder htmlBuilder = new StringBuilder();
		htmlBuilder.append("<table width=\"100%\">");
		for (int deviceIndex = 0; deviceIndex < devices.size(); ++deviceIndex) {
			final String device = devices.get(deviceIndex);
			String delBtnName = "delBtn"+(deviceIndex+1);
			
			Button delBtn = new Button("delete");
			delBtn.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					deviceService.deleteDevice(device, new AsyncCallback<Void>(){
						
						@Override
						public void onSuccess(Void result) {
						}
						
						@Override
						public void onFailure(Throwable caught) {
							onError("Error while trying to delete device", caught);
						}
						
					});
				}
			});
			
			
			String viewDeviceUrl = makeUrl("/view/"+device);
			
			htmlBuilder.append("<tr>");
			htmlBuilder.append("<td style=\"width:50px;\">").append(deviceIndex+1).append("</td>");
			htmlBuilder.append("<td style=\"width:*;\"><a href=\"").append(viewDeviceUrl).append("\">").append(device).append("</a></td>");
			htmlBuilder.append("<td style=\"width:200px;\" id=\"").append(delBtnName).append("\"></td>");
			htmlBuilder.append("</tr>");
			
			widgetMap.put(delBtnName, delBtn);
		}
		htmlBuilder.append("</table>");
		
		HTMLPanel panel = new HTMLPanel(htmlBuilder.toString());
		
		set(DEVICES_HEADER_CONTAINER, new HTML("<b>" + devices.size() + " Devices Available:</b>"));
		set(DEVICE_NAMES_CONTAINER, panel);
		
		for (Map.Entry<String, Widget> entry:widgetMap.entrySet()) {
			panel.add(entry.getValue(), entry.getKey());
		}
		
	}

	private void set(String container, Widget widget) {
		RootPanel rootPanel = RootPanel.get(container);
		if (rootPanel != null) {
			rootPanel.clear();
			rootPanel.add(widget);
		}
	}
	
//	private void clear(String container) {
//		RootPanel rootPanel = RootPanel.get(container);
//		if (rootPanel != null) {
//			rootPanel.clear();
//		}
//	}

	private void onError(String msg, Throwable caught) {
		GWT.log(msg, caught);
		set("errorLabelContainer", new HTML(caught.getMessage()));
	}
	
	private String makeUrl(String path) {
		UrlBuilder urlBuilder = new UrlBuilder();
		
		urlBuilder.setProtocol(Window.Location.getProtocol());
		urlBuilder.setHost(Window.Location.getHost());
		
		String port = Window.Location.getPort();
		if (port!=null && !port.isEmpty())
			urlBuilder.setPort(Integer.parseInt(port));
		
		urlBuilder.setPath(path);
		
		String hash = Window.Location.getHash();
		if (hash!=null && !hash.isEmpty())
			urlBuilder.setHash(hash);
		
		Map<String,List<String>> paramMap = Window.Location.getParameterMap();
		for (Map.Entry<String, List<String>> paramEntry:paramMap.entrySet()) {
			List<String> paramsList = paramEntry.getValue();
			String[] paramsArray = new String[paramsList.size()];
			paramsArray = paramsList.toArray(paramsArray);
			urlBuilder.setParameter(paramEntry.getKey(), paramsArray);
		}
		
		return urlBuilder.buildString();
	}

}
