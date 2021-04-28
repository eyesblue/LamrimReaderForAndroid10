package eyes.blue;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.view.MenuItemCompat;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eyes.blue.RemoteDataSource.RemoteSource;

public class SpeechMenuActivity extends AppCompatActivity {
	String logTag=getClass().getName();
	FileSysManager fsm=null;
	ImageButton btnDownloadAll, btnMaintain,  btnManageStorage;
	TextView downloadAllTextView;
	boolean speechFlags[];//, subtitleFlags[]=null;
	String[] descs, subjects, rangeDescs;
	ArrayList<HashMap<String,Boolean>> fakeList = new ArrayList<HashMap<String,Boolean>>();
	SimpleAdapter adapter=null;
	ListView speechList=null;
//	private PowerManager.WakeLock wakeLock = null;
	SharedPreferences runtime = null;
	// The handle for close the dialog.
	AlertDialog itemManageDialog = null;
	int manageItemIndex=-1;
	SingleDownloadThread downloader = null;
	boolean fireLockKey = false;
	final int PLAY=0,UPDATE=1,	DELETE=2, DOWNLOAD=3, CANCEL=4, SRC1=5, SRC2=6;
	boolean isCallFromDownloadCmd=false;
	//ProgressDialog pd = null;
	ProgressBar downloadProg=null;
	DownloadProgressListener dpListener=null;
	AlertDialog netAccessWarnDialog;
	ButtonUpdater buttonUpdater=null;
	private DownloadAllServiceReceiver downloadAllServiceReceiver=null;
	IntentFilter downloadAllServiceIntentFilter=null;
	View rootView =null;
	private FirebaseAnalytics mFirebaseAnalytics;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.speech_menu);
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

		rootView = findViewById(R.id.speechMenuRootView);
		speechList=(ListView) findViewById(R.id.list);
		btnDownloadAll=(ImageButton) findViewById(R.id.btnDownloadAll);
		btnMaintain=(ImageButton) findViewById(R.id.btnMaintain);
		btnManageStorage=(ImageButton) findViewById(R.id.btnManageStorage);
		downloadAllTextView=(TextView)findViewById(R.id.downloadAllTextView);

		downloadAllServiceReceiver = new DownloadAllServiceReceiver();
		downloadAllServiceIntentFilter = new IntentFilter("eyes.blue.action.DownloadAllService");
	//	PowerManager powerManager=(PowerManager) getSystemService(Context.POWER_SERVICE);
	//	wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getClass().getName());
		//wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
		runtime = getSharedPreferences(getString(R.string.runtimeStateFile), 0);
		fireLocker.start();
		//if(!wakeLock.isHeld()){wakeLock.acquire();}
		//if(wakeLock.isHeld())wakeLock.release();
		fsm=new FileSysManager(this);
		//pd= getDlprgsDialog();

		downloadProg=new ProgressBar(SpeechMenuActivity.this,null, android.R.attr.progressBarStyleHorizontal);
		dpListener=new DownloadProgressListener(){
			@Override
			public void setMax(final int val){runOnUiThread(new Runnable() {
				@Override
				public void run() {
					downloadProg.setMax(val);
				}
			});}
			@Override
			public void setProgress(final int val){runOnUiThread(new Runnable() {
				@Override
				public void run() {
					downloadProg.setProgress(val);
				}
			});}
		};

		final QuickAction fileReadyQAction = new QuickAction(this);
		fileReadyQAction.addActionItem(new ActionItem(PLAY, getString(R.string.dlgManageSrcPlay), ApiLevelAdaptor.getDrawable(SpeechMenuActivity.this, R.mipmap.ic_media_play)));
		fileReadyQAction.addActionItem(new ActionItem(UPDATE, getString(R.string.dlgManageSrcUpdate), ApiLevelAdaptor.getDrawable(SpeechMenuActivity.this, R.mipmap.ic_update)));
		fileReadyQAction.addActionItem(new ActionItem(DELETE, getString(R.string.dlgManageSrcDel), ApiLevelAdaptor.getDrawable(SpeechMenuActivity.this, R.mipmap.ic_delete)));
		fileReadyQAction.addActionItem(new ActionItem(CANCEL, getString(R.string.dlgCancel), ApiLevelAdaptor.getDrawable(SpeechMenuActivity.this, R.mipmap.ic_return)));
	
		fileReadyQAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
			@Override
			public void onItemClick(QuickAction quickAction, int pos, int actionId) {
				switch(actionId){
				case PLAY:
					resultAndPlay(manageItemIndex);
                    Util.fireKeyValue("ButtonClick", "QuickActionMenuPlay");
					break;
				case UPDATE:
					DialogInterface.OnClickListener updateListener=new DialogInterface.OnClickListener(){
						@Override
						public void onClick(DialogInterface dialog, int which) {
							final ProgressDialog pd= new ProgressDialog(SpeechMenuActivity.this);
							File f=fsm.getLocalMediaFile(manageItemIndex);
							if(f!=null && !fsm.isFromUserSpecifyDir(f))f.delete();
							downloadSrc(manageItemIndex);
							Util.fireKeyValue("ButtonClick", "QuickActionMenuUpdate");
						}};

					BaseDialogs.showDelWarnDialog(SpeechMenuActivity.this, getString(R.string.file), updateListener, null);
					break;
				case DELETE:
					DialogInterface.OnClickListener deleteListener=new DialogInterface.OnClickListener(){
						@Override
						public void onClick(DialogInterface dialog, int which) {
							File f=fsm.getLocalMediaFile(manageItemIndex);
							if(f!=null && fsm.isFromUserSpecifyDir(f)){
								Toast.makeText(SpeechMenuActivity.this,"此檔案位於使用者指定位置，不可刪除！",Toast.LENGTH_LONG).show();
								return;
							}
							if(f!=null && !f.delete()) {
								Toast.makeText(SpeechMenuActivity.this, "刪除失敗！", Toast.LENGTH_LONG).show();
								return;
							}
							updateUi(manageItemIndex,true);
                            Util.fireKeyValue("ButtonClick", "QuickActionMenuDelete");
						}};

					BaseDialogs.showDelWarnDialog(SpeechMenuActivity.this, getString(R.string.file), deleteListener, null);
					break;
				case CANCEL:
                    Util.fireKeyValue("ButtonClick", "QuickActionMenuCancel");
					break;
				};
			}
		});

		final QuickAction fileNotReadyQAction = new QuickAction(this);
		fileNotReadyQAction.addActionItem(new ActionItem(DOWNLOAD, getString(R.string.dlgManageSrcDL), ApiLevelAdaptor.getDrawable(SpeechMenuActivity.this, R.mipmap.ic_download)));
		fileNotReadyQAction.addActionItem(new ActionItem(CANCEL, getString(R.string.dlgCancel), ApiLevelAdaptor.getDrawable(SpeechMenuActivity.this, R.mipmap.ic_return)));
		fileNotReadyQAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
			@Override
			public void onItemClick(QuickAction quickAction, int pos, int actionId) {
				switch(actionId){
					case DOWNLOAD:
						downloadSrc(manageItemIndex);
						break;
					case CANCEL:
						break;
				}
			}});

		final QuickAction downloadAllRemoteSelect = new QuickAction(this);
		for(int i=0;i<Util.getRemoteSource(this).length;i++)
			downloadAllRemoteSelect.addActionItem(new ActionItem(i, Util.getRemoteSource(this)[i].getName(), ApiLevelAdaptor.getDrawable(SpeechMenuActivity.this, R.mipmap.ic_global_lamrim)));

		downloadAllRemoteSelect.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
			@Override
			public void onItemClick(QuickAction quickAction, int pos, int actionId) {
				SharedPreferences.Editor editor=runtime.edit();
				editor.putInt(getString(R.string.remoteSrcIndexKey),actionId);
				editor.commit();
			}});


		String[] infos =getResources().getStringArray(R.array.desc);
		descs=new String[infos.length];
		subjects=new String[infos.length];
		rangeDescs=new String[infos.length];
		for(int i=0;i<infos.length;i++){
	//		FirebaseCrashlytics.getInstance().log("Desc: "+infos[i]);
			String[] sep=infos[i].split("-");
			descs[i]=sep[0];
			if(sep.length>1)subjects[i]=sep[1];
			if(sep.length>2)rangeDescs[i]=sep[2];
		}

		speechFlags=new boolean[SpeechData.name.length];

		// Initial fakeList
		HashMap<String,Boolean> fakeItem = new HashMap<String,Boolean>();
		fakeItem.put("title", false);
		fakeItem.put("desc", false);
		for(int i = 0; i< SpeechData.name.length; i++)
			fakeList.add( fakeItem );


		adapter = new SpeechListAdapter(this, fakeList,
				 R.layout.speech_row, new String[] { "page", "desc" },
					new int[] { R.id.pageContentView, R.id.pageNumView });

		speechList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
				FirebaseCrashlytics.getInstance().log("User click position "+position);
				if(fireLock())return;

				if(fsm.isFilesReady(position)){
					FirebaseCrashlytics.getInstance().log("File exist, return play.");
					resultAndPlay(position);
				}
				else {
					FirebaseCrashlytics.getInstance().log("File not exist, start download.");
					downloadSrc(position);
				}
				//AnalyticsApplication.sendEvent("ui_action", "SpeechMenuActivity", "select_item_"+SpeechData.getSubtitleName(position));
		}});

		speechList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View v,int position, long id) {
				manageItemIndex=position;
				if(speechFlags[position]) fileReadyQAction.show(v);
				else fileNotReadyQAction.show(v);

                Util.fireKeyValue("ButtonClick", "ShowQuickActionMenu");
				return true;
			}


		});

		speechList.setAdapter( adapter );
		 //啟用按鍵過濾功能
		speechList.setTextFilterEnabled(true);

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run(){
				buttonUpdater=new ButtonUpdater();
				buttonUpdater.start();
			}
		},200);

		btnMaintain.setOnClickListener(new View.OnClickListener (){
			@Override
			public void onClick(View arg0) {
				if(fireLock())return;
				maintain();
                Util.fireKeyValue("ButtonClick", "MaintainFilesButtonClicked");
			}});

		btnManageStorage.setOnClickListener(new View.OnClickListener (){
			@Override
			public void onClick(View v) {
				if(fireLock())return;
				final Intent storageManage = new Intent(SpeechMenuActivity.this, StorageManageActivity.class);
				startActivity(storageManage);
				buttonUpdater.cancel();
                Util.fireKeyValue("ButtonClick", "ManageStorageButtonClicked");
			}});

	 }
	
// End of onCreate
	
	@Override
	protected void onResume() {
		super.onResume();
		this.registerReceiver(downloadAllServiceReceiver, downloadAllServiceIntentFilter);

		final int speechMenuPage=runtime.getInt("speechMenuPage", 0);
//		final int speechMenuPageShift=runtime.getInt("speechMenuPageShift", 0);
		int lastPage=runtime.getInt("lastViewItem", -1);

		if(lastPage==-1){
			lastPage=speechMenuPage+10;
			if(lastPage>= SpeechData.name.length)
				lastPage= SpeechData.name.length-1;
		}
		refreshFlags(speechMenuPage,++lastPage,true);
		refreshFlags(0,speechFlags.length,false);
		
		Bundle b=this.getIntent().getExtras();
		if(b==null)return;
		isCallFromDownloadCmd=true;
		
		int[] resource =b.getIntArray("index");
		if(resource == null || resource.length<=0){
			FirebaseCrashlytics.getInstance().log( "Start SpeechMenuActivity with download command, but no media index extras, skip download.");
			return;
		}
		downloadSrc(resource);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		this.unregisterReceiver(downloadAllServiceReceiver);
		SharedPreferences.Editor editor = runtime.edit();

		editor.putInt("speechMenuPage",speechList.getFirstVisiblePosition());
		View v=speechList.getChildAt(0);  
        editor.putInt("speechMenuPageShift",(v==null)?0:v.getTop());
        editor.putInt("lastViewItem",speechList.getLastVisiblePosition());
        editor.commit();
	}
	
/*	@Override
	public void onBackPressed() {
		playWindow.putExtra("index", -1);
		setResult(RESULT_OK,new Intent().putExtras(playWindow));
		if(wakeLock.isHeld())wakeLock.release();
		finish();
	}
	*/
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		try{
			buttonUpdater.cancel(false);
		}catch(Exception e){e.printStackTrace();};
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		SharedPreferences playRecord = getSharedPreferences(getString(R.string.speechModeRecordFile), 0);
		int mediaIndex=playRecord.getInt("mediaIndex",-1);
		int position=playRecord.getInt("playPosition",-1);
		if(mediaIndex != -1){
			MenuItem item=menu.add(getString(R.string.reloadLastState)+": "+ SpeechData.getSubtitleName(mediaIndex)+": "+Util.getMsToHMS(position, "\"", "\'", false));
			item.setIcon(R.mipmap.ic_reload_last_state);
			MenuItemCompat.setShowAsAction(item, MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			return super.onCreateOptionsMenu(menu);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId()==android.R.id.home) {
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}

		String menuStr=item.getTitle().toString();
		if(menuStr.startsWith(getString(R.string.reloadLastState))){
			FirebaseCrashlytics.getInstance().log("User click reload last state button.");
			Intent playWindow = new Intent();
			playWindow.putExtra("reloadLastState", true);
			setResult(Activity.RESULT_OK, playWindow);
            Util.fireKeyValue("ButtonClick", "ReloadLastState");
			finish();
		}
		return true;
	}
	
	private void resultAndPlay(int position){
		FirebaseCrashlytics.getInstance().log("Speech menu "+position+"th item clicked.");
		Intent playWindow = new Intent();
		playWindow.putExtra("index", position);
		setResult(Activity.RESULT_OK, playWindow);
		finish();
	}
	
	private void updateUi(final int i, boolean shiftToIndex){
		File speech=fsm.getLocalMediaFile(i);
		speechFlags[i]=(speech!=null && speech.exists());

		if(shiftToIndex){
			refreshListView();
			speechList.post(new Runnable(){
				@Override
				public void run() {
					speechList.setSelection(i);
			}});
		}
		else{
			final int serial=speechList.getFirstVisiblePosition();
			View v = speechList.getChildAt(0);
			final int shift = (v == null) ? 0 : v.getTop();

			speechList.post(new Runnable(){
				@Override
				public void run() {
					adapter.notifyDataSetChanged();
					speechList.setSelectionFromTop(serial, shift);
			}});
		}
	}

	// 從頭掃描一次確認音檔是否存在
	private void refreshFlags(final int start,final int end,final boolean isRefreshView){
		FirebaseCrashlytics.getInstance().log( "Refresh flags: start="+start+", end="+end);
		Thread t=new Thread(new Runnable(){
			@Override
			public void run() {
				for(int i=start;i<end;i++){
					synchronized(speechFlags){
						speechFlags[i]=(fsm.isFilesReady(i));
					}
				}
				if(isRefreshView)refreshListView();
			}
		});
		 t.start();
	}
	
	private void refreshListView(){
		final int speechListPage=runtime.getInt("speechMenuPage", 0);
		final int speechListPageShift=runtime.getInt("speechMenuPageShift", 0);
		adapter = new SpeechListAdapter(SpeechMenuActivity.this, fakeList,
				 R.layout.speech_row, new String[] { "page", "desc" },
					new int[] { R.id.pageContentView, R.id.pageNumView });
		
		speechList.post(new Runnable(){
			@Override
			public void run() {
				speechList.setAdapter( adapter );
				adapter.notifyDataSetChanged();
				speechList.setSelectionFromTop(speechListPage, speechListPageShift);
			}});
	}
	
	private void downloadSrc(final int... index){
		FirebaseCrashlytics.getInstance().log( "downloadSrc been call.");
		for(int i=0;i<index.length;i++){
			File mediaFile=fsm.getLocalMediaFile(index[i]);
		
			if(mediaFile==null){
				Util.showErrorToast(SpeechMenuActivity.this, getString(R.string.dlgDescDownloadFail));
				return;
			}
		}
		
		checkNetAccessPermission(new Runnable(){
			@Override
			public void run() {
				lockScreen();
				downloader = new SingleDownloadThread(index);
				//pd.show();
				downloader.start();
			}});
	}
	
	private void downloadAllSrc(){
		checkNetAccessPermission(new Runnable(){
			@Override
			public void run() {
				showThreadsSelectDialog();
			}});
	}
	
	public void checkNetAccessPermission(final Runnable task){
		boolean isShowNetAccessWarn=runtime.getBoolean(getString(R.string.isShowNetAccessWarn), true);
		boolean isAllowAccessNet=runtime.getBoolean(getString(R.string.isAllowNetAccess), false);
		FirebaseCrashlytics.getInstance().log("ShowNetAccessWarn: "+isShowNetAccessWarn+", isAllowNetAccess: "+isAllowAccessNet);
		if(isShowNetAccessWarn ||(!isShowNetAccessWarn && !isAllowAccessNet)){
			rootView.post(new Runnable() {
				public void run() {
					if(netAccessWarnDialog==null || !netAccessWarnDialog.isShowing()){
//						if(!wakeLock.isHeld()){wakeLock.acquire();}
						netAccessWarnDialog=getNetAccessDialog(task);
						netAccessWarnDialog.setCanceledOnTouchOutside(false);
						netAccessWarnDialog.show();
					}
				}
			});
			return ;
		}
		else task.run();
	}
	
	private AlertDialog getNetAccessDialog(final Runnable task){
		final SharedPreferences.Editor editor = runtime.edit();
       
		LayoutInflater adbInflater = LayoutInflater.from(this);
		View eulaLayout = adbInflater.inflate(R.layout.net_access_warn_dialog, null);
		final CheckBox dontShowAgain= (CheckBox) eulaLayout.findViewById(R.id.skip);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(eulaLayout);
		builder.setTitle(getString(R.string.dlgNetAccessTitle));
		builder.setMessage(getString(R.string.dlgNetAccessMsg));
		builder.setPositiveButton(getString(R.string.dlgAllow), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				FirebaseCrashlytics.getInstance().log("Check box check status: "+dontShowAgain.isChecked());
				editor.putBoolean(getString(R.string.isShowNetAccessWarn), !dontShowAgain.isChecked());
				editor.putBoolean(getString(R.string.isAllowNetAccess), true);
				editor.commit();
				try{
					dialog.dismiss();
				}catch(Exception e){e.printStackTrace();}
				task.run();
			}
		});
		builder.setNegativeButton(getString(R.string.dlgDisallow), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				FirebaseCrashlytics.getInstance().log("Check box check status: "+dontShowAgain.isChecked());
				editor.putBoolean(getString(R.string.isShowNetAccessWarn), !dontShowAgain.isChecked());
				editor.putBoolean(getString(R.string.isAllowNetAccess), false);
				editor.commit();
//				if(wakeLock.isHeld())wakeLock.release();
				dialog.cancel();
				if(isCallFromDownloadCmd){
					isCallFromDownloadCmd=false;
					setResult(RESULT_CANCELED);
					buttonUpdater.cancel();
					finish();
				}
			}
		});
		return builder.create();
	}
	
	@SuppressLint("NewApi")
	private void showThreadsSelectDialog() {
		View selecter = null;
		View layout=null;

		LayoutInflater factory = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		layout = factory.inflate(R.layout.thread_num_picker, null);

		final NumberPicker v = (NumberPicker)layout.findViewById(R.id.numPicker);

//			NumberPicker v = new NumberPicker(this);
		v.setMinValue(1);
		v.setMaxValue(4);
		v.setValue(4);
//			LinearLayoutCompat.LayoutParams layoutParam =new LinearLayoutCompat.LayoutParams(
//					LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
//					LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
//					Gravity.CENTER);
//			v.setLayoutParams(layoutParam);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.msgSelectThread));
		builder.setView(layout);

		builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						int count = 0;
						NumberPicker np = (NumberPicker) v;
						count = np.getValue();

						FirebaseCrashlytics.getInstance().log(	"Start download all service with thread count "	+ count);

						try{
							dialog.dismiss();
						}catch(Exception e){e.printStackTrace();}
						Intent intent = new Intent(SpeechMenuActivity.this,	DownloadAllService.class);
						intent.putExtra("threadCount", count);
						FirebaseCrashlytics.getInstance().log(	"Start download all service.");
						startService(intent);
//						if (wakeLock.isHeld())wakeLock.release();
					}
				});
		builder.setNegativeButton(getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
//						if (wakeLock.isHeld())wakeLock.release();
						dialog.cancel();
					}
				});
		builder.show();
	}
	
	private void maintain(){
		final ProgressDialog pd= new ProgressDialog(SpeechMenuActivity.this);
		pd.setCancelable(false);
		pd.setTitle(getString(R.string.dlgTitleMaintaining));
		pd.setMessage(getString(R.string.dlgMsgMaintaining));
		pd.show();
//		if(!wakeLock.isHeld()){wakeLock.acquire();}
		
/*		AsyncTask<Void, Void, Void> runner=new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				FileSysManager.maintainStorages();
				pd.dismiss();
				if(wakeLock.isHeld())wakeLock.release();
				return null;
			}
		};
		
		runner.execute();
*/	
		Thread t=new Thread(new Runnable(){
			@Override
			public void run() {
				fsm.maintainStorages();
				if(pd.isShowing())pd.dismiss();
//				if(wakeLock.isHeld())wakeLock.release();
				return;
			}});
		t.start();
	}
	
	private boolean fireLock(){
		if(fireLockKey){
			FirebaseCrashlytics.getInstance().log("Fire locked");
			return true;
		}
		fireLockKey=true;
		synchronized(fireLocker){
			fireLocker.notify();
		}
		FirebaseCrashlytics.getInstance().log("Fire released, lock it!");
		return false;
	}
	
	Thread fireLocker=new Thread(){
		@Override
		public void run(){
			while(true){
					try {
						synchronized(this){
							wait();
						}
					} catch (InterruptedException e1) {	e1.printStackTrace();}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {e.printStackTrace();}
					FirebaseCrashlytics.getInstance().log("Fire locker release the lock.");
					fireLockKey=false;

			}
		}
	};
	
	private void updateDownloadAllBtn(){
		btnDownloadAll.postDelayed(new Runnable(){
			@Override
			public void run() {
				boolean isAlive=isMyServiceRunning(DownloadAllService.class);
				if(!isAlive){
					downloadAllTextView.setText(getString(R.string.btnDownloadAll));
					btnDownloadAll.setOnClickListener(new View.OnClickListener (){
						@Override
						public void onClick(View arg0) {
							synchronized(btnDownloadAll){
								if(fireLock())return;
								downloadAllSrc();
                                Util.fireKeyValue("ButtonClick", "DownloadAllButtonClicked");
							}
						}});
				}
				else {
					downloadAllTextView.setText(getString(R.string.btnCancle));
					btnDownloadAll.setOnClickListener(new View.OnClickListener (){
						@Override
						public void onClick(View arg0) {
							synchronized(btnDownloadAll){
								Intent intent = new Intent(SpeechMenuActivity.this, DownloadAllService.class);
								FirebaseCrashlytics.getInstance().log("Stop download all service.");
								stopService(intent);
								//NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
								//mNotificationManager.cancel(DownloadAllService.notificationId);
								ApiLevelAdaptor.removeNotification(SpeechMenuActivity.this);
							}
					}});
				}
			}
		},500);
		
		
	}

	private boolean isMyServiceRunning(Class<?> serviceClass) {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (serviceClass.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}

	public class DownloadAllServiceReceiver extends BroadcastReceiver {
		public DownloadAllServiceReceiver(){}
		@SuppressLint("NewApi")
		@Override
		public void onReceive(Context context, Intent intent) {
			String action=intent.getStringExtra("action");
			if(action.equalsIgnoreCase("start") || action.equalsIgnoreCase("stop")){
				FirebaseCrashlytics.getInstance().log("Receive broadcast message: service "+action);
				updateDownloadAllBtn();
			}
			else if(action.equalsIgnoreCase("download")){
				int index=intent.getIntExtra("index", -1);
				if(index==-1){
					Log.e("SpeechMenuActivity","The broadcast index should not -1 !!!");
					return;
				}
				
				boolean isSuccess=intent.getBooleanExtra("isSuccess", false);
				FirebaseCrashlytics.getInstance().log("broadcast receive index "+index+" download "+isSuccess);
				
				/* It should update the background of item of speechList, but I havn't not find a good way to do it.*/
				if(isSuccess)updateUi(index, false);
			}
			else if(action.equalsIgnoreCase("terminate")){
				FirebaseCrashlytics.getInstance().log("Receive broadcast message: service "+action);
				buttonUpdater.cancel();
				updateDownloadAllBtn();
			}
			else if(action.equalsIgnoreCase("error")){
				Util.showErrorToast(SpeechMenuActivity.this, intent.getStringExtra("desc"));
			}
		}
	}

	/*
	private ProgressDialog getDlprgsDialog(){
		pd=new ProgressDialog(this);
		pd.setTitle(getString(R.string.dlgResDownload));
		pd.setMessage("");
		pd.setCancelable(true);
		pd.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(pd.isShowing())pd.dismiss();
				
				if(downloader!=null && !downloader.isCancelled()){
					FirebaseCrashlytics.getInstance().log("The download procedure been cancel.");
					downloader.stopRun();
//					if(wakeLock.isHeld())wakeLock.release();
				}
				if(isCallFromDownloadCmd){
					FirebaseCrashlytics.getInstance().log("The download is start by download command, return caller activity now.");
					isCallFromDownloadCmd=false;
					setResult(RESULT_CANCELED);
					buttonUpdater.cancel();
					finish();
				}
			}
		});
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		return pd;
	}
	*/
	
	private AlertDialog getDownloadAgainDialog(final int ... index){
		FirebaseCrashlytics.getInstance().log( "There is download fail, show download again dialog.");
		String msg=String.format(getString(R.string.dlgMsgDlNotComplete), SpeechData.getNameId(index[0]));
		final AlertDialog.Builder dialog = new AlertDialog.Builder(SpeechMenuActivity.this);
		dialog.setTitle(msg); 
		dialog.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
		    	downloadSrc(index);
		    }
		});
		dialog.setNegativeButton(getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
//		    	if (wakeLock.isHeld())wakeLock.release();
		    	try{
					dialog.dismiss();
				}catch(Exception e){e.printStackTrace();}
		    	if(isCallFromDownloadCmd){
		    		isCallFromDownloadCmd=false;
					setResult(RESULT_CANCELED);
					buttonUpdater.cancel();
					finish();
		    	}
		    }
		});
		
		return dialog.create();
	}
	
	public void lockScreen(){
		new Handler().post(new Runnable(){
			@Override
			public void run() {
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}});
	}
	public void unlockScreen(){
		rootView.post(new Runnable(){
			@Override
			public void run() {
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}});
	}
	
	public class ButtonUpdater extends Thread {
		long activeTime=180000;
		JSONObject executing = null;
		boolean cancelled = false;

		public void cancel() {
			cancelled = true;
		}

		public void cancel(boolean dontCare) {
			cancelled = true;
		}

		@Override
		public void run() {
			activeTime+=System.currentTimeMillis();

			while (!cancelled && System.currentTimeMillis()< activeTime) {
				synchronized (btnDownloadAll) {
					FirebaseCrashlytics.getInstance().log( "Button updater awake");
					updateDownloadAllBtn();

					try {
						btnDownloadAll.wait(1500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			FirebaseCrashlytics.getInstance().log( "Button updater terminate");
			return;
		}
	}

	class SpeechListAdapter extends SimpleAdapter {
		float textSize = 0;

		public SpeechListAdapter(Context context,
				List<? extends Map<String, ?>> data, int resource,
				String[] from, int[] to) {
			super(context, data, resource, from, to);
		}

		@SuppressLint("NewApi")
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
//				FirebaseCrashlytics.getInstance().log( "row=null, construct it.");
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.speech_row, parent, false);
			}

//			FirebaseCrashlytics.getInstance().log( "Set "+SpeechData.getNameId(position)+": is speech exist: "+speechFlags[position]+", is subtitle exist: "+subtitleFlags[position]);
			TextView title = (TextView) row.findViewById(R.id.title);
			TextView subject = (TextView) row.findViewById(R.id.subject);
			TextView speechDesc = (TextView) row.findViewById(R.id.speechDesc);
			TextView rangeDesc = (TextView) row.findViewById(R.id.rangeDesc);

			if(speechFlags[position])
				ApiLevelAdaptor.setBackground(row, ApiLevelAdaptor.getDrawable(SpeechMenuActivity.this, R.drawable.speech_menu_item_e));
			else
				ApiLevelAdaptor.setBackground(row, ApiLevelAdaptor.getDrawable(SpeechMenuActivity.this, R.drawable.speech_menu_item_d));

			title.setText(SpeechData.getNameId(position));
			subject.setText(subjects[position]);
			speechDesc.setText(descs[position]);
			rangeDesc.setText(rangeDescs[position]);
			return row;
		}
	}
	
	class SingleDownloadThread extends Thread {
		boolean isCancelled = false;
		int[] tasks=null;
		
		public SingleDownloadThread(int ... tasks){
			this.tasks=tasks;
		}
		public void stopRun(){
			isCancelled = true;
		}
		
		public boolean isCancelled(){
			return isCancelled;
		}
		
		@Override
		public void run(){
			boolean hasFailure=false;
			RemoteSource rs = Util.getRemoteSource(SpeechMenuActivity.this)[0];
			
			for(int i=0;i<tasks.length;i++){
				boolean mediaExist=false;
				File mediaFile=fsm.getLocalMediaFile(tasks[i]);
				try{
					mediaExist=mediaFile.exists();
				}catch(NullPointerException npe){
					Util.showErrorToast(SpeechMenuActivity.this, getString(R.string.errStorageNotReady));
					Util.fireException("The storage media has not usable, skip.", npe);
					unlockScreen();
					return;
				}
				
				if(!mediaExist) {
					showProgDialog(tasks[i]);
					FirebaseCrashlytics.getInstance().log("*********** Download file ***********");
					mediaExist = HttpDownloader.download(SpeechMenuActivity.this,rs.getMediaFileAddress(tasks[i]), mediaFile.getAbsolutePath(), dpListener);
					dismissProgDialog();
				}
				if(!mediaExist)hasFailure=true;
			}

			unlockScreen();
			
			if(!hasFailure){
				rootView.post(new Runnable(){
					@Override
					public void run() {
						//if(pd.isShowing())pd.dismiss();
						resultAndPlay(tasks[0]);
						buttonUpdater.cancel();
						finish();
					}});
				return;
			}

			FirebaseCrashlytics.getInstance().log("The download has failure, show download again dialog.");
			if(!isCallFromDownloadCmd)
				rootView.post(new Runnable(){
					@Override
					public void run() {
						try{
							final AlertDialog dialog=getDownloadAgainDialog(tasks);
							//if(pd.isShowing())pd.dismiss();
//						if(!wakeLock.isHeld()){wakeLock.acquire();}
							dialog.show();
						}catch(Exception e){
							Util.fireException("Error happen while show download progress dialog.", e);
						}
					}});
		}

		AlertDialog progDialog=null;
		private void showProgDialog(final int targetIndex){
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					FirebaseCrashlytics.getInstance().log("Show single download dialog for "+SpeechData.getNameId(targetIndex));
					downloadProg=new ProgressBar(SpeechMenuActivity.this,null, android.R.attr.progressBarStyleHorizontal); // The view must recreate or [The specified child already has a parent.] must happen.
					progDialog = new AlertDialog.Builder(SpeechMenuActivity.this)
							.setTitle(getString(R.string.dlgResDownload))
							.setMessage(String.format(getString(R.string.dlgDescDownloading),SpeechData.getNameId(targetIndex), getString(R.string.typeAudio)))
							.setView(downloadProg)
							.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									HttpDownloader.stopRun();
									dialog.cancel();
								}
							})
							.create();

					progDialog.show();
				}
			});
		}

		private void dismissProgDialog(){
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					FirebaseCrashlytics.getInstance().log("*********** Dismiss damn progress bar ***********");
					if(progDialog.isShowing())
						try {
							progDialog.dismiss();
						}catch(Exception e){e.printStackTrace();}	// 此處可能在視窗已經離開Activity後才關閉 progress dialog造成 not attached to window manager 錯誤，直接忽略。
				}
			});
		}
		/*
		private boolean download(String url, String outputPath, final int mediaIndex,	final int type) {
			pd.setProgress(0);
			FirebaseCrashlytics.getInstance().log( "Download file from " + url);
			File tmpFile = new File(outputPath + getString(R.string.downloadTmpPostfix));
			long startTime = System.currentTimeMillis(), respWaitStartTime;

			int readLen = -1, counter = 0, bufLen = getResources().getInteger(R.integer.downloadBufferSize);
//			Checksum checksum = new CRC32();
			FileOutputStream fos = null;

			// HttpClient httpclient = getNewHttpClient();
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(url);
			HttpResponse response = null;
			int respCode = -1;
			if (isCancelled) {
				FirebaseCrashlytics.getInstance().log(
						"User canceled, download procedure skip!");
				return false;
			}
			
			runOnUiThread(new Runnable(){
				@Override
				public void run() {
					try{
						pd.setTitle(getResources().getString(R.string.dlgTitleConnecting));
						pd.setMessage(String.format(getString(R.string.dlgDescConnecting), SpeechData.getNameId(mediaIndex),
								(type == getResources().getInteger(R.integer.MEDIA_TYPE)) ? getString(R.string.typeAudio) : getString(R.string.typeSubtitle)));
					}catch(Exception e){e.printStackTrace();}
				}});
			
			try {
				respWaitStartTime = System.currentTimeMillis();
				response = httpclient.execute(httpget);
				respCode = response.getStatusLine().getStatusCode();

				// For debug
				if (respCode != HttpStatus.SC_OK) {
					httpclient.getConnectionManager().shutdown();
					System.out.println("CheckRemoteThread: Return code not equal 200! check return "+ respCode);
					return false;
				}
			} catch (ClientProtocolException e) {
				httpclient.getConnectionManager().shutdown();
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				httpclient.getConnectionManager().shutdown();
				e.printStackTrace();
				return false;
			}

			if (isCancelled) {
				httpclient.getConnectionManager().shutdown();
				FirebaseCrashlytics.getInstance().log(
						"User canceled, download procedure skip!");
				return false;
			}

            Crashlytics.setDouble("ResponseTimeOfDownload", (System.currentTimeMillis() - respWaitStartTime));
			HttpEntity httpEntity = response.getEntity();
			InputStream is = null;
			try {
				is = httpEntity.getContent();
			} catch (IllegalStateException e2) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				httpclient.getConnectionManager().shutdown();
				tmpFile.delete();
				e2.printStackTrace();
				return false;
			} catch (IOException e2) {
				httpclient.getConnectionManager().shutdown();
				tmpFile.delete();
				e2.printStackTrace();
				return false;
			}

			if (isCancelled) {
				FirebaseCrashlytics.getInstance().log(
						"User canceled, download procedure skip!");
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				httpclient.getConnectionManager().shutdown();
				tmpFile.delete();
				return false;
			}

			final long contentLength = httpEntity.getContentLength();

			pd.setMax((int) contentLength);

			try {
				fos = new FileOutputStream(tmpFile);
			} catch (FileNotFoundException e1) {
				FirebaseCrashlytics.getInstance().log(
						"File Not Found Exception happen while create output temp file ["
								+ tmpFile.getName() + "] !");
				httpclient.getConnectionManager().shutdown();
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				tmpFile.delete();
				e1.printStackTrace();
				return false;
			}

			if (isCancelled) {
				httpclient.getConnectionManager().shutdown();
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				tmpFile.delete();
				FirebaseCrashlytics.getInstance().log(
						"User canceled, download procedure skip!");
				return false;
			}

			
			runOnUiThread(new Runnable(){
				@Override
				public void run() {
					try{
						pd.setTitle(R.string.dlgTitleDownloading);
						pd.setMessage(String.format(getString(R.string.dlgDescDownloading),	SpeechData.getNameId(mediaIndex),
							(type == getResources().getInteger(R.integer.MEDIA_TYPE)) ? getString(R.string.typeAudio) : getString(R.string.typeSubtitle)));
						pd.setMax((int) contentLength);
					}catch(Exception e){e.printStackTrace();}
				}});
			

			try {
				byte[] buf = new byte[bufLen];
				FirebaseCrashlytics.getInstance().log( Thread.currentThread().getName()
						+ ": Start read stream from remote site, is="
						+ ((is == null) ? "NULL" : "exist") + ", buf="
						+ ((buf == null) ? "NULL" : "exist"));
				while ((readLen = is.read(buf)) != -1) {

					counter += readLen;
					fos.write(buf, 0, readLen);
//					checksum.update(buf, 0, readLen);
					pd.setProgress(counter);

					if (isCancelled) {
						httpclient.getConnectionManager().shutdown();
						try {
							is.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						try {
							fos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						tmpFile.delete();
						FirebaseCrashlytics.getInstance().log(
								"User canceled, download procedure skip!");
						return false;
					}
				}
				is.close();
				fos.flush();
				fos.close();
			} catch (IOException e) {
				httpclient.getConnectionManager().shutdown();
				try {
					is.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
				try {
					fos.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
				tmpFile.delete();
				e.printStackTrace();
				FirebaseCrashlytics.getInstance().log( Thread.currentThread().getName()
						+ ": IOException happen while download media.");
				return false;
			}

			if (counter != contentLength || isCancelled) {
				httpclient.getConnectionManager().shutdown();
				tmpFile.delete();
				return false;
			}

			// rename the protected file name to correct file name
			tmpFile.renameTo(new File(outputPath));
			httpclient.getConnectionManager().shutdown();
			FirebaseCrashlytics.getInstance().log( Thread.currentThread().getName() + ": Download finish, return true.");
			return true;
		}
		*/
	}
	
	
}
