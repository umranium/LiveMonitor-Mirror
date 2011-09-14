package au.csiro.antplus.scanner;

import android.util.Log;

public class SdmChannel extends AntChannel {

	public SdmChannel(AntChannelManager antChannelManager) {
		super(antChannelManager);
		this.deviceType = 0x7C;
		this.period = 8134;
	}
	
	@Override
	public void decodeMsg(byte[] msg) {
		Log.i(Constants.TAG, this.getClass().getSimpleName()+" decoding msg.");
	}

}
