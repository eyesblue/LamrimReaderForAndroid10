package eyes.blue.bgmusicplayer;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.security.CryptoPrimitive;
import java.util.ArrayList;

import eyes.blue.BaseDialogs;
import eyes.blue.FileSysManager;
import eyes.blue.R;
import eyes.blue.SpeechData;
import eyes.blue.Util;

public class MainActivity extends AppCompatActivity implements PlaybackService.PlaybackListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener{

    private PlaybackService service;
    private Intent serviceIntent;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedEditor;

    //private String currentPath = Environment.getExternalStorageDirectory().toString();
    int currentPlayIndex=-1;
    private ListFilesAdapter adapter;
    ListView listView;
    private ArrayList<Integer> currentPosition = new ArrayList<>();
    private ArrayList<String> songCollectionPathsToSend = new ArrayList<>();
    private boolean isRepeatOne = false;
    private TextView currentPosTextView, mediaDurationTextView;
    private ImageView playPauseButton;
    private SeekBar seekBar;
    FileSysManager fsm;
    private Menu optMenu;
    String logTag=getClass().getName();

    private ArrayList<Integer> currentDirItems = new ArrayList<>();
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bgmusic_player_activity);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        serviceIntent = new Intent(this, PlaybackService.class);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        fsm=new FileSysManager(MainActivity.this);
        sharedPreferences = getSharedPreferences(getString(R.string.SHARED_MAINKEY), 0);
        sharedEditor = sharedPreferences.edit();
        isRepeatOne = sharedPreferences.getBoolean(getString(R.string.SHARED_IS_ONE_SONG_REPEATING), false);

        final DrawerLayout drawerLayout = findViewById(R.id.drawerlayout);
        drawerLayout.bringToFront();
        //currentPosition.add(0);

        //songnameTextView = findViewById(R.id.mainactivity_songname_textview);
        currentPosTextView = findViewById(R.id.mainactivity_current_pos_textview);
        mediaDurationTextView = findViewById(R.id.mainactivity_duration_textview);
        seekBar = findViewById(R.id.mainactivity_seekbar);
        seekBar.setOnSeekBarChangeListener(this);
        ImageView prevButton = findViewById(R.id.mainactivity_prev_button);
        playPauseButton = findViewById(R.id.mainactivity_play_button);
        ImageView nextButton = findViewById(R.id.mainactivity_next_button);
        prevButton.setOnClickListener(this);
        playPauseButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);

        //updateFavoriteFolders();

        listView = findViewById(R.id.listview);
        adapter = new ListFilesAdapter(this, currentDirItems);
        /*
        adapter.setOnFileCheckedChangedListener(new ListFilesAdapter.OnFileCheckedChangeListener() {
            @Override
            public void onFileCheckedChanged(int index, boolean newValue) {
                if (!newValue) {
                    if (service != null) {
                        songCollectionPathsToSend.remove(index);
                        drawerLayoutListAdapter.notifyDataSetChanged();
                        service.changeFilesToPlay(index, newValue);
                    }
                }
            }
        });
        */
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int index=currentDirItems.get(i);
                File file = fsm.getLocalMediaFile(index);
                Crashlytics.log(Log.DEBUG, logTag, "File been click: "+file.getAbsolutePath());

                if(!file.exists()){
                    BaseDialogs.showDialog(MainActivity.this, "播放錯誤", "檔案不存在，請回到下載頁面下載音檔。",null,null,null,true);
                    Crashlytics.log(Log.ERROR, logTag, SpeechData.getSubtitleName(index)+" file been click in ListView, but file gone.");
                    return;
                }
                if(service==null){
                    BaseDialogs.showDialog(MainActivity.this, "程式錯誤", "播放服務未連接，請回到上一頁並重新開啟此頁面嘗試是否能重連播放服務，若持續錯誤請回報作者。",null,null,null,true);
                    Crashlytics.log(Log.ERROR, logTag, SpeechData.getSubtitleName(index)+" file been click in ListView, but service not connect.");
                    return;
                }

                service.jumpToLamrimIndex(MainActivity.this, index,0);
                startService(serviceIntent);
                adapter.setSelectedIndex(i);
                adapter.notifyDataSetChanged();
            }
        });

        listDir();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
        /*
        String strToSave = "";
        for (int cnt = 0; cnt < songCollectionPathsToSend.size(); cnt++) {
            strToSave += songCollectionPathsToSend.get(cnt) + getString(R.string.DIVIDER);
        }
        sharedEditor.putString(getString(R.string.SHARED_LAST_SONGS_ARRAY), strToSave);
        sharedEditor.putInt(getString(R.string.SHARED_LAST_SONGS_INDEX), currentPosition.get(0));
        sharedEditor.apply();
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bgmusic_player_mainactivity_menu, menu);
        optMenu=menu;
        if (isRepeatOne) {
            menu.findItem(R.id.mainmenu_menuitem_repeat).setIcon(R.mipmap.ic_play_repeat);
            menu.findItem(R.id.mainmenu_menuitem_repeat).setTitle(R.string.single_repeat);
        } else {
            menu.findItem(R.id.mainmenu_menuitem_repeat).setIcon(R.mipmap.ic_play_next);
            menu.findItem(R.id.mainmenu_menuitem_repeat).setTitle(R.string.all_repeat);
        }
        return (super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mainmenu_menuitem_repeat) {
            Crashlytics.log(Log.DEBUG, logTag,"Event from Activity UI: REPEAT button clicked.");
            Util.fireSelectEvent(mFirebaseAnalytics,logTag,Util.BUTTON_CLICK,"REPEAT_IN_PLAY_BGM_ACTIVITY");

            isRepeatOne = !isRepeatOne;
            if (isRepeatOne) {
                if(currentPlayIndex!=-1)
                    Toast.makeText(MainActivity.this, String.format(getString(R.string.single_repeat),SpeechData.getSubtitleName(currentPlayIndex)),Toast.LENGTH_LONG).show();
                item.setIcon(R.mipmap.ic_play_repeat);
                //optMenu.findItem(R.id.mainmenu_menuitem_repeat).setTitle(R.string.single_repeat);
                sharedEditor.putBoolean(getString(R.string.SHARED_IS_ONE_SONG_REPEATING), true);
            } else {
                Toast.makeText(MainActivity.this,R.string.all_repeat,Toast.LENGTH_LONG).show();
                item.setIcon(R.mipmap.ic_play_next);
                //optMenu.findItem(R.id.mainmenu_menuitem_repeat).setTitle(R.string.all_repeat);
                sharedEditor.putBoolean(getString(R.string.SHARED_IS_ONE_SONG_REPEATING), false);
            }
            sharedEditor.apply();

            if (service != null) {
                service.changeRepeatState(isRepeatOne);
            }
        } else if (item.getItemId() == R.id.mainmenu_menuitem_quit) {
            stopService(serviceIntent);
            finish();
        }
      /* else if (item.getItemId() == R.id.mainmenu_menuitem_favourite) {
            Set<String> set;
            if (sharedPreferences.getStringSet(getString(R.string.SHARED_FAV_DIRS), null) == null) {
                set = new HashSet<>();
            } else {
                set = sharedPreferences.getStringSet(getString(R.string.SHARED_FAV_DIRS), null);
            }
            set.add(currentPath);
            sharedEditor.putStringSet(getString(R.string.SHARED_FAV_DIRS), set);
            sharedEditor.apply();
            //updateFavoriteFolders();
        }
       */
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        BaseDialogs.showDialog(
                MainActivity.this,
                "停止播放",
                "即將停止背景播放，您確定嗎？",
                null,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        serviceIntent = new Intent(MainActivity.this, PlaybackService.class);
                        stopService(serviceIntent);
                        finish();
                        MainActivity.super.onBackPressed();
                    }
                },
                null,
                true);

    }

    /* 當Activity處於背景狀態，又接收到啟動 Intent 時，不會再執行onCreate，而是執行此onNewIntent，
       但若系統砍掉此背景狀態後的Activity時，下在再收到Intent則會回到onCreate。

       此處僅判斷是否為關閉Intent，若為關閉信號，則關閉Service並結束。
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(getString(R.string.NOTIFICATION_ACTION_QUIT))) {
                if (serviceIntent == null) {
                    serviceIntent = new Intent(this, PlaybackService.class);
                }
                stopService(serviceIntent);
                finish();
            }
        }
    }

    private boolean firstConnection=true;
    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Crashlytics.log(Log.DEBUG, logTag, "PlaybackService connected.");
            PlaybackService.LocalBinder binder = (PlaybackService.LocalBinder) iBinder;
            service = binder.getService();
            if (service != null) {
                service.setPlaybackListener(MainActivity.this);

                // UI創建完成後即自動播放原來的位置
                if(firstConnection) {
                    firstConnection=false;
                    Intent intent = getIntent();
                    int mediaIndex = intent.getIntExtra("MEDIA_INDEX", 0);
                    int timePos = intent.getIntExtra("TIME_POSITION", 0);
                    service.jumpToLamrimIndex(MainActivity.this, mediaIndex, timePos);
                    startService(serviceIntent);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    @Override
    public void onSeekUpdate(final String title, final int currentPosition, final int duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setTitle(title);
                //songnameTextView.setText(title);
                seekBar.setMax(duration);
                seekBar.setProgress(currentPosition);
                currentPosTextView.setText(formatDuration(currentPosition));
                mediaDurationTextView.setText(formatDuration(duration));
            }
        });
    }

    /* 針對 currentPath 的路徑將目錄內的檔案加入到 currentDirItems，最後更新 listview 顯示內容 */
    private void listDir() {
        Crashlytics.log(Log.DEBUG, logTag, "Into listDir.");
        final SeekBar sb=new SeekBar(MainActivity.this);
        sb.setMax(SpeechData.name.length);
        sb.setProgress(0);
        final AlertDialog dialog=BaseDialogs.showDialog(MainActivity.this,"掃描中","檔案掃描中...",sb,null,null,false);

        Runnable r=new Runnable() {
            private void setProg(final int i){
                sb.setProgress(i);
            }
            @Override
            public void run() {
                Intent intent = getIntent();
                int mediaIndex = intent.getIntExtra("MEDIA_INDEX", 0);
                int listIndex=-1;
                for(int i=0;i<320;i++) {
                    //Crashlytics(Log.DEBUG, logTag, getClass().getName(), "Check is file exist: "+ SpeechData.getSubtitleName(i));
                    File f = fsm.getLocalMediaFile(i);
                    if (f.exists()){
                        currentDirItems.add(i);
                        if(i==mediaIndex)
                            listIndex=i;
                    }
                    setProg(i);
                    final int prog=i;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setMessage("檔案掃描中... "+prog+"/320");
                        }
                    });
                }

                final int index=listIndex;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        adapter.setSelectedIndex(index);
                        adapter.notifyDataSetChanged();

                        if(index!=-1){
                            Crashlytics.log(Log.DEBUG, logTag, "=======>Move ListView to index "+SpeechData.getSubtitleName(index));
                            listView.setSelectionFromTop(index,0);
                        }
                    }
                });

                Crashlytics.log(Log.DEBUG, logTag, "Search speech media thread finish.");
            }
        };
        Thread t=new Thread(r);
        t.start();
    }

    @NonNull
    public static String formatDuration(int num) {
        int minute = num / 1000 / 60;
        int second = (num - minute * 60 * 1000) / 1000;
        if (second < 10) {
            return String.valueOf(minute) + ":0" + String.valueOf(second);
        } else {
            return String.valueOf(minute) + ":" + String.valueOf(second);
        }
    }

    @Override
    public void onPlaybackStateChanged(final boolean isPlaying) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                } else {
                    playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                }
            }
        });

    }

    @Override
    public void onCurrentFilePositionChanged(final int newPosition) {
        Crashlytics.log(Log.DEBUG,logTag,"Play index changed, Move list view to "+newPosition+"("+SpeechData.getSubtitleName(newPosition)+")");
        currentPlayIndex=newPosition;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listView.setSelectionFromTop(newPosition,0);
                adapter.setSelectedIndex(newPosition);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.mainactivity_prev_button:
                if (service != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                        service.onPrevButtonClicked();
                        }
                    }).start();
                    Crashlytics.log(Log.DEBUG, logTag,"Event from Activity UI: PREV button clicked.");
                    Util.fireSelectEvent(mFirebaseAnalytics,logTag,Util.BUTTON_CLICK,"PREV_IN_PLAY_BGM_ACTIVITY");
                }
                break;
            case R.id.mainactivity_play_button:
                if (service != null)
                    service.playPausePlayback();
                Crashlytics.log(Log.DEBUG, logTag,"Event from Activity UI: PLAY/PAUSE button clicked.");
                Util.fireSelectEvent(mFirebaseAnalytics,logTag,Util.BUTTON_CLICK,"PLAY/PAUSE_IN_PLAY_BGM_ACTIVITY");
                break;
            case R.id.mainactivity_next_button:
                if (service != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                        service.playNextMedia(false);
                        }
                    }).start();
                    Crashlytics.log(Log.DEBUG, logTag,"Event from Activity UI: NEXT button clicked.");
                    Util.fireSelectEvent(mFirebaseAnalytics,logTag,Util.BUTTON_CLICK,"NEXT_IN_PLAY_BGM_ACTIVITY");
                }
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (service != null)
                service.seekSong(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }


}