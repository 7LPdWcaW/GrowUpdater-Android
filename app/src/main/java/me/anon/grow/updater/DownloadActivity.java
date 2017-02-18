package me.anon.grow.updater;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import cz.msebera.android.httpclient.Header;
import me.anon.grow.helper.PermissionHelper;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static me.anon.grow.updater.R.id.download;

/**
 * Displays change log and handles downloading/installing
 */
public class DownloadActivity extends AppCompatActivity
{
	private static final int REQUEST_STORAGE_PERMISSION = 0x1;
	private CheckUpdateReceiver.Version version;

	@Override protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.download_view);
		getSupportActionBar().hide();

		version = (CheckUpdateReceiver.Version)getIntent().getExtras().getSerializable("version");

		if (version == null)
		{
			finish();
			return;
		}

		DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(this);
		DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(this);

		Date actionDate = new Date(version.releaseDate);
		Calendar actionCalendar = GregorianCalendar.getInstance();
		actionCalendar.setTime(actionDate);
		String releaseDate = dateFormat.format(actionDate) + " " + timeFormat.format(actionDate);

		((TextView)findViewById(R.id.version)).setText(version.toString() + " released " + releaseDate);
		((TextView)findViewById(R.id.changelog)).setText(version.releaseNotes);

		findViewById(R.id.download).setOnClickListener(new View.OnClickListener()
		{
			@Override public void onClick(View v)
			{
				findViewById(R.id.cancel).setVisibility(View.GONE);
				v.setEnabled(false);

				downloadAndInstall();
			}
		});

		findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener()
		{
			@Override public void onClick(View v)
			{
				finish();
			}
		});
	}

	private void downloadAndInstall()
	{
		if (!PermissionHelper.hasPermission(this, WRITE_EXTERNAL_STORAGE))
		{
			PermissionHelper.doPermissionCheck(this, WRITE_EXTERNAL_STORAGE, REQUEST_STORAGE_PERMISSION, "Storage permission is required to write download to disk");
			return;
		}

		final ProgressBar progress = (ProgressBar)findViewById(R.id.progress);
		AsyncHttpClient client = new AsyncHttpClient();
		client.setUserAgent("Android/7LPdWcaW:GrowUpdater");
		client.get(version.downloadUrl, new FileAsyncHttpResponseHandler(new File(getExternalCacheDir(), version.toString() + ".apk"))
		{
			@Override public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file)
			{
				if (progress != null)
				{
					progress.setProgress(0);
					Toast.makeText(progress.getContext(), "There was a problem downloading the update", Toast.LENGTH_SHORT).show();
					findViewById(download).setEnabled(true);
					findViewById(R.id.cancel).setVisibility(View.VISIBLE);
				}
			}

			@Override public void onSuccess(int statusCode, Header[] headers, File file)
			{
				BroadcastReceiver onComplete = new BroadcastReceiver()
				{
					public void onReceive(Context ctxt, Intent intent)
					{
						unregisterReceiver(this);
						finish();
					}
				};

				registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(FileProvider.getUriForFile(DownloadActivity.this, getApplicationContext().getPackageName() + ".provider", getTargetFile()), "application/vnd.android.package-archive");
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}

			@Override public void onProgress(long bytesWritten, long totalSize)
			{
				if (progress != null)
				{
					progress.setProgress((int)bytesWritten);
					progress.setMax((int)totalSize);
				}
			}
		});
	}

	@Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == REQUEST_STORAGE_PERMISSION)
		{
			downloadAndInstall();
		}
	}
}
