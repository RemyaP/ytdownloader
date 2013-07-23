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

package dentex.youtube.downloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;

import dentex.youtube.downloader.service.DownloadsService;
import dentex.youtube.downloader.utils.FetchGanttFunction;
import dentex.youtube.downloader.utils.Observer;
import dentex.youtube.downloader.utils.PopUps;
import dentex.youtube.downloader.utils.RhinoRunner;
import dentex.youtube.downloader.utils.Utils;

public class ShareActivity extends Activity {
	
	private ProgressBar progressBar1;
	private ProgressBar progressBarD;
	private ProgressBar progressBarL;
	private ProgressBar filesizeProgressBar;
    public static final String USER_AGENT_FIREFOX = "Mozilla/5.0 (X11; Linux i686; rv:10.0) Gecko/20100101 Firefox/10.0";
	private static final String DEBUG_TAG = "ShareActivity";
    private TextView tv;
    private ListView lv;
    public ArrayAdapter<String> aA;
    List<String> links = new ArrayList<String>();
    List<String> codecs = new ArrayList<String>();
    List<String> qualities = new ArrayList<String>();
    List<String> stereo = new ArrayList<String>();
    List<String> sizes = new ArrayList<String>();
    List<String> itags = new ArrayList<String>();
    List<String> listEntries = new ArrayList<String>();
    private String titleRaw;
    private String basename;
    public int pos;
    public static File path;
    public String validatedLink;
    public static DownloadManager dm;
    public static long enqueue;
	String videoFilename = "video";
	public String vFilename = "";
    public static Uri videoUri;
	public boolean videoOnExt;
    private int icon;
    public ScrollView generalInfoScrollview;
	public CheckBox showAgain1;
	public CheckBox showAgain2;
	public CheckBox showAgain3;
	public TextView userFilename;
	public static final File dir_Downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
	public static final File dir_DCIM = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
	public static final File dir_Movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
	public File sdcard = Environment.getExternalStorageDirectory();
	boolean sshInfoCheckboxEnabled;
	boolean generalInfoCheckboxEnabled;
	boolean fileRenameEnabled;
	public static File chooserFolder;
	private AsyncDownload asyncDownload;
	public boolean isAsyncDownloadRunning = false;
	public String videoFileSize = "empty";
	private AsyncSizeQuery sizeQuery;
	public AlertDialog helpDialog;
	public AlertDialog waitBox;
	private AlertDialog.Builder  helpBuilder;
	private AlertDialog.Builder  waitBuilder;
	private Bitmap img;
	private ImageView imgView;
	private String videoId;
	public static String pt1;
	public static String pt2;
	public static String noDownloads;
	public static Observer.YtdFileObserver videoFileObserver;
	public static NotificationManager mNotificationManager;
	public static NotificationCompat.Builder mBuilder;
	public static String onlineVersion;
	public static List<Long> sequence = new ArrayList<Long>();
	String audioFilename = "audio";
	public static String audioCodec = "";
	public static Context mContext;
	boolean showSizesInVideoList;
	boolean showSingleSize;
	ContextThemeWrapper boxThemeContextWrapper = new ContextThemeWrapper(this, R.style.BoxTheme);
	public int count;
	public String extrType;
	public String aquality;
	public boolean audioExtrEnabled = false;
	public String ganttFunction = null;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getBaseContext();
        
    	// Theme init
    	Utils.themeInit(this);
    	
        setContentView(R.layout.activity_share);
        
    	showSizesInVideoList = YTD.settings.getBoolean("show_size_list", false);

    	// Language init
    	Utils.langInit(this);
        
        // loading views from the layout xml
        tv = (TextView) findViewById(R.id.textView1);
        
        progressBarD = (ProgressBar) findViewById(R.id.progressBarD);
        progressBarL = (ProgressBar) findViewById(R.id.progressBarL);
        
        String theme = YTD.settings.getString("choose_theme", "D");
    	if (theme.equals("D")) {
    		progressBar1 = progressBarD;
    		progressBarL.setVisibility(View.GONE);
    	} else {
    		progressBar1 = progressBarL;
    		progressBarD.setVisibility(View.GONE);
    	}

        imgView = (ImageView)findViewById(R.id.imgview);
        
        lv = (ListView) findViewById(R.id.list);

        // YTD update initialization
        updateInit();
        
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                try {
                	handleSendText(intent, action);
                	Utils.logger("d", "handling ACTION_SEND", DEBUG_TAG);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(DEBUG_TAG, "Error: " + e.getMessage(), e);
                }
            }
        }
        
        if (Intent.ACTION_VIEW.equals(action)) {
            try {
            	handleSendText(intent, action);
            	Utils.logger("d", "handling ACTION_VIEW", DEBUG_TAG);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(DEBUG_TAG, "Error: " + e.getMessage(), e);
            }
        }
    }

    public static Context getContext() {
        return mContext;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_share, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId()){
        	case R.id.menu_donate:
    			startActivity(new Intent(this, DonateActivity.class));
    			return true;
        	case R.id.menu_settings:
        		startActivity(new Intent(this, SettingsActivity.class));
        		return true;
        	case R.id.menu_about:
        		startActivity(new Intent(this, AboutActivity.class));
        		return true;
        	case R.id.menu_dashboard:
        		startActivity(new Intent(this, DashboardActivity.class));
        		return true;
        	case R.id.menu_tutorials:
        		startActivity(new Intent(this, TutorialsActivity.class));
        		return true;
        	default:
        		return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(inAppCompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        Utils.logger("v", "_onStart", DEBUG_TAG);
    }
    
    /*@Override
    protected void onRestart() {
    	super.onRestart();
    	Utils.logger("v", "_onRestart");
    }

    @Override
    public void onPause() {
    	super.onPause();
    	Utils.logger("v", "_onPause");
    }*/
    
    @Override
    protected void onStop() {
        super.onStop();
    	unregisterReceiver(inAppCompleteReceiver);
    	Utils.logger("v", "_onStop", DEBUG_TAG);
    }
    
    @Override
	public void onBackPressed() {
    	super.onBackPressed();
    	// To cancel the asyncDownload task only on back button pressed (not when switching to other activities)
    	if (isAsyncDownloadRunning) {
    		asyncDownload.cancel(true);
    	}
		Utils.logger("v", "_onBackPressed", DEBUG_TAG);
	}

    void handleSendText(Intent intent, String action) throws IOException {

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
        	
        	String sharedText = null;
			if (action.equals(Intent.ACTION_SEND)) {
            	sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        	} else if (action.equals(Intent.ACTION_VIEW)) {
        		sharedText = intent.getDataString();
        	}
            
			if (sharedText != null) {
	            if (linkValidator(sharedText) == "not_a_valid_youtube_link") {
	            	badOrNullLinkAlert();
	            } else if (sharedText != null) {
	            	showGeneralInfoTutorial();
	            	asyncDownload = new AsyncDownload();
	            	asyncDownload.execute(validatedLink);
	            }
			} else {
				badOrNullLinkAlert();
			}
        } else {
        	progressBar1.setVisibility(View.GONE);
        	tv.setText(getString(R.string.no_net));
        	PopUps.showPopUp(getString(R.string.no_net), getString(R.string.no_net_dialog_msg), "alert", this);
        	Button retry = (Button) findViewById(R.id.share_activity_retry_button);
        	retry.setVisibility(View.VISIBLE);
        	retry.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Utils.reload(ShareActivity.this);					
				}
			});
        }
    }

	public void badOrNullLinkAlert() {
		progressBar1.setVisibility(View.GONE);
		tv.setText(getString(R.string.bad_link));
		PopUps.showPopUp(getString(R.string.error), getString(R.string.bad_link_dialog_msg), "alert", this);
	}
    
    void showGeneralInfoTutorial() {
        generalInfoCheckboxEnabled = YTD.settings.getBoolean("general_info", true);
        if (generalInfoCheckboxEnabled == true) {
        	AlertDialog.Builder adb = new AlertDialog.Builder(boxThemeContextWrapper);
    	    LayoutInflater adbInflater = LayoutInflater.from(ShareActivity.this);
    	    View generalInfo = adbInflater.inflate(R.layout.dialog_general_info, null);
    	    generalInfoScrollview = (ScrollView) generalInfo.findViewById(R.id.generalInfoScrollview);

    	    showAgain1 = (CheckBox) generalInfo.findViewById(R.id.showAgain1);
    	    showAgain1.setChecked(true);
    	    adb.setView(generalInfo);
    	    adb.setTitle(getString(R.string.tutorial_title));    	    
    	    adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
    	    	public void onClick(DialogInterface dialog, int which) {
    	    		if (!showAgain1.isChecked()) {
    	    			YTD.settings.edit().putBoolean("general_info", false).commit();
    	    			sshInfoCheckboxEnabled = YTD.settings.getBoolean("general_info", true);
    	    			Utils.logger("d", "generalInfoCheckboxEnabled: " + generalInfoCheckboxEnabled, DEBUG_TAG);
    	    		}
        		}
        	});
    	    if (! ((Activity) this).isFinishing()) {
    	    	adb.show();
    	    }
        }
    }
    
    private String linkValidator(String sharedText) {
    	Pattern pattern = Pattern.compile("(http://|https://).*(v=.{11}).*");
        Matcher matcher = pattern.matcher(sharedText);
        if (matcher.find()) {
            validatedLink = matcher.group(1) + "www.youtube.com/watch?" + matcher.group(2);
            videoId = matcher.group(2).replace("v=", "");
            return validatedLink;
        }
        return "not_a_valid_youtube_link";
    }
    
    public static void assignPath() {
    	boolean Location = YTD.settings.getBoolean("swap_location", false);
        
        if (Location == false) {
            String location = YTD.settings.getString("standard_location", "Downloads");
            Utils.logger("d", "location: " + location, DEBUG_TAG);
            
            if (location.equals("DCIM") == true) {
            	path = dir_DCIM;
            }
            if (location.equals("Movies") == true) {
            	path = dir_Movies;
            } 
            if (location.equals("Downloads") == true) {
            	path = dir_Downloads;
            }
            
        } else {
        	String cs = YTD.settings.getString("CHOOSER_FOLDER", "");
        	chooserFolder = new File(cs);
        	if (chooserFolder.exists()) {
        		Utils.logger("d", "chooserFolder: " + chooserFolder, DEBUG_TAG);
        		path = chooserFolder;
        	} else {
        		path = dir_Downloads;
        		Utils.logger("w", "chooserFolder not found, falling back to Download path", DEBUG_TAG);
        	}
        }
        
        if (!path.exists()) {
        	if (new File(path.getAbsolutePath()).mkdirs()) {
        		Utils.logger("w", "destination path not found, creating it now", DEBUG_TAG);
        	} else {
        		Log.e(DEBUG_TAG, "Something really bad happened with the download destination...");
        	}
        	
        }
        	
        Utils.logger("d", "path: " + path, DEBUG_TAG);
    }

    private class AsyncDownload extends AsyncTask<String, Integer, String> {

		protected void onPreExecute() {
    		isAsyncDownloadRunning = true;
    		tv.setText(R.string.loading);
    		progressBar1.setIndeterminate(true);
    		progressBar1.setVisibility(View.VISIBLE);
    	}
    	
    	protected String doInBackground(String... urls) {
            try {
            	Utils.logger("d", "doInBackground...", DEBUG_TAG);
            	
            	if (YTD.settings.getBoolean("show_thumb", false)) {
            		downloadThumbnail(generateThumbUrl());
            	}
            	
            	return downloadUrl(urls[0]);
            } catch (IOException e) {
            	Log.e(DEBUG_TAG, "downloadUrl: " + e.getMessage());
		    	BugSenseHandler.sendExceptionMessage(DEBUG_TAG + "-> downloadUrl: ", e.getMessage(), e);
                return "e";
            } catch (RuntimeException re) {
            	Log.e(DEBUG_TAG, "downloadUrl: " + re.getMessage());
		    	BugSenseHandler.sendExceptionMessage(DEBUG_TAG + "-> downloadUrl: ", re.getMessage(), re);
		    	//Toast.makeText(ShareActivity.this, getString(R.string.error), Toast.LENGTH_LONG).show();
		    	return "e";
            }
        }
    	
    	public void doProgress(int value){
            publishProgress(value);
        }
    	
    	protected void onProgressUpdate(Integer... values) {
    		progressBar1.setProgress(values[0]);
    	}

        @Override
        protected void onPostExecute(String result) {

        	progressBar1.setVisibility(View.GONE);
        	
        	if (YTD.settings.getBoolean("show_thumb", false)) {
        		imgView.setImageBitmap(img);
        	}
        	isAsyncDownloadRunning = false;
        	
            if (result == "e") {
            	tv.setText(getString(R.string.invalid_url_short));
                PopUps.showPopUp(getString(R.string.error), getString(R.string.invalid_url), "alert", ShareActivity.this);
                titleRaw = getString(R.string.invalid_response);
            }

            String[] lv_arr = listEntries.toArray(new String[0]);
            
            if (lv_arr.length == 0) {
            	PopUps.showPopUp(getString(R.string.login_required), getString(R.string.login_required_dialog_msg), "info", ShareActivity.this);
            	TextView info = (TextView) findViewById(R.id.share_activity_info);
            	info.setVisibility(View.VISIBLE);
            }
            
            aA = new ArrayAdapter<String>(ShareActivity.this, android.R.layout.simple_list_item_1, lv_arr);
            
            lv.setAdapter(aA);
            
            Utils.logger("d", "LISTview done with " + lv_arr.length + " items.", DEBUG_TAG);

            tv.setText(titleRaw);
            
            lv.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					//Utils.logger("i", "Selected link: " + links.get(pos), DEBUG_TAG);
					
					assignPath();
					
                    pos = position;     
                    //pos = 45;		// to test IndexOutOfBound Exception...
                    
                	helpBuilder = new AlertDialog.Builder(boxThemeContextWrapper);
                    helpBuilder.setIcon(android.R.drawable.ic_dialog_info);
                    helpBuilder.setTitle(getString(R.string.list_click_dialog_title));
                    
                    if (showSizesInVideoList) {
                    	showSingleSize = true;
                    } else {
                    	showSingleSize = YTD.settings.getBoolean("show_size", false);
                    }
					
					try {
                        if (!showSingleSize) {
                        	helpBuilder.setMessage(titleRaw + 
                        			getString(R.string.codec) + " " + codecs.get(pos) + 
                					getString(R.string.quality) + " " + qualities.get(pos) + stereo.get(pos));
                        } else {
                        	if (!showSizesInVideoList) {
                        		sizeQuery = new AsyncSizeQuery();
                        		sizeQuery.execute(links.get(position));
                        	} else {
                        		helpBuilder.setMessage(titleRaw + 
                            			getString(R.string.codec) + " " + codecs.get(pos) + 
                    					getString(R.string.quality) + " " + qualities.get(pos) + stereo.get(pos) +
                    					getString(R.string.size) + " " + sizes.get(pos));
                        	}
                        }
					} catch (IndexOutOfBoundsException e) {
			    		Toast.makeText(ShareActivity.this, getString(R.string.video_list_error_toast), Toast.LENGTH_SHORT).show();
			    	}
					
                    helpBuilder.setPositiveButton(getString(R.string.list_click_download_local), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        	try {
                        		fileRenameEnabled = YTD.settings.getBoolean("enable_rename", false);
	                            if (fileRenameEnabled == true) {
									AlertDialog.Builder adb = new AlertDialog.Builder(boxThemeContextWrapper);
	                            	LayoutInflater adbInflater = LayoutInflater.from(ShareActivity.this);
		                    	    View inputFilename = adbInflater.inflate(R.layout.dialog_input_filename, null);
		                    	    userFilename = (TextView) inputFilename.findViewById(R.id.input_filename);
		                    	    userFilename.setText(basename);
		                    	    adb.setView(inputFilename);
		                    	    adb.setTitle(getString(R.string.rename_dialog_title));
		                    	    adb.setMessage(getString(R.string.rename_dialog_msg));
		                    	    
		                    	    adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		                    	    	public void onClick(DialogInterface dialog, int which) {
		                    	    		basename = userFilename.getText().toString();
		                    	    		vFilename = composeVideoFilename();
											callDownloadManager(links.get(pos));
		                    	    	}
		                    	    });
		                    	    
		                    	    adb.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {
		                	        	public void onClick(DialogInterface dialog, int which) {
		                	                // cancel
		                	            }
		                	        });
		                    	    
		                    	    if (! ((Activity) ShareActivity.this).isFinishing()) {
		                    	    	adb.show();
		                    	    }
	                            } else {
	                            	vFilename = composeVideoFilename();
	                            	callDownloadManager(links.get(pos));
	                            }
                        	} catch (IndexOutOfBoundsException e) {
    							Toast.makeText(ShareActivity.this, getString(R.string.video_list_error_toast), Toast.LENGTH_SHORT).show();
    						}
                        }
                    });
					
                    // show central button for SSH send if enabled in prefs
                    if (!YTD.settings.getBoolean("ssh_to_longpress_menu", false)) {
	                    helpBuilder.setNeutralButton(getString(R.string.list_click_download_ssh), new DialogInterface.OnClickListener() {
	
	                        public void onClick(DialogInterface dialog, int which) {
	                        	sendViaSsh();
	                        }
	                    });
                    }

                    helpBuilder.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            //Toast.makeText(ShareActivity.this, "Download canceled...", Toast.LENGTH_SHORT).show();
                        }
                    });
                    
                    if ((!showSingleSize) || (showSizesInVideoList && showSingleSize)) {
                    	helpDialog = helpBuilder.create();
                    	
                    	if (! ((Activity) ShareActivity.this).isFinishing()) {
                    		helpDialog.show();
                	    }
                    }
                }
            });
            
            lv.setLongClickable(true);
            lv.setOnItemLongClickListener(new OnItemLongClickListener() {

            	@Override
            	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
            		pos = position;
            		AlertDialog.Builder builder = new AlertDialog.Builder(boxThemeContextWrapper);
            		if (!YTD.settings.getBoolean("ssh_to_longpress_menu", false)) {
	            		builder.setTitle(R.string.long_click_title).setItems(R.array.long_click_entries, new DialogInterface.OnClickListener() {
					    	public void onClick(DialogInterface dialog, int which) {
					    		vFilename = composeVideoFilename();
					    		switch (which) {
					    			case 0: // copy
					    				copy(position);
					    				break;
					    			case 1: // share
					    				share(position);
					    		}
					    	}
	            		});
            		} else {
            			builder.setTitle(R.string.long_click_title).setItems(R.array.long_click_entries2, new DialogInterface.OnClickListener() {
					    	public void onClick(DialogInterface dialog, int which) {
					    		vFilename = composeVideoFilename();
					    		switch (which) {
					    			case 0: // copy
					    				copy(position);
					    				break;
					    			case 1: // share
					    				share(position);
					    				break;
					    			case 2: // SSH
					    				sendViaSsh();
					    		}
					    	}
	            		});
            		}
            		builder.create();
            		if (! ((Activity) ShareActivity.this).isFinishing()) {
            			builder.show();
            		}
				    return true;
            	}
            });
        }
        
        private void share(final int position) {
			Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
			sharingIntent.setType("text/plain");
			sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, vFilename);
			sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, links.get(position));
			startActivity(Intent.createChooser(sharingIntent, "Share YouTube link:"));
		}

		private void copy(final int position) {
			ClipData cmd = ClipData.newPlainText("simple text", links.get(position));
			ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			cb.setPrimaryClip(cmd);
		}
        
		private boolean useQualitySuffix() {
        	boolean enabled = YTD.settings.getBoolean("enable_q_suffix", true);
        	return enabled;
        }
        
		private String composeVideoFilename() {
        	if (useQualitySuffix()) {
        		videoFilename = basename + "_" + qualities.get(pos) + stereo.get(pos) + "." + codecs.get(pos);
        	} else {
    	    	videoFilename = basename + stereo.get(pos) + "." + codecs.get(pos);
        	}
    	    Utils.logger("d", "videoFilename: " + videoFilename, DEBUG_TAG);
    	    return videoFilename;
        }

		private void callConnectBot() {
        	Context context = getApplicationContext();
    		PackageManager pm = context.getPackageManager();
    		
    		final String connectBotFlavour = YTD.settings.getString("connectbot_flavour", "org.connectbot");
    		
    		String connectBotFlavourPlain = "ConnectBot";
    		if (connectBotFlavour.equals("sk.vx.connectbot")) connectBotFlavourPlain = "VX " + connectBotFlavourPlain;
    		if (connectBotFlavour.equals("org.woltage.irssiconnectbot")) connectBotFlavourPlain = "Irssi " + connectBotFlavourPlain;
    		
			Intent appStartIntent = pm.getLaunchIntentForPackage(connectBotFlavour);
    		if (null != appStartIntent) {
    			Utils.logger("d", "appStartIntent: " + appStartIntent, DEBUG_TAG);
    			context.startActivity(appStartIntent);
    		} else {
    			AlertDialog.Builder cb = new AlertDialog.Builder(boxThemeContextWrapper);
    	        cb.setTitle(getString(R.string.callConnectBot_dialog_title, connectBotFlavourPlain));
    	        cb.setMessage(getString(R.string.callConnectBot_dialog_msg));
    	        icon = android.R.drawable.ic_dialog_alert;
    	        cb.setIcon(icon);
    	        cb.setPositiveButton(getString(R.string.callConnectBot_dialog_positive), new DialogInterface.OnClickListener() {
    	            public void onClick(DialogInterface dialog, int which) {
    	            	Intent intent = new Intent(Intent.ACTION_VIEW); 
    	            	intent.setData(Uri.parse("market://details?id=" + connectBotFlavour));
    	            	try {
    	            		startActivity(intent);
    	            	} catch (ActivityNotFoundException exception){
    	            		PopUps.showPopUp(getString(R.string.no_market), getString(R.string.no_net_dialog_msg), "alert", ShareActivity.this);
    	            	}
    	            }
    	        });
    	        cb.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {
    	        	public void onClick(DialogInterface dialog, int which) {
    	                // cancel
    	            }
    	        });

    	        AlertDialog helpDialog = cb.create();
    	        
    	        if (! ((Activity) ShareActivity.this).isFinishing()) {
    	        	helpDialog.show();
        		}
    		}
        }

		private void sendViaSsh() {
			try {
				String wgetCmd;
				vFilename = composeVideoFilename();
				
				Boolean shortSshCmdEnabled = YTD.settings.getBoolean("enable_connectbot_short_cmd", false);
				if (shortSshCmdEnabled) {
					wgetCmd = "wget -e \"convert-links=off\" --keep-session-cookies --save-cookies /dev/null --no-check-certificate \'" + 
							links.get(pos) + "\' -O " + vFilename;
				} else {
					wgetCmd = "REQ=`wget -q -e \"convert-links=off\" --keep-session-cookies --save-cookies /dev/null --no-check-certificate \'" + 
							validatedLink + "\' -O-` && urlblock=`echo $REQ | grep -oE \'url_encoded_fmt_stream_map\": \".*\' | sed -e \'s/\", \".*//\'" + 
							" -e \'s/url_encoded_fmt_stream_map\": \"//\'` && urlarray=( `echo $urlblock | sed \'s/,/\\n\\n/g\'` ) && N=" + pos + 
							" && block=`echo \"${urlarray[$N]}\" | sed -e \'s/%3A/:/g\' -e \'s/%2F/\\//g\' -e \'s/%3F/\\?/g\' -e \'s/%3D/\\=/g\'" + 
							" -e \'s/%252C/%2C/g\' -e \'s/%26/\\&/g\' -e \'s/%253A/\\:/g\' -e \'s/\", \"/\"-\"/\' -e \'s/sig=/signature=/\'" + 
							" -e \'s/x-flv/flv/\' -e \'s/\\\\\\u0026/\\&/g\'` && url=`echo $block | grep -oE \'http://.*\' | sed -e \'s/&type=.*//\'" + 
							" -e \'s/&signature=.*//\' -e \'s/&quality=.*//\' -e \'s/&fallback_host=.*//\'` && sig=`echo $block | " +
							"grep -oE \'signature=.{81}\'` && downloadurl=`echo $url\\&$sig | sed \'s/&itag=[0-9][0-9]&signature/\\&signature/\'` && " +
							"wget -e \"convert-links=off\" --keep-session-cookies --save-cookies /dev/null --tries=5 --timeout=45 --no-check-certificate " +
							"\"$downloadurl\" -O " + vFilename;
				}
				
				Utils.logger("d", "wgetCmd: " + wgetCmd, DEBUG_TAG);
			    
				ClipData cmd = ClipData.newPlainText("simple text", wgetCmd);
			    ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			    cb.setPrimaryClip(cmd);
			    
			    sshInfoCheckboxEnabled = YTD.settings.getBoolean("ssh_info", true);
			    if (sshInfoCheckboxEnabled == true) {
			        AlertDialog.Builder adb = new AlertDialog.Builder(boxThemeContextWrapper);
				    LayoutInflater adbInflater = LayoutInflater.from(ShareActivity.this);
				    View sshInfo = adbInflater.inflate(R.layout.dialog_ssh_info, null);
				    showAgain2 = (CheckBox) sshInfo.findViewById(R.id.showAgain2);
				    showAgain2.setChecked(true);
				    adb.setView(sshInfo);
				    adb.setTitle(getString(R.string.ssh_info_tutorial_title));
				    adb.setMessage(getString(R.string.ssh_info_tutorial_msg));
				    adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				    	public void onClick(DialogInterface dialog, int which) {
				    		if (!showAgain2.isChecked()) {
				    			YTD.settings.edit().putBoolean("ssh_info", false).apply();
				    			sshInfoCheckboxEnabled = YTD.settings.getBoolean("ssh_info", true);
				    			Utils.logger("d", "sshInfoCheckboxEnabled: " + sshInfoCheckboxEnabled, DEBUG_TAG);
				    		}
				    		callConnectBot(); 
			    		}
			    	});
				    if (! ((Activity) ShareActivity.this).isFinishing()) {
	    	        	adb.show();
	        		}
			    } else {
			    	callConnectBot();
			    }
			} catch (IndexOutOfBoundsException e) {
				Toast.makeText(ShareActivity.this, getString(R.string.video_list_error_toast), Toast.LENGTH_SHORT).show();
			}
		}
	}
    
    private void callDownloadManager(String link) {
		videoUri = Uri.parse(path.toURI() + vFilename);
        Utils.logger("d", "videoUri: " + videoUri, DEBUG_TAG);
        
        dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        Request request = null;
		try {
			request = new Request(Uri.parse(link));
			request.setDestinationUri(videoUri);
			request.allowScanningByMediaScanner();
			request.setVisibleInDownloadsUi(false);
			
			String visValue = YTD.settings.getString("download_manager_notification", "VISIBLE");
			int vis;
			if (visValue.equals("VISIBLE_NOTIFY_COMPLETED")) {
				vis = DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED;
			} else if (visValue.equals("HIDDEN")) {
				vis = DownloadManager.Request.VISIBILITY_HIDDEN;
			} else {
				vis = DownloadManager.Request.VISIBILITY_VISIBLE;
			}
			request.setNotificationVisibility(vis);
			request.setTitle(videoFilename);
			request.setDescription(getString(R.string.ytd_video));
		} catch (IllegalArgumentException e) {
	    	Log.e(DEBUG_TAG, "callDownloadManager: " + e.getMessage());
	    	YTD.NoDownProvPopUp(this);
	    	BugSenseHandler.sendExceptionMessage(DEBUG_TAG + "-> callDownloadManager: ", e.getMessage(), e);
	    }
    	
    	Intent intent1 = new Intent(ShareActivity.this, DownloadsService.class);
    	intent1.putExtra("COPY", false);

		try {
			try {
				enqueue = dm.enqueue(request);
			} catch (IllegalArgumentException e) {
		    	Log.e(DEBUG_TAG, "callDownloadManager: " + e.getMessage());
		    	YTD.NoDownProvPopUp(this);
		    	BugSenseHandler.sendExceptionMessage(DEBUG_TAG + "-> callDownloadManager: ", e.getMessage(), e);
		    } catch (NullPointerException ne) {
		    	Log.e(DEBUG_TAG, "callDownloadApk: " + ne.getMessage());
		    	BugSenseHandler.sendExceptionMessage(DEBUG_TAG + "-> callDownloadApk: ", ne.getMessage(), ne);
		    	Toast.makeText(this, getString(R.string.error), Toast.LENGTH_LONG).show();
		    }
        	Utils.logger("d", "_ID " + enqueue + " enqueued", DEBUG_TAG);
        } catch (SecurityException e) {
        	// handle path on etxSdCard:
        	Utils.logger("w", e.getMessage(), DEBUG_TAG);
        	showExtsdcardInfo();
        	intent1.putExtra("COPY", true);
        	videoOnExt = true;
        	tempDownloadToSdcard(request);
        }
		
		startService(intent1);
		
		String aExt = findAudioCodec();
		
		Utils.addEntryToJsonFile(
				mContext, 
				String.valueOf(enqueue), 
				YTD.JSON_DATA_TYPE_V, 
				YTD.JSON_DATA_STATUS_I, 
				path.getAbsolutePath(), 
				vFilename, 
				basename, 
				aExt, 
				"-", 
				false);
    	
		sequence.add(enqueue);
		YTD.videoinfo.edit().putLong(vFilename, enqueue).apply();
		
		if (videoOnExt == true) {
			videoFileObserver = new Observer.YtdFileObserver(dir_Downloads.getAbsolutePath());
		} else {
			videoFileObserver = new Observer.YtdFileObserver(path.getAbsolutePath());
		}
		videoFileObserver.startWatching();
    }
    
    private String findAudioCodec() {
    	String aExt = null;
    	
		if (codecs.get(pos).equals("webm")) aExt = ".ogg";
	    if (codecs.get(pos).equals("mp4")) aExt = ".aac";
	    if (codecs.get(pos).equals("flv") && qualities.get(pos).equals("small")) aExt = ".mp3";
	    if (codecs.get(pos).equals("flv") && qualities.get(pos).equals("medium")) aExt = ".aac";
	    if (codecs.get(pos).equals("flv") && qualities.get(pos).equals("large")) aExt = ".aac";
	    if (codecs.get(pos).equals("3gpp")) aExt = ".aac";

    	return aExt;
    }
    
    private String generateThumbUrl() {
		// link example "http://i2.ytimg.com/vi/8wr-uQX1Grw/mqdefault.jpg"
    	Random random = new Random();
    	int num = random.nextInt(4 - 1) + 1;
    	String url = "http://i" + num + ".ytimg.com/vi/" + videoId + "/mqdefault.jpg";
    	Utils.logger("i", "thumbnail url: " + url, DEBUG_TAG);
    	return url;
	}

    private void showExtsdcardInfo() {
        generalInfoCheckboxEnabled = YTD.settings.getBoolean("extsdcard_info", true);
        if (generalInfoCheckboxEnabled == true) {
        	AlertDialog.Builder adb = new AlertDialog.Builder(boxThemeContextWrapper);
    	    LayoutInflater adbInflater = LayoutInflater.from(ShareActivity.this);
    	    View generalInfo = adbInflater.inflate(R.layout.dialog_extsdcard_info, null);
    	    showAgain3 = (CheckBox) generalInfo.findViewById(R.id.showAgain3);
    	    showAgain3.setChecked(true);
    	    adb.setView(generalInfo);
    	    adb.setTitle(getString(R.string.extsdcard_info_title));    	    
    	    adb.setMessage(getString(R.string.extsdcard_info_msg));

    	    adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
    	    	public void onClick(DialogInterface dialog, int which) {
    	    		if (showAgain3.isChecked() == false) {
    	    			YTD.settings.edit().putBoolean("extsdcard_info", false).commit();
    	    			sshInfoCheckboxEnabled = YTD.settings.getBoolean("extsdcard_info", true);
    	    			Utils.logger("d", "generalInfoCheckboxEnabled: " + generalInfoCheckboxEnabled, DEBUG_TAG);
    	    		}
        		}
        	});
    	    if (! ((Activity) ShareActivity.this).isFinishing()) {
	        	adb.show();
    		}
        }
    }
      
    private void tempDownloadToSdcard(Request request) {
    	videoUri = Uri.parse(dir_Downloads.toURI() + vFilename);
        Utils.logger("d", "** NEW ** videoUri: " + videoUri, DEBUG_TAG);
        request.setDestinationUri(videoUri);
        try {
        	enqueue = dm.enqueue(request);
        } catch (IllegalArgumentException e) {
	    	Log.e(DEBUG_TAG, "tempDownloadToSdcard: " + e.getMessage());
	    	BugSenseHandler.sendExceptionMessage(DEBUG_TAG + "-> tempDownloadToSdcard", e.getMessage(), e);
	    } catch (NullPointerException ne) {
	    	Log.e(DEBUG_TAG, "callDownloadApk: " + ne.getMessage());
	    	BugSenseHandler.sendExceptionMessage(DEBUG_TAG + "-> tempDownloadToSdcard: ", ne.getMessage(), ne);
	    	Toast.makeText(this, getString(R.string.error), Toast.LENGTH_LONG).show();
	    }catch (SecurityException se) {
	    	Log.e(DEBUG_TAG, "callDownloadApk: " + se.getMessage());
	    	BugSenseHandler.sendExceptionMessage(DEBUG_TAG + "-> tempDownloadToSdcard: ", se.getMessage(), se);
	    	Toast.makeText(this, getString(R.string.error), Toast.LENGTH_LONG).show();
	    }
    }

    public static void NotificationHelper() {
    	pt1 = mContext.getString(R.string.notification_downloading_pt1);
    	pt2 = mContext.getString(R.string.notification_downloading_pt2);
    	noDownloads = mContext.getString(R.string.notification_no_downloads);
    	
    	mBuilder =  new NotificationCompat.Builder(mContext);
    	
    	mBuilder.setSmallIcon(R.drawable.ic_stat_ytd)
    	        .setContentTitle(mContext.getString(R.string.title_activity_share))
    	        .setContentText(mContext.getString(R.string.notification_downloading_pt1) + " " + sequence.size() + " " + mContext.getString(R.string.notification_downloading_pt2));
    	
    	mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    	
    	Intent notificationIntent = new Intent(mContext, DashboardActivity.class);
    	PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
    	mBuilder.setContentIntent(contentIntent);
    	mNotificationManager.notify(1, mBuilder.build());
	}

	// Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as a string.
    private String downloadUrl(String myurl) throws IOException, RuntimeException {
        InputStream is = null;
        // Only display the first "len" characters of the retrieved web page content.
        int len = 2000000;
        Utils.logger("d", "The link is: " + myurl, DEBUG_TAG);
        if (!asyncDownload.isCancelled()) {
	        try {
	            URL url = new URL(myurl);
	            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	            conn.setRequestProperty("User-Agent","<em>" + USER_AGENT_FIREFOX + "</em>");
	            conn.setReadTimeout(20000 /* milliseconds */);
	            conn.setConnectTimeout(30000 /* milliseconds */);
	            conn.setInstanceFollowRedirects(false);
	            conn.setRequestMethod("GET");
	            conn.setDoInput(true);
	            //Starts the query
	            conn.connect();
	            int response = conn.getResponseCode();
	            Utils.logger("d", "The response is: " + response, DEBUG_TAG);
	            is = conn.getInputStream();
	
	            //Convert the InputStream into a string
	            if (!asyncDownload.isCancelled()) {
	            	return readIt(is, len);
	            } else {
	            	Utils.logger("d", "asyncDownload cancelled @ 'return readIt'", DEBUG_TAG);
	            	return null;
	            }
	            
	        //Makes sure that the InputStream is closed after the app is finished using it.
	        } finally {
	            if (is != null) {
	                is.close();
	            }
	        }
        } else {
        	Utils.logger("d", "asyncDownload cancelled @ 'downloadUrl' begin", DEBUG_TAG);
        	return null;
        }
    }
    
    // Reads an InputStream and converts it to a String.
    private String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        String content = new String(buffer);
       	return urlBlockMatchAndDecode(content);
    }

    private String urlBlockMatchAndDecode(String content) {
		
		if (asyncDownload.isCancelled()) {
			Utils.logger("d", "asyncDownload cancelled @ urlBlockMatchAndDecode begin", DEBUG_TAG);
			return "Cancelled!";
		}
		
        findVideoFilenameBase(content);

        Pattern pattern = Pattern.compile("url_encoded_fmt_stream_map\\\": \\\"(.*?)\\\"");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
        	Pattern blockPattern = Pattern.compile(",");
            Matcher blockMatcher = blockPattern.matcher(matcher.group(1));
            if (blockMatcher.find() && !asyncDownload.isCancelled()) {
            	String[] CQS = matcher.group(1).split(blockPattern.toString());
            	count = (CQS.length-1);
                Utils.logger("d", "number of entries found: " + count, DEBUG_TAG);
                int index = 0;
                progressBar1.setIndeterminate(false);
                ganttFunction = null;
                while ((index+1) < CQS.length) {
                	try {
						CQS[index] = URLDecoder.decode(CQS[index], "UTF-8");
					} catch (UnsupportedEncodingException e) {
						Log.e(DEBUG_TAG, e.getMessage());
					}
                	
                	asyncDownload.doProgress((int) ((index / (float) count) * 100));

                    codecMatcher(CQS[index], index);
                    qualityMatcher(CQS[index], index);
                    stereoMatcher(CQS[index], index);
                    resolutionMatcher(CQS[index], index);
                    linkComposer(CQS[index], index);
                    Utils.logger("v", "block " + index + ": " + CQS[index], DEBUG_TAG);
                    index++;
                }
                listEntriesBuilder();
            } else {
            	Utils.logger("d", "asyncDownload cancelled @ 'findCodecAndQualityAndLinks' match", DEBUG_TAG);
            } 
            return "Match!";
        } else {
            return "No Match";
        }
    }

	private void findVideoFilenameBase(String content) {
        Pattern titlePattern = Pattern.compile("<title>(.*?)</title>");
        Matcher titleMatcher = titlePattern.matcher(content);
        if (titleMatcher.find()) {
            titleRaw = titleMatcher.group().replaceAll("(<| - YouTube</)title>", "");
            titleRaw = android.text.Html.fromHtml(titleRaw).toString();
            basename = titleRaw.replaceAll("\\W", "_");
        } else {
            basename = "Youtube Video";
        }
        Utils.logger("d", "findVideoFilenameBase: " + basename, DEBUG_TAG);
    }

    private void listEntriesBuilder() {
    	Iterator<String> codecsIter = codecs.iterator();
        Iterator<String> qualitiesIter = qualities.iterator();
        Iterator<String> stereoIter = stereo.iterator();
        Iterator<String> sizesIter = sizes.iterator();
        Iterator<String> itagsIter = itags.iterator();
        
        boolean showSize = YTD.settings.getBoolean("show_size_list", false);
        boolean showRes = YTD.settings.getBoolean("show_resolutions", false);
    	
        while (codecsIter.hasNext()) {
        	String size;
        	
			if (showSize) {
        		size = " - " + sizesIter.next();
        	} else {
        		size = "";
        	}
        	
        	String res;
			if (showRes) {
        		res = itagsIter.next();
        	} else {
        		res = qualitiesIter.next();
        	}
        	
        	try {
				listEntries.add(codecsIter.next().toUpperCase(Locale.ENGLISH).replace("WEBM", "WebM") + 
						" - " + res + stereoIter.next() + size);
        	} catch (NoSuchElementException e) {
        		listEntries.add("//");
        	}
        }
    }
    
    private void linkComposer(String block, int i) {
    	Pattern urlPattern = Pattern.compile("url=(.+?)\\\\u0026");
    	Matcher urlMatcher = urlPattern.matcher(block);
    	String url = null;
		if (urlMatcher.find()) {
    		url = urlMatcher.group(1);
    	} else {
    		Pattern urlPattern2 = Pattern.compile("url=(.+?)$");
    		Matcher urlMatcher2 = urlPattern2.matcher(block);
    		if (urlMatcher2.find()) {
        		url = urlMatcher2.group(1);
        	} else {
        		Log.e(DEBUG_TAG, "url: " + url);
        	}
    	}
    		
		Pattern sigPattern = Pattern.compile("sig=(.+?)\\\\u0026");
    	Matcher sigMatcher = sigPattern.matcher(block);
    	String sig = null;
		if (sigMatcher.find()) {
    		sig = "signature=" + sigMatcher.group(1);
    		Utils.logger("d", "sig found on step 1 (\u0026)", DEBUG_TAG);
    	} else {
    		Pattern sigPattern2 = Pattern.compile("sig=(.+?)$");
    		Matcher sigMatcher2 = sigPattern2.matcher(block);
    		if (sigMatcher2.find()) {
    			sig = "signature=" + sigMatcher2.group(1);
    			Utils.logger("d", "sig found on step 2 ($)", DEBUG_TAG);
        	} else {
        		Pattern sigPattern3 = Pattern.compile("sig=([[0-9][A-Z]]{39,40}\\.[[0-9][A-Z]]{39,40})");
        		Matcher sigMatcher3 = sigPattern3.matcher(block);
        		if (sigMatcher3.find()) {
        			sig = "signature=" + sigMatcher3.group(1);
        			Utils.logger("d", "sig found on step 3 ([[0-9][A-Z]]{39,40})", DEBUG_TAG);
        		} else {
        			Pattern sigPattern4 = Pattern.compile("s=([[0-9][A-Z][\\.]]{82,92})");
        			Matcher sigMatcher4 = sigPattern4.matcher(block);
        			if (sigMatcher4.find()) {
        				Utils.logger("d", "sig found on step 4 (s=); length is " + sigMatcher4.group(1).length(), DEBUG_TAG);

        				if (ganttFunction == null) {
        					Utils.logger("i", "gantt's function: Fetching it online...", DEBUG_TAG);
        					
        					FetchGanttFunction ff = new FetchGanttFunction();
        					ganttFunction = ff.doFetch(false, "http://userscripts.org/scripts/review/25105");
        					
        					if (ganttFunction == null) {
        						Utils.logger("w", "gantt's function: switching from 'userscripts.org' to 'sourceforge.net'", DEBUG_TAG);
        						ganttFunction = ff.doFetch(true, "http://sourceforge.net/projects/ytdownloader/files/utils/function_decryptSignature/download");
        					}
        				}

        				String decipheredSig = RhinoRunner.decipher(sigMatcher4.group(1), ganttFunction);
        				
        				sig = "signature=" + decipheredSig;
        			} else {
        				
        				Log.e(DEBUG_TAG, "sig: " + sig);
        			}
        		}
        	}
    	}

		Utils.logger("d", "url " + i + ": " + url, DEBUG_TAG);
		Utils.logger("d", "sig " + i + ": " + sig, DEBUG_TAG);
    	
		String composedLink = url + "&" + sig;

		links.add(composedLink);
		//Utils.logger("i", composedLink);
		if (YTD.settings.getBoolean("show_size_list", false) && !asyncDownload.isCancelled()) {
			String size = getVideoFileSize(composedLink);
			sizes.add(size);
        	Utils.logger("d", "size " + i + ": " + size, DEBUG_TAG);
		}
	}

	private class AsyncSizeQuery extends AsyncTask<String, Void, String> {
    	
    	protected void onPreExecute() {
    		waitBuilder = new AlertDialog.Builder(boxThemeContextWrapper);
    		LayoutInflater adbInflater = LayoutInflater.from(ShareActivity.this);
    	    View barView = adbInflater.inflate(R.layout.wait_for_filesize, null);
    	    filesizeProgressBar = (ProgressBar) barView.findViewById(R.id.filesizeProgressBar);
    	    filesizeProgressBar.setVisibility(View.VISIBLE);
    	    waitBuilder.setView(barView);
    	    waitBuilder.setIcon(android.R.drawable.ic_dialog_info);
    	    waitBuilder.setTitle(R.string.wait);
    	    waitBuilder.setMessage(titleRaw + 
    	    		getString(R.string.codec) + " " + codecs.get(pos) + 
					getString(R.string.quality) + " " + qualities.get(pos) + stereo.get(pos));
    	    waitBox = waitBuilder.create();
    	    if (! ((Activity) ShareActivity.this).isFinishing()) {
	        	waitBox.show();
    		}
    	}

		protected String doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            return getVideoFileSize(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
        	
        	filesizeProgressBar.setVisibility(View.GONE);
        	if (! ((Activity) ShareActivity.this).isFinishing()) {
        		waitBox.dismiss();
        	}
        	
        	videoFileSize = result;
        	Utils.logger("d", "size " + pos + ": " + result, DEBUG_TAG);
        	helpBuilder.setMessage(titleRaw + 
        			getString(R.string.codec) + " " + codecs.get(pos) + 
					getString(R.string.quality) + " " + qualities.get(pos) + stereo.get(pos) +
					getString(R.string.size) + " " + videoFileSize);
        	helpDialog = helpBuilder.create();
            if (! ((Activity) ShareActivity.this).isFinishing()) {
	        	helpDialog.show();
    		}
        }
	}
    
    private String getVideoFileSize(String link) {
    	String size;
		try {
			final URL uri = new URL(link);
			HttpURLConnection ucon = (HttpURLConnection) uri.openConnection();
			ucon.connect();
			int file_size = ucon.getContentLength();
			size = Utils.MakeSizeHumanReadable(file_size, true);
		} catch(IOException e) {
			size = "n.a.";
		}
		return size;
	}

    private void codecMatcher(String currentCQ, int i) {
        Pattern codecPattern = Pattern.compile("(webm|mp4|flv|3gpp)");
        Matcher codecMatcher = codecPattern.matcher(currentCQ);
        if (codecMatcher.find()) {
            codecs.add(codecMatcher.group());
        } else {
            codecs.add("NoMatch");
        }
        //Utils.logger("d", "CQ index: " + i + ", Codec: " + codecs.get(i), DEBUG_TAG);
    }

    private void qualityMatcher(String currentCQ, int i) {
        Pattern qualityPattern = Pattern.compile("(highres|hd1080|hd720|large|medium|small)");
        Matcher qualityMatcher = qualityPattern.matcher(currentCQ);
        if (qualityMatcher.find()) {
            qualities.add(qualityMatcher.group().replace("highres", "4K"));
        } else {
            qualities.add("NoMatch");
        }
        //Utils.logger("d", "CQ index: " + i + ", Quality: " + qualities.get(i), DEBUG_TAG);
    }
    
    private void stereoMatcher(String currentCQ, int i) {
        Pattern qualityPattern = Pattern.compile("stereo3d=1");
        Matcher qualityMatcher = qualityPattern.matcher(currentCQ);
        if (qualityMatcher.find()) {
            stereo.add(qualityMatcher.group().replace("stereo3d=1", "_3D"));
        } else {
            stereo.add("");
        }
        //Utils.logger("d", "CQ index: " + i + ", Quality: " + qualities.get(i), DEBUG_TAG);
    }
    
    private void resolutionMatcher(String currentCQ, int i) {
    	Pattern itagPattern = Pattern.compile("itag=([0-9]{1,3})&");
    	Matcher itagMatcher = itagPattern.matcher(currentCQ);
    	if (itagMatcher.find()) {
    		String itag = itagMatcher.group(1);
    		String res = "";
    		switch (Integer.parseInt(itag)) {
    		case 5:
    			res = "240p";
    			break;
    		case 6:
    			res = "270p";
    			break;
    		case 17:
    			res = "144p";
				break;
    		case 18:
    			res = "270p/360p";
				break;
    		case 22:
				res = "720p";
				break;
    		case 34:
				res = "360p";
				break;
    		case 35:
				res = "480p";
				break;
    		case 36:
				res = "240p";
				break;
    		case 37:
				res = "1080p";
				break;
    		case 38:
				res = "3072p";
				break;
    		case 43:
				res = "360p";
				break;
    		case 44:
				res = "480p";
				break;
    		case 45:
				res = "720p";
				break;
    		case 46:
				res = "1080p";
				break;
    		case 82:
				res = "360p";
				break;
    		case 83:
				res = "240p";
				break;
    		case 84:
				res = "720p";
				break;
    		case 85:
				res = "520p";
				break;
    		case 100:
				res = "360p";
				break;
    		case 101:
				res = "360p";
				break;
    		case 102:
				res = "720p";
				break;
    		}
    		
			itags.add(res);
    	} else {
    		itags.add("");
        }
        Utils.logger("d", "CQ index: " + i + ", itag: " + itags.get(i), DEBUG_TAG);
    }
    
    private void downloadThumbnail(String fileUrl) {
    	InputStream is = null;
    	URL myFileUrl = null;
    	try {
    		myFileUrl = new URL(fileUrl);
    	} catch (MalformedURLException e) {
    		try {
				myFileUrl =  new URL("https://raw.github.com/dentex/ytdownloader/master/dentex.youtube.downloader/assets/placeholder.png");
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}
    		e.printStackTrace();
    	}
    	try {
    		HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();
    		conn.setDoInput(true);
    		conn.connect();
    		is = conn.getInputStream();

    		img = BitmapFactory.decodeStream(is);
    	} catch (IOException e) {
    		InputStream assIs = null;
    		AssetManager assMan = getAssets();
            try {
				assIs = assMan.open("placeholder.png");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
            img = BitmapFactory.decodeStream(assIs);
		}
    }
    
    private void updateInit() {
		int prefSig = YTD.settings.getInt("APP_SIGNATURE", 0);
		Utils.logger("d", "prefSig: " + prefSig, DEBUG_TAG);
		
		if (prefSig == SettingsActivity.SettingsFragment.YTD_SIG_HASH) {
				Utils.logger("d", "YTD signature in PREFS: update check possile", DEBUG_TAG);
				
				if (YTD.settings.getBoolean("autoupdate", false)) {
					Utils.logger("i", "autoupdate enabled", DEBUG_TAG);
					SettingsActivity.SettingsFragment.autoUpdate(ShareActivity.this);
				}
		} else {
			Utils.logger("d", "different or null YTD signature. Update check cancelled.", DEBUG_TAG);
		}
	}

    BroadcastReceiver inAppCompleteReceiver = new BroadcastReceiver() {

		@Override
        public void onReceive(Context context, Intent intent) {
			//Utils.logger("d", "inAppCompleteReceiver: onReceive CALLED", DEBUG_TAG);
	        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2);
	        
	        if (enqueue != -1 && id != -2 && id == enqueue && !videoOnExt) {
	            Query query = new Query();
	            query.setFilterById(id);
	            Cursor c = dm.query(query);
	            if (c.moveToFirst()) {
	                int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
	                int status = c.getInt(columnIndex);
	                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(boxThemeContextWrapper);
                        helpBuilder.setIcon(android.R.drawable.ic_dialog_info);
                        helpBuilder.setTitle(getString(R.string.information));
                        helpBuilder.setMessage(getString(R.string.download_complete_dialog_msg1) + titleRaw + getString(R.string.download_complete_dialog_msg2));
                        helpBuilder.setPositiveButton(getString(R.string.download_complete_dialog_positive), new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {

                                Intent v_intent = new Intent();
                                v_intent.setAction(android.content.Intent.ACTION_VIEW);
                                v_intent.setDataAndType(videoUri, "video/*");
                                startActivity(v_intent);
                            }
                        });

                        helpBuilder.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                            	// cancel
                            }
                        });

                        AlertDialog helpDialog = helpBuilder.create();
                        if (! ((Activity) context).isFinishing()) {
                        	helpDialog.show();
                        }
                    }
                }
            }
        }
    };
}
