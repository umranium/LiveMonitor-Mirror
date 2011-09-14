package au.csiro.antplus.scanner;

import android.util.Log;

public class BscChannel extends AntChannel {

	public BscChannel(AntChannelManager antChannelManager) {
		super(antChannelManager);
		this.deviceType = 121;
		this.period = 8118;
	}
	
	@Override
	public void decodeMsg(byte[] msg) {
		Log.i(Constants.TAG, this.getClass().getSimpleName()+" decoding msg.");
	}

}
