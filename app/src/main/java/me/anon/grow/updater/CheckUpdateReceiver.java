package me.anon.grow.updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

/**
 * Receiver called when Grow Tracker is opened. Used for checking for updates
 */
public class CheckUpdateReceiver extends BroadcastReceiver
{
	public static CheckCallbackListener checkCallback;

	public static interface CheckCallbackListener
	{
		public void onUpdateChecked(boolean downloaded);
	}

	@Override public void onReceive(Context context, Intent intent)
	{
		PreferenceManager.getDefaultSharedPreferences(context).edit()
			.putLong("last_checked", System.currentTimeMillis())
			.apply();


	}
}
