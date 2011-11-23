package com.urremote.bridge.common;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

public class PrimaryAccountUtil {
	
	public static String getPrimaryAccount(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account[] accounts = accountManager.getAccounts();
		if (accounts.length==0) {
			throw new RuntimeException("No accounts found on the phone. Please setup an account first.");
		}
		Account mainAccount = accounts[0];
		return mainAccount.name;
	}
	

}
