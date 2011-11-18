package au.csiro.antplus.scanner;

public class RandomAntChannel extends AntChannel {
	
	public static interface DeviceIdSet {
		public void onDeviceIdSet(RandomAntChannel channel);
		public void onScanTimeOut(RandomAntChannel channel);
	}
	
	private DeviceIdSet onDeviceIdSet;

	public RandomAntChannel(AntChannelManager antChannelManager) {
		super(antChannelManager);
		this.period = 8070;
	}
	
	public void setFrequency(byte frequency) {
		this.frequency = frequency;
	}
	
	public void setNetworkNumber(byte networkNumber) {
		this.networkNumber = networkNumber;
	}
	
	public void setDeviceType(byte deviceType) {
		this.deviceType = deviceType;
	}
	
	public void setPeriod(int period) {
		this.period = period;
	}
	
	public DeviceIdSet getOnDeviceIdSet() {
		return onDeviceIdSet;
	}
	 
	public void setOnDeviceIdSet(DeviceIdSet onDeviceIdSet) {
		this.onDeviceIdSet = onDeviceIdSet;
	}
	
	@Override
	public void setDeviceId(short deviceId) {
		if (this.deviceId!=deviceId) {
			super.setDeviceId(deviceId);
			if (onDeviceIdSet!=null) {
				onDeviceIdSet.onDeviceIdSet(this);
			}
		}
	}
	
	@Override
	public void onSearchTimeOut() {
		super.onSearchTimeOut();
		if (onDeviceIdSet!=null) {
			onDeviceIdSet.onScanTimeOut(this);
		}
	}
	
}
