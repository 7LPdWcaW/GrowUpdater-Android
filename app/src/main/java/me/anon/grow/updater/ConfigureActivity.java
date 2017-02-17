package me.anon.grow.updater;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

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

	public static class ConfigureFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener
	{
		private static final int REQUEST_STORAGE_PERMISSION = 0x1;

		@Override public void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.preferences);

			findPreference("about").setOnPreferenceClickListener(this);
		}

		@Override public boolean onPreferenceClick(Preference preference)
		{
			if (preference.getKey().equals("about"))
			{
				
				return true;
			}

			return false;
		}
	}
}
