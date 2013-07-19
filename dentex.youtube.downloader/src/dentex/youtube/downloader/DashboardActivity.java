package dentex.youtube.downloader;

import group.pals.android.lib.ui.filechooser.FileChooserActivity;
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile;
import group.pals.android.lib.ui.filechooser.services.IFileProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import dentex.youtube.downloader.ffmpeg.FfmpegController;
import dentex.youtube.downloader.ffmpeg.ShellUtils.ShellCallback;
import dentex.youtube.downloader.utils.PopUps;
import dentex.youtube.downloader.utils.Utils;

public class DashboardActivity extends Activity{
	
	private final static String DEBUG_TAG = "DashboardActivity";
	public static boolean isDashboardRunning;
	ContextThemeWrapper boxThemeContextWrapper = new ContextThemeWrapper(this, R.style.BoxTheme);
	private NotificationCompat.Builder aBuilder;
	public NotificationManager aNotificationManager;
	private int totSeconds;
	private int currentTime;
	protected File audioFile;
	protected String basename;
	public String aSuffix = ".audio";
	public String vfilename;
	private boolean removeVideo;
	private boolean removeAudio;
	//private String extrType;
	private ListView lv;
	private Editable searchText;
	
	//private int index;
	
	static List<String> idEntries = new ArrayList<String>();
	static List<String> typeEntries = new ArrayList<String>();
	static List<String> statusEntries = new ArrayList<String>();
	static List<String> pathEntries = new ArrayList<String>();
	static List<String> filenameEntries = new ArrayList<String>();
	static List<String> basenameEntries = new ArrayList<String>();
	static List<String> audioExtEntries = new ArrayList<String>();
	static List<String> sizeEntries = new ArrayList<String>();
	static List<String> mediaIdEntries = new ArrayList<String>();
	
	private static List<DashboardListItem> itemsList = new ArrayList<DashboardListItem>();
	private static DashboardAdapter da;
	private boolean isSearchBarVisible;
	private DashboardListItem currentItem = null;
	private TextView userFilename;
	private boolean extrTypeIsMp3Conv;
	int posX;
	public String type;
	public boolean isFfmpegRunning = false;
	
	private Editable tagAuthor;
	private Editable tagAlbum;
	private Editable tagTitle;
	private Editable tagYear;
	private Editable tagGenre;
	private boolean newClick;
	
	public static Activity sDashboard;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Theme init
    	Utils.themeInit(this);
    	
		setContentView(R.layout.activity_dashboard);
		
		// Language init
    	Utils.langInit(this);
    	
    	sDashboard = DashboardActivity.this;
    	
    	if (da != null) clearAdapterAndLists();
    	readJson(this);
    	buildList();
    	
    	lv = (ListView) findViewById(R.id.dashboard_list);
    	
    	da = new DashboardAdapter(itemsList, this);
    	
    	if (da.isEmpty()) {
            showEmptyListInfo(this);
    	} else {
    		lv.setAdapter(da);
    	}
    	
    	lv.setTextFilterEnabled(true);
    	
    	lv.setClickable(true);
    	
    	lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
        		currentItem = da.getItem(position); // in order to refer to the filtered item
        		newClick = true;
        		final boolean ffmpegEnabled = YTD.settings.getBoolean("enable_advanced_features", false);				
        		
        		AlertDialog.Builder builder = new AlertDialog.Builder(boxThemeContextWrapper);
        		builder.setTitle(currentItem.getFilename());
        		
        		if (currentItem.getStatus().equals(getString(R.string.json_status_completed)) && !isFfmpegRunning) {
        		
	        		if (currentItem.getType().equals(getString(R.string.json_type_video))) {
	        			
	        			// handle click on a **VIDEO** file entry
		        		builder.setItems(R.array.dashboard_click_entries, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
		
			    				final File in = new File (currentItem.getPath(), currentItem.getFilename());
			    				if (ffmpegEnabled) {
			    					switch (which) {
			    					case 0: // open
			    						Intent openIntent = new Intent(Intent.ACTION_VIEW);
			    						openIntent.setDataAndType(Uri.fromFile(in), "video/*");
			    						startActivity(Intent.createChooser(openIntent,"dialog title"));
			    						break;
					    			case 1: // extract audio only
					    				AlertDialog.Builder builder0 = new AlertDialog.Builder(boxThemeContextWrapper);
					    			    LayoutInflater inflater0 = getLayoutInflater();
					    			    final View view0 = inflater0.inflate(R.layout.dialog_audio_extr_only, null);
		
					    			    builder0.setView(view0)
					    			           .setPositiveButton("OK", new DialogInterface.OnClickListener() {
					    			               @Override
					    			               public void onClick(DialogInterface dialog, int id) {
					    			            	   
					    			            	   CheckBox cb0 = (CheckBox) view0.findViewById(R.id.rem_video_0);
					    			            	   removeVideo = cb0.isChecked();
		
					    			            	   Utils.logger("v", "Launching FFmpeg on: " + in +
					    			            			   "\n-> mode: extraction only" +
					    			            			   "\n-> remove video: " + removeVideo, DEBUG_TAG);
					    			            	   
					    			            	   ffmpegJob(in, null);
					    			               }
					    			           })
					    			           .setNegativeButton(R.string.dialogs_negative, new DialogInterface.OnClickListener() {
					    			               public void onClick(DialogInterface dialog, int id) {
					    			                   //
					    			               }
					    			           });      
					    			    
					    			    secureShowDialog(builder0);
					    			    
			    						break;
					    			case 2: // extract audio and convert to mp3
					    				AlertDialog.Builder builder1 = new AlertDialog.Builder(boxThemeContextWrapper);
					    			    LayoutInflater inflater1 = getLayoutInflater();
					    			    
					    			    final View view1 = inflater1.inflate(R.layout.dialog_audio_extr_mp3_conv, null);
		
					    			    builder1.setView(view1)
					    			           .setPositiveButton("OK", new DialogInterface.OnClickListener() {
					    			               @Override
					    			               public void onClick(DialogInterface dialog, int id) {
					    			            	   
					    			            	   final Spinner sp = (Spinner) view1.findViewById(R.id.mp3_spinner);
					    			            	   final String bitrate = String.valueOf(sp.getSelectedItem());
					    			            	   
					    			            	   CheckBox cb1 = (CheckBox) view1.findViewById(R.id.rem_video_1);
					    			            	   removeVideo = cb1.isChecked();
					    			            	   
					    			            	   Utils.logger("v", "Launching FFmpeg on: " + in +
					    			            			   "\n-> mode: conversion to mp3 with bitrate: " + bitrate + 
					    			            			   "\n-> remove video: " + removeVideo, DEBUG_TAG);
					    			            	   
					    			            	   ffmpegJob(in, bitrate);
					    			               }
					    			           })
					    			           .setNegativeButton(R.string.dialogs_negative, new DialogInterface.OnClickListener() {
					    			               public void onClick(DialogInterface dialog, int id) {
					    			                   //
					    			               }
					    			           });      
					    			    
					    			    secureShowDialog(builder1);
		
					    				/*break;
					    			case 2:
			
					    				break;
					    			case 3:
					    				*/
			    					}
			    				} else {
			    					notifyFfmpegNotInstalled();
			    				}
							}
		        		});
		        		
		        		secureShowDialog(builder);
			    		
					} else if (currentItem.getType().equals(getString(R.string.json_type_audio_extr))) {
						
						// handle click on a **AUDIO** file entry
						builder.setItems(R.array.dashboard_click_entries_audio, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
		
			    				final File in = new File (currentItem.getPath(), currentItem.getFilename());
			    				if (ffmpegEnabled) {
			    					switch (which) {
			    					case 0: // open
			    						Intent openIntent = new Intent(Intent.ACTION_VIEW);
			    						openIntent.setDataAndType(Uri.fromFile(in), "audio/*");
			    						startActivity(Intent.createChooser(openIntent,"dialog title"));
			    						break;
					    			case 1: // convert to mp3
					    				AlertDialog.Builder builder0 = new AlertDialog.Builder(boxThemeContextWrapper);
					    			    LayoutInflater inflater0 = getLayoutInflater();
					    			    final View view2 = inflater0.inflate(R.layout.dialog_audio_mp3_conv, null);
		
					    			    builder0.setView(view2)
					    			           .setPositiveButton("OK", new DialogInterface.OnClickListener() {
					    			               @Override
					    			               public void onClick(DialogInterface dialog, int id) {
					    			            	   
					    			            	   final Spinner sp = (Spinner) view2.findViewById(R.id.mp3_spinner_a);
					    			            	   final String bitrate = String.valueOf(sp.getSelectedItem());
					    			            	   
					    			            	   CheckBox cb2 = (CheckBox) view2.findViewById(R.id.rem_original_audio);
					    			            	   removeAudio = cb2.isChecked();
		
					    			            	   Utils.logger("v", "Launching FFmpeg on: " + in +
					    			            			   "\n-> mode: conversion to mp3 from audio file" +
					    			            			   "\n-> remove audio: " + removeAudio, DEBUG_TAG);
					    			            	   
					    			            	   ffmpegJob(in, bitrate);
					    			               }
					    			           })
					    			           .setNegativeButton(R.string.dialogs_negative, new DialogInterface.OnClickListener() {
					    			               public void onClick(DialogInterface dialog, int id) {
					    			                   //
					    			               }
					    			           });      
					    			    
					    			    secureShowDialog(builder0);
			    					}
		    					} else {
		    						notifyFfmpegNotInstalled();
			                	    
			    				}
							}
		        		});
		        		
		        		secureShowDialog(builder);
					}
        		}
			}	
    	});
    	
    	lv.setLongClickable(true);
    	lv.setOnItemLongClickListener(new OnItemLongClickListener() {

        	@Override
        	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        		
        		AlertDialog.Builder builder = new AlertDialog.Builder(boxThemeContextWrapper);
        		
        		currentItem = da.getItem(position); // in order to refer to the filtered item
        		
        		builder.setTitle(currentItem.getFilename());
        		
        		if (currentItem.getStatus().equals(getString(R.string.json_status_completed))) {
	        		//builder.setItems(R.array.dashboard_long_click_entries, new DialogInterface.OnClickListener() {
        			final ArrayAdapter<CharSequence> cla = CustomLongClickAdapter.createFromResource(
        					boxThemeContextWrapper,
        		            android.R.layout.simple_list_item_1, 
        		            true);
        			builder.setAdapter(cla, new DialogInterface.OnClickListener() {
	
						public void onClick(DialogInterface dialog, int which) {
				    		switch (which) {
				    			case 0:
				    				copy(currentItem);
				    				break;
				    			case 1:
				    				move(currentItem);
				    				break;
				    			case 2:
				    				rename(currentItem);
				    				break;
				    			case 3:
				    				removeFromDashboard(currentItem);
				    				break;
				    			case 4:
				    				delete(currentItem);
				    		}
	
						}
	        		});
        		} else if (currentItem.getStatus().equals(getString(R.string.json_status_in_progress))) {
        			
        		} else if (currentItem.getStatus().equals(getString(R.string.json_status_failed))) {
        			
        		}
	        	secureShowDialog(builder);
			    return true;
        		
        	}
    	});
	}
	
	private void copy(DashboardListItem currentItem) {
		Intent intent = new Intent(DashboardActivity.this,  FileChooserActivity.class);
    	if (intent != null) {
    		intent.putExtra(FileChooserActivity._Rootpath, (Parcelable) new LocalFile(Environment.getExternalStorageDirectory()));
    		intent.putExtra(FileChooserActivity._FilterMode, IFileProvider.FilterMode.DirectoriesOnly);
    		startActivityForResult(intent, 1);
    	}
	}
	
	private void move(DashboardListItem currentItem) {
		Intent intent = new Intent(DashboardActivity.this,  FileChooserActivity.class);
    	if (intent != null) {
    		intent.putExtra(FileChooserActivity._Rootpath, (Parcelable) new LocalFile(Environment.getExternalStorageDirectory()));
    		intent.putExtra(FileChooserActivity._FilterMode, IFileProvider.FilterMode.DirectoriesOnly);
    		startActivityForResult(intent, 2);
    	}
	}
	
	private void rename(final DashboardListItem currentItem) {
		AlertDialog.Builder adb = new AlertDialog.Builder(boxThemeContextWrapper);
		LayoutInflater adbInflater = LayoutInflater.from(DashboardActivity.this);
	    View inputFilename = adbInflater.inflate(R.layout.dialog_input_filename, null);
	    userFilename = (TextView) inputFilename.findViewById(R.id.input_filename);
	    userFilename.setText(currentItem.getFilename());
	    adb.setView(inputFilename);
	    adb.setTitle(getString(R.string.rename_dialog_title));
	    //adb.setMessage(getString(R.string.rename_dialog_msg));
	    
	    adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	    	public void onClick(DialogInterface dialog, int which) {
	    		String input = userFilename.getText().toString();
	    		File in = new File(currentItem.getPath(), currentItem.getFilename());
	    		File renamed = new File(currentItem.getPath(), input);
	    		if (in.renameTo(renamed)) {
	    			// set new name to the list item
	    			currentItem.setFilename(input);
	    			
	    			// update the JSON file entry
	    			Utils.addEntryToJsonFile(
							DashboardActivity.this, 
							currentItem.getId(), 
							currentItem.getType(), 
							currentItem.getStatus(), 
							currentItem.getPath(), 
							input, 
							Utils.getFileNameWithoutExt(input), 
							currentItem.getAudioExt(), 
							currentItem.getSize(),
							false);
	    			
	    			// remove references for the old file
	    			String mediaUriString = YTD.videoinfo.getString(in.getAbsolutePath(), "non-ext");
	    			Utils.logger("d", "mediaString: " + mediaUriString, DEBUG_TAG);
	    			
	    			// check if it actually exists (for video on-extSdCard/copied/renamed mediaUriString is not stored)
	    			if (mediaUriString.equals("non-ext")) {
	    				DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
	    				long id = Long.parseLong(currentItem.getId());
	    				if (dm.remove(id) > 0) {
	    					Utils.logger("d", id + " (DownloadManager) removed", DEBUG_TAG);
	    					// remove entries from videoinfo shared pref
	    					/*YTD.videoinfo.edit()
    	    					.remove(id + YTD.VIDEOINFO_FILENAME)
    	    					.remove(id + YTD.VIDEOINFO_PATH)
    	    					.remove(id + YTD.VIDEOINFO_AUDIO_FILENAME)
    	    					.remove(in.getName())
    	    					.apply();*/
	    				}
	    			} else {
	    				Uri mediaUri = Uri.parse(mediaUriString);
	    				// remove media file reference from MediaStore library via ContentResolver
	    				if (getContentResolver().delete(mediaUri, null, null) > 0) {
	    					Utils.logger("d", mediaUri.toString() + " (ContentResolver) removed", DEBUG_TAG);
	    				} else {
	    					Utils.logger("w", mediaUri.toString() + " (ContentResolver) NOT removed", DEBUG_TAG);
	    				}
	    				// remove entry from videoinfo shared pref (dm video)
	    				YTD.videoinfo.edit().remove(in.getAbsolutePath()).apply();
	    			}
	    			
	    			// scan the new file
	    			Utils.scanMedia(DashboardActivity.this, 
							new String[]{ renamed.getAbsolutePath() }, 
							new String[]{ "video/*" });
	    			
	    			// refresh the dashboard
	    			refreshlist(DashboardActivity.this);
	    			
	    			Utils.logger("d", in.getName() + " _renamed to_ " + input, DEBUG_TAG);
	    		} else {
	    			Log.e(DEBUG_TAG, in.getName() + " NOT renamed");
	    		}
	    		
	    		// hide keyboard
	    		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
	    		imm.hideSoftInputFromWindow(userFilename.getWindowToken(), 0);
	    	}
	    });
	    
	    adb.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int which) {
                // cancel
            }
        });
	    
	    secureShowDialog(adb);
	}
	
	public void  removeFromDashboard(final DashboardListItem currentItem) {
		Utils.removeEntryFromJsonFile(DashboardActivity.this, currentItem.getId());
		refreshlist(DashboardActivity.this);
	}

	public void delete(final DashboardListItem currentItem) {
		AlertDialog.Builder del = new AlertDialog.Builder(boxThemeContextWrapper);
		del.setTitle(getString(R.string.attention));
		del.setMessage(getString(R.string.delete_video_confirm));
		del.setIcon(android.R.drawable.ic_dialog_alert);
		del.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				final File fileToDel = new File(currentItem.getPath(), currentItem.getFilename());
				new AsyncDelete().execute(fileToDel);
			}
		});
		
		del.setNegativeButton(R.string.dialogs_negative, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// cancel
			}
		});
		
		secureShowDialog(del);
	}
	
	@Override
    public void onResume(){
    	super.onResume();
    	Utils.logger("v", "_onResume", DEBUG_TAG);
    	isDashboardRunning = true;
    }
   
    @Override
    public void onPause() {
    	super.onPause();
    	Utils.logger("v", "_onPause", DEBUG_TAG);
    	isDashboardRunning = false;
    }

	public static void showEmptyListInfo(Activity activity) {
		TextView info = (TextView) activity.findViewById(R.id.dashboard_activity_info);
		info.setVisibility(View.VISIBLE);
		Utils.logger("v", "__dashboard is empty__", DEBUG_TAG);
	}
	
	private class AsyncDelete extends AsyncTask<File, Void, Boolean> {

		File fileToDelete;
		
		protected Boolean doInBackground(File... fileToDel) {
			fileToDelete = fileToDel[0];
			return doDelete(currentItem, fileToDel[0], true);
		}
		
		@Override
		protected void onPostExecute(Boolean success) {
			if (success) {
				notifyDeletionOk(currentItem, fileToDelete);
			} else {
				notifyDeletionUnsuccessful(currentItem, fileToDelete);
			}
		}
	}
	
	private boolean doDelete(final DashboardListItem currentItem, File fileToDel, boolean removeFromJsonAlso) {
		Utils.logger("v", "----------> BEGIN delete", DEBUG_TAG);
		boolean res = false;
		if (currentItem.getStatus().equals(getString(R.string.json_status_completed))) {
			
			// get media Uri string stored in "videoinfo" prefs
			String mediaUriString = YTD.videoinfo.getString(fileToDel.getAbsolutePath(), "non-ext");
			Utils.logger("d", "mediaString: " + mediaUriString, DEBUG_TAG);
			
			// check if it actually exists (for video on-extSdCard/copied/renamed mediaUriString is not stored)
			if (mediaUriString.equals("non-ext")) {
				// video NOT on extSdCard -> use DownloadManager to remove file and delete MediaStore ref.
				res = removeViaDm(currentItem, fileToDel);
			} else {
				// video on extSdCard -> manually delete file 
				res = removeManually(currentItem, fileToDel, mediaUriString);
			}
		} else if (currentItem.getStatus().equals(getString(R.string.json_status_in_progress))) {
			if (currentItem.getType().equals(getString(R.string.json_type_video))) {
				// video download in progress -> use DownloadManager anyway to remove file
				res = removeViaDm(currentItem, fileToDel);
			}
		}
		
		if (removeFromJsonAlso) {
			// remove entry from JSON and reload Dashboard
			Utils.removeEntryFromJsonFile(DashboardActivity.this, currentItem.getId());
		}
		
		refreshlist(DashboardActivity.this);
		Utils.logger("v", "----------> END delete", DEBUG_TAG);
		
		return res;
	}

	public boolean removeManually(DashboardListItem currentItem, File fileToDel, String mediaUriString) {
		if (fileToDel.delete()) {			
			// parse media Uri
			Uri mediaUri = Uri.parse(mediaUriString);
			// remove media file reference from MediaStore library via ContentResolver
			if (getContentResolver().delete(mediaUri, null, null) > 0) {
				Utils.logger("d", mediaUri.toString() + " (ContentResolver) removed", DEBUG_TAG);
			} else {
				Utils.logger("w", mediaUri.toString() + " (ContentResolver) NOT removed", DEBUG_TAG);
			}
			// remove entry from videoinfo shared pref
			YTD.videoinfo.edit().remove(fileToDel.getAbsolutePath()).apply();
			return true;
		} else {
			return false;
		}
	}

	public boolean removeViaDm(final DashboardListItem currentItem, File fileToDel) {
		DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		long id = Long.parseLong(currentItem.getId());
		if (dm.remove(id) > 0) {
			Utils.logger("d", id + " (DownloadManager) removed", DEBUG_TAG);
			// remove entries from videoinfo shared pref
			/*YTD.videoinfo.edit()
				.remove(id + YTD.VIDEOINFO_FILENAME)
				.remove(id + YTD.VIDEOINFO_PATH)
				.remove(id + YTD.VIDEOINFO_AUDIO_FILENAME)
				.remove(fileToDel.getName())
				.apply();*/
			YTD.videoinfo.edit().remove(fileToDel.getName()).apply();
			return true;
		} else {
			Utils.logger("w", id + " (DownloadManager) NOT removed", DEBUG_TAG);
			return false;
		}
	}

	public void notifyDeletionUnsuccessful(final DashboardListItem currentItem, File fileToDel) {
		Utils.logger("w", fileToDel.getPath() + " NOT deleted.", DEBUG_TAG);
		Toast.makeText(DashboardActivity.this, 
				getString(R.string.delete_video_failed, currentItem.getFilename()), 
				Toast.LENGTH_LONG).show();
	}

	public void notifyDeletionOk(final DashboardListItem currentItem, File fileToDel) {
		Utils.logger("d", fileToDel.getPath() + " successfully deleted.", DEBUG_TAG);
		Toast.makeText(DashboardActivity.this, 
				getString(R.string.delete_video_ok, currentItem.getFilename()), 
				Toast.LENGTH_LONG).show();
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
	
	public static void clearAdapterAndLists() {
		// clear the adapter
		da.clear();
		
		// empty the Lists
		idEntries.clear();
		typeEntries.clear();
		statusEntries.clear();
		pathEntries.clear();
		filenameEntries.clear();
		basenameEntries.clear();
		audioExtEntries.clear();
		sizeEntries.clear();
	}

	private static void buildList() {
		Iterator<String> idsIter = idEntries.iterator();
		Iterator<String> typesIter = typeEntries.iterator();
		Iterator<String> statusesIter = statusEntries.iterator();
		Iterator<String> pathsIter = pathEntries.iterator();
		Iterator<String> filenamesIter = filenameEntries.iterator();
		Iterator<String> basenamesIter = basenameEntries.iterator();
		Iterator<String> audioExtIter = audioExtEntries.iterator();
		Iterator<String> sizesIter = sizeEntries.iterator();
		
		while (statusesIter.hasNext()) {
			itemsList.add(new DashboardListItem(
					idsIter.next(),
					typesIter.next(),
					statusesIter.next()
						.replace(YTD.JSON_DATA_STATUS_C, sDashboard.getString(R.string.json_status_completed))
						.replace(YTD.JSON_DATA_STATUS_I, sDashboard.getString(R.string.json_status_in_progress))
						.replace(YTD.JSON_DATA_STATUS_F, sDashboard.getString(R.string.json_status_failed)),
					pathsIter.next(), 
					filenamesIter.next(), 
					basenamesIter.next(),
					audioExtIter.next(), 
					sizesIter.next()));
		}
	}
	
	private static void readJson(Context context) {
		// parse existing/init new JSON 
		String previousJson = Utils.parseJsonDashboardFile(context);
				
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
				typeEntries.add(jO.getString(YTD.JSON_DATA_TYPE));
				statusEntries.add(jO.getString(YTD.JSON_DATA_STATUS));
				pathEntries.add(jO.getString(YTD.JSON_DATA_PATH));
				filenameEntries.add(jO.getString(YTD.JSON_DATA_FILENAME));
				basenameEntries.add(jO.getString(YTD.JSON_DATA_BASENAME));
				audioExtEntries.add(jO.getString(YTD.JSON_DATA_AUDIO_EXT));
				sizeEntries.add(jO.getString(YTD.JSON_DATA_SIZE));
			}
		} catch (JSONException e) {
			Log.e(DEBUG_TAG, e.getMessage());
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
            @SuppressWarnings("unchecked")
			List<LocalFile> files = (List<LocalFile>) data.getSerializableExtra(FileChooserActivity._Results);
            	
        	File chooserFolder = files.get(0);
        	Utils.logger("d", "file-chooser selection: " + chooserFolder.getPath(), DEBUG_TAG);
        	Utils.logger("d", "origin file's folder:   " + currentItem.getPath(), DEBUG_TAG);
        	
        	File in = new File(currentItem.getPath(), currentItem.getFilename());
			//File out = new File(chooserFolder, "copy_" + currentItem.getFilename());
			File out = new File(chooserFolder, currentItem.getFilename());
			
	        switch (requestCode) {
	        case 1: // ------------- > COPY
	        	
	        	if (chooserFolder.getPath().equals(currentItem.getPath())) {
	        		out = new File(chooserFolder, "copy_" + currentItem.getFilename());
	        	}

    			if (!out.exists()) {
		        	switch (Utils.pathCheck(chooserFolder)) {
		    		case 0:
		    			// Path on standard sdcard
		    			new AsyncCopy().execute(in, out);
		        		break;
		    		case 1:
		    			// Path not writable
		    			PopUps.showPopUp(getString(R.string.system_warning_title), getString(R.string.system_warning_msg), "alert", DashboardActivity.this);
		    			break;
		    		case 2:
		    			// Path not mounted
		    			Toast.makeText(DashboardActivity.this, getString(R.string.sdcard_unmounted_warning), Toast.LENGTH_SHORT).show();
		        	}
    			} else {
	        		PopUps.showPopUp(getString(R.string.long_press_warning_title), getString(R.string.long_press_warning_msg2), "info", DashboardActivity.this);
	        	}
    			break;
    			
	        case 2: // ------------- > MOVE
	        	
	        	if (!chooserFolder.getPath().equals(currentItem.getPath())) {
	        		if (!out.exists()) {
			        	switch (Utils.pathCheck(chooserFolder)) {
			    		case 0:
			    			// Path on standard sdcard
			    			new AsyncMove().execute(in, out);		
			        		break;
			    		case 1:
			    			// Path not writable
			    			PopUps.showPopUp(getString(R.string.system_warning_title), getString(R.string.system_warning_msg), "alert", DashboardActivity.this);
			    			break;
			    		case 2:
			    			// Path not mounted
			    			Toast.makeText(DashboardActivity.this, getString(R.string.sdcard_unmounted_warning), Toast.LENGTH_SHORT).show();
			        	}
	        		} else {
		        		PopUps.showPopUp(getString(R.string.long_press_warning_title), getString(R.string.long_press_warning_msg2), "info", DashboardActivity.this);
		        	}
	        	} else {
	        		PopUps.showPopUp(getString(R.string.long_press_warning_title), getString(R.string.long_press_warning_msg), "info", DashboardActivity.this);
	        	}
	        }
		}
    }
	
	private class AsyncMove extends AsyncTask<File, Void, Integer> {
		
		File out;
		
		protected void onPreExecute() {
			Utils.logger("d", currentItem.getFilename() + " ---> BEGIN move", DEBUG_TAG);
			Toast.makeText(DashboardActivity.this, 
					currentItem.getFilename() + ": " + getString(R.string.move_progress), 
					Toast.LENGTH_SHORT).show();
		}
		
		protected Integer doInBackground(File... file) {
			out = file[1];
			try {
				Utils.copyFile(file[0], file[1]);
				doDelete(currentItem, file[0], false);
				return 0;
			} catch (IOException e) {
				return 1;
			}
		}
		
		@Override
		protected void onPostExecute(Integer res) {
			switch (res) {
			case 0:
				Toast.makeText(DashboardActivity.this, 
						currentItem.getFilename() + ": " + getString(R.string.move_ok), 
						Toast.LENGTH_LONG).show();
				Utils.logger("i", currentItem.getFilename() + " --> END move: OK", DEBUG_TAG);
				
				Utils.scanMedia(DashboardActivity.this, 
						new String[]{ out.getAbsolutePath() }, 
						new String[]{ "video/*" });
				
				Utils.addEntryToJsonFile(
						DashboardActivity.this, 
						currentItem.getId(), 
						currentItem.getType(), 
						currentItem.getStatus(), 
						out.getParent(), 
						out.getName(), 
						currentItem.getBasename(), 
						currentItem.getAudioExt(), 
						currentItem.getSize(), 
						false);
				break;
				
			case 1:
				Toast.makeText(DashboardActivity.this, 
						currentItem.getFilename() + ": " + getString(R.string.move_error), 
						Toast.LENGTH_LONG).show();
				Log.e(DEBUG_TAG, currentItem.getFilename() + " --> END move: FAILED");
			}
			
			refreshlist(DashboardActivity.this);
		}
	}
	
	private class AsyncCopy extends AsyncTask<File, Void, Integer> {
		
		File out;
		
		protected void onPreExecute() {
			Utils.logger("d", currentItem.getFilename() + " ---> BEGIN copy", DEBUG_TAG);
			Toast.makeText(DashboardActivity.this, 
					currentItem.getFilename() + ": " + getString(R.string.copy_progress), 
					Toast.LENGTH_SHORT).show();
		}
		
		protected Integer doInBackground(File... file) {
			out = file[1];
			try {
				Utils.copyFile(file[0], file[1]);
				return 0;
			} catch (IOException e) {
				return 1;
			}
		}
		
		@Override
		protected void onPostExecute(Integer res) {
			switch (res) {
			case 0:
				Toast.makeText(DashboardActivity.this, 
						currentItem.getFilename() + ": " + getString(R.string.copy_ok), 
						Toast.LENGTH_LONG).show();
				Utils.logger("i", currentItem.getFilename() + " --> END copy: OK", DEBUG_TAG);
				
				Utils.scanMedia(DashboardActivity.this, 
						new String[]{ out.getAbsolutePath() }, 
						new String[]{ "video/*" });
				
				Utils.addEntryToJsonFile(
						DashboardActivity.this, 
						currentItem.getId(), 
						currentItem.getType(), 
						currentItem.getStatus(), 
						out.getParent(), 
						out.getName(), 
						currentItem.getBasename(), 
						currentItem.getAudioExt(), 
						currentItem.getSize(), 
						true);
				break;
			
			case 1:
				Toast.makeText(DashboardActivity.this, 
						currentItem.getFilename() + ": " + getString(R.string.copy_error), 
						Toast.LENGTH_LONG).show();
				Log.e(DEBUG_TAG, currentItem.getFilename() + " --> END copy: FAILED");
			}
			
			refreshlist(DashboardActivity.this);
		}
	}
	
	public static void refreshlist(final Activity activity) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				
				clearAdapterAndLists();
			    
			    // refill the Lists and re-populate the adapter
			    readJson((Context) activity);
				buildList();
				
				if (da.isEmpty()) {
		            showEmptyListInfo(activity);
		    	}
				
				// refresh the list view
				da.notifyDataSetChanged();
			}
		});
	}
	
	// #####################################################################
	
	public void editId3Tags(View view) {		
		AlertDialog.Builder builder = new AlertDialog.Builder(boxThemeContextWrapper);
	    LayoutInflater inflater0 = getLayoutInflater();
	    final View id3s = inflater0.inflate(R.layout.dialog_edit_id3, null);
	    
	    final EditText authorEt = (EditText) id3s.findViewById(R.id.id3_et_author);
	    final EditText titleEt = (EditText) id3s.findViewById(R.id.id3_et_title);	
	    final EditText albumEt = (EditText) id3s.findViewById(R.id.id3_et_album);
	    final EditText genreEt = (EditText) id3s.findViewById(R.id.id3_et_genre);
	    final EditText yearEt = (EditText) id3s.findViewById(R.id.id3_et_year);
	    
	    if (tagTitle == null || newClick) {
			titleEt.setText(currentItem.getBasename());
		} else {
			titleEt.setText(tagTitle);
		}
	    
	    if (tagYear == null || newClick) {
			Calendar cal = new GregorianCalendar();
			int y = cal.get(Calendar.YEAR);
			yearEt.setText(String.valueOf(y));
		} else {
			yearEt.setText(tagYear);
		}
	    
	    if (!newClick) {
			authorEt.setText(tagAuthor);
			albumEt.setText(tagAlbum);
			genreEt.setText(tagGenre);
		}
	    
		builder.setView(id3s)
	           .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	               @Override
	               public void onClick(DialogInterface dialog, int id) {
	            	   // TODO
	            	   tempStoreTags(authorEt, titleEt, albumEt, genreEt, yearEt);
	               }
	           })
	           .setNegativeButton(R.string.dialogs_negative, new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int id) {
	                   // cancel
	            	   tempStoreTags(authorEt, titleEt, albumEt, genreEt, yearEt);
	               }
	           });      
	    
	    secureShowDialog(builder);
	    newClick = false;
	}
	
	public void tempStoreTags(final EditText authorEt, final EditText titleEt, 
			final EditText albumEt, final EditText genreEt, final EditText yearEt) {
		tagAlbum = albumEt.getText();
		tagAuthor = authorEt.getText();
		tagTitle = titleEt.getText();
		tagYear = yearEt.getText();
		tagGenre = genreEt.getText();
	}
	
	

	public void ffmpegJob(final File fileToConvert, final String mp3BitRate) {
		isFfmpegRunning = true;
		
		vfilename = currentItem.getFilename();
		
		// audio job notification init
		aBuilder =  new NotificationCompat.Builder(this);
		aNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		aBuilder.setSmallIcon(R.drawable.ic_stat_ytd);
		aBuilder.setContentTitle(vfilename);
		
		String aExt = currentItem.getAudioExt();
		basename = currentItem.getBasename();
		
		final String audioFileName;
		// "compose" the audio file
		if (mp3BitRate != null) {
			extrTypeIsMp3Conv = true;
			audioFileName = basename + "_" + mp3BitRate + ".mp3";
		} else {
			extrTypeIsMp3Conv = false;
			audioFileName = basename + aExt;
			
		}
		
		audioFile = new File(fileToConvert.getParent(), audioFileName);

		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				
				FfmpegController ffmpeg = null;
			    try {
			    	ffmpeg = new FfmpegController(DashboardActivity.this);
			    	
			    	// Toast + Notification + Log ::: Audio job in progress...
			    	String text = null;
			    	if (!extrTypeIsMp3Conv) {
						text = getString(R.string.audio_extr_progress);
						type = getString(R.string.json_type_audio_extr);
					} else {
						text = getString(R.string.audio_conv_progress);
						type = getString(R.string.json_type_audio_mp3);
					}
			    	Toast.makeText(DashboardActivity.this,"YTD: " + text, Toast.LENGTH_LONG).show();
			    	
			    	/*Utils.addEntryToJsonFile(
							DashboardActivity.this, 
							currentItem.getId(), 
							type, 
							YTD.JSON_DATA_STATUS_I,
							currentItem.getPath(), 
							audioFile.getName(), 
							currentItem.getBasename(), 
							"", 
							"-", 
							true);
					
					refreshlist(DashboardActivity.this);*/
					
			    	aBuilder.setContentTitle(audioFileName);
			        aBuilder.setContentText(text);
					aNotificationManager.notify(2, aBuilder.build());
					Utils.logger("i", vfilename + " " + text, DEBUG_TAG);
			    } catch (IOException ioe) {
			    	Log.e(DEBUG_TAG, "Error loading ffmpeg. " + ioe.getMessage());
			    }
			    
			    ShellDummy shell = new ShellDummy();
			    
			    try {
					ffmpeg.extractAudio(fileToConvert, audioFile, mp3BitRate, shell);
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
			if (extrTypeIsMp3Conv) {
				getAudioJobProgress(shellLine);
			}
			Utils.logger("d", shellLine, DEBUG_TAG);
		}

		@Override
		public void processComplete(int exitValue) {
			Utils.logger("i", "FFmpeg process exit value: " + exitValue, DEBUG_TAG);
			String text = null;
			Intent audioIntent = new Intent(Intent.ACTION_VIEW);
			if (exitValue == 0) {

				// Toast + Notification + Log ::: Audio job OK
				if (!extrTypeIsMp3Conv) {
					text = getString(R.string.audio_extr_completed);
				} else {
					text = getString(R.string.audio_conv_completed);
				}
				Utils.logger("d", vfilename + " " + text, DEBUG_TAG);
				
				audioFile = addSuffixToAudioFile(basename, audioFile);
				Toast.makeText(DashboardActivity.this,  audioFile.getName() + ": " + text, Toast.LENGTH_LONG).show();
				aBuilder.setContentTitle(audioFile.getName());
				aBuilder.setContentText(text);			
				audioIntent.setDataAndType(Uri.fromFile(audioFile), "audio/*");
				PendingIntent contentIntent = PendingIntent.getActivity(DashboardActivity.this, 0, audioIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        		aBuilder.setContentIntent(contentIntent);
        		
        		// write id3 tags
				if (extrTypeIsMp3Conv) {
					try {
						Utils.logger("d", "writing ID3 tags...", DEBUG_TAG);
						addId3Tags(audioFile);
					} catch (ID3WriteException e) {
						Log.e(DEBUG_TAG, "Unable to write id3 tags", e);
					} catch (IOException e) {
						Log.e(DEBUG_TAG, "Unable to write id3 tags", e);
					}
				}
				
				Utils.scanMedia(getApplicationContext(), 
						new String[] {audioFile.getAbsolutePath()}, 
						new String[] {"audio/*"});
				
				// remove selected video upon successful audio extraction
				if (removeVideo) {
					final File fileToDel = new File(currentItem.getPath(), currentItem.getFilename());
					new AsyncDelete().execute(fileToDel);
				}
				
				// add audio file to the JSON file entry
				Utils.addEntryToJsonFile(
						DashboardActivity.this, 
						currentItem.getId(), 
						type, 
						YTD.JSON_DATA_STATUS_C,
						currentItem.getPath(), 
						audioFile.getName(), 
						currentItem.getBasename(), 
						"", 
						Utils.MakeSizeHumanReadable((int) audioFile.length(), true), 
						true);
				
				refreshlist(DashboardActivity.this);
				
				Utils.setNotificationDefaults(aBuilder);
			} else {
				setNotificationForAudioJobError();
				
				Utils.addEntryToJsonFile(
						DashboardActivity.this, 
						currentItem.getId(), 
						type, 
						YTD.JSON_DATA_STATUS_F,
						currentItem.getPath(), 
						audioFile.getName(), 
						currentItem.getBasename(), 
						"", 
						"-", 
						true);
				
				refreshlist(DashboardActivity.this);
			}
			
			aBuilder.setProgress(0, 0, false);
			aNotificationManager.cancel(2);
			aNotificationManager.notify(2, aBuilder.build());
			
			isFfmpegRunning = false;
		}
		
		@Override
		public void processNotStartedCheck(boolean started) {
			if (!started) {
				Utils.logger("w", "FFmpeg process not started or not completed", DEBUG_TAG);

				// Toast + Notification + Log ::: Audio job error
				setNotificationForAudioJobError();
			}
			aNotificationManager.notify(2, aBuilder.build());
			isFfmpegRunning = false;
		}
    }
    
	public File addSuffixToAudioFile(String aBaseName, File extractedAudioFile) {
		// Rename audio file to add a more detailed suffix, 
		// but only if it has been matched from the ffmpeg console output
		if (!extrTypeIsMp3Conv &&
				extractedAudioFile.exists() && 
				!aSuffix.equals(".audio")) {
			String newName = aBaseName + aSuffix;
			File newFile = new File(currentItem.getPath(), newName);
			if (extractedAudioFile.renameTo(newFile)) {
				Utils.logger("i", extractedAudioFile.getName() + " renamed to: " + newName, DEBUG_TAG);
				return newFile;
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
	        meta.setSongTitle(basename);
	        Calendar cal = Calendar.getInstance();
	        int year = cal.get(Calendar.YEAR);
	        meta.setYear(String.valueOf(year));
	        new MyID3().update(src, src_set, meta);
        }
	}

	private void findAudioSuffix(String shellLine) {
		Pattern audioPattern = Pattern.compile("#0:0.*: Audio: (.+), .+?(mono|stereo .default.|stereo)(, .+ kb|)"); 
		Matcher audioMatcher = audioPattern.matcher(shellLine);
		if (audioMatcher.find() && !extrTypeIsMp3Conv) {
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
			
			Utils.logger("i", "Audio suffix found: " + aSuffix, DEBUG_TAG);
		}
	}

	public void setNotificationForAudioJobError() {
		String text;
		if (!extrTypeIsMp3Conv) {
			text = getString(R.string.audio_extr_error);
		} else {
			text = getString(R.string.audio_conv_error);
		}
		Log.e(DEBUG_TAG, vfilename + " " + text);
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
			aNotificationManager.notify(2, aBuilder.build());
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

	public void secureShowDialog(AlertDialog.Builder adb) {
		//builder.create();
		if (! ((Activity) DashboardActivity.this).isFinishing()) {
			adb.show();
		}
	}

	public void notifyFfmpegNotInstalled() {
		Utils.logger("w", "FFmpeg not installed/enabled", DEBUG_TAG);
		
		AlertDialog.Builder adb = new AlertDialog.Builder(boxThemeContextWrapper);
		adb.setTitle(getString(R.string.ffmpeg_not_enabled_title));
		adb.setMessage(getString(R.string.ffmpeg_not_enabled_msg));
		
		adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				startActivity(new Intent(DashboardActivity.this,  SettingsActivity.class));
			}
		
		});
		
		adb.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
		        // cancel
		    }
		});
		
		secureShowDialog(adb);
	}
}
