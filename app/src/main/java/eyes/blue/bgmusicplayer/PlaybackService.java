package eyes.blue.bgmusicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.util.ArrayList;

import eyes.blue.FileSysManager;
import eyes.blue.R;
import eyes.blue.SpeechData;
import eyes.blue.Util;

public class PlaybackService extends Service implements MediaPlayer.OnCompletionListener {
    public final int REQ_AUDIO_FOCUS_FAIL =1;
    public final int DISK_SPACE_NOT_ENOUGH =2;
    public final int CREATE_MEDIA_PLAYER_FAIL =3;

    public interface PlaybackListener {
        void onSeekUpdate(String filename, int currentPosition, int duration);

        void onPlaybackStateChanged(boolean isPlaying);

        void onCurrentFilePositionChanged(int newPosition);
    }

    class MediaFile {
        private String path;
        private boolean toBePlayed;

        public MediaFile(String path, boolean toBePlayed) {
            this.path = path;
            this.toBePlayed = toBePlayed;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public boolean isToBePlayed() {
            return toBePlayed;
        }

        public void setToBePlayed(boolean toBePlayed) {
            this.toBePlayed = toBePlayed;
        }
    }


    private PlaybackListener listener;
    private final IBinder binder = new LocalBinder();
    private PowerManager.WakeLock wakeLock;

    //private TelephonyManager telephonyManager;
    //private BroadcastReceiver telephonyStateManager;
    private NotificationManager notificationManager;
    private RemoteViews notificationView;
    private Notification.Builder notification;
    private int NOTIFICATION_ID = 123;
    private SharedPreferences preferences;

    AudioManager audioManager;
    AudioAttributes playbackAttributes;
    AudioFocusRequest focusRequest;

    private MediaPlayer mediaPlayer;
    private ArrayList<String> filesToPlay;
    //private ArrayList<MediaFile> filesToPlay2 = new ArrayList<>();
    private int currentFilePosition;
    private boolean isRepeatOne = true;
    private String logTag=getClass().getName();
    private Context context;
    private long lastPrevBtnClickTime;
    private int lastPrevBtnDelayTime=1500;
    FirebaseAnalytics mFirebaseAnalytics;


    public class LocalBinder extends Binder {
        PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        context=this;
        mFirebaseAnalytics=FirebaseAnalytics.getInstance(this);
        preferences = getSharedPreferences(getString(R.string.SHARED_MAINKEY), 0);
        isRepeatOne =preferences.getBoolean(getString(R.string.SHARED_IS_ONE_SONG_REPEATING),false);
        Crashlytics.log(Log.DEBUG, logTag, "Load repeat single state: "+ isRepeatOne);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        notification = new Notification.Builder(this);
        if (android.os.Build.VERSION.SDK_INT <= 23)
            notificationView = new RemoteViews(getApplication().getPackageName(), R.layout.bgmusic_player_notification_layout16);
        else
            notificationView = new RemoteViews(getApplication().getPackageName(), R.layout.bgmusic_player_notification_layout);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = "PlaybackService";
            CharSequence channelName = "LamrimReader";
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            Crashlytics.log(Log.DEBUG,logTag, "Channel name: LamrimReader");
            channel.setDescription("Background playback service of LamrimReader");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.setVibrationPattern(new long[]{0});
            channel.setSound(null, null);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
            notification.setChannelId(channel.getId());
            notification.setStyle(new Notification.BigTextStyle().bigText(getString(R.string.bgplayer_notification_long_text)));
        }
        notification.setContent(notificationView);
        notification.setSmallIcon(android.R.drawable.ic_media_pause);

        //Intent intent = new Intent(getApplication(), MainActivity.class);
        // intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Intent openactivityIntent = new Intent(this, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(this, 0, openactivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationView.setOnClickPendingIntent(R.id.notification_leftmost_icon, openPendingIntent);

        Intent pauseIntent = new Intent(this, NotificationIntentService.class);
        pauseIntent.setAction(getString(R.string.NOTIFICATION_ACTION_PAUSEPLAY));
        PendingIntent pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, 0);
        notificationView.setOnClickPendingIntent(R.id.notification_pause, pausePendingIntent);

        Intent rewindIntent = new Intent(this, NotificationIntentService.class);
        rewindIntent.setAction(getString(R.string.NOTIFICATION_ACTION_PREV));
        PendingIntent rewindPendingIntent = PendingIntent.getService(this, 0, rewindIntent, 0);
        notificationView.setOnClickPendingIntent(R.id.notification_rewind, rewindPendingIntent);

        Intent ffIntent = new Intent(this, NotificationIntentService.class);
        ffIntent.setAction(getString(R.string.NOTIFICATION_ACTION_NEXT));
        PendingIntent ffPendingIntent = PendingIntent.getService(this, 0, ffIntent, 0);
        notificationView.setOnClickPendingIntent(R.id.notif_ff, ffPendingIntent);

        Intent exitIntent = new Intent(this, NotificationIntentService.class);
        exitIntent.setAction(getString(R.string.NOTIFICATION_ACTION_QUIT));
        //PendingIntent exitPendingIntent = PendingIntent.getActivity(this, 0, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //notificationView.setOnClickPendingIntent(R.id.notif_exit, exitPendingIntent);
        PendingIntent exitPendingIntent = PendingIntent.getService(this, 0, exitIntent, 0);
        notificationView.setOnClickPendingIntent(R.id.notif_exit, exitPendingIntent);

        startForeground(NOTIFICATION_ID, notification.build());

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ":LamrimReaderBackgroundPlay");
        Crashlytics.log(Log.DEBUG,logTag, "Get wake lock.");
        wakeLock.acquire();

        /*
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    if (mediaPlayer != null) {
                        mediaPlayer.pause();
                        transferPlaybackState(false);
                    }
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);

        telephonyStateManager = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)
                        || intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)
                        || intent.getAction().equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                    if (mediaPlayer != null) {
                        mediaPlayer.pause();
                        transferPlaybackState(false);
                    }
                }
            }
        };


        IntentFilter telephonyStateIntents = new IntentFilter();
        telephonyStateIntents.addAction(Intent.ACTION_HEADSET_PLUG);
        telephonyStateIntents.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        telephonyStateIntents.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(telephonyStateManager, telephonyStateIntents);

         */
    }

    /*
    * Notification 與鎖定畫面的控制介面會從 onStartCommand 送命令進來。
    * */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Crashlytics.log(Log.DEBUG,logTag, "Into onStartCommand()");
        if (intent != null && intent.getAction() != null && intent.getAction().equals(getString(R.string.NORMAL_START_INTENT_ACTION))) {
            Crashlytics.log(Log.DEBUG, logTag,"Event from Notification: Normal start intent, load last process point.");
            Util.fireSelectEvent(mFirebaseAnalytics,logTag,Util.BUTTON_CLICK,"PLAY/PAUSE_ON_Notification");
            int index=preferences.getInt("MEDIA_INDEX", 0);
            int pos=preferences.getInt("PLAY_TIME", 0);
            currentFilePosition = index;

            if(playLamrimIndex(context,index,pos)!=0)return START_STICKY;

        }
        if (notification != null) {
            startForeground(NOTIFICATION_ID, notification.build());
        }
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(getString(R.string.NOTIFICATION_ACTION_PAUSEPLAY))) {
                this.playPausePlayback();
                Crashlytics.log(Log.DEBUG, logTag,"Event from Notification: Play/Pause button clicked.");
                Util.fireSelectEvent(mFirebaseAnalytics,logTag,Util.BUTTON_CLICK,"PLAY/PAUSE_ON_Notification");
            } else if (intent.getAction().equals(getString(R.string.NOTIFICATION_ACTION_NEXT))) {
                this.playNextMedia(false);
                Crashlytics.log(Log.DEBUG, logTag,"Event from Notification: NEXT button clicked.");
                Util.fireSelectEvent(mFirebaseAnalytics,logTag,Util.BUTTON_CLICK,"NEXT_ON_Notification");
            } else if (intent.getAction().equals(getString(R.string.NOTIFICATION_ACTION_PREV))) {
                this.onPrevButtonClicked();
                Crashlytics.log(Log.DEBUG, logTag,"Event from Notification: PREV button clicked.");
                Util.fireSelectEvent(mFirebaseAnalytics,logTag,Util.BUTTON_CLICK,"PREV_ON_Notification");
            } else if (intent.getAction().equals(getString(R.string.NOTIFICATION_ACTION_QUIT))) {
                transferPlaybackState(false);
                if(mediaPlayer!=null){
                    SharedPreferences.Editor editor=preferences.edit();
                    editor.putInt("MEDIA_INDEX", currentFilePosition);
                    editor.putInt("PLAY_TIME", mediaPlayer.getCurrentPosition());
                    editor.apply();
                    Crashlytics.log(Log.DEBUG,logTag,"Save MediaIndex="+currentFilePosition+", PlayTimePosition="+mediaPlayer.getCurrentPosition()+" before close service.");
                }
                Crashlytics.log(Log.DEBUG, logTag,"Event from Notification: EXIT button clicked.");
                Util.fireSelectEvent(mFirebaseAnalytics,logTag,Util.BUTTON_CLICK,"EXIT_ON_Notification");
                stopSelf();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock.isHeld()) {
            Crashlytics.log(Log.DEBUG,logTag, "Wake lock release.");
            wakeLock.release();
        }
        notificationManager.cancelAll();

        if(myUpdateInfoThread !=null)
            myUpdateInfoThread.setStop();

        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
        }
        mediaPlayer = null;
        quitAudioFocus();
        //unregisterReceiver(telephonyStateManager);
    }

    private int playLamrimIndex(Context c, int index, int timePos) {
        currentFilePosition=index;
        Crashlytics.log(Log.DEBUG, logTag,"Play Lamim Index: "+index+", time: "+MainActivity.formatDuration(timePos));
        if (mediaPlayer != null) {
            Crashlytics.log(Log.DEBUG, logTag,"Release MediaPlayer");
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
        }

        if(!requestAudioFocus()) {
            Toast.makeText(context, "音效裝置佔用中，向系統取得播放音效失敗！", Toast.LENGTH_LONG).show();
            return 1;   // 要求 Audio Focus 失敗。
        }

        mediaPlayer = null;
        final File mediaFile=new FileSysManager(c).getLocalMediaFile(index);
        if(mediaFile==null) {
            Toast.makeText(context, "檔案不存在且儲存空間不足！", Toast.LENGTH_LONG).show();
            return 2;   // 檔案不存在，且磁碟空間不足。
        }
        mediaPlayer = MediaPlayer.create(this, Uri.fromFile(mediaFile));
        if(mediaPlayer == null){
            Toast.makeText(context, "無法開啟播放器，"+SpeechData.getSubtitleName(index)+"音檔可能已損毀！", Toast.LENGTH_LONG).show();
            return 3;   // 當檔案壞掉時可能會發生。
        }
        //mediaPlayer = MediaPlayer.create(this, Uri.parse(filesToPlay2.get(currentFilePosition).getPath()));
        mediaPlayer.setOnPreparedListener(getOnPreparedListener(timePos)); // My Code
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.start();
        transferPlaybackState(true);

        if (listener != null) {
            listener.onCurrentFilePositionChanged(index);
        }

        notification.setSmallIcon(android.R.drawable.ic_media_play);
        notificationView.setTextViewText(R.id.title, SpeechData.getSubtitleName(index));
        notificationView.setTextViewText(R.id.subtitle, getResources().getStringArray(R.array.desc)[index]);
        //notificationView.setTextViewText(R.id.notification_text, FileItem.getFilenameFromPath(filesToPlay2.get(currentFilePosition).getPath()));
        notification.setContent(notificationView);
        notificationManager.notify(NOTIFICATION_ID, notification.build());

        //String filename = filesToPlay.get(currentFilePosition).substring(filesToPlay.get(currentFilePosition).lastIndexOf("/") + 1);
        //String filename = filesToPlay2.get(currentFilePosition).getPath().substring(filesToPlay2.get(currentFilePosition).getPath().lastIndexOf("/") + 1);

        //if (Looper.myLooper() == null)
        //    Looper.prepare();

        //Toast.makeText(PlaybackService.this, mediaFile.getName(), Toast.LENGTH_SHORT).show();

        //Looper.loop();

        if(myUpdateInfoThread !=null) myUpdateInfoThread.setStop();
        Crashlytics.log(Log.DEBUG,logTag, "Start Update UI Thread.");
        myUpdateInfoThread = new MyUpdateInfoRunnable();
        myUpdateInfoThread.start();
        return 0;
    }

    /* My code.*/
    private MediaPlayer.OnPreparedListener getOnPreparedListener(final int timePos) {
        return new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.seekTo(timePos);
            }
        };
    }

    private void transferPlaybackState(boolean isPlaying) {
        if (listener != null) {
            listener.onPlaybackStateChanged(isPlaying);
        }
    }


    public void playPausePlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            pause();
        else if (mediaPlayer != null) {
            playFromPause();
        } else {
            Crashlytics.log(Log.DEBUG,logTag,"Play/Pause: MediaPlayer not exist create and play speech.");
            SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.SHARED_MAINKEY), 0);
            int index=sharedPreferences.getInt("MEDIA_INDEX", 0);
            int time=sharedPreferences.getInt("PLAY_TIME", 0);
            jumpToLamrimIndex(context, index,time);
            startService(new Intent(this, PlaybackService.class));
            changeRepeatState(!sharedPreferences.getBoolean(getString(R.string.SHARED_IS_ONE_SONG_REPEATING), false));
        }
    }

    private void playFromPause(){
        Crashlytics.log(Log.DEBUG,logTag,"Play/Pause: MediaPlayer exist and not playing, play speech.");
        if(!requestAudioFocus())
           return;

        if(mediaPlayer!=null)mediaPlayer.start();
        if(myUpdateInfoThread !=null) myUpdateInfoThread.setStop();
        myUpdateInfoThread =new MyUpdateInfoRunnable();
        myUpdateInfoThread.start();
        transferPlaybackState(true);
        notificationView.setImageViewResource(R.id.notification_pause, android.R.drawable.ic_media_pause);
        notificationManager.notify(NOTIFICATION_ID, notification.build());
    }

    private void pause(){
        Crashlytics.log(Log.DEBUG,logTag,"Play/Pause: MediaPlayer exist and playing, pause play.");
        if(mediaPlayer!=null)mediaPlayer.pause();
        if(myUpdateInfoThread !=null) myUpdateInfoThread.setStop();
        transferPlaybackState(false);
        notification.setSmallIcon(android.R.drawable.ic_media_pause);
        notificationView.setImageViewResource(R.id.notification_pause, android.R.drawable.ic_media_play);
        notificationManager.notify(NOTIFICATION_ID, notification.build());
    }

    public void jumpToLamrimIndex(Context c, int index, int timePos){
        playLamrimIndex(c, index,timePos);
    }

    public void changeRepeatState(boolean newState) {
        Crashlytics.log(Log.DEBUG, logTag,"Set repeat for "+((isRepeatOne)?"ALL.":"SINGLE."));
        isRepeatOne = newState;
    }

    public void setPlaybackListener(PlaybackListener listener) {
        this.listener = listener;
    }


    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Crashlytics.log(Log.DEBUG,logTag, SpeechData.getSubtitleName(currentFilePosition)+" play finish, switch to next media, isRepeatOne="+ isRepeatOne);
        playNextMedia(true);
        Crashlytics.log(Log.DEBUG,logTag, "Start play "+SpeechData.getSubtitleName(currentFilePosition));
    }

    public void seekSong(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    /*
    * 若上一首的按鈕被按下，先確認是否是連續按二下，若是連續按二下則播放上一首，若是第一次按下
    * 則回到原本播放的音檔的最初位置。
    * */
    public void onPrevButtonClicked(){
        long time=System.currentTimeMillis();
        long diff=time-lastPrevBtnClickTime;
        lastPrevBtnClickTime=time;

        if(diff<lastPrevBtnDelayTime)
            playPrevMedia();
        else {
            if(playLamrimIndex(PlaybackService.this, currentFilePosition, 0)!=0) return;
        }
        updateMediaInfoUI();
    }

    private void playPrevMedia(){
        if(myUpdateInfoThread !=null) myUpdateInfoThread.setStop();

        int index=getPrevMedia(currentFilePosition);
        if(index == -1){
            Toast.makeText(PlaybackService.this,"廣論App背景播放器：所有音檔皆已被刪除，無法繼續播放。",Toast.LENGTH_LONG).show();
            Crashlytics.log(Log.ERROR, logTag, "No media file exist after playback onCompletion, skip play.");
            return;
        }
        currentFilePosition=index;

        playLamrimIndex(PlaybackService.this, currentFilePosition, 0);
    }

    /* 以 currentFilePosition 為目前位置，播放下一個音檔, 此函式根據 isCheckRepeatAll 變數決定是否參考 repeatAll 變數，若使用者按下下一首按鈕時，不應該參考repeatAll變數，
       repeatAll變數應該只有結束播放時需要參考，onCompletion() 與 nextButton 都會呼叫這個函式。
    */
    public void playNextMedia(boolean isRefRepeatOne){
        if(myUpdateInfoThread !=null) myUpdateInfoThread.setStop();
        if (isRefRepeatOne && isRepeatOne) {
            Crashlytics.log(Log.DEBUG, logTag, "playNextMedia(): Repeat play "+SpeechData.getSubtitleName(currentFilePosition));
            File mediaFile = new FileSysManager(context).getLocalMediaFile(currentFilePosition);
            if (!mediaFile.exists()) {
                Toast.makeText(PlaybackService.this,"廣論App背景播放器：音檔 "+SpeechData.getSubtitleName(currentFilePosition)+" 已被刪除，無法再次播放。",Toast.LENGTH_LONG).show();
                Crashlytics.log(Log.ERROR, logTag, "playNextMedia(): "+SpeechData.getSubtitleName(currentFilePosition)+" file not exist can't play, skip play.");
                return;
            }
        }
        else {
            Crashlytics.log(Log.DEBUG, logTag, "playNextMedia(): Play next media of "+SpeechData.getSubtitleName(currentFilePosition));
            int index=getNextMedia(currentFilePosition);
            if(index == -1){
                Toast.makeText(PlaybackService.this,"廣論App背景播放器：所有音檔皆已被刪除，無法繼續播放下一音檔。",Toast.LENGTH_LONG).show();
                Crashlytics.log(Log.ERROR, logTag, "playNextMedia(): No media file exist, skip play.");
                return;
            }
            Crashlytics.log(Log.DEBUG, logTag, "playNextMedia(): "+SpeechData.getSubtitleName(currentFilePosition)+" ===> "+SpeechData.getSubtitleName(index));
            currentFilePosition=index;
        }

        if(playLamrimIndex(PlaybackService.this, currentFilePosition, 0)!=0)return;
        updateMediaInfoUI();
    }

    /*
     * 從 lastPlay 的編號開始向前(lastPlay-1)尋找存在的音檔並回應其編號，若到最初的音檔(index=0)都沒有則從最尾(index=319)開始找，
     * 直到目前播放的(lastPlay)音檔都沒有則回傳-1，代表連lastPlay目前播放的音檔都不存在了。
     * */
    private int getPrevMedia(int lastPlay){
        Crashlytics.log(Log.DEBUG, logTag, "Find prev exist media from: "+lastPlay);
        int index=lastPlay;
        FileSysManager fsm=new FileSysManager(context);
        for(int i=0;i<SpeechData.name.length;i++){
            index--;
            if(index==-1)
                index=SpeechData.name.length-1;
            File file=fsm.getLocalMediaFile(index);
            Crashlytics.log(Log.DEBUG, logTag, "Check: "+SpeechData.getSubtitleName(index)+((file.exists())?" exist.":" not exist."));
            if(file.exists())return index;
        }
        return -1;
    }

    /*
    * 從 lastPlay 的編號開始向下(lastPlay+1)尋找存在的音檔並回應其編號，若到最後一號(320)都沒有則從頭(0)開始找，
    * 直到目前播放的(lastPlay)音檔都沒有則回傳-1，代表連lastPlay目前播放的音檔都不存在了。
    * */
    private int getNextMedia(int lastPlay){
        Crashlytics.log(Log.DEBUG, logTag, "Find next exist media from: "+lastPlay);
        int index=lastPlay;
        FileSysManager fsm=new FileSysManager(context);
        for(int i=0;i<SpeechData.name.length;i++){
            index++;
            if(index==SpeechData.name.length)
                index=0;
            File file=fsm.getLocalMediaFile(index);
            if(file==null)continue;
            Crashlytics.log(Log.DEBUG, logTag, "Check: "+SpeechData.getSubtitleName(index)+((file.exists())?" exist.":" not exist."));
            if(file.exists())return index;
        }
        return -1;
    }

    private void updateMediaInfoUI() {
        if (listener != null && mediaPlayer != null) {
            //Log.d(logTag, "Update info for "+SpeechData.getSubtitleName(currentFilePosition)+" "+ Util.getMsToHMS(mediaPlayer.getCurrentPosition()));
            try {
                listener.onSeekUpdate(SpeechData.getSubtitleName(currentFilePosition) + " - " + getResources().getStringArray(R.array.desc)[currentFilePosition], mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
            }catch(NullPointerException npe){
                npe.printStackTrace();
                Crashlytics.log(Log.ERROR, logTag,"NullPointerException happen while update UI for media information.");
            }
        }
    }

    private String[] getMediaInfo(int index) {
        File mediaFile = new FileSysManager(context).getLocalMediaFile(index);
        return getMediaInfo(mediaFile.getName());
    }
    /*
    * 傳入音檔的檔名，此函式從檔名中擷取資訊，並回傳為ret{title, subtitle}。
    * */
    private String[] getMediaInfo(String name){
        String[] ele=name.split("-");
        String[] ret=new String[2];
        ret[0]=ele[1];
        ret[1]="廣論第";
        if(!ele[2].contains("~"))ret[1]=ele[2];
        else {
            String[] sp=ele[2].split("~");
            String[] pl=sp[0].replace("P","").split("L");
            ret[1]+=Integer.parseInt(pl[0])+"頁"+Integer.parseInt(pl[1])+"行 ~ 第";
            pl=sp[1].replace("P","").split("L");
            pl[1]=pl[1].replace(".mp3","");
            ret[1]+=Integer.parseInt(pl[0])+"頁"+Integer.parseInt(pl[1])+"行";
        }
        return ret;
    }

    private boolean requestAudioFocus(){
        int reqFocus=audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if(reqFocus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            Toast.makeText(PlaybackService.this,"廣論App背景播放無法取得Audio Focus，無法播放，請重新啟動背景播放。",Toast.LENGTH_LONG);
            Crashlytics.log(Log.ERROR,logTag,"Can't get Audio Focus, skip play.");
            return false;
        }
        Crashlytics.log(Log.DEBUG,logTag,"Get Audio Focus success!");
        return true;
    }

    public void quitAudioFocus(){
        Crashlytics.log(Log.DEBUG,logTag,"Quit Audio Focus.");
        if(audioManager != null && audioFocusChangeListener != null)audioManager.abandonAudioFocus(audioFocusChangeListener);
    }

    MyUpdateInfoRunnable myUpdateInfoThread = null;
    class MyUpdateInfoRunnable extends Thread {
        boolean isStart=true;
        public void setStop(){
            Crashlytics.log(Log.DEBUG,logTag, "Stop Update UI Thread.");
            isStart=false;
        }
        @Override
        public void run() {
            Crashlytics.log(Log.DEBUG,logTag, "Update UI Thread start.");
            while(isStart) {
                updateMediaInfoUI();
                int time=0;
                if(mediaPlayer!=null && mediaPlayer.isPlaying())
                    time=mediaPlayer.getCurrentPosition();
                else
                    Crashlytics.log(Log.DEBUG, logTag,"MediaPlayer="+mediaPlayer+", isPlaying()="+((mediaPlayer!=null)?mediaPlayer.isPlaying():"NULL"));
                time%=1000;
                try { Thread.sleep(1000-time); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
            Crashlytics.log(Log.DEBUG,logTag,"Update UI Thread terminated.");
        }
    }

    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            switch(focusChange){
                case AudioManager.AUDIOFOCUS_GAIN:
                    // 實測於 Android 9,無法記錄最後的播放器狀態，當電話來時，系統先行停止MediaPlayer的播放，才呼叫onAudioFocusChange()
                    // 所以每次 lost focus 時查詢 MediaPlayer 的狀態必為暫停狀態，所以取得 Focus 後無法還原播放器的狀態。
                    Crashlytics.log(Log.DEBUG,logTag,"Get Audio focus.");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    Crashlytics.log(Log.DEBUG,logTag,"Lost Audio focus forever, pause playback.");
                    pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    Crashlytics.log(Log.DEBUG,logTag,"Lost Audio focus temporarily, pause playback.");
                    pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    Crashlytics.log(Log.DEBUG,logTag,"Lost Audio focus temporarily(can duck), pause playback.");
                    pause();
                    break;
            }
        }
    };
}
