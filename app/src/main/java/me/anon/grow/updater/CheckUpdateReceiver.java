package me.anon.grow.updater;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
			if (this.major > otherVersion.major)
			{
				return true;
			}
			else if (this.major == otherVersion.major)
			{
				if (this.minor > otherVersion.minor)
				{
					return true;
				}
				else if (this.minor == otherVersion.minor)
				{
					if (this.hot > otherVersion.hot)
					{
						return true;
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
			}

			return false;
		}

		@Override public String toString()
		{
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

		public boolean equals(Version other)
		{
			return toString().equals(other);
		}
	}

	@Override public void onReceive(final Context context, Intent intent)
	{
		long lastChecked = getDefaultSharedPreferences(context).getLong("last_checked", 0);
		long oneDay = TimeUnit.DAYS.toMillis(1);
		final boolean force = true;//intent.getExtras().containsKey("force");
		if (System.currentTimeMillis() - lastChecked > oneDay || force)
		{
			getDefaultSharedPreferences(context).edit()
				.putLong("last_checked", System.currentTimeMillis())
				.apply();

			if (checkCallback != null)
			{
				checkCallback.onUpdateChecked();
			}

			try
			{
				PackageManager packageManager = context.getPackageManager();
				PackageInfo packageInfo = packageManager.getPackageInfo("me.anon.grow", 0);

				final int versionCode = packageInfo.versionCode;
				final String versionName = packageInfo.versionName;
				final Version currentVersion = Version.parse(versionName);

				if (currentVersion == null)
				{
					return;
				}

				ApplicationInfo ai = packageManager.getApplicationInfo("me.anon.grow", PackageManager.GET_META_DATA);

				if (ai.metaData != null)
				{
					currentVersion.appType = ai.metaData.getString("me.anon.grow.APP_TYPE", "original");
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
							Version releaseVersion = Version.parse(jsonObject.get("name").getAsString());

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

								releaseVersion.releaseNotes = jsonObject.get("body").getAsString();

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
								return o1.equals(o2) ? 0 : (o1.newerThan(o2) ? 1 : -1);
							}
						});

						Version latest = releases.get(0);
						Version latestStable = releases.get(0);

						for (Version release : releases)
						{
							if (release.type.equals(""))
							{
								latestStable = release;
								break;
							}
						}

						boolean experimental = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("experimental", false);
						if ((!latest.type.equals("") && experimental && latest.newerThan(currentVersion))
						|| latestStable.newerThan(currentVersion))
						{
							// send notification
							if (context != null)
							{
								sendUpdateNotification(context, experimental ? latest : latestStable);
							}
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
			catch (PackageManager.NameNotFoundException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendUpdateNotification(Context context, Version release)
	{
		Intent downloadActivity = new Intent(context, DownloadActivity.class);
		downloadActivity.putExtra("version", release);

		PendingIntent downloadIntent = PendingIntent.getActivity(context, 0, downloadActivity, PendingIntent.FLAG_CANCEL_CURRENT);

		Notification notification = new Notification.Builder(context)
			.setContentTitle("There is an update available")
			.setContentText("A newer version of GrowTracker is available to download")
			.setStyle(new Notification.BigTextStyle()
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
