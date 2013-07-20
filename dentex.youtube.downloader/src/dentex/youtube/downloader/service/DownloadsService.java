/***
 	Copyright (c) 2012-2013 Samuele Rini
 	
	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with this program. If not, see http://www.gnu.org/licenses
	
	***
	
	https://github.com/dentex/ytdownloader/
    https://sourceforge.net/projects/ytdownloader/
	
	***
	
	Different Licenses and Credits where noted in code comments.
*/


package dentex.youtube.downloader.service;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import dentex.youtube.downloader.DashboardActivity;
import dentex.youtube.downloader.R;
import dentex.youtube.downloader.ShareActivity;
import dentex.youtube.downloader.YTD;
import dentex.youtube.downloader.utils.Utils;

public class DownloadsService extends Service {
	
	private final static String DEBUG_TAG = "DownloadsService";
	public static boolean copyEnabled;
	public static Context nContext;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		Utils.logger("d", "service created", DEBUG_TAG);
		
		//BugSenseHandler.initAndStartSession(this, YTD.BugsenseApiKey);
		
		nContext = getBaseContext();
		registerReceiver(downloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}
	
	public static Context getContext() {
        return nContext;
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		copyEnabled = intent.getBooleanExtra("COPY", false);
		Utils.logger("d", "Copy to extSdcard: " + copyEnabled, DEBUG_TAG);
		
		/*audio = intent.getStringExtra("AUDIO");
		Utils.logger("d", "Audio extraction: " + audio, DEBUG_TAG);*/
		
		super.onStartCommand(intent, flags, startId);
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Utils.logger("d", "service destroyed", DEBUG_TAG);
	    unregisterReceiver(downloadComplete);
	}

	BroadcastReceiver downloadComplete = new BroadcastReceiver() {

		String vFilename;
		String absolutePath;
		String aExt;
		String basename;

		@Override
    	public void onReceive(final Context context, Intent intent) {
    		Utils.logger("d", "downloadComplete: onReceive CALLED", DEBUG_TAG);
    		long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
    		int ID = (int) id;
    		
    		String previousJson = Utils.parseJsonDashboardFile(context);
    		JSONObject mO = null;
    		try {
    			mO = new JSONObject(previousJson);
    			JSONObject obj = mO.optJSONObject(String.valueOf(id));
    			if (obj != null) {
    				absolutePath = obj.getString(YTD.JSON_DATA_PATH);
    				vFilename = obj.getString(YTD.JSON_DATA_FILENAME);
    				basename = obj.getString(YTD.JSON_DATA_BASENAME);
    				aExt = obj.getString(YTD.JSON_DATA_AUDIO_EXT);
    			}
    		} catch (JSONException e1) {
    			Log.e(DEBUG_TAG, e1.getMessage());
    		}
    		
			Query query = new Query();
			query.setFilterById(id);
			Cursor c = ShareActivity.dm.query(query);
			if (c.moveToFirst()) {
				
				int statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
				int reasonIndex = c.getColumnIndex(DownloadManager.COLUMN_REASON);
				int status = c.getInt(statusIndex);
				int reason = c.getInt(reasonIndex);
				
				int sizeIndex = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
				int sizeRaw = c.getInt(sizeIndex);
				String size = Utils.MakeSizeHumanReadable(sizeRaw, true);

				switch (status) {
				
				case DownloadManager.STATUS_SUCCESSFUL:
					Utils.logger("d", "_ID " + id + " SUCCESSFUL (status " + status + ")", DEBUG_TAG);
					
					// copy job notification init
					NotificationCompat.Builder cBuilder =  new NotificationCompat.Builder(context);
					NotificationManager cNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			    	cBuilder.setSmallIcon(R.drawable.ic_stat_ytd);
					cBuilder.setContentTitle(vFilename);

					/*
					 *  Copy to extSdCard
					 */
					if (copyEnabled) {
						File in = new File(ShareActivity.dir_Downloads, vFilename);
						File dst = new File(absolutePath, vFilename);
						
						try {
							removeIdUpdateNotification(id);
						} catch (NullPointerException e) {
							Log.e(DEBUG_TAG, "NullPointerException on removeIdUpdateNotification(id)");
						}
							
						Intent intent2 = new Intent(Intent.ACTION_VIEW);

						try {
							// Toast + Notification + Log ::: Copy in progress...
							Toast.makeText(context,"YTD: " + context.getString(R.string.copy_progress), Toast.LENGTH_LONG).show();
					        cBuilder.setContentText(context.getString(R.string.copy_progress));
							cNotificationManager.notify(ID, cBuilder.build());
							Utils.logger("i", "_ID " + ID + " Copy in progress...", DEBUG_TAG);
							
							Utils.copyFile(in, dst);
							
							// Toast + Notification + Log ::: Copy OK
							Toast.makeText(context,  vFilename + ": " + context.getString(R.string.copy_ok), Toast.LENGTH_LONG).show();
					        cBuilder.setContentText(context.getString(R.string.copy_ok));
					        intent2.setDataAndType(Uri.fromFile(dst), "video/*");
							Utils.logger("i", "_ID " + ID + " Copy OK", DEBUG_TAG);
							
							Utils.setNotificationDefaults(cBuilder);
							Utils.scanMedia(getApplicationContext(), 
										new String[] {dst.getAbsolutePath()}, 
										new String[] {"video/*"});
						                  
							if (ShareActivity.dm.remove(id) == 0) {
								Toast.makeText(context, "YTD: " + getString(R.string.download_remove_failed), Toast.LENGTH_LONG).show();
								Log.e(DEBUG_TAG, "temp download file NOT removed");
								
				        	} else { 
				        		Utils.logger("v", "temp download file removed", DEBUG_TAG);
				        		
				        		// to add the completed file on extSdCard into the dm list;
				        		//Uri dstUri = Uri.fromFile(dst);
				        		
				        		/*Utils.logger("i", "dst: " + dst.getAbsolutePath(), DEBUG_TAG);
				        		ShareActivity.dm.addCompletedDownload(vfilename, 
				        				getString(R.string.ytd_video), 
				        				true, 
				        				"video/mp4", 
				        				dst.getAbsolutePath(),
				        				sizeRaw,
				        				false);*/
				        	}
						} catch (IOException e) {
							// Toast + Notification + Log ::: Copy FAILED
							Toast.makeText(context, vFilename + ": " + getString(R.string.copy_error), Toast.LENGTH_LONG).show();
							cBuilder.setContentText(getString(R.string.copy_error));
							intent2.setDataAndType(Uri.fromFile(in), "video/*");
							Log.e(DEBUG_TAG, "_ID " + ID + "Copy to extSdCard FAILED");
						} finally {
							PendingIntent contentIntent = PendingIntent.getActivity(nContext, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
			        		cBuilder.setContentIntent(contentIntent);
							cNotificationManager.notify(ID, cBuilder.build());
						}
					}
					
					Utils.addEntryToJsonFile(
							nContext, 
							String.valueOf(id), 
							YTD.JSON_DATA_TYPE_V, 
							YTD.JSON_DATA_STATUS_C, 
							absolutePath, 
							vFilename, 
							basename, 
							aExt, 
							size, 
							false);
					
					if (DashboardActivity.isDashboardRunning)
						DashboardActivity.refreshlist(DashboardActivity.sDashboard);
					
					break;
					
				case DownloadManager.STATUS_FAILED:
					Log.e(DEBUG_TAG, "_ID " + id + " FAILED (status " + status + ")");
					Log.e(DEBUG_TAG, " Reason: " + reason);
					Toast.makeText(context,  vFilename + ": " + getString(R.string.download_failed), Toast.LENGTH_LONG).show();
					
					Utils.addEntryToJsonFile(
							nContext, 
							String.valueOf(id), 
							YTD.JSON_DATA_TYPE_V, 
							YTD.JSON_DATA_STATUS_F, 
							absolutePath, 
							vFilename, 
							basename, 
							aExt, 
							size, 
							false);
					
					if (DashboardActivity.isDashboardRunning)
						DashboardActivity.refreshlist(DashboardActivity.sDashboard);
					
					break;
					
				default:
					Utils.logger("w", "_ID " + id + " completed with status " + status, DEBUG_TAG);
				}
				
				try {
					removeIdUpdateNotification(id);
				} catch (NullPointerException e) {
					Log.e(DEBUG_TAG, "NullPointerException on removeIdUpdateNotification(id)");
				}
	        }
    	}
    };

    public static void removeIdUpdateNotification(long id) {
		if (id != 0) {
			if (ShareActivity.sequence.remove(id)) {
				Utils.logger("d", "_ID " + id + " REMOVED from Notification", DEBUG_TAG);
			} else {
				Utils.logger("d", "_ID " + id + " Already REMOVED from Notification", DEBUG_TAG);
			}
		} else {
			Utils.logger("w", "_ID  not found!", DEBUG_TAG);
		}
		
		if (!copyEnabled) Utils.setNotificationDefaults(ShareActivity.mBuilder);
		
		if (ShareActivity.sequence.size() > 0) {
			ShareActivity.mBuilder.setContentText(ShareActivity.pt1 + " " + ShareActivity.sequence.size() + " " + ShareActivity.pt2);
			ShareActivity.mNotificationManager.notify(1, ShareActivity.mBuilder.build());
		} else {
			ShareActivity.mBuilder.setContentText(ShareActivity.noDownloads);
	        ShareActivity.mNotificationManager.notify(1, ShareActivity.mBuilder.build());
			Utils.logger("d", "No downloads in progress; stopping FileObserver and DownloadsService", DEBUG_TAG);
			ShareActivity.videoFileObserver.stopWatching();
			nContext.stopService(new Intent(DownloadsService.getContext(), DownloadsService.class));
		}
	}
}
