package dentex.youtube.downloader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cmc.music.common.ID3WriteException;
import org.cmc.music.metadata.MusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.Toast;
import dentex.youtube.downloader.ffmpeg.FfmpegController;
import dentex.youtube.downloader.ffmpeg.ShellUtils.ShellCallback;
import dentex.youtube.downloader.utils.Utils;

public class DashboardActivity extends Activity{
	
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
	private ListView lv;
	private Editable searchText;
	//private DownloadManager dm;
	
	//private int index;
	
	List<String> idEntries = new ArrayList<String>();
	List<String> statusEntries = new ArrayList<String>();
	List<String> pathEntries = new ArrayList<String>();
	List<String> filenameEntries = new ArrayList<String>();
	List<String> sizeEntries = new ArrayList<String>();
	List<String> mediaIdEntries = new ArrayList<String>();
	
	List<DashboardListItem> itemsList = new ArrayList<DashboardListItem>();
	DashboardAdapter da;
	private boolean isSearchBarVisible;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//BugSenseHandler.initAndStartSession(this, YTD.BugsenseApiKey);
		
		// Theme init
    	Utils.themeInit(this);
    	
		setContentView(R.layout.activity_dashboard);
		
		// Language init
    	Utils.langInit(this);
    	
    	//dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
    	
    	readJson();
    	
    	buildList();
    	
    	lv = (ListView) findViewById(R.id.dashboard_list);
    	
    	da = new DashboardAdapter(itemsList, this);
    	lv.setAdapter(da);
    	
    	lv.setTextFilterEnabled(true);
    	
    	lv.setLongClickable(true);
    	lv.setOnItemLongClickListener(new OnItemLongClickListener() {

        	@Override
        	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        		AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
        		
        		final DashboardListItem currentItem = da.getItem(position); // in order to refer to the filtered item
        		
        		builder.setTitle(currentItem.getFilename()).setItems(R.array.dashboard_long_click_entries, new DialogInterface.OnClickListener() {
			    	public void onClick(DialogInterface dialog, int which) {
			    		/*switch (which) {
			    			case 0:
			    				// 1st item
			    				break;
			    			case 1:
			    				// 2nd item
			    		}*/
			    		// only one item for the moment: delete. TODO move, etc.
			    		AlertDialog.Builder del = new AlertDialog.Builder(DashboardActivity.this);
			    		del.setTitle(getString(R.string.attention));
			    		del.setMessage(getString(R.string.delete_video_confirm));
			    		
			    		del.setPositiveButton("OK", new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int which) {
			    				/*int id = Integer.parseInt(currentItem.getId());
			    				if (dm.remove(id) > 0 ){
			    					Utils.logger("d", "deleteVideo: _ID " + id + " successfully removed", DEBUG_TAG);
			    				} else {
			    					Utils.logger("w", "deleteVideo: _ID " + id + " NOT removed", DEBUG_TAG);
			    				}*/
								File fileToDel = new File(currentItem.getPath(), currentItem.getFilename());
								if (fileToDel.delete()) {
									Utils.logger("d", fileToDel.getName() + " successfully deleted.", DEBUG_TAG);
									Toast.makeText(DashboardActivity.this, 
											getString(R.string.delete_video_ok, currentItem.getFilename()), 
											Toast.LENGTH_LONG).show();
									
									//1
									/*Intent delIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileToDel));
									delIntent.setType("video/*");
									sendBroadcast(delIntent);*/
									
									//2
									String mediaString = YTD.videoinfo.getString(fileToDel.getAbsolutePath(), "non-ext");
									Utils.logger("d", "mediaString: " + mediaString, DEBUG_TAG);
									if (mediaString.equals("non-ext")) {
										DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
										long id = Long.parseLong(currentItem.getId());
										if (dm.remove(id) > 0) {
											Utils.logger("d", id + " (DownloadManager) removed", DEBUG_TAG);
										}
									} else {
										Uri mediaUri = Uri.parse(mediaString);
										long mediaId = ContentUris.parseId(mediaUri);
										Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
										Uri itemUri = ContentUris.withAppendedId(videoUri, mediaId);
										if (getContentResolver().delete(itemUri, null, null) > 0) {
											Utils.logger("d", mediaId + " (ContentResolver) removed", DEBUG_TAG);
										}
									}
									
									Utils.removeEntryFromJsonFile(DashboardActivity.this, currentItem.getId());
									Utils.reload(DashboardActivity.this);
								} else {
									Utils.logger("w", fileToDel.getName() + " NOT deleted.", DEBUG_TAG);
									Toast.makeText(DashboardActivity.this, 
											getString(R.string.delete_video_failed, currentItem.getFilename()), 
											Toast.LENGTH_LONG).show();
								}
			    			}
			    		});
			    		
			    		del.setNegativeButton(R.string.dialogs_negative, new DialogInterface.OnClickListener() {
			    			public void onClick(DialogInterface dialog, int which) {
			    				// cancel
			    			}
			    		});
			    		
			    		AlertDialog delDialog = del.create();
			    		if (! ((Activity) DashboardActivity.this).isFinishing()) {
                        	delDialog.show();
                        }
			    	}
        		});
        		
	        	builder.create();
	    		if (! ((Activity) DashboardActivity.this).isFinishing()) {
	    			builder.show();
	    		}
			    return true;
        	}
    	});
	}

	public void spawnSearchBar() {
		Utils.logger("d", "showing searchbar...", DEBUG_TAG);
		
		EditText inputSearch = new EditText(DashboardActivity.this);
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		inputSearch.setLayoutParams(layoutParams);
		
		if (TextUtils.isEmpty(searchText)) {
			inputSearch.setHint(R.string.menu_search);
		} else {
			inputSearch.setText(searchText);
		}
		
		inputSearch.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
		inputSearch.setSingleLine();
		inputSearch.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		inputSearch.setId(999);
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.dashboard);
		layout.addView(inputSearch, 0);
		isSearchBarVisible = true;
		
    	inputSearch.addTextChangedListener(new TextWatcher() {
        
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				Utils.logger("d", "Text ["+s+"] - Start ["+start+"] - Before ["+before+"] - Count ["+count+"]", DEBUG_TAG);
				
				if (count < before) da.resetData();
				da.getFilter().filter(s.toString());
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
    	});
	}
	
	public void hideSearchBar() {
		Utils.logger("d", "hiding searchbar...", DEBUG_TAG); 
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.dashboard);
		EditText inputSearch = (EditText) findViewById(999);
		
		// hide keyboard
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(inputSearch.getWindowToken(), 0);
		
		// store text and remove EditText
		searchText = inputSearch.getEditableText();
		layout.removeView(inputSearch);
		
		isSearchBarVisible = false;
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_dashboard, menu);
        return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId()){
        	case R.id.menu_search:
    			if (!isSearchBarVisible) {
    				spawnSearchBar();
    			} else {
    				hideSearchBar();
    			}
    			return true;
        	default:
        		return super.onOptionsItemSelected(item);
        }
    }

	private void buildList() {
		Iterator<String> idsIter = idEntries.iterator();
		Iterator<String> statusesIter = statusEntries.iterator();
		Iterator<String> pathsIter = pathEntries.iterator();
		Iterator<String> filenamesIter = filenameEntries.iterator();
		Iterator<String> sizesIter = sizeEntries.iterator();
		
		while (statusesIter.hasNext()) {
			itemsList.add(new DashboardListItem(
					idsIter.next(),
					statusesIter.next(),
					pathsIter.next(), 
					filenamesIter.next(), 
					sizesIter.next()));
		}
	}
	
	private void readJson() {
		// parse existing/init new JSON 
		String previousJson = Utils.parseJsonDashboardFile(this);
				
		JSONObject jV = null;
		try {
			jV = new JSONObject(previousJson);
			//Utils.logger("v", "current json:\n" + previousJson, DEBUG_TAG);
			@SuppressWarnings("unchecked")
			Iterator<Object> ids = jV.keys();
			while (ids.hasNext()) {
				String id = (String) ids.next();
				JSONObject jO = new JSONObject();
				jO = jV.getJSONObject(id);
				idEntries.add(id);
				statusEntries.add(jO.getString(YTD.JSON_DATA_STATUS));
				pathEntries.add(jO.getString(YTD.JSON_DATA_PATH));
				filenameEntries.add(jO.getString(YTD.JSON_DATA_FILENAME));
				sizeEntries.add(jO.getString(YTD.JSON_DATA_SIZE));
			}
		} catch (JSONException e) {
			Log.e(DEBUG_TAG, e.getMessage());
		}
	}

	// #####################################################################
	
	public void ffmpegJob(final int ID) {
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
		
		int ID;

		@Override
		public void shellOut(String shellLine) {
			findAudioSuffix(shellLine);
			if (audio.equals("conv")) {
				getAudioJobProgress(shellLine, ID);
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
				setNotificationForAudioJobError(ID);
			}
			
			aBuilder.setProgress(0, 0, false);
			aNotificationManager.cancel(ID*ID);
			aNotificationManager.notify(ID*ID, aBuilder.build());
			
			deleteVideo(ID);
		}
		
		@Override
		public void processNotStartedCheck(boolean started) {
			if (!started) {
				Utils.logger("w", "FFmpeg process not started or not completed", DEBUG_TAG);

				// Toast + Notification + Log ::: Audio job error
				setNotificationForAudioJobError(ID);
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

	public void setNotificationForAudioJobError(int ID) {
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
	
	private void getAudioJobProgress(String shellLine, int ID) {
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

	public void deleteVideo(int ID) {
		// remove downloaded video upon successful audio extraction
		if (removeVideo) {
			if (ShareActivity.dm.remove(ID) > 0 ){
				Utils.logger("d", "deleteVideo: _ID " + ID + " successfully removed", DEBUG_TAG);
			} else {
				Utils.logger("w", "deleteVideo: _ID " + ID + " NOT removed", DEBUG_TAG);
			}
		}
	}
}
