package com.urremote.bridge.common;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;

public class PrimaryAccountUtil {
	
	public static String getPrimaryAccount(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		
		Account[] accounts = accountManager.getAccountsByType("com.google");
		if (accounts.length==0) {
			throw new RuntimeException("No google accounts found on the phone. Please setup a google account first.");
		}
		Account mainAccount = accounts[0];
		Log.i(Constants.TAG, "Primary account found: "+mainAccount.name+", type="+mainAccount.type);
		return mainAccount.name;
	}
	

}
