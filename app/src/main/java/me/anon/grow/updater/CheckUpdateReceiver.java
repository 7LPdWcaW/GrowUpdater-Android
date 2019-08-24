package me.anon.grow.updater;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.core.app.NotificationCompat;
import cz.msebera.android.httpclient.Header;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Receiver called when Grow Tracker is opened. Used for checking for updates
 */
public class CheckUpdateReceiver extends BroadcastReceiver
{
	public static CheckCallbackListener checkCallback;

	public static interface CheckCallbackListener
	{
		public void onUpdateChecked();
	}

	/**
	 * Static struct for versions and comparisons
	 */
	public static class Version implements Serializable
	{
		public int major;
		public int minor;
		public int hot;
		public String type = "";
		public int iteration = 0;
		public String downloadUrl = "";
		public String releaseNotes = "";
		public long releaseDate = 0;

		public String appType = "original";

		public static Version parse(String version)
		{
			if (version.equalsIgnoreCase("alpha"))
			{
				Version v = new Version();
				v.major = -1;
				v.minor = -1;
				v.hot = -1;

				v.type = "alpha";
				v.iteration = 1;
				return v;
			}
			else if (version.equalsIgnoreCase("beta"))
			{
				Version v = new Version();
				v.major = -1;
				v.minor = -1;
				v.hot = -1;

				v.type = "beta";
				v.iteration = 1;
				return v;
			}

			Pattern regex = Pattern.compile("v?([0-9]{1,2})(.?)([0-9]{1,2})((.?)([0-9]{1,2}))?(-([a-zA-Z]+)([0-9]+))?");
			Matcher matcher = regex.matcher(version);

			while (matcher.find())
			{
				String major = matcher.group(1);
				String minor = matcher.group(3);
				String hot = matcher.group(6);
				String type = matcher.group(8);
				String iteration = matcher.group(9);

				try
				{
					Version v = new Version();
					v.major = major == null ? 0 : Integer.parseInt(major);
					v.minor = minor == null ? 0 : Integer.parseInt(minor);
					v.hot = hot == null ? 0 : Integer.parseInt(hot);

					v.type = type == null ? "" : type;
					v.iteration = iteration == null ? 0 : Integer.parseInt(iteration);

					return v;
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}

			return null;
		}

		public boolean log(String log)
		{
			System.out.println(log);
			return false;
		}

		public boolean newerThan(Version otherVersion)
		{
			if (toString().equalsIgnoreCase(otherVersion.toString()))
			{
				return false;
			}

			if (this.major == -1 && this.minor == -1 && this.hot == -1) return this.releaseDate > otherVersion.releaseDate;

			if (this.major > otherVersion.major)
			{
				return true;
			}
			else if (this.major == otherVersion.major)
			{
				if (this.minor > otherVersion.minor)
				{
					return !checkPreReleaseNewer(otherVersion);
				}
				else if (this.minor == otherVersion.minor)
				{
					if (this.hot > otherVersion.hot)
					{
						return !checkPreReleaseNewer(otherVersion);
					}
				}
				else
				{
					return false;
				}
			}

			if (this.type.equals("") && !otherVersion.type.equals(""))
			{
				return true;
			}
			else
			{
				return checkPreReleaseNewer(otherVersion);
			}
		}

		private boolean checkPreReleaseNewer(Version otherVersion)
		{
			if (this.type.equals(otherVersion.type) && this.iteration > otherVersion.iteration)
			{
				return true;
			}
			else
			{
				if (this.type.equals("beta") && otherVersion.type.equals("alpha"))
				{
					return true;
				}
			}

			return false;
		}

		@Override public String toString()
		{
			if (major == -1 && minor == -1 && hot == -1)
			{
				return type + " Nightly build";
			}

			String additional = "";

			if (hot > 0)
			{
				additional += "." + hot;
			}

			if (!type.equals(""))
			{
				additional += "-" + type;

				if (iteration > 0)
				{
					additional += iteration;
				}
			}

			return major + "." + minor + additional;
		}

		@Override public boolean equals(Object o)
		{
			return toString().equals(o.toString());
		}

		@Override public int hashCode()
		{
			return toString().hashCode();
		}
	}

	@Override public void onReceive(final Context context, Intent intent)
	{
		long lastChecked = getDefaultSharedPreferences(context).getLong("last_checked", 0);
		String frequency = getDefaultSharedPreferences(context).getString("check_frequency", "3600000");
		long freqInt = 0;
		try
		{
			freqInt = Long.parseLong(frequency);
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
		}

		final boolean force = intent.getExtras() != null && intent.getExtras().containsKey("force");
		if (System.currentTimeMillis() - lastChecked > freqInt || force)
		{
			getDefaultSharedPreferences(context).edit()
				.putLong("last_checked", System.currentTimeMillis())
				.apply();

			if (checkCallback != null)
			{
				checkCallback.onUpdateChecked();
			}

			PackageManager packageManager = context.getPackageManager();
			PackageInfo packageInfo = null;

			try
			{
				packageInfo = packageManager.getPackageInfo("me.anon.grow", 0);
			}
			catch (Exception e){}
			final int versionCode = packageInfo == null ? 0 : packageInfo.versionCode;
			final String versionName = packageInfo == null ? "0.0" : packageInfo.versionName;
			final Version currentVersion = Version.parse(versionName);

			if (currentVersion == null)
			{
				return;
			}

			try
			{
				ApplicationInfo ai = packageManager.getApplicationInfo("me.anon.grow", PackageManager.GET_META_DATA);

				if (ai.metaData != null)
				{
					currentVersion.appType = ai.metaData.getString("me.anon.grow.APP_TYPE", "original");
					currentVersion.releaseDate = Long.parseLong(ai.metaData.getString("me.anon.grow.VERSION_DATE", "" + System.currentTimeMillis()));
				}
			}
			catch (PackageManager.NameNotFoundException e)
			{
				e.printStackTrace();
			}

			AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
			asyncHttpClient.setUserAgent("Android/7LPdWcaW:GrowUpdater");
			asyncHttpClient.get("https://api.github.com/repos/7LPdWcaW/GrowTracker-Android/releases", new TextHttpResponseHandler()
			{
				@Override public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable)
				{

				}

				@Override public void onSuccess(int statusCode, Header[] headers, String responseString)
				{
					JsonArray jsonArray = (JsonArray)new JsonParser().parse(responseString);

					ArrayList<Version> releases = new ArrayList<Version>();

					for (JsonElement element : jsonArray)
					{
						JsonObject jsonObject = element.getAsJsonObject();
						String name = "";

						if (jsonObject.get("name") != JsonNull.INSTANCE && !TextUtils.isEmpty(jsonObject.get("name").getAsString()))
						{
							name = jsonObject.get("name").getAsString();
						}
						else
						{
							name = jsonObject.get("tag_name").getAsString();
						}

						Version releaseVersion = Version.parse(name);

						if (releaseVersion != null)
						{
							try
							{
								Date date = new SimpleDateFormat("yyyy-MM-dd").parse(jsonObject.get("published_at").getAsString());
								releaseVersion.releaseDate = date.getTime();
							}
							catch (ParseException e)
							{
								e.printStackTrace();
							}

							releaseVersion.releaseNotes = "";
							if (jsonObject.get("body") != JsonNull.INSTANCE)
							{
								releaseVersion.releaseNotes = jsonObject.get("body").getAsString();
							}

							for (JsonElement assets : jsonObject.get("assets").getAsJsonArray())
							{
								JsonObject assetObject = assets.getAsJsonObject();

								if (assetObject.get("content_type").getAsString().equals("application/vnd.android.package-archive"))
								{
									releaseVersion.downloadUrl = assetObject.get("browser_download_url").getAsString();
									releaseVersion.appType = assetObject.get("name").getAsString().contains("discrete") ? "discrete" : "original";
								}
							}

							releases.add(releaseVersion);
						}
					}

					Collections.sort(releases, new Comparator<Version>()
					{
						@Override public int compare(Version o1, Version o2)
						{
							if (o2.newerThan(o1)) return 1;
							if (o1.newerThan(o2)) return -1;

							return 0;
						}
					});

					Version latest = null;
					Version latestBeta = null;
					Version latestStable = null;

					for (Version release : releases)
					{
						if (release.type.equals("") && latestStable == null)
						{
							latestStable = release;
						}
						else if (release.type.equals("alpha") && latest == null)
						{
							latest = release;
						}
						else if (release.type.equals("beta") && latestBeta == null)
						{
							latestBeta = release;
						}
					}

					boolean experimental = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("experimental", false);
					boolean beta = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("beta", false);

					if (context == null) return;

					if (experimental && latest.newerThan(currentVersion))
					{
						sendUpdateNotification(context, latest);
					}
					else if (beta && latest.newerThan(currentVersion))
					{
						sendUpdateNotification(context, latestBeta);
					}
					else if (latest != null && latest.newerThan(currentVersion))
					{
						sendUpdateNotification(context, latest);
					}
					else
					{
						if (force)
						{
							Toast.makeText(context, "Already up-to-date", Toast.LENGTH_SHORT).show();
						}
					}
				}
			});
		}
	}

	private void sendUpdateNotification(Context context, Version release)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			int importance = NotificationManager.IMPORTANCE_DEFAULT;
			NotificationChannel channel = new NotificationChannel("updater", "Updater", importance);
			channel.setDescription("Displays notification when there is an update available");
			NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);
		}

		Intent downloadActivity = new Intent(context, DownloadActivity.class);
		downloadActivity.putExtra("version", release);

		PendingIntent downloadIntent = PendingIntent.getActivity(context, 0, downloadActivity, PendingIntent.FLAG_CANCEL_CURRENT);

		Notification notification = new NotificationCompat.Builder(context, "updater")
			.setContentTitle("There is an update available")
			.setContentText("A newer version of GrowTracker is available to download")
			.setStyle(new NotificationCompat.BigTextStyle()
				.bigText("A newer version of GrowTracker is available to download"))
			.setSmallIcon(R.mipmap.ic_notification)
			.addAction(0, "Download & update", downloadIntent)
			.setContentIntent(downloadIntent)
			.setAutoCancel(true)
		.build();

		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(1, notification);
	}
}
