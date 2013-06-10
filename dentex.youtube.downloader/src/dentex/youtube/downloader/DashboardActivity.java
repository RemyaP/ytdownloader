package dentex.youtube.downloader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cmc.music.common.ID3WriteException;
import org.cmc.music.metadata.MusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import dentex.youtube.downloader.ffmpeg.FfmpegController;
import dentex.youtube.downloader.ffmpeg.ShellUtils.ShellCallback;
import dentex.youtube.downloader.utils.Utils;

public class DashboardActivity extends ListActivity{
	
	private final static String DEBUG_TAG = "DashboardActivity";
	private NotificationCompat.Builder aBuilder;
	public NotificationManager aNotificationManager;
	private int totSeconds;
	private int currentTime;
	protected File in;
	protected File out;
	protected String aBaseName;
	public String aSuffix = ".audio";
	public String vfilename;
	private String acodec;
	private boolean removeVideo;
	private boolean copyEnabled;
	private String aFileName;
	private String audio;
	private int ID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//BugSenseHandler.initAndStartSession(this, YTD.BugsenseApiKey);
		
		// Theme init
    	Utils.themeInit(this);
    	
		setContentView(R.layout.activity_dashboard);
		
		// Language init
    	Utils.langInit(this);
    	
    	String[] list = readJson();
    	setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list));
	}

	private String[] readJson() {
		// parse existing/init new JSON 
		String previousJson = parseJsonDashboardFile(this);
				
		JSONArray jArray = null;
		List<String> filenames = new ArrayList<String>();
		try {
			jArray = new JSONArray(previousJson);
			for (int i = 0; i < jArray.length(); i++) {
	        	JSONObject jo = jArray.getJSONObject(i);
				String username = jo.getString("filename");
				filenames.add(username);
	        }
		} catch (JSONException e) {
			Log.e(DEBUG_TAG, e.getMessage());
		}
		
		Iterator<String> filenamesIter = filenames.iterator();
		
		List<String> listEntries = new ArrayList<String>();
		while (filenamesIter.hasNext()) {
			try {
            	listEntries.add(filenamesIter.next());
        	} catch (NoSuchElementException e) {
        		listEntries.add("//");
        	}
        }
		return listEntries.toArray(new String[0]);
	}

	public String parseJsonDashboardFile(Context context) {
		File jsonFile = new File(context.getDir(YTD.JSON_FOLDER, 0), YTD.JSON_FILENAME);
		String jsonString = null;
		if (jsonFile.exists()) {
			try {
				jsonString = Utils.readFromFile(jsonFile);
			} catch (IOException e1) {
				// TODO
				e1.printStackTrace();
			}
		} else {
			jsonString = "[]";
		}
		return jsonString;
	}
	
	// #####################################################################
	
	public void ffmpegJob() {
		// audio jobs notification init
		aBuilder =  new NotificationCompat.Builder(this);
		aNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		aBuilder.setSmallIcon(R.drawable.icon_nb);
		aBuilder.setContentTitle(vfilename);
		
		/*
		 *  Audio extraction/conversion
		 */
			
		if (removeVideo && copyEnabled) {
			in = new File(ShareActivity.dir_Downloads, vfilename);
		} else {
			in = new File(ShareActivity.path, vfilename);
		}
		
		acodec = YTD.settings.getString(vfilename + "FFext", ".audio");
		aBaseName = YTD.settings.getString(vfilename + "FFbase", ".audio");
		aFileName = aBaseName + acodec;
		out = new File(ShareActivity.path, aFileName);
	    
		new Thread(new Runnable() {

			@Override
			public void run() {
				
				Looper.prepare();
				
				FfmpegController ffmpeg = null;

			    try {
			    	ffmpeg = new FfmpegController(DashboardActivity.this);
			    	
			    	// Toast + Notification + Log ::: Audio job in progress...
			    	String text = null;
			    	if (audio.equals("extr")) {
						text = getString(R.string.audio_extr_progress);
					} else {
						text = getString(R.string.audio_conv_progress);
					}
			    	Toast.makeText(DashboardActivity.this,"YTD: " + text, Toast.LENGTH_LONG).show();
			    	aBuilder.setContentTitle(aFileName);
			        aBuilder.setContentText(text);
					aNotificationManager.notify(ID*ID, aBuilder.build());
					Utils.logger("i", "_ID " + ID + " " + text, DEBUG_TAG);
			    } catch (IOException ioe) {
			    	Log.e(DEBUG_TAG, "Error loading ffmpeg. " + ioe.getMessage());
			    }
			    
			    ShellDummy shell = new ShellDummy();
			    String mp3BitRate = YTD.settings.getString("mp3_bitrate", getString(R.string.mp3_bitrate_default));
			    
			    try {
					ffmpeg.extractAudio(in, out, audio, mp3BitRate, shell);
			    } catch (IOException e) {
					Log.e(DEBUG_TAG, "IOException running ffmpeg" + e.getMessage());
				} catch (InterruptedException e) {
					Log.e(DEBUG_TAG, "InterruptedException running ffmpeg" + e.getMessage());
				}
			    
	            Looper.loop();
			}
    	}).start();
	}
	
	private class ShellDummy implements ShellCallback {

		@Override
		public void shellOut(String shellLine) {
			findAudioSuffix(shellLine);
			if (audio.equals("conv")) {
				getAudioJobProgress(shellLine);
			}
			Utils.logger("d", shellLine, DEBUG_TAG);
		}

		@Override
		public void processComplete(int exitValue) {
			Utils.logger("i", "FFmpeg process exit value: " + exitValue, DEBUG_TAG);
			String text = null;
			Intent audioIntent =  new Intent(Intent.ACTION_VIEW);
			if (exitValue == 0) {

				// Toast + Notification + Log ::: Audio job OK
				if (audio.equals("extr")) {
					text = getString(R.string.audio_extr_completed);
				} else {
					text = getString(R.string.audio_conv_completed);
				}
				Utils.logger("d", "_ID " + ID + " " + text, DEBUG_TAG);
				
				final File renamedAudioFilePath = renameAudioFile(aBaseName, out);
				Toast.makeText(DashboardActivity.this,  renamedAudioFilePath.getName() + ": " + text, Toast.LENGTH_LONG).show();
				aBuilder.setContentTitle(renamedAudioFilePath.getName());
				aBuilder.setContentText(text);			
				audioIntent.setDataAndType(Uri.fromFile(renamedAudioFilePath), "audio/*");
				PendingIntent contentIntent = PendingIntent.getActivity(DashboardActivity.this, 0, audioIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        		aBuilder.setContentIntent(contentIntent);
        		
        		// write id3 tags
				if (audio.equals("conv")) {
					try {
						Utils.logger("d", "writing ID3 tags...", DEBUG_TAG);
						addId3Tags(renamedAudioFilePath);
					} catch (ID3WriteException e) {
						Log.e(DEBUG_TAG, "Unable to write id3 tags", e);
					} catch (IOException e) {
						Log.e(DEBUG_TAG, "Unable to write id3 tags", e);
					}
				}
				
				// calls to media scanner
				if (copyEnabled) {
					if (!removeVideo) {
						Utils.scanMedia(getApplicationContext(), 
								new String[] {in.getAbsolutePath(), renamedAudioFilePath.getAbsolutePath()}, 
								new String[] {"video/*", "audio/*"});
					} else {
						Utils.scanMedia(getApplicationContext(), 
								new String[] {renamedAudioFilePath.getAbsolutePath()}, 
								new String[] {"audio/*"});
					}
				} else {
					Utils.scanMedia(getApplicationContext(), 
							new String[] {renamedAudioFilePath.getAbsolutePath()}, 
							new String[] {"audio/*"});
				}
				
				Utils.setNotificationDefaults(aBuilder);
			} else {
				setNotificationForAudioJobError();
			}
			
			aBuilder.setProgress(0, 0, false);
			aNotificationManager.cancel(ID*ID);
			aNotificationManager.notify(ID*ID, aBuilder.build());
			
			deleteVideo();
		}
		
		@Override
		public void processNotStartedCheck(boolean started) {
			if (!started) {
				Utils.logger("w", "FFmpeg process not started or not completed", DEBUG_TAG);

				// Toast + Notification + Log ::: Audio job error
				setNotificationForAudioJobError();
			}
			aNotificationManager.notify(ID*ID, aBuilder.build());
		}
    }
    
	public File renameAudioFile(String aBaseName, File extractedAudioFile) {
		// Rename audio file to add a more detailed suffix, 
		// but only if it has been matched from the ffmpeg console output
		if (audio.equals("extr") &&
				extractedAudioFile.exists() && 
				!aSuffix.equals(".audio")) {
			String newFileName = aBaseName + aSuffix;
			File newFileNamePath = new File(ShareActivity.path, newFileName);
			if (extractedAudioFile.renameTo(newFileNamePath)) {
				Utils.logger("i", extractedAudioFile.getName() + " renamed to: " + newFileName, DEBUG_TAG);
				return newFileNamePath;
			} else {
				Log.e(DEBUG_TAG, "Unable to rename " + extractedAudioFile.getName() + " to: " + aSuffix);
			}
		}
		return extractedAudioFile;
	}

	/* method addId3Tags adapted from Stack Overflow:
	 * 
	 * http://stackoverflow.com/questions/9707572/android-how-to-get-and-setchange-id3-tagmetadata-of-audio-files/9770646#9770646
	 * 
	 * Q: http://stackoverflow.com/users/849664/chirag-shah
	 * A: http://stackoverflow.com/users/903469/mkjparekh
	 */

	public void addId3Tags(File src) throws IOException, ID3WriteException {
        MusicMetadataSet src_set = new MyID3().read(src);
        if (src_set == null) {
            Log.w(DEBUG_TAG, "no metadata");
        } else {
	        MusicMetadata meta = new MusicMetadata("ytd");
	        meta.setAlbum("YTD Extracted Audio");
	        meta.setArtist("YTD");
	        meta.setSongTitle(aBaseName);
	        Calendar cal = Calendar.getInstance();
	        int year = cal.get(Calendar.YEAR);
	        meta.setYear(String.valueOf(year));
	        new MyID3().update(src, src_set, meta);
        }
	}

	private void findAudioSuffix(String shellLine) {
		Pattern audioPattern = Pattern.compile("#0:0.*: Audio: (.+), .+?(mono|stereo .default.|stereo)(, .+ kb|)"); 
		Matcher audioMatcher = audioPattern.matcher(shellLine);
		if (audioMatcher.find() && audio.equals("extr")) {
			String oggBr = "a";
			String groupTwo = "n";
			if (audioMatcher.group(2).equals("stereo (default)")) {
				if (vfilename.contains("hd")) {
					oggBr = "192k";
				} else {
					oggBr = "128k";
				}
				groupTwo = "stereo";
			} else {
				oggBr = "";
				groupTwo = audioMatcher.group(2);
			}
			
			aSuffix = "_" +
					groupTwo + 
					"_" + 
					audioMatcher.group(3).replace(", ", "").replace(" kb", "k") + 
					oggBr + 
					"." +
					audioMatcher.group(1).replaceFirst(" (.*/.*)", "").replace("vorbis", "ogg");
			
			Utils.logger("i", "AudioSuffix: " + aSuffix, DEBUG_TAG);
		}
	}

	public void setNotificationForAudioJobError() {
		String text;
		if (audio.equals("extr")) {
			text = getString(R.string.audio_extr_error);
		} else {
			text = getString(R.string.audio_conv_error);
		}
		Log.e(DEBUG_TAG, "_ID " + ID + " " + text);
		Toast.makeText(DashboardActivity.this,  "YTD: " + text, Toast.LENGTH_LONG).show();
		aBuilder.setContentText(text);
	}
	
	private void getAudioJobProgress(String shellLine) {
		Pattern totalTimePattern = Pattern.compile("Duration: (..):(..):(..)\\.(..)");
		Matcher totalTimeMatcher = totalTimePattern.matcher(shellLine);
		if (totalTimeMatcher.find()) {
			totSeconds = getTotSeconds(totalTimeMatcher);
		}
		
		Pattern currentTimePattern = Pattern.compile("time=(..):(..):(..)\\.(..)");
		Matcher currentTimeMatcher = currentTimePattern.matcher(shellLine);
		if (currentTimeMatcher.find()) {
			currentTime = getTotSeconds(currentTimeMatcher);
		}
		
		if (totSeconds != 0) {
			aBuilder.setProgress(totSeconds, currentTime, false);
			aNotificationManager.notify(ID*ID, aBuilder.build());
		}
	}

	private int getTotSeconds(Matcher timeMatcher) {
		int h = Integer.parseInt(timeMatcher.group(1));
		int m = Integer.parseInt(timeMatcher.group(2));
		int s = Integer.parseInt(timeMatcher.group(3));
		int f = Integer.parseInt(timeMatcher.group(4));
		
		long hToSec = TimeUnit.HOURS.toSeconds(h);
		long mToSec = TimeUnit.MINUTES.toSeconds(m);
		
		int tot = (int) (hToSec + mToSec + s);
		if (f > 50) tot = tot + 1;
		
		Utils.logger("i", "h=" + h + " m=" + m + " s=" + s + "." + f + " -> tot=" + tot,	DEBUG_TAG);
		return tot;
	}

	public void deleteVideo() {
		// remove downloaded video upon successful audio extraction
		if (removeVideo) {
			if (ShareActivity.dm.remove(ID) > 0 ){
				Utils.logger("d", "deleteVideo: _ID " + ID + " successfully removed", DEBUG_TAG);
			}
		}
	}
}
