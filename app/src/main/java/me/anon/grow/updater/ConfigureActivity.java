package me.anon.grow.updater;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * // TODO: Add class description
 */
public class ConfigureActivity extends AppCompatActivity
{
	@Override protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.toolbar_activity);
		setTitle("GrowUpdater configuration");

		if (savedInstanceState == null)
		{
			getFragmentManager().beginTransaction()
				.replace(R.id.fragment_holder, new ConfigureFragment(), "configure")
				.commit();
		}
	}

	public static class ConfigureFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, CheckUpdateReceiver.CheckCallbackListener
	{
		private static final int REQUEST_STORAGE_PERMISSION = 0x1;

		@Override public void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.preferences);

			findPreference("about").setOnPreferenceClickListener(this);
			findPreference("last_checked").setOnPreferenceClickListener(this);

			long lastChecked = getPreferenceManager().getSharedPreferences().getLong("last_checked", 0);
			String lastCheckedDateStr = "never";

			if (lastChecked > 0)
			{
				DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getActivity());
				DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getActivity());

				Date actionDate = new Date(lastChecked);
				Calendar actionCalendar = GregorianCalendar.getInstance();
				actionCalendar.setTime(actionDate);
				lastCheckedDateStr = dateFormat.format(actionDate) + " " + timeFormat.format(actionDate);
			}

			findPreference("last_checked").setTitle("Last checked: " + lastCheckedDateStr);
		}

		@Override public boolean onPreferenceClick(Preference preference)
		{
			if (preference.getKey().equals("about"))
			{
				String readme = "";

				try
				{
					InputStream stream = new BufferedInputStream(getActivity().getAssets().open("readme.html"), 8192);
					int len = 0;
					byte[] buffer = new byte[8192];

					while ((len = stream.read(buffer)) != -1)
					{
						readme += new String(buffer, 0, len);
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

				new AlertDialog.Builder(getActivity())
					.setMessage(Html.fromHtml(readme))
					.setPositiveButton("Close", null)
					.show();

				return true;
			}
			else if (preference.getKey().equals("last_checked"))
			{
				CheckUpdateReceiver.checkCallback = this;
				Intent broadcast = new Intent(getActivity(), CheckUpdateReceiver.class);
				broadcast.putExtra("force", true);
				getActivity().sendBroadcast(broadcast);
			}

			return false;
		}

		@Override public void onDestroy()
		{
			CheckUpdateReceiver.checkCallback = null;
			super.onDestroy();
		}

		@Override public void onUpdateChecked()
		{
			onCreate(null);
		}
	}
}
