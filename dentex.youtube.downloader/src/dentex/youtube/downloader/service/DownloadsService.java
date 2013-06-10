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
	along with this program. If not, see <http>http://www.gnu.org/licenses
	
	***
	
	https://github.com/dentex/ytdownloader/
    https://sourceforge.net/projects/ytdownloader/
	
	***
	
	Different Licenses and Credits where noted in code comments.
*/


package dentex.youtube.downloader.service;

import java.io.File;
import java.io.IOException;

import org.json.JSONArray;
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
import dentex.youtube.downloader.R;
import dentex.youtube.downloader.ShareActivity;
import dentex.youtube.downloader.YTD;
import dentex.youtube.downloader.utils.Utils;

public class DownloadsService extends Service {
	
	private final static String DEBUG_TAG = "DownloadsService";
	//private SharedPreferences settings = YTD.settings;
	//private SharedPreferences videoinfo = YTD.videoinfo;
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
		
		//settings = getSharedPreferences(YTD.PREFS_NAME, 0);
		//videoinfo = getSharedPreferences(YTD.VIDEOINFO_NAME, 0);
		
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

		@Override
    	public void onReceive(final Context context, Intent intent) {
    		Utils.logger("d", "downloadComplete: onReceive CALLED", DEBUG_TAG);
    		long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
    		int ID = (int) id;
    		String vfilename = YTD.videoinfo.getString(String.valueOf(id) + YTD.VIDEOINFO_FILENAME, "video");
    		String path = YTD.videoinfo.getString(String.valueOf(id) + YTD.VIDEOINFO_PATH, ShareActivity.path.getAbsolutePath());
    		
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
			    	cBuilder.setSmallIcon(R.drawable.icon_nb);
					cBuilder.setContentTitle(vfilename);

					/*
					 *  Copy to extSdCard
					 */
					if (copyEnabled) {
						File in = new File(ShareActivity.dir_Downloads, vfilename);
						File dst = new File(path, vfilename);
						
						if (YTD.settings.getBoolean("enable_own_notification", true) == true) {
							try {
								removeIdUpdateNotification(id);
							} catch (NullPointerException e) {
								Log.e(DEBUG_TAG, "NullPointerException on removeIdUpdateNotification(id)");
							}
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
							Toast.makeText(context,  vfilename + ": " + context.getString(R.string.copy_ok), Toast.LENGTH_LONG).show();
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
				        		
				        		// TODO dm.addCompletedDownload to add the completed file on extSdCard into the dm list; NOT working
				        		//Uri dstUri = Uri.fromFile(dst); // <-- tried also this; see (1)

				        		/*Utils.logger("i", "dst: " + dst.getAbsolutePath(), DEBUG_TAG);
				        		ShareActivity.dm.addCompletedDownload(vfilename, 
				        				getString(R.string.ytd_video), 
				        				true, 
				        				"video/*", 
				        				dst.getAbsolutePath(), // <-- dstUri.getEncodedPath(), // (1) 
				        				size,
				        				false);*/
				        	}
						} catch (IOException e) {
							// Toast + Notification + Log ::: Copy FAILED
							Toast.makeText(context, vfilename + ": " + getString(R.string.copy_error), Toast.LENGTH_LONG).show();
							cBuilder.setContentText(getString(R.string.copy_error));
							intent2.setDataAndType(Uri.fromFile(in), "video/*");
							Log.e(DEBUG_TAG, "_ID " + ID + "Copy to extSdCard FAILED");
						} finally {
							PendingIntent contentIntent = PendingIntent.getActivity(nContext, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
			        		cBuilder.setContentIntent(contentIntent);
							cNotificationManager.notify(ID, cBuilder.build());
						}
					}
					
					writeToJsonFile(id, true, path, vfilename, size);
					
					break;
					
				case DownloadManager.STATUS_FAILED:
					Log.e(DEBUG_TAG, "_ID " + id + " FAILED (status " + status + ")");
					Log.e(DEBUG_TAG, " Reason: " + reason);
					Toast.makeText(context,  vfilename + ": " + getString(R.string.download_failed), Toast.LENGTH_LONG).show();
					
					writeToJsonFile(id, false, path, vfilename, size);
					
					break;
					
				default:
					Utils.logger("w", "_ID " + id + " completed with status " + status, DEBUG_TAG);
				}
				
				if (YTD.settings.getBoolean("enable_own_notification", true) == true) {
					try {
						removeIdUpdateNotification(id);
					} catch (NullPointerException e) {
						Log.e(DEBUG_TAG, "NullPointerException on removeIdUpdateNotification(id)");
					}
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
			Log.e(DEBUG_TAG, "_ID  not found!");
		}
		
		if (!copyEnabled) Utils.setNotificationDefaults(ShareActivity.mBuilder);
		
		if (ShareActivity.sequence.size() > 0) {
			ShareActivity.mBuilder.setContentText(ShareActivity.pt1 + " " + ShareActivity.sequence.size() + " " + ShareActivity.pt2);
			ShareActivity.mNotificationManager.notify(ShareActivity.mId, ShareActivity.mBuilder.build());
		} else {
			ShareActivity.mBuilder.setContentText(ShareActivity.noDownloads);
	        ShareActivity.mNotificationManager.notify(ShareActivity.mId, ShareActivity.mBuilder.build());
			Utils.logger("d", "No downloads in progress; stopping FileObserver and DownloadsService", DEBUG_TAG);
			ShareActivity.videoFileObserver.stopWatching();
			nContext.stopService(new Intent(DownloadsService.getContext(), DownloadsService.class));
		}
	}
    
	private void writeToJsonFile(long id, boolean completed, String path, String vfilename, String size) {
		// parse existing/init new JSON 
		File jsonFile = new File(nContext.getDir(YTD.JSON_FOLDER, 0), YTD.JSON_FILENAME);
		String previousJson = null;
		if (jsonFile.exists()) {
			try {
				previousJson = Utils.readFromFile(jsonFile);
			} catch (IOException e1) {
				// TODO
				e1.printStackTrace();
			}
		} else {
			//previousJson = "{}";	//v1
			previousJson = "[]";	//v2
		}
		
		// create new "complex" object
		//JSONObject mO = null;	//v1
		JSONArray jA = null;	//v2
		JSONObject jO = new JSONObject();
		
		try {
			//mO = new JSONObject(previousJson);
			jA = new JSONArray(previousJson);
			jO.put("completed", completed);
			jO.put("path", path);
			jO.put("filename", vfilename);
			jO.put("size", size);
			//mO.put(String.valueOf(id), jO);	//v1
			jO.put("id", String.valueOf(id));	//v2
			jA.put(jO);							//v2
		} catch (JSONException e1) {
			// TODO
			e1.printStackTrace();
		}
		
		// generate string from the object
		String jsonString = null;
		try {
			//jsonString = mO.toString(4);	//v1
			jsonString = jA.toString(4);	//v2
		} catch (JSONException e1) {
			// TODO
			e1.printStackTrace();
		}

		// write back JSON file
		Utils.writeToFile(jsonFile, jsonString);
	}
}
