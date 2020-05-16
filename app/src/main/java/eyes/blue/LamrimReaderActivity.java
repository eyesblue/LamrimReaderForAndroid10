package eyes.blue;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.annotation.IdRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GestureDetectorCompat;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import afzkl.development.colorpickerview.dialog.ColorPickerDialog;
import afzkl.development.colorpickerview.view.ColorPickerView;
import eyes.blue.modified.MyListView;
import eyes.blue.modified.OnDoubleTapEventListener;

import static eyes.blue.MediaPlayerController.MP_PREPARED;

//import com.winsontan520.wversionmanager.library.WVersionManager;

/**
 * 更新: $$Date: 2013-12-29 12:01:44 +0800 (Sun, 29 Dec 2013) $$ 作者: $$Author:
 * kingofeyesblue@gmail.com $$ 版本: $$Revision: 111 $$ ID ：$$Id:
 * LamrimReaderActivity.java 111 2013-12-29 04:01:44Z kingofeyesblue@gmail.com
 * $$
 */
public class LamrimReaderActivity extends AppCompatActivity {
    /**
     * Called when the activity is first created.
     */
    private static final long serialVersionUID = 4L;
    final String logTag = getClass().getName();
    final static String funcInto = "Function Into";
    final static String funcLeave = "Function Leave";

    final static int STORAGE_ACCESS_PERMISSION_REQUEST = 1;

    final static int SPEECH_MENU_RESULT = 0;
    final static int THEORY_MENU_RESULT = 1;
    final static int SPEECH_MENU_RESULT_REGION = 2;
    final static int GLOBAL_LAMRIM_RESULT = 3;
    final static int SELECT_FG_PIC_RESULT = 4;
    final static int SUBTITLE_MODE = 1;
    final static int READING_MODE = 2;

    final static int SPEECH_PLAY_MODE = 0;
    final static int REGION_PLAY_MODE = 1;
    final static int GL_PLAY_MODE = 2;

    static int textDefSize, textMinSize, textMaxSize;
    int subtitleViewRenderMode = SUBTITLE_MODE;
    int playMode = -1;
    static int mediaIndex = -1, subtitleIndex = 0;// subtitleIndex=目前正在播放中的字幕index
    MediaPlayerController mpController;
    //	private PowerManager powerManager = null;
    private PowerManager.WakeLock wakeLock = null;
    static int screenOnTime = 0;
    MyListView bookView = null;
    ImageView renderView = null;
    TextView subtitleView = null;
    static TextView infoTextView;
    SharedPreferences runtime = null, playRecord = null;
    SharedPreferences.Editor runtimeEditor;
    MenuItem rootMenuItem, options, speechMenu, globalLamrim, setRegion, playRegionRec, swRenderMode, prjWeb, exitApp;

    FileSysManager fsm = null;
    RelativeLayout rootLayout = null;

    public static Typeface defFont = null, bktFont = null;

    // the 3 object is paste on the popupwindow object, it not initial at
    // startup.
    SimpleAdapter regionRecordAdapter = null;
    ArrayList<HashMap<String, String>> regionFakeList = null;
    ListView regionListView = null;

    HashMap<String, String> fakeSample = new HashMap();
    PackageInfo pkgInfo = null;

    View actionBarControlPanel = null;
    //    ImageView bookIcon = null;
    EditText jumpPage = null;
    SeekBar volumeController = null;
    ImageButton textSize = null;
    ImageButton search = null;

    int[][] readingModeSEindex = null;
    String readingModeAllSubtitle = null;
    static Point screenDim = new Point();
    static Button modeSwBtn = null;
    GlRecord glRecord = null;
    Object bookViewMountPointKey = new Object();
    int[] bookViewMountPoint = {0, 0};

    int theoryHighlightRegion[] = new int[4];//{startPage, startLine, endPage, endLine}
    int[][] GLamrimSect = new int[2][3];
    int GLamrimSectIndex = -1;
    String actionBarTitle = "";
    String regionStartInfo, regionEndInfo;

    PrevNextListener prevNextListener = null;
    final int[] regionSet = {-1, -1, -1, -1};

    final ImageView.ScaleType scaleType[] = {ImageView.ScaleType.CENTER_CROP, ImageView.ScaleType.CENTER_INSIDE, ImageView.ScaleType.FIT_XY, ImageView.ScaleType.FIT_START, ImageView.ScaleType.FIT_CENTER, ImageView.ScaleType.FIT_END, ImageView.ScaleType.CENTER, ImageView.ScaleType.MATRIX};
    //WVersionManager versionManager = null;

    int bookMap[][] = null;
    public Boolean isActivityLoaded = Boolean.valueOf(false);

    // =================== For search view =====================
    AlertDialog searchDialog;
    SubtitleSearch[] subtitleSearch = new SubtitleSearch[320]; // The variable hold all subtitle for search.
    TextView subtitleSearchHeaderTextView = null; // We can't get the reference back if the view add to listview, must hold it on global.
    ArrayList<SubtitleSearchIndex> subtitleSearchResult = new ArrayList<>();
    //SubtitleSearchAdapter subtitleSearchAdapter = null;
    //ImageButton searchLastBtn = null, searchNextBtn = null;
    //EditText searchInput = null;
    //ListView subtitleSearchList = null;
    //boolean isSearchLamrim = true;
    // =========================================================
    long appStartTimeMs = -1;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        // try{
        super.onCreate(savedInstanceState);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Fabric.with(this, new Crashlytics());
///		requestWindowFeature(Window.FEATURE_NO_TITLE);
//		requestWindowFeature(com.actionbarsherlock.view.Window.FEATURE_ACTION_BAR_OVERLAY);

        appStartTimeMs = System.currentTimeMillis();
        setContentView(R.layout.main);

        Crashlytics.log(Log.DEBUG, logTag, "set content view finish");
        Crashlytics.log(Log.DEBUG, logTag,"=====================set content view finish===================");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "LamrimReader");
        screenOnTime = getResources().getInteger(R.integer.screenOnTime);

        fsm = new FileSysManager(this);
        fsm.checkFileStructure();

        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        runtime = getSharedPreferences(getString(R.string.runtimeStateFile), 0);
        runtimeEditor = runtime.edit();

        int mode = runtime.getInt(getString(R.string.playModeKey), -1);
        switch (mode) {
            case SPEECH_PLAY_MODE:
                playRecord = getSharedPreferences(getString(R.string.speechModeRecordFile), 0);
                break;
            case REGION_PLAY_MODE:
                playRecord = getSharedPreferences(getString(R.string.regionPlayModeRecordFile), 0);
                break;
            case GL_PLAY_MODE:
                playRecord = getSharedPreferences(getString(R.string.GLModeRecordFile), 0);
                break;
        }

        Log.d(funcInto, "******* Into LamrimReader.onCreate *******");

        /*
        // Check new version
        versionManager = new WVersionManager(LamrimReaderActivity.this);
        versionManager.setTitle(getString(R.string.msgNewVerHasRes));
        versionManager.setUpdateNowLabel(getString(R.string.msgUpdateNow));
        versionManager.setRemindMeLaterLabel(getString(R.string.msgRemindMe));
        versionManager.setIgnoreThisVersionLabel(getString(R.string.msgSkipTheVer));
        versionManager.setReminderTimer(10);
        versionManager.setVersionContentUrl(getString(R.string.versionCheckUrl)); // your update content url, see the response format below
        versionManager.setUpdateUrl(getString(R.string.updateWebPage));
        versionManager.checkVersion();
*/

        if (savedInstanceState != null) Crashlytics.log(Log.DEBUG, logTag, "The savedInstanceState is not null!");
        Crashlytics.log(Log.DEBUG, logTag, "mediaIndex=" + mediaIndex);
//		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, logTag);
//		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        rootLayout = (RelativeLayout) findViewById(R.id.rootLayout);

        try {
            pkgInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e3) {
            e3.printStackTrace();
        }

        try {
            getWindowManager().getDefaultDisplay().getSize(screenDim);
        } catch (NoSuchMethodError ignore) { // Older device
            screenDim.x = getWindowManager().getDefaultDisplay().getWidth();
            screenDim.y = getWindowManager().getDefaultDisplay().getHeight();
        }
        // The value will get portrait but not landscape value sometimes,
        // exchange it if happen.
        if (screenDim.x < screenDim.y)
            screenDim.set(screenDim.y, screenDim.x);

        textDefSize = (int) ((TextView) findViewById(R.id.subtitleView)).getTextSize();
        textMinSize = getResources().getInteger(R.integer.textMinSize);
        textMaxSize = getResources().getInteger(R.integer.textMaxSize);
        Crashlytics.log(Log.DEBUG, logTag, "Get font size: max=" + textMaxSize + ", def=" + textDefSize + ", min=" + textMinSize);

        modeSwBtn = (Button) findViewById(R.id.modeSwBtn);
        modeSwBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    modeSwBtn.setPressed(false);

                    if(subtitleViewRenderMode == READING_MODE && mpController != null && mpController.getMediaPlayerState() >= MediaPlayerController.MP_PREPARED) {
                        int index = mpController.getSubtitleIndex(mpController.getCurrentPosition());
                        if (index == -1)
                            return true;
                        // subtitleView.bringPointIntoView(readingModeSEindex[index][0]);
                        try {
                            // *************** Bug here **************
                            // Here will happen error while readingModeSEindex array under construct, but access fire by user at above line.
                            int line = subtitleView.getLayout().getLineForOffset(readingModeSEindex[index][0]);
                            subtitleView.scrollTo(subtitleView.getScrollX(), subtitleView.getLineBounds(line, null) - subtitleView.getLineHeight());
                            mpController.seekTo(mpController.getCurrentPosition());
                        } catch (Exception et) {
                            et.printStackTrace();
                            Crashlytics.log(Log.DEBUG, logTag, "ReadingModeSEindex been access before construct finish.");
                            Crashlytics.logException(et);
                        }
                    }

                    return true;
                }

                modeSwBtn.setPressed(true);
                int height = (int) (screenDim.y - event.getRawY());
//				float upBoundDp = (float) getResources().getInteger(R.integer.subtitleScrollTouchBtnHeightPercentDp) / 100 * screenDim.y;
                int minHeight = (int) subtitleView.getLineHeight();
                //int maxHeight = (int) (rootLayout.getHeight() - upBoundDp);
                int maxHeight = (int) (rootLayout.getHeight() - modeSwBtn.getHeight());

                // Fix the modeSwBtn over the view while show controller view.
                if (mpController.getControllerView().isShown()) {
                    height = height - mpController.getControllerView().getHeight();
                    maxHeight = maxHeight - mpController.getControllerView().getHeight();
                }

                // int maxHeight=(int)
                // (rootLayout.getHeight()-getResources().getDisplayMetrics().density*getResources().getInteger(R.integer.subtitleScrollTouchUpperBoundDp));

                // synchronized (mpController){
                // set Subtitle mode
                if (height <= minHeight) {
                    height = minHeight;
                    setSubtitleViewMode(SUBTITLE_MODE);
                    Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.BUTTON_CLICK, "SWITCH_TO_SUBTITLE_MODE");

                    if (mpController.getMediaPlayerState() == MediaPlayerController.MP_PLAYING && mpController.getSubtitle() != null) {
                        if (mpController.getCurrentPosition() == -1)
                            return true;
                        setSubtitleViewText(mpController.getSubtitle(mpController.getCurrentPosition()).text);
                    } else
                        setSubtitleViewText(getString(R.string.dlgHintShowMpController));
                }
                // set reading mode
                else {

                    // It is first time into reading mode, set the all text to
                    // subtitleView, but not set text every time.
                    if (subtitleViewRenderMode == SUBTITLE_MODE) {
                        if (mpController == null || !mpController.isSubtitleReady() || readingModeAllSubtitle == null) {
                            Util.showErrorToast(LamrimReaderActivity.this, getString(R.string.dlgHintLoadMediaBeforeSwitchToReadingMode));
                            return true;
                        }
                        setSubtitleViewMode(READING_MODE);
                        Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.BUTTON_CLICK, "SWITCH_TO_READING_MODE");
                    }
                }
                Crashlytics.log(Log.DEBUG, logTag, "Set height to: "+height);
                if (height > maxHeight)
                    height = maxHeight;

                subtitleView.setHeight(height);

                return true;
            }
        });

        // ============== Show info text view if enable =============
        infoTextView = (TextView) findViewById(R.id.infoTextView);
        boolean isShowInfoText = runtime.getBoolean(getString(R.string.isShowInfoTextViewKey), false);
        if (isShowInfoText) infoTextView.setVisibility(View.VISIBLE);
        else infoTextView.setVisibility(View.GONE);

        LayoutInflater factory = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        actionBarControlPanel = factory.inflate(R.layout.lamrimreader_actionbar_control_panel, null);
//        bookIcon = (ImageView) actionBarControlPanel.findViewById(R.id.bookIcon);
        /*
         * bookIcon.setOnClickListener(new View.OnClickListener(){
         *
         * @Override public void onClick(View v) { if(mediaIndex<0 ||
         * mediaIndex>=SpeechData.name.length)return; final int
         * pageNum=SpeechData.refPage[mediaIndex]-1; if(pageNum==-1)return;
         * //bookView.setItemChecked(pageNum, true); setTheoryArea(pageNum, 0);
         * Crashlytics.log(Log.DEBUG, logTag,"Jump to theory page index "+pageNum); //
         * adapter.notifyDataSetChanged(); }});
         */
        jumpPage = (EditText) actionBarControlPanel.findViewById(R.id.jumpPage);
        jumpPage.setGravity(Gravity.CENTER);
        jumpPage.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // event will be null in some version of Android, the function will trigger twice in single tap that cause incorrect action, it must check KeyEvent.ACTION_DOWN avoid the situation.
                if (event == null || event.getAction() != KeyEvent.ACTION_DOWN) return false;

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(jumpPage.getWindowToken(), 0);

                int num;
                String input = jumpPage.getText().toString().trim();
                if (input.length() == 0 || !input.matches("[0-9]+")) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            bookView.setSelectionFromTop(bookView.getFirstVisiblePosition(), 0);
                        }
                    }, 200);
                    return false;
                }
                num = Integer.parseInt(jumpPage.getText().toString());
                // =============== Check Special command =================
                if (num == 999) {
                    boolean isShowInfoText = !runtime.getBoolean(getString(R.string.isShowInfoTextViewKey), false);
                    runtimeEditor.putBoolean(getString(R.string.isShowInfoTextViewKey), isShowInfoText);
                    runtimeEditor.apply();
                    if (isShowInfoText) {
                        Crashlytics.log(Log.DEBUG, logTag, "Enable information text view");
                        infoTextView.setVisibility(View.VISIBLE);
                        Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.BUTTON_CLICK, "ENABLE_INFO_TEXT");
                    } else {
                        Crashlytics.log(Log.DEBUG, logTag, "Disable information text view");
                        infoTextView.setVisibility(View.GONE);
                        Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.BUTTON_CLICK, "DISABLE_INFO_TEXT");
                    }
                    runtimeEditor.putBoolean(getString(R.string.isShowInfoTextViewKey), isShowInfoText);
                    runtimeEditor.apply();
                    jumpPage.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            jumpPage.setText("" + (bookView.getFirstVisiblePosition() + 1));
                        }
                    }, 200);
                    return true;
                }

                // The number is not special number, do jump page.
                if (num > bookView.getCount())
                    num = bookView.getCount();
                else if (num < 1)
                    num = 1;

                final int pageNum = num - 1;

                bookView.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        bookView.setSelectionFromTop(pageNum, 0);
                        Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.BUTTON_CLICK, "JUMP_PAGE_" + pageNum);
                    }
                }, 200);
                // bookView.setItemChecked(num-1, true);
                // bookView.setSelection(pageNum);
                Crashlytics.log(Log.DEBUG, logTag, "Jump to theory page index " + (num - 1));
                // adapter.notifyDataSetChanged();
                return false;
            }
        });

        final ImageButton themeSwitcher = (ImageButton) actionBarControlPanel.findViewById(R.id.themeSwitcher);
        if(themeSwitcher != null) {
            boolean isDarkTheme = runtime.getBoolean(getString(R.string.isDarkThemeKey), true);
            if (!isDarkTheme) {
                themeSwitcher.setSelected(true);
            } else themeSwitcher.setSelected(false);
            themeSwitcher.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    boolean isDark_Theme = runtime.getBoolean(getString(R.string.isDarkThemeKey), true);
                    isDark_Theme = !isDark_Theme;
                    runtimeEditor.putBoolean(getString(R.string.isDarkThemeKey), isDark_Theme);
                    runtimeEditor.commit();

                    if (!isDark_Theme) {
                        themeSwitcher.setSelected(true);
                    } else themeSwitcher.setSelected(false);

                    // Destroy the adapter of BookView and reload parameter.
                    int bookPosition = bookView.getFirstVisiblePosition();
                    View v = bookView.getChildAt(0);
                    int bookShift = (v == null) ? 0 : v.getTop();
                    bookView.rebuildView();
//				int defTheoryTextSize = getResources().getInteger(R.integer.defFontSize);
                    final int theoryTextSize = runtime.getInt(getString(R.string.bookFontSizeKey), textDefSize);
                    bookView.setTextSize(theoryTextSize);
                    bookView.setSelectionFromTop(bookPosition, bookShift);
                    Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.BUTTON_CLICK, "SWITCH_TO_" + ((isDark_Theme) ? "DARK" : "LIGHT" + "_THEME"));
                }
            });
        }
        final ImageButton playBgm = (ImageButton) actionBarControlPanel.findViewById(R.id.playBgm);
        if(playBgm!=null)
            playBgm.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.BUTTON_CLICK, "PLAY_SPEECH_BACKGROUND");

                    int position = -1;
                    if (mediaIndex < 0 || mpController == null || !mpController.isPlayerReady() || (position = mpController.getCurrentPosition()) < 0) {
                        BaseDialogs.showSimpleErrorDialog(LamrimReaderActivity.this, getString(R.string.errUnknowPlayerState));
                        return;
                    }

                    BaseDialogs.showDialog(LamrimReaderActivity.this, getString(R.string.dlgBgPlayMode), getString(R.string.msgBgPlayDesc) + SpeechData.getNameId(mediaIndex) + " - " + Util.getMsToHMS(position, "分", "秒", false), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    saveRuntime();

                                    Intent intent = new Intent();
                                    intent.setAction(Intent.ACTION_VIEW);
                                    File file = fsm.getLocalMediaFile(mediaIndex);

                                    if (Build.VERSION.SDK_INT < 24)
                                        intent.setDataAndType(Uri.fromFile(file), "audio/*");
                                    else
                                        intent.setDataAndType(FileProvider.getUriForFile(LamrimReaderActivity.this, getApplicationContext().getPackageName() + ".provider", file), "audio/*"); // fix exposed beyond app through Intent.getData() @ 1.4.12

                                    if (intent.resolveActivity(getPackageManager()) != null)
                                        startActivity(intent);
                                    else {
                                        BaseDialogs.showDialog(LamrimReaderActivity.this, getString(R.string.errNoMediaPlayer), getString(R.string.msgInstallPlayerFirst),
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int val) {
                                                        final String appPackageName = getString(R.string.recommandMusicPlayerPkg); // getPackageName() from Context or Activity object
                                                        try {
                                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                                                        } catch (android.content.ActivityNotFoundException anfe) {
                                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                                        }
                                                    }
                                                },
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int val) {
                                                        dialog.dismiss();
                                                    }
                                                },
                                                true);
                                    }

                                    if (wakeLock.isHeld()) wakeLock.release();
                                    finish();
                                }
                            },
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int val) {
                                    dialog.dismiss();
                                }
                            }, true);

                }
            });

        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeController = (SeekBar) actionBarControlPanel.findViewById(R.id.volumeController);
        if(volumeController!=null) {
            volumeController.setMax(maxVolume);
            volumeController.setProgress(curVolume);
            volumeController.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar arg0) {
                    volumeController.setSelected(false);
                    Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.STATISTICS, "VOLUME_CONTROL_WITH_SEEKBAR");

                }

                @Override
                public void onStartTrackingTouch(SeekBar arg0) {
                    volumeController.setSelected(true);
                }

                @Override
                public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                    volumeController.setSelected(true);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, 0);
                }
            });
        }
        textSize = (ImageButton) actionBarControlPanel.findViewById(R.id.textSize);
        if(textSize!=null) {
            textSize.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    //textSize.setBackgroundColor(getResources().getColor(R.color.themeLightColor));
                    //if (Build.VERSION.SDK_INT >= 16) textSize.setBackground(getResources().getDrawable(R.drawable.speech_menu_item_e));
                    //else textSize.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_menu_item_e));
                    textSize.setSelected(true);
                    showSetTextSizeDialog();
                }
            });
        }

        search = (ImageButton) actionBarControlPanel.findViewById(R.id.search);
        search.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                search.setEnabled(false);
                search.setSelected(true);
                showSearchDialog();
                search.setEnabled(true);
            }
        });
        fakeSample.put(null, null);
        RegionRecord.init(this);
        regionFakeList = new ArrayList<HashMap<String, String>>();
        for (int i = 0; i < RegionRecord.records.size(); ++i)
            regionFakeList.add(fakeSample);

        regionRecordAdapter = new RegionRecordAdapter(this, regionFakeList,
                android.R.layout.simple_list_item_2, new String[]{"title", "desc"},
                new int[]{android.R.id.text1, android.R.id.text2});

        if (mpController != null)
            Crashlytics.log(Log.DEBUG, logTag, "The media player controller is not null in onCreate!!!!!");
        if (mpController == null)
            createMpController();

        subtitleView = (TextView) findViewById(R.id.subtitleView);
        subtitleView.setTypeface(Util.getFont(LamrimReaderActivity.this, runtime));

        final GestureDetectorCompat subtitleViewGestureListener = new GestureDetectorCompat(
                //getApplicationContext(), new SimpleOnGestureListener() {
                LamrimReaderActivity.this, new SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                Crashlytics.log(Log.DEBUG, logTag, "SubtitleView been clicked, Show media plyaer control panel.");

                if (mpController == null) {
                    setSubtitleViewText(getString(R.string.errPlayerRecycled));
                    createMpController();
                    return false;
                }
                if (mpController.getMediaPlayerState() >= MP_PREPARED) {
                    showMediaController();
///							showTitle();
                }

                Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.STATISTICS, "Subtitle_SingleTap");
                return true;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                Crashlytics.log(Log.DEBUG, logTag, "SubtitleView been double clicked.");
                // If it stay in subtitle mode, do nothing.
                if (subtitleViewRenderMode == SUBTITLE_MODE)
                    return false;
                if (mpController.getMediaPlayerState() == MediaPlayerController.MP_PLAYING && mpController.getSubtitle() != null) {
                    int index = mpController.getSubtitleIndex(mpController.getCurrentPosition());
                    if (index == -1)
                        return true;
                    // subtitleView.bringPointIntoView(readingModeSEindex[index][0]);
                    try {
                        // *************** Bug here **************
                        // Here will happen error while readingModeSEindex array under construct, but access fire by user at above line.
                        int line = subtitleView.getLayout().getLineForOffset(readingModeSEindex[index][0]);
                        subtitleView.scrollTo(subtitleView.getScrollX(), subtitleView.getLineBounds(line, null) - subtitleView.getLineHeight());
                    } catch (Exception et) {
                        et.printStackTrace();
                        Crashlytics.log(Log.DEBUG, logTag, "ReadingModeSEindex been access before construct finish.");
                        Crashlytics.logException(et);
                    }
                }
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                    float distanceX, float distanceY) {
                if (subtitleViewRenderMode == READING_MODE) {
                    int y = (int) (subtitleView.getScrollY() + distanceY);

                    // Unknown problem, there will return null on some
                    // machine.
                    Layout layout = subtitleView.getLayout();
                    Crashlytics.log(Log.DEBUG, logTag, "Layout is " + ((layout == null) ? "null" : "not null"));
                    if (layout == null)
                        return true;
                    // ======================================================
                    int bottom = subtitleView.getLineBounds(
                            subtitleView.getLayout().getLineForOffset(
                                    subtitleView.getText().length()),
                            null)
                            - subtitleView.getMeasuredHeight()
                            + subtitleView.getLineHeight();
                    Crashlytics.log(Log.DEBUG, logTag, "Org Y=" + y + "layout.height=" + subtitleView.getLayoutParams().height + ", subtitle.height=" + subtitleView.getHeight() + ", measureHeight=" + subtitleView.getMeasuredHeight());
                    if (y < 0)
                        y = 0;
                    if (y > bottom)
                        y = bottom;
                    // if(subtitleView.getLayoutParams().height-subtitleView.getMeasuredHeight()-y<0)y=subtitleView.getLayoutParams().height-subtitleView.getMeasuredHeight();
                    subtitleView.scrollTo(subtitleView.getScrollX(), y);
                    Crashlytics.log(Log.DEBUG, logTag, "Scroll subtitle view to " + subtitleView.getScrollX() + ", " + y);
                }
                return true;
            }
        });

        //final ScaleGestureDetector stScaleGestureDetector = new ScaleGestureDetector(this.getApplicationContext(), new SimpleOnScaleGestureListener() {
        final ScaleGestureDetector stScaleGestureDetector = new ScaleGestureDetector(LamrimReaderActivity.this, new SimpleOnScaleGestureListener() {
            //		class MyGestureDetector implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, GestureDetector.{

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                Crashlytics.log(Log.DEBUG, logTag, "Begin scale called factor: " + detector.getScaleFactor());
//						AnalyticsApplication.sendEvent("ui_action", "subtitle_event", "scale_start", null);
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float size = subtitleView.getTextSize() * detector.getScaleFactor();

                if (size <= textMinSize && subtitleView.getTextSize() == textMinSize)
                    return true;
                else if (size >= textMaxSize && subtitleView.getTextSize() == textMaxSize)
                    return true;

                if (size < textMinSize) size = textMinSize;
                else if (size > textMaxSize) size = textMaxSize;

                // Log.d(getClass().getName(),"Get scale rate: "+detector.getScaleFactor()+", current Size: "+adapter.getTextSize()+", setSize: "+adapter.getTextSize()*detector.getScaleFactor());
                subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
                // Log.d(getClass().getName(),"Realy size after setting: "+adapter.getTextSize());
                if (subtitleViewRenderMode == SUBTITLE_MODE)
                    subtitleView.setHeight(subtitleView.getLineHeight());

                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                runtimeEditor.putInt(getString(R.string.subtitleFontSizeKey), (int) subtitleView.getTextSize());
                runtimeEditor.commit();
                Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.STATISTICS, "FINGER_SCALE_ON_SUBTITLE_VIEW");
            }
        });

        subtitleView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                try {
                    if (event.getPointerCount() == 2) {
                        return stScaleGestureDetector.onTouchEvent(event);
                    }

                    Crashlytics.log(Log.DEBUG, logTag, "Call subtitleViewGestureListener");
                    boolean res = subtitleViewGestureListener.onTouchEvent(event);
                    return res;
                    // Crashlytics.log(Log.DEBUG, logTag, "Subtitle OnTouchListener return "+res);
                } catch (Exception e) {
                    e.printStackTrace();
                    //Util.fireException("Exception happen in OnTouchListener of SubtitleView.", e);
                    return true;
                }

            }

        });

        bookView = (MyListView) findViewById(R.id.bookPageGrid);
//		bookView.setFadeColor(getResources().getColor(R.color.defSubtitleBGcolor));
//		int defTheoryTextSize = getResources().getInteger(R.integer.defFontSize);
        final int theoryTextSize = runtime.getInt(getString(R.string.bookFontSizeKey), textDefSize);
        bookView.setTextSize(theoryTextSize);
        int bookPage = runtime.getInt("bookPage", 0);
        int bookPageShift = runtime.getInt("bookPageShift", 0);
        bookView.setSelectionFromTop(bookPage, bookPageShift);

        bookView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, final int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if (view == null)
                    return;
                // if(bookList == null)return;
                String input = jumpPage.getText().toString().trim();
                if (input.length() == 0 || !input.matches("[0-9]+"))
                    return;
                int num = Integer.parseInt(jumpPage.getText().toString());
                if (num < 0 || num > bookView.getCount())
                    return;

                int showNum = Integer.parseInt(jumpPage.getText().toString());
                if (showNum == firstVisibleItem + 1)
                    return;

                jumpPage.post(new Runnable() {
                    @Override
                    public void run() {
                        jumpPage.setText(String.valueOf(firstVisibleItem + 1));
                    }
                });
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }
        });

        bookView.setOnDoubleTapEventListener(new OnDoubleTapEventListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (bookViewMountPoint[0] == -1) return true;
                synchronized (bookViewMountPointKey) {
                    //bookView.rebuildView();
                    bookView.setViewToPosition(bookViewMountPoint[0], bookViewMountPoint[1]);
                    //bookView.setSelectionFromTop(bookViewMountPoint[0], bookViewMountPoint[1]);
                }
                Crashlytics.log(Log.DEBUG, logTag, "Jump to theory page index " + bookViewMountPoint[0] + " shift " + bookViewMountPoint[1]);
                Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.STATISTICS, "DOUBLE_CLICK_ON_BOOKVIEW");
                return true;
            }
        });

        bookView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Crashlytics.log(Log.DEBUG, logTag, "Hide media player controller.");
                    hideMediaController(false);
                }
                return false;
            }
        });

        renderView = (ImageView) findViewById(R.id.renderView);
        File renderImage = null;
        String imgPath = runtime.getString(getString(R.string.renderImgFgPathKey), null);
        if (imgPath != null)
            renderImage = new File(imgPath);

        if (renderImage != null && renderImage.exists())
            renderView.setImageURI(Uri.fromFile(renderImage));
        else renderView.setImageResource(R.drawable.master);

        renderView.setScaleType(scaleType[runtime.getInt(getString(R.string.renderImgScaleKey), 0)]);
        int color = runtime.getInt(getString(R.string.renderImgBgColorKey), 0);
        renderView.setBackgroundColor(color);
        renderView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Crashlytics.log(Log.DEBUG, logTag, "Into onLongClickListener of render image.");
                showRenderModeFirstLevelMenu();
                return true;
            }
        });
        renderView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Crashlytics.log(Log.DEBUG, logTag, "Into onLongClickListener of render image.");
                if (mpController == null) {
                    setSubtitleViewText(getString(R.string.errPlayerRecycled));
                    createMpController();
                    return;
                }
                hideMediaController(false);
            }
        });

        String appSubtitle = getString(R.string.app_name) + " V" + pkgInfo.versionName;
        ActionBar actionBar = getSupportActionBar();
        // Disable show App icon.
        actionBar.setDisplayShowHomeEnabled(false);
        if (actionBar != null) actionBar.setSubtitle(appSubtitle);

        int onCreateSpendTimeMs = (int) (System.currentTimeMillis() - appStartTimeMs);
        Crashlytics.log(Log.DEBUG, logTag, "=============== onCreate spend time: " + onCreateSpendTimeMs);
        //Crashlytics.setDouble("InitialMainActivityTime", onCreateSpendTimeMs);

    }// ========================================== End of OnCreate() ============================================

    // For catch global event, acquire again if user action happen.
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
//		Crashlytics.log(Log.DEBUG, logTag,"Into dispatchTouchEvent() of activity.");
        if (wakeLock.isHeld()) wakeLock.release();
        if (!wakeLock.isHeld()) {
            wakeLock.acquire(screenOnTime);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void swapRegionSet() {
        Crashlytics.log(Log.DEBUG, logTag, "RegionSet={" + regionSet[0] + ", " + regionSet[1] + ", " + regionSet[2] + ", " + regionSet[3] + "} before swap.");
        if (regionSet[0] < regionSet[2]) return;
        if (regionSet[0] <= regionSet[2] && regionSet[1] <= regionSet[3]) return;

        int swap = regionSet[0];
        regionSet[0] = regionSet[2];
        regionSet[2] = swap;

        swap = regionSet[1];
        regionSet[1] = regionSet[3];
        regionSet[3] = swap;

        Crashlytics.log(Log.DEBUG, logTag, "RegionSet={" + regionSet[0] + ", " + regionSet[1] + ", " + regionSet[2] + ", " + regionSet[3] + "} after swap.");
    }

    private void shareSegment(RegionRecord record) {
        boolean isShareWithREST = getResources().getBoolean(R.bool.isShareWithREST);
        if (isShareWithREST)
            shareSegmentRest(record.title, record.mediaStart, record.startTimeMs, record.mediaEnd, record.endTimeMs, record.theoryPageStart, record.theoryStartLine, record.theoryPageEnd, record.theoryEndLine);
        else
            shareSegment(record.title, record.mediaStart, record.startTimeMs, record.mediaEnd, record.endTimeMs, record.theoryPageStart, record.theoryStartLine, record.theoryPageEnd, record.theoryEndLine);
    }

    private void shareSegment(String title, int speechStartIndex, int speechStartMs, int speechEndIndex, int speechEndMs, int theoryPageStart, int theoryStartLine, int theoryPageEnd, int theoryEndLine) {
        boolean isOutputSecondLink = false;
        String firebase = "https://xe74n.app.goo.gl/?apn=eyes.blue&afl=https://lamrimreader-cmd.eyes-blue.com/play&link=";
        String lamrimCmdUri = getString(R.string.lamrimCmdUri) + "play?";
        String queryStr = "mode=region";
        String speechStart = GlRecord.getSpeechIndexToStr(speechStartIndex) + ":" + Util.getMsToHMS(speechStartMs, ":", "", true);
        String speechEnd = GlRecord.getSpeechIndexToStr(speechEndIndex) + ":" + Util.getMsToHMS(speechEndMs, ":", "", true);
        String theoryStart = (theoryPageStart + 1) + ":" + (theoryStartLine + 1);
        String theoryEnd = (theoryPageEnd + 1) + ":" + (theoryEndLine + 1);

        queryStr += "&speechStart=" + speechStart + "&speechEnd=" + speechEnd + "&theoryStart=" + theoryStart + "&theoryEnd=" + theoryEnd;
        try {
            if (title != null) queryStr += "&title=" + URLEncoder.encode(title, "utf8");
            firebase += URLEncoder.encode(lamrimCmdUri + queryStr, "utf8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.BUTTON_CLICK, "SHARE_REGION");

        String msgTitle = ((title.isEmpty()) ? "" : title + " - ") + getString(R.string.msgShareTitle);
        String secTitle =getString(R.string.msgHintClickLink);

        String outputStr = msgTitle + firebase;
        if (isOutputSecondLink) outputStr += "\n" + secTitle + lamrimCmdUri + queryStr;

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        //sendIntent.putExtra(Intent.EXTRA_TEXT, lamrimCmdUri + queryStr);
        sendIntent.putExtra(Intent.EXTRA_TEXT, outputStr);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.shareRegion)));
    }

    private void shareSegmentRest(String title, int speechStartIndex, int speechStartMs, int speechEndIndex, int speechEndMs, int theoryPageStart, int theoryStartLine, int theoryPageEnd, int theoryEndLine) {
        String lamrimCmdUri = getString(R.string.lamrimCmdUri);
        String rest = "play/region/";
        String speechStart = GlRecord.getSpeechIndexToStr(speechStartIndex) + ":" + Util.getMsToHMS(speechStartMs, ":", "", true);
        String speechEnd = GlRecord.getSpeechIndexToStr(speechEndIndex) + ":" + Util.getMsToHMS(speechEndMs, ":", "", true);
        String theoryStart = (theoryPageStart + 1) + ":" + (theoryStartLine + 1);
        String theoryEnd = (theoryPageEnd + 1) + ":" + (theoryEndLine + 1);

        try {
            rest += speechStart + "/" + speechEnd + "/" + theoryStart + "/" + theoryEnd;

            if (title != null)
                rest += "/" + URLEncoder.encode(title, "utf8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.BUTTON_CLICK, "SHARE_REGION");

        String msgTitle = ((title.isEmpty()) ? "" : title + " - ") + getString(R.string.msgShareTitle);

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, msgTitle + "https://xe74n.app.goo.gl/?apn=eyes.blue&afl=https://lamrimreader-cmd.eyes-blue.com/play&link=" + lamrimCmdUri + rest);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.shareRegion)));
    }


    public void showOnRegionOptionDialog(final int mediaIndex, final int mediaPosition) {
        final AlertDialog setRegionOptDialog = new AlertDialog.Builder(LamrimReaderActivity.this).create();

        LayoutInflater factory = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View v = factory.inflate(R.layout.region_option_dialog, null);
//	    TextView mediaPositionDesc=(TextView) v.findViewById(R.id.time);
        TextView startDesc = (TextView) v.findViewById(R.id.leftBoundDesc);
        TextView endDesc = (TextView) v.findViewById(R.id.rightBoundDesc);

        final LinearLayout leftBound = (LinearLayout) v.findViewById(R.id.setLeftBound);
        final LinearLayout rightBound = (LinearLayout) v.findViewById(R.id.setRightBound);
        final LinearLayout saveOpt = (LinearLayout) v.findViewById(R.id.saveOpt);
//	    final LinearLayout shareOpt=(LinearLayout) v.findViewById(R.id.shareOpt);
        final ImageView save = (ImageView) v.findViewById(R.id.save);
//	    final ImageView share=(ImageView) v.findViewById(R.id.share);

//	    String timeStr=String.format(mediaPositionDesc.getText().toString(), SpeechData.getNameId(mediaIndex)+":"+Util.getMsToHMS(mediaPosition,":","",true));
//	    mediaPositionDesc.setText(timeStr);

        if (regionSet[0] != -1) {
            startDesc.setText(SpeechData.getNameId(regionSet[0]) + ":" + Util.getMsToHMS(regionSet[1], ":", "", true));
        }
        if (regionSet[2] != -1) {
            endDesc.setText(SpeechData.getNameId(regionSet[2]) + ":" + Util.getMsToHMS(regionSet[3], ":", "", true));
        }
        if (regionSet[0] != -1 && regionSet[2] != -1) {
            saveOpt.setEnabled(true);
//		    shareOpt.setEnabled(true);
            save.setEnabled(true);
//	    	share.setEnabled(true);
        } else {
            saveOpt.setEnabled(false);
//		    shareOpt.setEnabled(false);
            save.setEnabled(false);
//	    	share.setEnabled(false);
        }

        leftBound.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mpController == null) {
                    setSubtitleViewText(getString(R.string.errPlayerRecycled));
                    createMpController();
                    return;
                }
                if (mpController.getSubtitle() == null) {
                    Util.showErrorToast(LamrimReaderActivity.this, getString(R.string.dlgFailLoadSubtitle));
                    return;
                }
                /*if(regionSet[2]!=-1 && Math.abs(mediaIndex-regionSet[2])>1){
					Crashlytics.log(Log.DEBUG, logTag,"regionSet[0]-regionSet[2]="+(regionSet[0]-regionSet[2]));
					BaseDialogs.showErrorDialog(LamrimReaderActivity.this, "只能標記相鄰的音檔");
					return;
				}
				*/
                regionSet[0] = mediaIndex;
                int i = Util.subtitleBSearch(mpController.getSubtitle(), mediaPosition);
                regionSet[1] = mpController.getSubtitle()[i].startTimeMs;
                regionStartInfo = mpController.getSubtitle()[i].text;


                if (bookMap != null && bookMap[i] != null) {
                    theoryHighlightRegion[0] = bookMap[i][0];
                    theoryHighlightRegion[1] = bookMap[i][1];
                }

                try {
                    setRegionOptDialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }    // Don't force close if problem here.
            }
        });
        rightBound.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mpController == null) {
                    setSubtitleViewText(getString(R.string.errPlayerRecycled));
                    createMpController();
                    return;
                }
                if (mpController.getSubtitle() == null) {
                    Util.showErrorToast(LamrimReaderActivity.this, getString(R.string.dlgFailLoadSubtitle));
                    return;
                }
				/*if(regionSet[0]!=-1 && Math.abs(regionSet[0]-mediaIndex)>1){
					BaseDialogs.showErrorDialog(LamrimReaderActivity.this, "只能標記相鄰的音檔");
					return;
				}*/
                regionSet[2] = mediaIndex;
                int i = Util.subtitleBSearch(mpController.getSubtitle(), mediaPosition);
                regionSet[3] = mpController.getSubtitle()[i].endTimeMs;
                regionEndInfo = mpController.getSubtitle()[i].text;

                if (bookMap != null && bookMap[i] != null) {
                    theoryHighlightRegion[2] = bookMap[i][0];
                    theoryHighlightRegion[3] = bookMap[i][1];
                }

                try {
                    setRegionOptDialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }    // Don't force close if problem here.
            }
        });
        saveOpt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Math.abs(regionSet[0] - regionSet[2]) > 1) {
                    BaseDialogs.showSimpleErrorDialog(LamrimReaderActivity.this, getString(R.string.dlgOnlyAcceptNearAudio));
                    return;
                }

                swapRegionSet();
                // startMediaIndex, startTimeMs, endMediaIndex, endTimeMs, theoryStartPage, theoryStartLine, theoryEndPage, theoryEndLine, startSubtitle, endSubtitle
                String[] subtitleInfo = Util.getRegionInfo(fsm, regionSet);
                if (subtitleInfo == null) subtitleInfo = new String[]{getString(R.string.noData), getString(R.string.noData)};
                BaseDialogs.showEditRegionDialog(LamrimReaderActivity.this, regionSet[0], regionSet[1], regionSet[2], regionSet[3], theoryHighlightRegion[0], theoryHighlightRegion[1], theoryHighlightRegion[2], theoryHighlightRegion[3], subtitleInfo[0] + " ~ " + subtitleInfo[1], -1, new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        regionFakeList.add(fakeSample);
                                        if (regionRecordAdapter != null)
                                            Crashlytics.log(Log.DEBUG, logTag, "Warring: the regionRecordAdapter = null !!!");
                                        else
                                            regionRecordAdapter.notifyDataSetChanged();

                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                                imm.hideSoftInputFromWindow(jumpPage.getWindowToken(), 0);
                                                hideMediaController(false);
//                       showMediaController();
                                            }
                                        }, 200);
                                    }
                                });
                            }
                        }
                );
                try {
                    setRegionOptDialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }    // Don't force close if problem here.
            }
        });

        setRegionOptDialog.setView(v);
        setRegionOptDialog.setTitle(getString(R.string.dlgSavePositionAs));
        setRegionOptDialog.setCanceledOnTouchOutside(true);
        setRegionOptDialog.show();
    }

    private void createMpController() {
        mpController = new MediaPlayerController(LamrimReaderActivity.this,
                LamrimReaderActivity.this.findViewById(R.id.mediaControllerMountPoint), fsm,
                new MediaPlayerControllerListener() {
                    @Override
                    public void onSubtitleChanged(final int index, final SubtitleElement subtitle) {
                        // Log.d(getClass().getName(), "Set subtitle: "+
                        // subtitle.text);
                        // ========== 設定資訊顯示 =============
                        subtitleIndex = index + 1;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // ========== 論文區域變色 ======================
                                int[] highlightWord = bookView.getHighlightWord();

                                if (bookMap != null && bookMap[index] != null)
                                    if (highlightWord == null || !(bookMap[index][0] == highlightWord[0] && bookMap[index][1] == highlightWord[1] && bookMap[index][2] == highlightWord[2] && bookMap[index][3] == highlightWord[3])) {
                                        Crashlytics.log(Log.DEBUG, logTag, "Set highlight at Page: " + bookMap[index][0] + ", Line: " + bookMap[index][1] + ", Word: " + bookMap[index][2] + ", Length: " + bookMap[index][3]);
                                        bookView.setHighlightWord(bookMap[index][0], bookMap[index][1], bookMap[index][2], bookMap[index][3]);
                                        synchronized (bookViewMountPointKey) {
                                            bookViewMountPoint[0] = bookMap[index][0];
                                            bookViewMountPoint[1] = bookMap[index][1];
                                        }
                                    }

                                // =========== 字幕區運算 ================
                                switch (subtitleViewRenderMode) {
                                    case SUBTITLE_MODE:
                                        subtitleView.setText(subtitle.text);
                                        int lineCount = subtitleView.getLineCount();// There will return 0 sometimes.
                                        if (lineCount < 1)
                                            lineCount = 1;
                                        subtitleView.setHeight(subtitleView.getLineHeight() * lineCount);

                                        break;
                                    case READING_MODE:
                                        // SpannableString str=new
                                        // SpannableString
                                        // (subtitleView.getText());
                                        SpannableString str = new SpannableString(readingModeAllSubtitle);
                                        // Spannable WordtoSpan = (Spannable)
                                        // subtitleView.getText();
                                        try {
                                            // *************** Bug here **************
                                            // Here will happen error while readingModeSEindex array under construct, but access fire by user at above line.
                                            str.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.subtitleRedingModeHilightColor)),
                                                    readingModeSEindex[index][0],
                                                    readingModeSEindex[index][1],
                                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                            subtitleView.setText(str);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            //Util.fireException("mediaIndex=" + mediaIndex + ", subtitleIndex=" + index + ", totalLen=" + str.length(), e);
                                        }
                                        break;
                                }
                            }
                        });
                    }

                    @Override
                    public void onPlayerError() {
                        setSubtitleViewText(getString(R.string.errＷhilePlayMedia));
                        Crashlytics.log(Log.ERROR, logTag, "Player error cause onPlayerError() been called.");
                    }

                    @Override
                    public void onSeek(final int index, final SubtitleElement subtitle) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (infoTextView.isShown())
                                    setInfoText(subtitle.startTimeMs);
                                // synchronized (mpController){
                                switch (subtitleViewRenderMode) {
                                    case SUBTITLE_MODE:
                                        //Util.showSubtitlePopupWindow(LamrimReaderActivity.this, subtitle.text + " - (" + Util.getMsToHMS(subtitle.startTimeMs, "\"", "'", false) + " - " + Util.getMsToHMS(subtitle.endTimeMs, "\"", "'", false) + ") #" + (index + 1));

                                        //Util.showSubtitleToast(LamrimReaderActivity.this, subtitle.text+ " - (" + Util.getMsToHMS(subtitle.startTimeMs, "\"", "'", false) + " - "	+ Util.getMsToHMS(subtitle.endTimeMs, "\"", "'", false) + ')');
                                        //		if(bookMap[index]!=null){
                                        //			//Log.d(getClass().getName(),"Highlight page"+(bookMap[index][0]+1)+", line "+(bookMap[index][1]+1)+", word "+(bookMap[index][2]+1)+", length="+subtitle.text.length());
                                        //			bookView.setHighlightWord(bookMap[index][0], bookMap[index][1], bookMap[index][2], bookMap[index][3]);
                                        //		}
                                        break;
                                    case READING_MODE:
                                        SpannableString str = new SpannableString(readingModeAllSubtitle);
                                        try {
                                            // *************** Bug here **************
                                            // Here will happen error while readingModeSEindex array under construct, but access fire by user at above line.
                                            str.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.subtitleRedingModeHilightColor)),
                                                    readingModeSEindex[index][0], readingModeSEindex[index][1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                            subtitleView.setText(str);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            //Util.fireException("mediaIndex=" + mediaIndex + ", subtitleIndex=" + index + ", totalLen=" + str.length(), e);
                                        }
                                        break;
                                }
                                ;
                            }
                        });
                    }

                    // @Override
                    // public void startMoment(){setSubtitleViewText("");}


                    @Override
                    public void onMediaPrepared() {
                        if (mpController == null) {
                            setSubtitleViewText(getString(R.string.errPlayerRecycled));
                            createMpController();
                            return;
                        }
                        Crashlytics.log(Log.DEBUG, logTag, "MediaPlayer prepared, show controller.");
                        //AnalyticsApplication.sendEvent("play_action", "player_event", SpeechData.getSubtitleName(mediaIndex) + "_prepared");

                        //if (mpController.isSubtitleReady()) {
                        SubtitleElement[] se = mpController.getSubtitle();
                        readingModeSEindex = new int[se.length][2];
                        readingModeAllSubtitle = new String();
                        int wordCounter = 0;
                        //	int pageStart = SpeechData.refPage[mediaIndex], pageEnd=-1; ---
                        //	if(mediaIndex!=SpeechData.name.length-1)
                        //		pageEnd=SpeechData.refPage[mediaIndex+1];
                        //	else
                        //		pageEnd=SpeechData.refPage[mediaIndex];

                        int[][] mediaBookMaps = BookMap.getMediaIndex(mediaIndex);
                        Crashlytics.log(Log.DEBUG, logTag, "載入論文音檔對應表: " + SpeechData.getSubtitleName(mediaIndex));
                        if (mediaBookMaps != null) {
                            bookMap = new int[se.length][]; // For setHighlightWord(int startPage, int line, int startIndex, int length)

                            for (int i = 0; i < mediaBookMaps.length; i++) {
                                int index = mediaBookMaps[i][1];
                                if (index >= se.length) {
                                    String errMsg=String.format(getString(R.string.subtitleNotComp),  SpeechData.getSubtitleName(mediaIndex));
                                    Util.showErrorToast(LamrimReaderActivity.this, errMsg);
                                    setSubtitleViewText(errMsg);
                                    //Util.fireException("Theory index over subtitle index at " + SpeechData.getSubtitleName(mediaIndex) + " read index=" + index + ", array length=" + se.length, new ArrayIndexOutOfBoundsException());
                                    return;
                                }

                                bookMap[index] = new int[4];
                                bookMap[index][0] = mediaBookMaps[i][BookMap.PAGE];
                                bookMap[index][1] = mediaBookMaps[i][BookMap.LINE];
                                bookMap[index][2] = mediaBookMaps[i][BookMap.WORD];
                                bookMap[index][3] = mediaBookMaps[i][BookMap.LENGTH];
                            }

                            int[] last = null;
                            for (int i = 0; i < bookMap.length; i++) {
                                if (bookMap[i] != null) {
                                    last = bookMap[i];
                                }
                                if (last == null) continue;
                                bookMap[i] = last;
                            }
                        } else bookMap = null;

                        //for(int i=0;i<bookMap.length;i++){
                        //	Log.d(getClass().getName(),"bookmap["+i+"] = "+bookMap[i][0]+","+bookMap[i][1]+","+bookMap[i][2]+","+bookMap[i][3]);
                        //}

                        for (int i = 0; i < se.length; i++) {
                            readingModeSEindex[i][0] = wordCounter;
                            wordCounter += se[i].text.length();
                            readingModeSEindex[i][1] = wordCounter;
                            readingModeAllSubtitle += se[i].text;

                            //			String str=se[i].text.replace("，", "").replace("。", "").replace("：", "").replace("？", "").replace("《", "").replace("》", "");

							/*	bookMap[i]=bookView.searchNext(pageStart, pageEnd, 0, 0, str);
								if(bookMap[i]!=null){
									last=new int[]{bookMap[i][0],bookMap[i][1],bookMap[i][2],str.length()};
									bookMap[i]=last;
								}

								else
									bookMap[i]=last;
							*/
                        }

                        //bookMap=BookMap.getMediaIndex(mediaIndex);
                        //} else {
                        //    setSubtitleViewText(getString(R.string.dlgHintMpControllerNoSubtitle));
                        //}

                        ActionBar actionBar = getSupportActionBar();
                        if (actionBar != null) actionBar.setTitle(actionBarTitle);
                        if (GLamrimSectIndex != -1) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        bookView.setHighlightLine(theoryHighlightRegion[0], theoryHighlightRegion[1], theoryHighlightRegion[2], theoryHighlightRegion[3]);
                                    } catch (Exception e) {
                                        Crashlytics.log(Log.ERROR, logTag, "Error happen while set the Highlight Line.");
                                        Crashlytics.log(Log.ERROR, logTag, e.getMessage());
                                        Crashlytics.logException(e);
                                        Util.showErrorToast(LamrimReaderActivity.this, getString(R.string.markRegionFail));
                                    }
                                }
                            }, 1000);

//							mpController.refreshSeekBar();
                            Crashlytics.log(Log.DEBUG, logTag, "GlobalLamrim mode: play index " + GLamrimSect[GLamrimSectIndex][0] + ", Sec: " + GLamrimSect[GLamrimSectIndex][1] + ":" + GLamrimSect[GLamrimSectIndex][2]);
                            Crashlytics.log(Log.DEBUG, logTag, "Mark theory: start page=" + theoryHighlightRegion[0] + " start line=" + theoryHighlightRegion[1] + ", offset=" + bookViewMountPoint[1]);

                            int regionStart = GLamrimSect[GLamrimSectIndex][1];
                            int regionEnd = GLamrimSect[GLamrimSectIndex][2];
                            int playPosition = playRecord.getInt("playPosition", GLamrimSect[0][1]);
                            if (regionEnd == -1) regionEnd = mpController.getDuration() - 1000;

                            //mpController.seekTo(GLamrimSect[GLamrimSectIndex][1]);
                            mpController.seekTo(playPosition);

                            if (GLamrimSect[1][0] == -1) {
                                setMediaControllerView(regionStart, regionEnd, false, false, glModePrevNextListener.getPrevPageListener(), glModePrevNextListener.getNextPageListener());
                            } else if (GLamrimSectIndex == 0) {
                                setMediaControllerView(regionStart, regionEnd, false, true, glModePrevNextListener.getPrevPageListener(), glModePrevNextListener.getNextPageListener());
                            } else {
                                setMediaControllerView(regionStart, regionEnd, true, false, glModePrevNextListener.getPrevPageListener(), glModePrevNextListener.getNextPageListener());
                            }

                            //mpController.start();

                            //
                            setSubtitleViewText(getString(R.string.dlgHintShowMpController));
                            showMediaController();
/*							}else if (regionPlayIndex != -1) {
							Crashlytics.log(Log.DEBUG, logTag, "Region Mode: This play event is region play, set play region.");
							bookView.setHighlightLine(theoryHighlightRegion[0], theoryHighlightRegion[1], theoryHighlightRegion[2], theoryHighlightRegion[3]);
							if(actionBar != null)actionBar.setTitle(getString(R.string.menuStrPlayRegionRecShortName)+": "+RegionRecord.records.get(regionPlayIndex).title);
							setMediaControllerView(RegionRecord.records.get(regionPlayIndex).startTimeMs,RegionRecord.records.get(regionPlayIndex).endTimeMs, true, true, normalModePrevNextListener.getPrevListener(), true, true, normalModePrevNextListener.getNextListener());
							mpController.seekTo(RegionRecord.records.get(regionPlayIndex).startTimeMs);
							mpController.start();
							regionPlayIndex = -1;*/
                        } else {
                            Crashlytics.log(Log.DEBUG, logTag, "Normal mode: The play event is fire by user select a new speech.");

                            // The title of actionBar will miss while restart the App.
                            actionBarTitle = SpeechData.getNameId(mediaIndex);
                            if (actionBar != null) actionBar.setTitle(actionBarTitle);

							/*
							final int pageNum = SpeechData.refPage[mediaIndex] - 1;
							Log.d(getClass().getName(),"The speech reference theory page "+pageNum);
							if (pageNum >= 0){
								synchronized(bookViewMountPointKey){
									bookViewMountPoint[0]=pageNum;
									bookViewMountPoint[1]=0;
								}
							}
							*/

                            int seekPosition = playRecord.getInt("playPosition", 0);
                            Crashlytics.log(Log.DEBUG, logTag, "Seek to last play positon " + seekPosition);
                            mpController.setPrevNextListeners(normalModePrevNextListener.getPrevPageListener(), normalModePrevNextListener.getNextPageListener());
                            mpController.seekTo(seekPosition);
                            setSubtitleViewText(getString(R.string.dlgHintShowMpController));
                            showMediaController();
                        }
                    }

                    @Override
                    public void getAudioFocusFail() {
                        setSubtitleViewText(getResources().getString(R.string.soundInUseError));
                    }

                    @Override
                    public void onStartPlay() {
                        Crashlytics.log(Log.DEBUG, logTag, "Hide Title bar.");
                        int seekPosition = mpController.getCurrentPosition();
                        mpController.seekTo(seekPosition);
//						hideTitle();
                    }

                    @Override
                    public void onPause() {
                        Crashlytics.log(Log.DEBUG, logTag, "Show Title bar.");
                    }

                    @Override
                    public void onComplatePlay() {
                        Crashlytics.log(Log.DEBUG, logTag, "Show Title bar.");
//						showTitle();
                        if (GLamrimSectIndex == 0 && GLamrimSect[1][0] != -1)
                            Util.showInfoToast(LamrimReaderActivity.this, getString(R.string.playFinishNext));
                        else Util.showInfoToast(LamrimReaderActivity.this, getString(R.string.playFinish));
//						if (wakeLock.isHeld())wakeLock.release();
                    }
                });

        mpController.setOnRegionClick(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showOnRegionOptionDialog(mediaIndex, mpController.getCurrentPosition());
                showMediaController();
            }
        });

        mpController.setOnPinClick(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mpController == null) {
                    setSubtitleViewText(getString(R.string.errPlayerRecycled));
                    createMpController();
                }
                if (mpController.getSubtitle() == null) {
                    setSubtitleViewText(getString(R.string.dlgFailLoadSubtitle));
                    return;
                }

                ImageButton reportBtn = (ImageButton) mpController.getControllerView().findViewById(R.id.pinBtn);
                boolean isClick = !reportBtn.isSelected();
                reportBtn.setSelected(isClick);
                if (isClick) {
                    mpController.setShowLongTerm(true);
                    showMediaController();
                } else {
                    mpController.setShowLongTerm(false);
                    hideMediaController(true);
                }
                Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.BUTTON_CLICK, "PinButtonOnMpController " + ((isClick) ? "Clicked" : "Release"));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Crashlytics.log(Log.DEBUG, logTag, "**** onStart() ****");
/*
		float modeSwBtnHeight = (float) getResources().getInteger(
				R.integer.subtitleScrollTouchBtnHeightPercentDp)
				/ 100 * screenDim.y;
		float modeSwBtnWidth = (float) getResources().getInteger(
				R.integer.subtitleScrollTouchBtnWidthPercentDp)
				/ 100 * screenDim.x;
		// modeSwBtn.getLayoutParams().width = (int) modeSwBtnWidth;
		// modeSwBtn.getLayoutParams().height = (int) modeSwBtnHeight;
*/

        //int defTitleTextSize = getResources().getInteger(R.integer.defFontSize);
        final int subtitleTextSize = runtime.getInt(getString(R.string.subtitleFontSizeKey), (int) subtitleView.getTextSize());
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, subtitleTextSize);
        // int bookPage = runtime.getInt("bookPage", 0);
        // jumpPage.setText(bookPage);

        // Show change log if need.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ChangeLog cl = new ChangeLog(LamrimReaderActivity.this);
                if (cl.firstRun())
                    cl.getLogDialog().show();
            }
        },250);



        Crashlytics.log(Log.DEBUG, logTag, "**** Leave onStart() ****");
    }

    public static Point getScreenDim() {
        return screenDim;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Crashlytics.log(Log.DEBUG, logTag, "**** Into onResume() ****");
        /*
         * While in the sleep mode, the life cycle into onPause, when user
         * active the application the life cycle become onResume -> onPause ->
         * onDestroy -> onCreate -> onStart -> onResume, the media player still
         * exist after the application recreate. the prepare method call twice
         * both in the two onResume, the second prepare will throw
         * illegalStageExcteption, and will cause error sometime, If the stage
         * into PREPARING, it mean it preparing the media source at first
         * onResume, then do nothing.
         */
        /*
         * try { if (mpController.getMediaPlayerState() >=
         * MediaPlayerController.MP_PREPARING){
         * Crashlytics.log(Log.DEBUG, logTag,"onResume: The state of MediaPlayer is PAUSE, start play."
         * ); //
         * mpController.setAnchorView(LamrimReaderActivity.this.findViewById
         * (android.R.id.content)); // mpController.showMediaPlayerController();
         * return; } } catch (IllegalStateException e) { e.printStackTrace();}
         */

        Intent cmdIntent = this.getIntent();
        Crashlytics.log(Log.DEBUG, logTag, "Check command intent : " + ((cmdIntent == null) ? "is null." : "not null."));
        if (cmdIntent != null && cmdIntent.getAction() != null && cmdIntent.getAction().equals(Intent.ACTION_VIEW)) {
            Crashlytics.log(Log.DEBUG, logTag, "Action: " + getIntent().getAction());

            // Here must check is the file exist, or unlimited loop happen [file not exist] -> [switch to SpeechMenuActivity] -> show network access dialog -> disallow -> [here] and so on.
//			int mStart=cmdIntent.getIntExtra("mediaStart", 0);
//			int mEnd=cmdIntent.getIntExtra("mediaEnd", 0);
//			Log.d(getClass().getName(), "Check is file exist : "+mStart+", "+mEnd);
//			if(!fsm.isFilesReady(mStart) || !fsm.isFilesReady(mEnd)){
//				Util.showErrorToast(LamrimReaderActivity.this, "音檔或字幕檔案不存在，無法載回最後狀態", 1000);
//				return;
//			}

            playMode = REGION_PLAY_MODE;
            playRecord = getSharedPreferences(getString(R.string.regionPlayModeRecordFile), 0);
            GLamrimSectIndex = 0;
            getIntent().setAction(Intent.ACTION_MAIN);
            String title = cmdIntent.getStringExtra("title");
            Crashlytics.log(Log.DEBUG, logTag, "Title: " + title);
            if (title != null)
                actionBarTitle = getString(R.string.menuStrPlayRegionRecShortName) + ": " + title;
            else actionBarTitle = getString(R.string.menuStrPlayRegionRecShortName) + ": "+getString(R.string.dlgUntitle);
            Crashlytics.log(Log.DEBUG, logTag, "actionBarTitle: " + title);
            startRegionPlay(cmdIntent.getIntExtra("mediaStart", 0),
                    cmdIntent.getIntExtra("startTimeMs", 0),
                    cmdIntent.getIntExtra("mediaEnd", 0),
                    cmdIntent.getIntExtra("endTimeMs", 0),
                    cmdIntent.getIntExtra("theoryStartPage", 0),
                    cmdIntent.getIntExtra("theoryStartLine", 0),
                    cmdIntent.getIntExtra("theoryEndPage", 0),
                    cmdIntent.getIntExtra("theoryEndLine", 0),
                    0, // play from 0th index(start index of GlamrimSec[][]).
                    actionBarTitle);
            // Set play from start position.
            SharedPreferences.Editor editor = playRecord.edit();
            editor.putInt("playPosition", cmdIntent.getIntExtra("startTimeMs", 0));
            editor.commit();
            Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.STATISTICS, "OPEN_REGION_FROM_OTHERS_SHARE");
            return;
        }

        // Check is mediaPlayer loaded.
        Crashlytics.log(Log.DEBUG, logTag, "onResume: Check is the MediaPlayer has ready");
        if (mpController.isPlayerReady() && mediaIndex != -1) {
            Crashlytics.log(Log.DEBUG, logTag, "onResume: The MediaPlayer has ready, skip reload.");
            return;
        }

        Crashlytics.log(Log.DEBUG, logTag, "onResume: Into reload last state procedure.");
        playMode = runtime.getInt("playMode", -1);
        if (playMode == -1) {    // Never played.
            Crashlytics.log(Log.DEBUG, logTag, "onResume: This is new install, never played, skip reload MediaPlayer.");
            return;
        }

        Crashlytics.log(Log.DEBUG, logTag, "Media index = " + mediaIndex);
        Crashlytics.log(Log.DEBUG, logTag, "Reload playMode " + playMode);
        if (playMode == SPEECH_PLAY_MODE) {
            Crashlytics.log(Log.DEBUG, logTag, "Reload SPEECH_PLAY_MODE");
            playRecord = getSharedPreferences(getString(R.string.speechModeRecordFile), 0);
            if (playRecord == null) return;
            mediaIndex = playRecord.getInt("mediaIndex", -1);
            Crashlytics.log(Log.DEBUG, logTag, "play index " + mediaIndex);
            if (mediaIndex == -1) return;

            // Here must check is the file exist, or unlimited loop happen [file not exist] -> [switch to SpeechMenuActivity] -> show network access dialog -> disallow -> [here] and so on.
            if (!fsm.isFilesReady(mediaIndex)) {
                String errMsg=String.format(getString(R.string.errRestorePlayStateFailLakeAudio), SpeechData.getSubtitleName(mediaIndex));
                Util.showErrorToast(LamrimReaderActivity.this, errMsg, 1000);
                setSubtitleViewText(errMsg);
                return;
            }
            Crashlytics.log(Log.DEBUG, logTag, "Call startPlay from onResume.");
            startPlay(mediaIndex);
        } else if (playMode == REGION_PLAY_MODE || playMode == GL_PLAY_MODE) {
            Crashlytics.log(Log.DEBUG, logTag, "play region mode, load media index " + mediaIndex);

            if (playMode == REGION_PLAY_MODE) {
                Crashlytics.log(Log.DEBUG, logTag, "Reload REGION_PLAY_MODE");
                playRecord = getSharedPreferences(getString(R.string.regionPlayModeRecordFile), 0);
                if (playRecord == null) return;
            } else {
                Crashlytics.log(Log.DEBUG, logTag, "Reload GL_PLAY_MODE");
                playRecord = getSharedPreferences(getString(R.string.GLModeRecordFile), 0);
                if (playRecord == null) return;
            }

            // Here must check is the file exist, or unlimited loop happen [file not exist] -> [switch to SpeechMenuActivity] -> show network access dialog -> disallow -> [here] and so on.
            int mStart = playRecord.getInt("startMediaIndex", -1);
            int mEnd = playRecord.getInt("endMediaIndex", -1);
            if (mStart == -1 || mEnd == -1) {
                Crashlytics.log(Log.DEBUG, logTag, "region start media=" + mStart + ", region end media=" + mEnd + ", skip load.");
                setSubtitleViewText(getString(R.string.errPlayFailArgErr));
                return;
            }
            Crashlytics.log(Log.DEBUG, logTag, "Check is file exist : " + mStart + ", " + mEnd);
            if (!fsm.isFilesReady(mStart) || !fsm.isFilesReady(mEnd)) {
                int leakMedia = -1;
                if (!fsm.isFilesReady(mStart)) leakMedia = mStart;
                else leakMedia = mEnd;
                String errMsg=String.format(getString(R.string.errPlayFailLakeAudio), SpeechData.getSubtitleName(leakMedia));
                Util.showErrorToast(LamrimReaderActivity.this, errMsg, 1000);
                setSubtitleViewText(errMsg);
                return;
            }

            int regionIndex = playRecord.getInt("regionIndex", 0);
            if (regionIndex == -1) regionIndex = 0;

            startRegionPlay(playRecord.getInt("startMediaIndex", -1),
                    playRecord.getInt("startMediaTime", -1),
                    playRecord.getInt("endMediaIndex", -1),
                    playRecord.getInt("endMediaTime", -1),
                    playRecord.getInt("theoryStartPage", -1),
                    playRecord.getInt("theoryStartLine", -1),
                    playRecord.getInt("theoryEndPage", -1),
                    playRecord.getInt("theoryEndLine", -1),
                    regionIndex,
                    playRecord.getString("title", "---")
            );
        }

        Crashlytics.log(Log.DEBUG, logTag, "**** Leave onResume() ****");
    }

    // I use the function check is the activity has load and ready for operation.
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            synchronized (isActivityLoaded) {
                isActivityLoaded = true;
            }
        }
    }

    public synchronized boolean isActivityLoaded() {
        synchronized (isActivityLoaded) {
            return isActivityLoaded;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Avoid memory leak
        hideMediaController(false);
        saveRuntime();

        try {
            if (mpController.getMediaPlayerState() == MediaPlayerController.MP_PLAYING)
                mpController.pause();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {

        Crashlytics.log(Log.DEBUG, logTag, "**** onDestroy ****");
        // fileDownloader.finish();
        mpController.finish();
        mpController = null;
        if (wakeLock.isHeld()) wakeLock.release();

        super.onDestroy();
        Crashlytics.log(Log.DEBUG, logTag, "**** onDestroy ****");
    }

    protected void saveRuntime() {
        Crashlytics.log(Log.DEBUG, logTag, "**** saveRuntime ****");
        SharedPreferences.Editor record = null;
        if (playMode == SPEECH_PLAY_MODE) {
            playRecord = getSharedPreferences(getString(R.string.speechModeRecordFile), 0);
            record = playRecord.edit();
        }
        if (playMode == REGION_PLAY_MODE) {
            playRecord = getSharedPreferences(getString(R.string.regionPlayModeRecordFile), 0);
            record = playRecord.edit();
        }
        if (playMode == GL_PLAY_MODE) {
            playRecord = getSharedPreferences(getString(R.string.GLModeRecordFile), 0);
            record = playRecord.edit();
        }

        Crashlytics.log(Log.DEBUG, logTag, "Save mediaIndex=" + mediaIndex);
        int bookPosition = bookView.getFirstVisiblePosition();
        View v = bookView.getChildAt(0);
        int bookShift = (v == null) ? 0 : v.getTop();
        runtimeEditor.putInt("bookPage", bookPosition);
        runtimeEditor.putInt("bookPageShift", bookShift);
        runtimeEditor.putInt(getString(R.string.playModeKey), playMode);
        runtimeEditor.commit();

        // The record will be null at first time switch to another activity after install
        if (record != null) {
            Crashlytics.log(Log.DEBUG, logTag, "MediaPlayer status=" + mpController.getMediaPlayerState());
            record.putInt("regionIndex", GLamrimSectIndex);
            record.putInt("mediaIndex", mediaIndex);
            // editor.putInt("playerStatus", mpController.getMediaPlayerState());
            if (mpController.getMediaPlayerState() > MediaPlayerController.MP_PREPARING) {
                int playPosition = mpController.getCurrentPosition();
                record.putInt("playPosition", playPosition);
            }
            record.commit();
        }
        Crashlytics.log(Log.DEBUG, logTag, "**** saveRuntime ****");
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                LamrimReaderActivity.this);
        builder.setTitle(getString(R.string.dlgExitTitle));
        builder.setMessage(getString(R.string.dlgExitMsg));
        builder.setPositiveButton(getString(R.string.dlgOk),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        saveRuntime();
                        if (wakeLock.isHeld()) wakeLock.release();
                        finish();
                    }
                });
        builder.setNegativeButton(getString(R.string.dlgCancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        builder.create().show();
        Crashlytics.log(Log.DEBUG, logTag, "**** onBackPressed ****");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // getSupportMenuInflater().inflate(R.menu.main, menu);
        // return super.onCreateOptionsMenu(menu);
        /*
        SubMenu rootMenu = menu.addSubMenu("");
        speechMenu = rootMenu.add(getString(R.string.menuStrSelectSpeech));
 //       globalLamrim = rootMenu.add(getString(R.string.globalLamrim));
        options = rootMenu.add(getString(R.string.menuOptions));
        setRegion = rootMenu.add(getString(R.string.menuStrSetRecord));
        playRegionRec = rootMenu.add(getString(R.string.menuStrPlayRegionRec));
        swRenderMode = rootMenu.add(getString(R.string.menuStrRenderMode));
        prjWeb = rootMenu.add(getString(R.string.menuStrOpenProjectWeb));
        exitApp = rootMenu.add(getString(R.string.exitApp));

        rootMenuItem = rootMenu.getItem();
        // rootMenuItem.setIcon(R.drawable.menu_down_48x48);
        options.setIcon(R.mipmap.ic_options);
        options.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        rootMenuItem.setIcon(R.mipmap.ic_menu_down);
        speechMenu.setIcon(R.mipmap.ic_speech);
//        globalLamrim.setIcon(R.drawable.ic_global_lamrim);
        setRegion.setIcon(R.mipmap.ic_region);
        playRegionRec.setIcon(R.mipmap.ic_region_play);
        swRenderMode.setIcon(R.mipmap.ic_render_mode);
        prjWeb.setIcon(R.mipmap.ic_project_web);
        exitApp.setIcon(R.mipmap.ic_exit_app);

        MenuItemCompat.setShowAsAction(rootMenuItem, MenuItem.SHOW_AS_ACTION_ALWAYS);
        getSupportActionBar().setCustomView(actionBarControlPanel);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        return super.onCreateOptionsMenu(menu);
        */
        getSupportActionBar().setCustomView(actionBarControlPanel);
        getSupportActionBar().setDisplayShowCustomEnabled(true);

        getMenuInflater().inflate(R.menu.lamrim_reader, menu);
        //if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        //    menu.findItem(R.id.menuOpts).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Crashlytics.log(Log.DEBUG, logTag, "****OptionsItemselected, select item=" + item.getItemId() + ", String=" + item.getTitle() + ", Order=" + item.getOrder() + " ****");
        // Here, thisActivity is the current activity


        if (Build.VERSION.SDK_INT >= 23)// 在點選選單的同時要求檔案授權，未授權不執行。
            if (ContextCompat.checkSelfPermission(LamrimReaderActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                //BaseDialogs.showSimpleErrorDialog(LamrimReaderActivity.this, getString(R.string.dlgNotGrantStorageYet), getString(R.string.dlgHintGrantStroage));
                BaseDialogs.showDialog(LamrimReaderActivity.this, getString(R.string.dlgNotGrantStorageYet), getString(R.string.dlgHintGrantStroage), new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int val) {
                        ActivityCompat.requestPermissions(LamrimReaderActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_ACCESS_PERMISSION_REQUEST);
                    }
                }, null, false);
                return true;
            }

        String gid = (String) item.getTitle();
        Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.MENU_CLICK, ((gid.length() == 0) ? "MENU_BUTTON" : gid) + "_PRESSED");

		/*  // Always show enabled menu item now.
		if (item.equals(rootMenuItem)) {
			Crashlytics.log(Log.DEBUG, logTag, "Create menu: can save region? " + mpController.isRegionPlay());
			if (RegionRecord.records.size() > 0) {
				playRegionRec.setEnabled(true);
				playRegionRec.setIcon(R.drawable.region);
			} else {
				playRegionRec.setEnabled(false);
				playRegionRec.setIcon(R.drawable.region_d);
			}
		}
		*/

        switch(item.getItemId()){
            case R.id.menuSpeech:
                startSpeechMenuActivity();
                break;
            case R.id.menuRegRec:
                if (mediaIndex == -1)
                    BaseDialogs.showSimpleErrorDialog(this, getString(R.string.dlgNotLoadAudioYet), getString(R.string.dlgHintLoadAudio));
                else
                    showOnRegionOptionDialog(mediaIndex, mpController.getCurrentPosition());
                break;
            case R.id.menuRegPlay:
                if (RegionRecord.records.size() > 0) showRecordListPopupMenu();
                else
                    BaseDialogs.showSimpleErrorDialog(this, getString(R.string.dlgNoRecord), getString(R.string.dlgHintRecord));
                break;
            case R.id.menuRender:
                switchMainView();
                break;
            case R.id.menuAbout:
                showAboutDialog();
                break;
            case R.id.menuExit:
                onBackPressed();
                break;
        }
        /*
        if (item.getTitle().equals(getString(R.string.menuStrSelectSpeech))) {
            startSpeechMenuActivity();
//        } else if (item.getTitle().equals(getString(R.string.globalLamrim))) {
//            startGlobalLamrimCalendarActivity();
        } else if (item.getTitle().equals(getString(R.string.menuStrSetRecord))) {
            if (mediaIndex == -1)
                BaseDialogs.showSimpleErrorDialog(this, getString(R.string.dlgNotLoadAudioYet), getString(R.string.dlgHintLoadAudio));
            else
                showOnRegionOptionDialog(mediaIndex, mpController.getCurrentPosition());

        } else if (item.getTitle().equals(getString(R.string.menuStrRenderMode))) {
            switchMainView();
        } else if (item.getTitle().equals(getString(R.string.menuStrPlayRegionRec))) {
            if (RegionRecord.records.size() > 0) showRecordListPopupMenu();
            else
                BaseDialogs.showSimpleErrorDialog(this, getString(R.string.dlgNoRecord), getString(R.string.dlgHintRecord));
        } else if (item.getTitle().equals(getString(R.string.menuStrOpenProjectWeb))) {
            startProjectWebUrl();
        } else if (item.getTitle().equals(getString(R.string.exitApp))) {
            onBackPressed();
            Log.d(funcLeave, "**** onOptionsItemSelected ****");
        }
         */

        Crashlytics.log(Log.DEBUG, logTag,"**** Into Options selected, select item=" + item.getItemId() + " ****");
        return true;
    }
    /*
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            final ImageButton themeSwitcher = (ImageButton) actionBarControlPanel.findViewById(R.id.themeSwitcher);
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                themeSwitcher.setVisibility(View.VISIBLE);

            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
                themeSwitcher.setVisibility(View.GONE);
            }
        }
    */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case STORAGE_ACCESS_PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void startSpeechMenuActivity() {
        final Intent speechMenu = new Intent(LamrimReaderActivity.this, SpeechMenuActivity.class);
//		if (wakeLock.isHeld())wakeLock.release();
        startActivityForResult(speechMenu, SPEECH_MENU_RESULT);
    }

    private void showAboutDialog(){
        final Intent aboutActivity = new Intent(LamrimReaderActivity.this, AboutActivity.class);
        startActivity(aboutActivity);
        /*
        LayoutInflater inflater = this.getLayoutInflater();
        View aboutView = inflater.inflate(R.layout.activity_about, null);

        TextView aboutVerText=aboutView.findViewById(R.id.aboutVerText);
        String versionStr=getString(R.string.version)+"："+ BuildConfig.VERSION_NAME+", "+ getString(R.string.resNum)+"："+ BuildConfig.VERSION_CODE;
        aboutVerText.setText(versionStr);

        TextView aboutMailToMe=aboutView.findViewById(R.id.aboutMailToMe);
        String mailToStr="%1$s：<a href=\"mailto:eyesblue@eyes-blue.com?&subject=%2$s&body=\n\n\n\n%3$s: %4$s, %5$s: %6$d\n%7$s: %8$s, Android%9$s\">eyesblue@eyes-blue.com</a>";
        mailToStr=String.format(mailToStr,getString(R.string.contactAuther), getString(R.string.app_name)+getString(R.string.contactAuther), getString(R.string.version), BuildConfig.VERSION_NAME, getString(R.string.resNum), BuildConfig.VERSION_CODE, getString(R.string.OSver), android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL, Build.VERSION.RELEASE+"("+Build.VERSION.SDK_INT+")");
        mailToStr=mailToStr.replaceAll("\n", "%0d%0a");
        Crashlytics.log(Log.DEBUG, logTag,"Mail to string: "+mailToStr);
        aboutMailToMe.setText(Html.fromHtml(mailToStr));
        aboutMailToMe.setMovementMethod(LinkMovementMethod.getInstance());

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(LamrimReaderActivity.this);
        dialogBuilder.setTitle(R.string.app_name)
                .setView(aboutView);
        dialogBuilder.create().show();

        */
    }

/*    private void startGlobalLamrimCalendarActivity() {
//		if (wakeLock.isHeld())wakeLock.release();
        final Intent calendarMenu = new Intent(LamrimReaderActivity.this, CalendarActivity.class);
        startActivityForResult(calendarMenu, GLOBAL_LAMRIM_RESULT);
    }
*/
/*
    private void startProjectWebUrl() {
//		if (wakeLock.isHeld())wakeLock.release();
        Uri uri = Uri.parse(getString(R.string.projectWebUrl));
        Intent it = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(it);
        } catch (android.content.ActivityNotFoundException ex) {
            Util.showErrorToast(LamrimReaderActivity.this, getString(R.string.errLakeBrowser));
        }
    }
*/

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Crashlytics.log(Log.DEBUG, logTag, "**** Into onActivityResult: Get result from: " + requestCode + " ****");

        if (resultCode == RESULT_CANCELED) {
            Crashlytics.log(Log.DEBUG, logTag, "User skip, do nothing.");
            return;
        }

        SharedPreferences.Editor record = null;
        switch (requestCode) {
            case SPEECH_MENU_RESULT:
                Crashlytics.log(Log.DEBUG, logTag, "Return from SPEECH_MENU_RESULT");
                if (intent == null) return;

                playMode = SPEECH_PLAY_MODE;
                playRecord = getSharedPreferences(getString(R.string.speechModeRecordFile), 0);
                int selected = -1, position = -1;
                // If the result intent indicate reload last state.
                if (intent.getBooleanExtra("reloadLastState", false)) {
                    selected = playRecord.getInt("mediaIndex", -1);
                    position = playRecord.getInt("playPosition", -1);
                } else {
                    selected = intent.getIntExtra("index", -1);
                    position = 0;
                }

                Crashlytics.log(Log.DEBUG, logTag, "OnResult: the user select index=" + selected);
                if (selected == -1 || position == -1) return;

                mpController.setPlayRegion(-1, -1);
                mpController.reset();
                mediaIndex = selected;
                GLamrimSectIndex = -1;


                record = playRecord.edit();
                record.putInt("mediaIndex", selected);
                record.putInt("playPosition", position);
                record.commit();

                runtimeEditor.putInt("playMode", playMode);
                runtimeEditor.commit();

                Crashlytics.log(Log.DEBUG, logTag, "Call reset player in onActivityResult.");
                //mpController.reset();
//			AnalyticsApplication.sendEvent("activity", "SpeechMenu_result", "select_index_"	+ selected, null);
                // After onActivityResult, the life-cycle will return to onStart,
                // do start downloader in OnResume.
                break;
            case SPEECH_MENU_RESULT_REGION: // the function seems never used.
                Crashlytics.log(Log.DEBUG, logTag, "Return from SPEECH_MENU_RESULT_REGION");
                mpController.reset();
                playMode = REGION_PLAY_MODE;
                mediaIndex = GLamrimSect[0][0];
                GLamrimSectIndex = 0;

                playRecord = getSharedPreferences(getString(R.string.regionPlayModeRecordFile), 0);
                record = playRecord.edit();
                record.putInt("mediaIndex", GLamrimSect[0][0]);
                record.putInt("playPosition", GLamrimSect[0][1]);
                record.commit();

                runtimeEditor.putInt("playMode", playMode);
                runtimeEditor.commit();

                Crashlytics.log(Log.DEBUG, logTag, "Mark theory: start page=" + theoryHighlightRegion[0] + " start line=" + theoryHighlightRegion[1] + ", offset=" + bookViewMountPoint[1]);

                //startPlay(mediaIndex);
                break;
            case GLOBAL_LAMRIM_RESULT:
                if (intent == null) return;

                playMode = GL_PLAY_MODE;
                playRecord = getSharedPreferences(getString(R.string.GLModeRecordFile), 0);
                record = playRecord.edit();
                if (glRecord == null) glRecord = new GlRecord();
                int playPosition = -1;

                if (intent.getBooleanExtra("reloadLastState", false)) {
                    int speechStartIndex = playRecord.getInt("startMediaIndex", -1);
                    int speechStartMs = playRecord.getInt("startMediaTime", -1);
                    int speechEndIndex = playRecord.getInt("endMediaIndex", -1);
                    int speechEndMs = playRecord.getInt("endMediaTime", -1);
                    int theoryStartPage = playRecord.getInt("theoryStartPage", -1);
                    int theoryStartLine = playRecord.getInt("theoryStartLine", -1);
                    int theoryEndPage = playRecord.getInt("theoryEndPage", -1);
                    int theoryEndLine = playRecord.getInt("theoryEndLine", -1);
                    // int = playRecord.getInt("regionIndex", -1);
                    actionBarTitle = playRecord.getString("title", "---");
                    playPosition = playRecord.getInt("playPosition", -1);
                    setRegionSec(speechStartIndex, speechStartMs, speechEndIndex, speechEndMs, theoryStartPage, theoryStartLine, theoryEndPage, theoryEndLine, actionBarTitle);
                } else {

                    glRecord.dateStart = intent.getStringExtra("dateStart");
                    glRecord.dateEnd = intent.getStringExtra("dateEnd");
                    glRecord.speechPositionStart = intent.getStringExtra("speechPositionStart");
                    glRecord.speechPositionEnd = intent.getStringExtra("speechPositionEnd");
                    glRecord.totalTime = intent.getStringExtra("totalTime");
                    glRecord.theoryLineStart = intent.getStringExtra("theoryLineStart");
                    glRecord.theoryLineEnd = intent.getStringExtra("theoryLineEnd");
                    glRecord.subtitleLineStart = intent.getStringExtra("subtitleLineStart");
                    glRecord.subtitleLineEnd = intent.getStringExtra("subtitleLineEnd");
                    glRecord.desc = intent.getStringExtra("desc");
                    String title = intent.getStringExtra("selectedDay");

                    String sec[] = title.split("/");
                    int[] speechStart = GlRecord.getSpeechStrToInt(glRecord.speechPositionStart);
                    Crashlytics.log(Log.DEBUG, logTag, "mediaIndex=" + GLamrimSect[0][0] + ", name=" + SpeechData.getSubtitleName(speechStart[0]));
                    actionBarTitle = getString(R.string.globalLamrimShortName) + ": " + sec[1] + "/" + sec[2] + " - " + SpeechData.getSubtitleName(speechStart[0]);//音檔部分顯示功能不正常
                    String regionInfo[] = glRecord.desc.split("……");
                    regionStartInfo = regionInfo[0].trim();
                    regionEndInfo = regionInfo[1].trim();
                    Crashlytics.log(Log.DEBUG, logTag, "Get data: " + glRecord);
                    setRegionSec(glRecord.speechPositionStart, glRecord.speechPositionEnd, glRecord.theoryLineStart, glRecord.theoryLineEnd, 0, actionBarTitle);


                    playPosition = GLamrimSect[0][1];
                }

                GLamrimSectIndex = 0;
                mediaIndex = GLamrimSect[0][0];

                Crashlytics.log(Log.DEBUG, logTag,"Set mediaIndex=" + mediaIndex + ", play position=" + GLamrimSect[0][0]);

                record.putInt("regionIndex", 0);
                record.putInt("mediaIndex", GLamrimSect[0][0]);
                record.putInt("playPosition", playPosition);
                record.commit();

                runtimeEditor.putInt("playMode", playMode);
                runtimeEditor.commit();

                mpController.reset();
                break;

            case SELECT_FG_PIC_RESULT:
                if (intent == null) return;
                final String filePath = intent.getStringExtra(FileDialogActivity.RESULT_PATH);
                File file = new File(filePath);

                if (!file.exists()) {
                    Util.showErrorToast(LamrimReaderActivity.this, getString(R.string.errOpenPicFail));
                    return;
                }

                renderView.setImageURI(Uri.fromFile(file));
                runtimeEditor.putString("renderImgFgPathKey", filePath);
                runtimeEditor.commit();
                break;
        }

        Crashlytics.log(Log.DEBUG, logTag,"Leave onActivityResult");
    }

    Thread startPlayThread = null;
    Object startPlayKey = new Object();

    public boolean startPlay(final int mediaIndex) {
        if (mediaIndex == -1) {
            Util.showErrorToast(LamrimReaderActivity.this, getString(R.string.errPlayFailArgErr), 1000);
            //Util.fireException("PLAY_EXCEPTION: the media index is -1.", new ArrayIndexOutOfBoundsException());
            return false;
        }
        // This avoid the unlimit loop that reload last state on onResume -> file not exist -> SpeechMenuActivity -> showDownloadDialog -> disallow -> onResume ... so on.
        if (!fsm.isFilesReady(mediaIndex)) {
            Util.showErrorToast(LamrimReaderActivity.this, getString(R.string.errPlayFailAudioNotFound), 1000);
            Crashlytics.log(Log.DEBUG, logTag, "startPlay: the media is not ready, skip play.");
            return false;
        }

        synchronized (startPlayKey) {

            if (startPlayThread != null && startPlayThread.isAlive()) {
                Crashlytics.log(Log.DEBUG, logTag, "The startPlay has a task running, skip the thread.");
                return false;
            }


            // It will not execute if there is the AsyncTask, maybe cause by only one UI thread.
//            startPlayThread = new Thread(new Runnable() {
//                @Override
//                public void run() {
            try {
                // Check duplicate load media.
						/*
						int loadingMedia=mpController.getLoadingMediaIndex();
						if(loadingMedia==mediaIndex){
							Crashlytics.log(Log.DEBUG, logTag,"The media index "+mediaIndex+" has loading, skip this procedure.");
							AnalyticsApplication.sendEvent("error", "loading_media",	"duplicate_thread", 1);
							return ;
						}*/

                Crashlytics.log(Log.DEBUG, logTag, "Start play index " + mediaIndex);
                // put the controller panel down, if the controller panel already been pin.
                mpController.setShowLongTerm(false);
                // Reset subtitle to SUBTITLE_MODE
                bookView.clearHighlightLine();
                setSubtitleViewMode(SUBTITLE_MODE);
                setSubtitleViewText(getString(R.string.dlgDescPrepareSpeech));
                Crashlytics.log(Log.DEBUG, logTag, Thread.currentThread().getName() + " setDataSource.");
                //mpController.setDataSource(getApplicationContext(),	mediaIndex);

                mpController.release();
                createMpController();
                mpController.setDataSource(LamrimReaderActivity.this, mediaIndex);
            } catch (IllegalArgumentException e) {
                setSubtitleViewText(getString(R.string.errIAEwhileSetPlayerSrc));
                Crashlytics.log(Log.ERROR, logTag,"IllegalArgumentException happen while prepare media "+SpeechData.getSubtitleName(mediaIndex));
                Crashlytics.logException(e);
                e.printStackTrace();
            } catch (SecurityException e) {
                setSubtitleViewText(getString(R.string.errSEwhileSetPlayerSrc));
                Crashlytics.log(Log.ERROR, logTag,"SecurityException happen while prepare media "+SpeechData.getSubtitleName(mediaIndex));
                Crashlytics.logException(e);
                e.printStackTrace();
            } catch (IllegalStateException e) {
                setSubtitleViewText(getString(R.string.errISEwhileSetPlayerSrc));
                Crashlytics.log(Log.ERROR, logTag,"IllegalStateException happen while prepare media "+SpeechData.getSubtitleName(mediaIndex));
                Crashlytics.logException(e);
                e.printStackTrace();
            } catch (IOException e) {
                setSubtitleViewText(String.format(getString(R.string.errIOEwhileSetPlayerSrc), SpeechData.getNameId(mediaIndex)));
                Crashlytics.log(Log.ERROR, logTag,"IOException happen while prepare media "+SpeechData.getSubtitleName(mediaIndex));
                Crashlytics.logException(e);
                e.printStackTrace();
            } catch (Exception e) {
                setSubtitleViewText(getString(R.string.errＷhilePlayMedia));
                Crashlytics.log(Log.ERROR, logTag,"Unknown Exception happen while prepare media "+SpeechData.getSubtitleName(mediaIndex));
                Crashlytics.logException(e);
                e.printStackTrace();
            }
        }// synchronized
        return true;
    }


	/*
	 * Don't do this with AsyncTask, there is only one AsyncTask alive, it will cause app response slowly.
	public boolean startPlay(final int mediaIndex) {
		File f = FileSysManager.getLocalMediaFile(mediaIndex);
		if (f == null || !f.exists()) {
			Log.d(getClass().getName(),"startPlay: the media is not exist, skip play.");
			return false;
		}

		AsyncTask<Void, Void, Void> runner = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					// Check duplicate load media.
					int loadingMedia=mpController.getLoadingMediaIndex();
					if(loadingMedia==mediaIndex){
						Crashlytics.log(Log.DEBUG, logTag,"The media index "+mediaIndex+" has loading, skip this procedure.");
						return null;
					}

					Crashlytics.log(Log.DEBUG, logTag,"Start play index "+mediaIndex);
					// Reset subtitle to SUBTITLE_MODE
					bookView.clearHighlightLine();
					setSubtitleViewMode(SUBTITLE_MODE);
					setSubtitleViewText(getString(R.string.dlgDescPrepareSpeech));
					mpController.setDataSource(getApplicationContext(),	mediaIndex);
				} catch (IllegalArgumentException e) {
					setSubtitleViewText(getString(R.string.errIAEwhileSetPlayerSrc));
					AnalyticsApplication.sendEvent("error", "player_error",	"IllegalArgumentException", null);
					e.printStackTrace();
				} catch (SecurityException e) {
					setSubtitleViewText(getString(R.string.errSEwhileSetPlayerSrc));
					AnalyticsApplication.sendEvent("error", "player_error",	"SecurityException", null);
					e.printStackTrace();
				} catch (IllegalStateException e) {
					setSubtitleViewText(getString(R.string.errISEwhileSetPlayerSrc));
					AnalyticsApplication.sendEvent("error", "player_error",	"IllegalStateException", null);
					e.printStackTrace();
				} catch (IOException e) {
					setSubtitleViewText(getString(R.string.errIOEwhileSetPlayerSrc));
					AnalyticsApplication.sendEvent("error", "player_error", "IOException",null);
					e.printStackTrace();
				}
				// }
				return null;
			}
		};
		runner.execute();
		return true;
	}
*/

    private void setSubtitleViewMode(final int mode) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mode == SUBTITLE_MODE) {
                    subtitleViewRenderMode = SUBTITLE_MODE;
                    subtitleView.setMovementMethod(null);
                    subtitleView.setVerticalScrollBarEnabled(false);
                    subtitleView.setHeight(subtitleView.getLineHeight());
                    subtitleView.setGravity(Gravity.CENTER);
                } else if (mode == READING_MODE) {
                    subtitleViewRenderMode = READING_MODE;
                    subtitleView.setMovementMethod(ScrollingMovementMethod.getInstance());
                    subtitleView.setScrollBarStyle(TextView.SCROLLBARS_INSIDE_OVERLAY);
                    subtitleView.setVerticalScrollBarEnabled(true);
                    subtitleView.setGravity(Gravity.LEFT);
                    setSubtitleViewText(readingModeAllSubtitle);
                }
            }
        });
    }

    public void setSubtitleViewText(final CharSequence s) {
        runOnUiThread(new Runnable() {
            public void run() {
                subtitleView.setText(s);
                int lineCount = subtitleView.getLineCount();// There will return
                // 0 sometimes.
                if (lineCount < 1)
                    lineCount = 1;
                subtitleView.setHeight(subtitleView.getLineHeight() * lineCount);
            }
        });
    }

    private void showSetTextSizeDialog() {
        LayoutInflater factory = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View v = factory.inflate(R.layout.set_text_size_dialog_view, null);
        RadioButton defFont = v.findViewById(R.id.defFont);
        RadioButton bktFont = v.findViewById(R.id.bktFont);
        bktFont.setTypeface(Typeface.createFromAsset(this.getAssets(), "BKT_Lamrim.ttf"));
        int fontOpt = runtime.getInt(getString(R.string.fontKey), getResources().getInteger(R.integer.defFontProp));
        if (fontOpt == getResources().getInteger(R.integer.defFontProp)) defFont.setChecked(true);
        else bktFont.setChecked(true);

        RadioGroup fontRadioGroup = v.findViewById(R.id.fontRadioGroup);
        fontRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                if (i == R.id.defFont) {
                    Crashlytics.log(Log.DEBUG, logTag, "User click default font.");
                    runtimeEditor.putInt(getString(R.string.fontKey), getResources().getInteger(R.integer.defFontProp));
                }
                if (i == R.id.bktFont) {
                    Crashlytics.log(Log.DEBUG, logTag, "User click BKT font.");
                    runtimeEditor.putInt(getString(R.string.fontKey), getResources().getInteger(R.integer.bktFontProp));
                }
                runtimeEditor.apply();
                bookView.rebuildView();
            }
        });

        final SeekBar theorySb = (SeekBar) v.findViewById(R.id.theorySizeBar);
        final SeekBar subtitleSb = (SeekBar) v.findViewById(R.id.subtitleSizeBar);
//		final int orgTheorySize = runtime.getInt(getString(R.string.bookFontSizeKey),
//				getResources().getInteger(R.integer.defFontSize)) - getResources().getInteger(R.integer.textMinSize);
        final int orgTheorySize = runtime.getInt(getString(R.string.bookFontSizeKey), textDefSize - textMinSize);
//		final int orgSubtitleSize = runtime.getInt(getString(R.string.subtitleFontSizeKey), getResources().getInteger(R.integer.defFontSize))
//				- getResources().getInteger(R.integer.textMinSize);
        final int orgSubtitleSize = runtime.getInt(getString(R.string.subtitleFontSizeKey), textDefSize - textMinSize);
//		final int textMaxSize = getResources().getInteger(R.integer.textMaxSize) - getResources().getInteger(R.integer.textMinSize);

        Crashlytics.log(Log.DEBUG, logTag, "Set theory size Max=" + (textMaxSize) + ", orgSize="
                + orgTheorySize + ", subtitle size Max=" + textMaxSize
                + ", orgSize=" + orgSubtitleSize);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                theorySb.setMax(textMaxSize - textMinSize);
                subtitleSb.setMax(textMaxSize - textMinSize);
                theorySb.setProgress(orgTheorySize - textMinSize);
                subtitleSb.setProgress(orgSubtitleSize - textMinSize);
            }
        });

        OnSeekBarChangeListener sbListener = new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, boolean fromUser) {
                if (!fromUser)
                    return;
                //final int minSize = getResources().getInteger(R.integer.textMinSize);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Crashlytics.log(Log.DEBUG, logTag,
                                "Seek bar get progress: " + progress + ", min size: "
                                        //+ getResources().getInteger(R.integer.textMinSize)
                                        + textMinSize
                                        + ", add:" + (progress + textMinSize));
                        if (seekBar.equals(theorySb)) {
                            bookView.setTextSize(progress + textMinSize);
                            bookView.refresh();
                        }
                        // theorySample.setTextSize;
                        else {
                            if (subtitleViewRenderMode == SUBTITLE_MODE) {
                                int lineCount = subtitleView.getLineCount();// There will return 0 sometimes.
                                if (lineCount < 1)
                                    lineCount = 1;
                                subtitleView.setHeight(subtitleView.getLineHeight() * lineCount);
                            }
                            Crashlytics.log(Log.DEBUG, logTag, "set font size " + (progress + textMinSize));
                            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, progress + textMinSize);
                        }
                        seekBar.setProgress(progress);
                    }
                });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                runtimeEditor.putInt(getString(R.string.bookFontSizeKey), (int) bookView.getTextSize());
                runtimeEditor.commit();
                Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.STATISTICS, "Change BookView text size with seek bar.");
            }
        };
        theorySb.setOnSeekBarChangeListener(sbListener);
        subtitleSb.setOnSeekBarChangeListener(sbListener);

        // dialog.show();

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog setTextSizeDialog = builder.create();
        setTextSizeDialog.setTitle(getString(R.string.menuOptions));
        setTextSizeDialog.setView(v);
        WindowManager.LayoutParams lp = setTextSizeDialog.getWindow().getAttributes();
        lp.alpha = 0.7f;
        setTextSizeDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Crashlytics.log(Log.DEBUG, logTag, "Write theory size: " + (int) theorySb.getProgress()
                        + ", subtitle size: " + subtitleSb.getProgress() + " to runtime.");
                runtimeEditor.putInt(getString(R.string.bookFontSizeKey), theorySb.getProgress()
                        //+ getResources().getInteger(R.integer.textMinSize));
                        + textMinSize);
                runtimeEditor.putInt(getString(R.string.subtitleFontSizeKey), subtitleSb.getProgress()
                        //+ getResources().getInteger(R.integer.textMinSize));
                        + textMinSize);
                runtimeEditor.commit();

                //textSize.setBackgroundColor(Color.BLACK);
                textSize.setSelected(false);
                Crashlytics.log(Log.DEBUG, logTag, "Check size after write to db: theory size: "
                        + runtime.getInt(getString(R.string.bookFontSizeKey), 0) + ", subtitle size: "
                        + runtime.getInt(getString(R.string.subtitleFontSizeKey), 0));
                // updateTextSize();
                try {
                    dialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }    // Don't force close if problem here.
            }
        });
        setTextSizeDialog.setCanceledOnTouchOutside(true);
        setTextSizeDialog.show();
    }

    private boolean loadSearchObj(final ProgressDialog pd) {
        final File subSearchCache = fsm.getSubtitleSearchCacheFile();
        if (subSearchCache.exists()) {    // 嘗試從快取檔案取回字幕搜尋快取物件
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pd.setTitle(getString(R.string.loadingData));
                    pd.setMessage(getString(R.string.loadingSubtitles));
                    pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    pd.show();
                }
            });

            final long startLoadTime = System.currentTimeMillis();
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(subSearchCache));
                subtitleSearch = (SubtitleSearch[]) ois.readObject();
            } catch (Exception e) {
                subSearchCache.delete();
                Util.showErrorToast(LamrimReaderActivity.this, getString(R.string.errLoadCatchFail));
                e.printStackTrace();
                return false;
            }
            Util.fireTimming(LamrimReaderActivity.this, mFirebaseAnalytics, getClass().getName(),Util.SPEND_TIME,"LOAD_ALL_SUBTITLE_FROM_CACHE", (int)(System.currentTimeMillis() - startLoadTime));
        } else {  // 從字幕檔案中重建字幕搜尋物件

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pd.setTitle(getString(R.string.loadingData));
                    pd.setMessage(getString(R.string.loadingSubtitles));
                    pd.setMax(320);
                    pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    pd.show();
                }
            });

            final long startLoadTime = System.currentTimeMillis();
            boolean noLeak = true;
            for (int i = 0; i < 320; i++) {
                // 若第i個字幕已經讀取到subtitleSearch物件中則不重複讀取
                if (subtitleSearch[i] != null) {
                    pd.incrementProgressBy(1);
                    noLeak = false;
                    continue;
                }

                subtitleSearch[i] = new SubtitleSearch(Util.loadSubtitle(LamrimReaderActivity.this, i));
                pd.incrementProgressBy(1);
                if (Thread.currentThread().isInterrupted()) return false;
            }

            if (noLeak)
                Util.fireTimming(LamrimReaderActivity.this, mFirebaseAnalytics, getClass().getName(),Util.SPEND_TIME,"LOAD_ALL_SUBTITLE_FROM_SRT_FILES", (int)(System.currentTimeMillis() - startLoadTime));

            // Write the object to disk with a new thread.
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final long startLoadTime = System.currentTimeMillis();
                    try {
                        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(subSearchCache));
                        oos.writeObject(subtitleSearch);
                        oos.flush();
                        oos.close();
                        System.out.println("Write finish");
                    } catch (Exception e) {
                        e.printStackTrace();
                        //Util.fireException("Error happen while write cache file of subtitle search object.", e);
                        return;
                    }
                    Util.fireTimming(LamrimReaderActivity.this, mFirebaseAnalytics, getClass().getName(),Util.SPEND_TIME,"SAVE_SUBTITLE_CACHE_TO_FILE", (int)(System.currentTimeMillis() - startLoadTime));
                }
            }).start();

        }

        if (Thread.currentThread().isInterrupted()) return false;
        return true;
    }

    private void showSearchSubtitleDialog(final View searchView, final String string) {
        final ProgressDialog pd = new ProgressDialog(this);

        Runnable run = new Runnable() {
            @Override
            public void run() {
                // =============== 載入字幕資訊 ==============
                boolean isSearchReady = true;
                for (int i = 0; i < 320; i++)   // 檢查記憶體中是否320個搜尋物件是否都存在
                    if (subtitleSearch[i] == null) {
                        isSearchReady = false;
                        break;
                    }

                long loadStartTime = System.currentTimeMillis();
                if (!isSearchReady) if (!loadSearchObj(pd)) {
                    Crashlytics.log(Log.DEBUG, logTag, "User cancel the build data procedure.");
                    return;
                }
                final long loadTime = System.currentTimeMillis() - loadStartTime;
                subtitleSearchResult.clear();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Thread t = new Thread() {
                            @Override
                            public void run() {
                                final SubtitleSearchAdapter subtitleSearchAdapter;
                                int counter = 0;
                                long searchStartTime = System.currentTimeMillis();

                                for (int i = 0; i < 320; i++) {
                                    int[][] res = subtitleSearch[i].search(string);
                                    pd.incrementProgressBy(1);
                                    if (res == null) continue;

                                    for (int j = 0; j < res.length; j++)
                                        subtitleSearchResult.add(new SubtitleSearchIndex(i, res[j], string.length()));

                                    counter += res.length;
                                }

                                final long searchTime = System.currentTimeMillis() - searchStartTime;
                                Crashlytics.log(Log.DEBUG, logTag, "Search result count: " + counter + ", spend: " + searchTime + "ms.");

                                ArrayList<HashMap<String, String>> subtitleSearchFakeList = new ArrayList<>();
                                for (int i = 0; i < subtitleSearchResult.size(); ++i)
                                    subtitleSearchFakeList.add(fakeSample);
                                subtitleSearchAdapter = new SubtitleSearchAdapter(LamrimReaderActivity.this, subtitleSearchFakeList,
                                        android.R.layout.simple_list_item_2, new String[]{"title", "desc"},
                                        new int[]{android.R.id.text1, android.R.id.text2});
                                // subtitleSearchAdapter.setSubtitleSearchIndex(subtitleSearchResult);
                                // ========================= 建立統計資訊與 UI ===============================
                                NumberFormat nf = NumberFormat.getInstance();
                                nf.setMaximumFractionDigits(3);    //小數後3位
                                String loadTimeStr = nf.format((double) loadTime / 1000);
                                String timeStr = nf.format((double) searchTime / 1000);
                                String text = String.format(getString(R.string.searchSummary), ""+counter, loadTimeStr, timeStr);
                                final SpannableString str = new SpannableString(text);
                                str.setSpan(new ForegroundColorSpan(Color.GREEN), 0, text.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

                                // =========================================================================
                                final ListView subtitleSearchList = (ListView) searchView.findViewById(R.id.listView);
                                subtitleSearchList.setOnItemClickListener(new OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        int pos = position - subtitleSearchList.getHeaderViewsCount();

                                        SubtitleSearchIndex ssi = subtitleSearchResult.get(pos);
                                        // =================== Check are files ready ====================
                                        if (!fsm.isFilesReady(mediaIndex)) {
                                            Util.showErrorToast(LamrimReaderActivity.this, String.format(getString(R.string.errFailToSwitchAudio), SpeechData.getSubtitleName(ssi.mediaIndex)));
                                            return;
                                        }
                                        // ==============================================================
                                        //SubtitleSearch sse = subtitleSearch[ssi.mediaIndex];
                                        //playNormalMode(ssi.mediaIndex, ssi.startTime);


                                        SubtitleSearch sse = subtitleSearch[ssi.mediaIndex];
                                        SubtitleElement se = sse.getSubtitle(ssi.getSubtitleIndex());
                                        playNormalMode(ssi.mediaIndex, se.startTimeMs);
                                    }
                                });

                                if (subtitleSearchHeaderTextView == null) {
                                    subtitleSearchHeaderTextView = new TextView(LamrimReaderActivity.this);
                                    subtitleSearchHeaderTextView.setGravity(Gravity.CENTER);
                                    subtitleSearchList.addFooterView(subtitleSearchHeaderTextView);
                                }


                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        subtitleSearchHeaderTextView.setText(str);
                                        if (subtitleSearchList.getHeaderViewsCount() == 0)
                                            subtitleSearchList.addHeaderView(subtitleSearchHeaderTextView, null, false);

                                        subtitleSearchList.setAdapter(subtitleSearchAdapter);
                                        pd.dismiss();
                                        subtitleSearchList.setEnabled(true);
                                        subtitleSearchList.setVisibility(View.VISIBLE);
                                    }
                                });
                                Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(), Util.BUTTON_CLICK,"SEARCH_SUBTITLE");
                            }
                        };
                        t.start();
                    }
                });
            }
        };

        final Thread t = new Thread(run);
        pd.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (t.isAlive()) {
                    t.interrupt();
                    Crashlytics.log(Log.DEBUG, logTag, "Stop build data thread of subtitle search.");
                }
            }
        });
        t.start();
    }

    private void searchFromHost(final View searchView, final String searchStr) {
        final ProgressDialog pd = ProgressDialog.show(this, getString(R.string.msgSearchSendingReq), getString(R.string.msgPlsWait), true);

        Thread t = new Thread(new Runnable() {

            private void setSearchNextBtnEnable(final ImageButton searchNextBtn){
                LamrimReaderActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        searchNextBtn.setEnabled(true);
                    }
                });
            }

            @Override
            public void run() {
                final ImageButton searchNextBtn = (ImageButton) searchView.findViewById(R.id.searchNextBtn);
                final ListView subtitleSearchList = (ListView) searchView.findViewById(R.id.listView);
                HttpURLConnection connection = null;
                String urlStr = null;
                JSONObject json = null;
                long startTime = System.currentTimeMillis();
                try {
                    urlStr = getString(R.string.searchHost) + "?fmt=json&str=" + URLEncoder.encode(searchStr, "UTF-8");
                } catch (UnsupportedEncodingException uee) {
                    pd.dismiss();
                    Util.showErrorToast(LamrimReaderActivity.this, getString(R.string.msgUTF8EncodeUnsupport));
                    setSearchNextBtnEnable(searchNextBtn);
                    return;
                }
                try {
                    URL url = new URL(urlStr);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("Accept-Charset", "utf-8,*");
                    Crashlytics.log(Log.DEBUG, logTag, url.toString());
                    Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(), Util.BUTTON_CLICK,"SEARCH_LAMRIM_FROM_SERVER");
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder stringBuilder = new StringBuilder();
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            stringBuilder.append(line).append("\n");
                        }
                        bufferedReader.close();
                        Crashlytics.log(Log.DEBUG, logTag, stringBuilder.toString());
                        json = new JSONObject(stringBuilder.toString());
                    } finally {
                        connection.disconnect();
                    }
                } catch (IOException | JSONException  e) {
                    pd.dismiss();
                    setSearchNextBtnEnable(searchNextBtn);
                    Util.showErrorToast(LamrimReaderActivity.this, getString(R.string.msgSendReqIOErr));
                    Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(), Util.STATISTICS,"SEARCH_LAMRIM_FROM_SERVER_FAILURE");
                    Crashlytics.log(Log.ERROR, logTag,"Error happen while search Lamrim from server.");
                    Crashlytics.log(Log.ERROR, logTag, e.getMessage());
                    Crashlytics.logException(e);
                    e.printStackTrace();
                    return;
                }
                if (json == null) {
                    pd.dismiss();
                    setSearchNextBtnEnable(searchNextBtn);
                    return;
                }

                long loadTime = System.currentTimeMillis() - startTime;
                int count = -1, searchTime = -1;
                JSONArray jArray = null;
                try {
                    count = json.getInt("count");
                    searchTime = json.getInt("spendTime");
                    jArray = (JSONArray) json.get("list");
                } catch (JSONException jse) {
                    pd.dismiss();
                    setSearchNextBtnEnable(searchNextBtn);
                    Util.showErrorToast(LamrimReaderActivity.this, getString(R.string.msgJsonParseErr));
                    Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(), Util.BUTTON_CLICK,"SEARCH_LAMRIM_FROM_SERVER_FAILURE");
                    jse.printStackTrace();
                    return;
                }

                if (count < 1) {
                    pd.dismiss();
                    runOnUiThread(new Runnable(){
                        @Override
                        public void run(){
                            subtitleSearchList.setAdapter(null);
                            subtitleSearchList.invalidate();
                        }
                    });
                    setSearchNextBtnEnable(searchNextBtn);
                    Util.showInfoToast(LamrimReaderActivity.this, getString(R.string.msgSearchNotFound));
                    return;
                }

                final JSONArray ssi = jArray;
                final ArrayList<RemoteSubtitleSearchIndex> subtitleSearchResult = new ArrayList<>();
                try {
                    for (int i = 0; i < count; i++) {
                        JSONObject jobj = ssi.getJSONObject(i);
                        int mediaIndex = jobj.getInt("mediaId");
                        int subSerlNum = jobj.getInt("subSerlNum");
                        int textIndex = jobj.getInt("startTime");
                        String subtitleText = jobj.getString("text");
                        int indexOfSubtitle = jobj.getInt("textIndex");
                        int lenOfSearchStr = searchStr.length();
                        subtitleSearchResult.add(new RemoteSubtitleSearchIndex(mediaIndex, subSerlNum, textIndex, subtitleText, indexOfSubtitle, lenOfSearchStr));
                    }
                } catch (JSONException jse) {
                    pd.dismiss();
                    setSearchNextBtnEnable(searchNextBtn);
                    Util.showErrorToast(LamrimReaderActivity.this, getString(R.string.msgJsonParseErr));
                    //Crashlytics.setString("ButtonClick", "SEARCH_LAMRIM_FROM_SERVER_FAILURE_PARSE_ERROR");
                    Crashlytics.log(Log.ERROR, logTag,"Parse JSON data failure that from Lamrim search server.");
                    Crashlytics.logException(jse);
                    Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(), Util.BUTTON_CLICK,"SEARCH_LAMRIM_FROM_SERVER_FAILURE_PARSE_ERROR");
                    jse.printStackTrace();
                    return;
                }

                Crashlytics.log(Log.DEBUG, logTag, "Search result count: " + count + ", Load time: " + loadTime + ", spend: " + searchTime + "ms.");

                ArrayList<HashMap<String, String>> subtitleSearchFakeList = new ArrayList<>();
                for (int i = 0; i < jArray.length(); ++i)
                    subtitleSearchFakeList.add(fakeSample);
                final RemoteSubtitleSearchAdapter subtitleSearchAdapter = new RemoteSubtitleSearchAdapter(LamrimReaderActivity.this, subtitleSearchFakeList,
                        android.R.layout.simple_list_item_2, new String[]{"title", "desc"},
                        new int[]{android.R.id.text1, android.R.id.text2});
                subtitleSearchAdapter.setSubtitleSearchIndex(subtitleSearchResult);

                // ========================= 建立統計資訊與 UI ===============================
                NumberFormat nf = NumberFormat.getInstance();
                nf.setMaximumFractionDigits(3);    //小數後3位
                String loadTimeStr = nf.format((double) loadTime / 1000);
                String timeStr = nf.format((double) searchTime / 1000);
                String text = String.format(getString(R.string.searchSummary), ""+count, loadTimeStr, timeStr);
                final SpannableString str = new SpannableString(text);
                str.setSpan(new ForegroundColorSpan(ContextCompat.getColor(LamrimReaderActivity.this, R.color.searchResultTimeSummary)), 0, text.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

                // =========================================================================
                subtitleSearchList.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        int pos = position - subtitleSearchList.getHeaderViewsCount();
                        try {
                            JSONObject data = ssi.getJSONObject(pos);
                            // =================== Check are files ready ====================
                            if (!fsm.isFilesReady(data.getInt("mediaId"))) {
                                pd.dismiss();
                                setSearchNextBtnEnable(searchNextBtn);
                                Util.showErrorToast(LamrimReaderActivity.this, String.format(getString(R.string.errFailToSwitchAudio), SpeechData.getSubtitleName(data.getInt("mediaId"))));
                                return;
                            }
                            playNormalMode(data.getInt("mediaId"), data.getInt("startTime"));
                        } catch (JSONException jse) {
                            pd.dismiss();
                            setSearchNextBtnEnable(searchNextBtn);
                            Util.showErrorToast(LamrimReaderActivity.this, getString(R.string.msgJsonParseErr));
                            Crashlytics.log(Log.ERROR, logTag,"JSON data parse failure that from Lamrim search server.");
                            return;
                        }
                    }
                });

                if (subtitleSearchHeaderTextView == null) {
                    subtitleSearchHeaderTextView = new TextView(LamrimReaderActivity.this);
                    subtitleSearchHeaderTextView.setGravity(Gravity.CENTER);
                    subtitleSearchList.addFooterView(subtitleSearchHeaderTextView);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.dismiss();
                        subtitleSearchHeaderTextView.setText(str);
                        if (subtitleSearchList.getHeaderViewsCount() == 0)
                            subtitleSearchList.addHeaderView(subtitleSearchHeaderTextView, null, false);

                        subtitleSearchList.setAdapter(subtitleSearchAdapter);
                        subtitleSearchList.setEnabled(true);

                        subtitleSearchList.invalidate();
                        subtitleSearchList.setVisibility(View.VISIBLE);
                        searchNextBtn.setEnabled(true);
                    }
                });
            }
        });
        t.start();

    }

    //TextView lamrimLabel=null;
    //TextView subtitleLabel=null;
    private void showSearchDialog() {
        Crashlytics.log(Log.DEBUG, logTag, "Into showSearchDialog.");
        LayoutInflater factory = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View searchView = factory.inflate(R.layout.search_view, null);
        final TextView lamrimLabel = (TextView) searchView.findViewById(R.id.lamrimLabel);
        final TextView subtitleLabel = (TextView) searchView.findViewById(R.id.subtitleLabel);
        final SeekBar sw = (SeekBar) searchView.findViewById(R.id.seekBar);
        final ImageButton searchLastBtn = (ImageButton) searchView.findViewById(R.id.searchLastBtn);
        final ImageButton searchNextBtn = (ImageButton) searchView.findViewById(R.id.searchNextBtn);
        final EditText searchInput = (EditText) searchView.findViewById(R.id.searchInput);
        final CheckBox searchFrom = (CheckBox) searchView.findViewById(R.id.searchFrom);
        final ListView subtitleSearchList = (ListView) searchView.findViewById(R.id.listView);
        final SearchListener onSearchLamrimListener = new SearchListener(searchLastBtn, searchNextBtn, searchInput);
        final String locale = getResources().getConfiguration().locale.getCountry();

        final Runnable swToSearchLamrim = new Runnable() {
            @Override
            public void run() {
                lamrimLabel.setEnabled(true);
                subtitleLabel.setEnabled(false);
                searchFrom.setEnabled(false);
                sw.setProgress(0);
                searchLastBtn.setVisibility(View.VISIBLE);
                searchNextBtn.setOnClickListener(onSearchLamrimListener);
                searchLastBtn.setOnClickListener(onSearchLamrimListener);
                subtitleSearchList.setVisibility(View.GONE);
                runtimeEditor.putBoolean("isLastSearchLamrim", true);
                runtimeEditor.commit();
            }
        };

        final Runnable swToSearchSubtitle = new Runnable() {
            @Override
            public void run() {
                lamrimLabel.setEnabled(false);
                subtitleLabel.setEnabled(true);
                searchFrom.setEnabled(true);
                sw.setProgress(1);
                searchLastBtn.setVisibility(View.GONE);
                searchNextBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String str = searchInput.getText().toString();
                        if (str.length() == 0) return;
                        if (str.length() < 2) {
                            Util.showErrorToast(LamrimReaderActivity.this, getString(R.string.msgSearchStrLenMustMoreThen2));
                            return;
                        }
                        long lastSearchTime = runtime.getLong(getString(R.string.lastSearchTimeKey), 0);
                        if (System.currentTimeMillis() - lastSearchTime < getResources().getInteger(R.integer.netSearchInterval)) // Click search too short time.
                            return;

                        // Save the time for check next button fire.
                        runtimeEditor.putLong(getString(R.string.lastSearchTimeKey), System.currentTimeMillis());
                        runtimeEditor.apply();

                        subtitleSearchList.setEnabled(false);
                        subtitleSearchList.setSelectionAfterHeaderView();

                        if (!searchFrom.isChecked()) {
                            showSearchSubtitleDialog(searchView, Util.simpToTradChar(LamrimReaderActivity.this, str));
                        } else {
                            // Search from Lamrim Search Service.
                            searchNextBtn.setEnabled(false);
                            searchFromHost(searchView, Util.simpToTradChar(LamrimReaderActivity.this, str));
                        }
                    }
                });
                searchLastBtn.setOnClickListener(null);
                if (subtitleSearchList.getAdapter() != null)
                    subtitleSearchList.setVisibility(View.VISIBLE);
                runtimeEditor.putBoolean("isLastSearchLamrim", false);
                runtimeEditor.commit();
            }
        };

        sw.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                if (progress == 0) {// Search Lamrim
                    swToSearchLamrim.run();
                } else {// Search Subtitle
                    swToSearchSubtitle.run();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        searchFrom.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Crashlytics.log(Log.DEBUG, logTag, "User change the state of CheckBox [searchFrom].");
                runtimeEditor.putBoolean(getString(R.string.searchFromKey), isChecked);
                runtimeEditor.commit();
            }
        });

        // 回存最後搜尋的字串
        String lastSearch = runtime.getString(getString(R.string.lastSearchLamrimKey), "");
        searchInput.setText(lastSearch);

        // 回存最後的搜尋目標狀態
        boolean isSearchLamrim = runtime.getBoolean("isLastSearchLamrim", true);
        if (isSearchLamrim) {
            sw.setProgress(0);
            swToSearchLamrim.run();
        } else {
            sw.setProgress(1);
            swToSearchSubtitle.run();
        }

        // 回存從何處取得資料CheckBox 善知識
        Boolean searchFromNet = runtime.getBoolean(getString(R.string.searchFromKey), true);
        searchFrom.setChecked(searchFromNet);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //AlertDialog searchDialog = builder.create();
        searchDialog = builder.create();
        searchDialog.setView(searchView);
        searchDialog.setCanceledOnTouchOutside(true);
        WindowManager.LayoutParams lp = searchDialog.getWindow().getAttributes();
        lp.alpha = 0.8f;
        searchDialog.getWindow().setAttributes(lp);
        searchDialog.setCanceledOnTouchOutside(true);
        searchDialog.show();
        searchDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                runtimeEditor.putString(getString(R.string.lastSearchLamrimKey), searchInput.getText().toString());
                runtimeEditor.commit();
                searchLastBtn.setEnabled(true);
                searchNextBtn.setEnabled(true);
                search.setSelected(false);
                //search.setBackgroundColor(Color.BLACK);
            }
        });
    }

    class SearchListener implements OnClickListener {
        int index[] = {-1, -1, -1};
        String lastSearchStr = null;
        ImageButton searchLastBtn, searchNextBtn;
        EditText searchInput;

        public SearchListener(ImageButton searchLastBtn, ImageButton searchNextBtn, EditText searchInput) {
            this.searchLastBtn = searchLastBtn;
            this.searchNextBtn = searchNextBtn;
            this.searchInput = searchInput;
        }

        @Override
        public void onClick(View v) {
            // Disable all button avoid mistake.
            searchLastBtn.setEnabled(false);
            searchNextBtn.setEnabled(false);

            // If no input stream skip.
            if (searchInput.getText().toString().length() == 0) {
                Crashlytics.log(Log.DEBUG, logTag, "User input length = 0, skip search");
                searchLastBtn.setEnabled(true);
                searchNextBtn.setEnabled(true);
                return;
            }

            final String str = searchInput.getText().toString();
            boolean isFirstSearch = (lastSearchStr == null);

            if (isFirstSearch || !lastSearchStr.equals(str)) {
                Crashlytics.log(Log.DEBUG, logTag, "It is first search.");
                index[0] = bookView.getFirstVisiblePosition();
                index[1] = 0;
                index[2] = 0;
            }

            try {
                int[] result = null;
                if (v.equals(searchNextBtn)) {
                    index[2]++;
                    Crashlytics.log(Log.DEBUG, logTag, "Change start word from " + index[2]);
                    Crashlytics.log(Log.DEBUG, logTag, "Search Next " + str + " from Page " + index[0] + " Line " + index[1] + " word " + index[2]);
                    result = bookView.searchNext(index[0], index[1], index[2], Util.simpToTradChar(LamrimReaderActivity.this, str));
                } else {
                    index[2]--;
                    Crashlytics.log(Log.DEBUG, logTag, "Change start word from " + index[2]);
                    Crashlytics.log(Log.DEBUG, logTag, "Search Last " + str + " from Page " + index[0] + " Line " + index[1] + " word " + index[2]);
                    if (isFirstSearch) {
                        int linearIndex = MyListView.getContentStr(index[0], 0, MyListView.TO_END).length();
                        result = bookView.searchLast(index[0], 0, linearIndex, Util.simpToTradChar(LamrimReaderActivity.this, str)); // It will set -1 to index[2] on first time search.
                    } else
                        result = bookView.searchLast(index[0], index[1], index[2], Util.simpToTradChar(LamrimReaderActivity.this, str)); // It will set -1 to index[2] on first time search.
                }

                if (result == null) {
                    Crashlytics.log(Log.DEBUG, logTag, "Not found.");
                    Util.showInfoToast(LamrimReaderActivity.this, getString(R.string.msgSearchNotFound));
                    searchLastBtn.setEnabled(true);
                    searchNextBtn.setEnabled(true);
                    return;
                } else {
                    index = result;
                    lastSearchStr = str;
                    bookView.setHighlightWord(index[0], index[1], index[2], str.length());
                    bookView.setViewToPosition(index[0], index[1]);
                }
            } catch (Exception e) {
                Crashlytics.log(Log.ERROR, logTag, "Error happen while Search: (" + str + ")");
                Crashlytics.log(Log.ERROR, logTag, e.getMessage());
                Crashlytics.logException(e);
            }
            searchLastBtn.setEnabled(true);
            searchNextBtn.setEnabled(true);
            Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.BUTTON_CLICK, "SEARCH_LAMRIM");
        }
    }

    private void showRecordListPopupMenu() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View popupView = inflater.inflate(R.layout.popup_record_list, null);
        Rect rectgle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectgle);
        int StatusBarHeight = rectgle.top;
        int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        int titleBarHeight = contentViewTop - StatusBarHeight;
        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        int subtitleViewHeight = ((TextView) findViewById(R.id.subtitleView)).getHeight();
        // int listViewHeight=screenHeight-titleBarHeight-subtitleViewHeight;
        int listViewHeight = screenHeight - contentViewTop;

        Log.i(logTag, "StatusBar Height= " + StatusBarHeight
                + " , TitleBar Height = " + titleBarHeight);
        final PopupWindow popupWindow = new PopupWindow(
                // findViewById(R.layout.popup_record_list),
                popupView,
                // LayoutParams.WRAP_CONTENT, listViewHeight);
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
//		popupWindow.setContentView(popupView);

        regionListView = (ListView) popupView.findViewById(R.id.recordListView);
        regionListView.setAdapter(regionRecordAdapter);
//		popupWindow.setWidth(popupView.getWidth());
        Crashlytics.log(Log.DEBUG, logTag, "There are " + regionRecordAdapter.getCount() + " items in regionList view.");
        regionListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View v, final int position, long id) {
                Crashlytics.log(Log.DEBUG, logTag, "Region record menu: item " + RegionRecord.records.get(position).title + " clicked.");
                RegionRecord rec = RegionRecord.records.get(position);
/*				int start=rec.mediaStart;
				int end = rec.mediaEnd;
				int intentCmd[] = null;

				if(start == end){
					File media = fsm.getLocalMediaFile(start);
					File subtitle = fsm.getLocalSubtitleFile(start);
					if (media == null || subtitle == null || !media.exists() || !subtitle.exists())
						intentCmd = new int[]{start};
				}
				else
					intentCmd = fsm.getUnreadyList(start, end);

				if(intentCmd != null){
					final Intent speechMenu = new Intent(LamrimReaderActivity.this,	SpeechMenuActivity.class);
					speechMenu.putExtra("index", intentCmd);
					if (wakeLock.isHeld())
						wakeLock.release();
					Log.d(getClass().getName(),"Call SpeechMenuActivity for download.");
					startActivityForResult(speechMenu, SPEECH_MENU_RESULT_REGION);
					popupWindow.dismiss();
				}
				*/
                playMode = REGION_PLAY_MODE;
                playRecord = getSharedPreferences(getString(R.string.regionPlayModeRecordFile), 0);
                actionBarTitle = getString(R.string.menuStrPlayRegionRecShortName) + ": " + rec.title;
                startRegionPlay(rec.mediaStart, rec.startTimeMs, rec.mediaEnd, rec.endTimeMs, rec.theoryPageStart, rec.theoryStartLine, rec.theoryPageEnd, rec.theoryEndLine, 0, actionBarTitle);

                // Set play from start of region record.
                SharedPreferences.Editor record = playRecord.edit();
                record.putInt("playPosition", rec.startTimeMs);
                record.commit();
                try {
                    popupWindow.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }    // Don't force close if problem here.
                Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.BUTTON_CLICK, "PLAY_SAVED_REGION_RECORD");
            }
        });

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                String pageKey = getString(R.string.regionRecordListViewPage);
                String pageShiftKey = getString(R.string.regionRecordListViewPageShift);
                int pageCount = regionListView.getFirstVisiblePosition();
                View v = regionListView.getChildAt(0);
                int shift = (v == null) ? 0 : v.getTop();

                runtimeEditor.putInt(pageKey, pageCount);
                runtimeEditor.putInt(pageShiftKey, shift);
                runtimeEditor.commit();
                Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.BUTTON_CLICK, "CANCLE_SELECT_SAVE_REGION_RECORD");
            }
        });
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        // popupWindow.setWidth((int)
        // (getWindowManager().getDefaultDisplay().getWidth()*0.4));

        // popupWindow.setContentView(findViewById(R.id.rootLayout));
        // AnimationUtils.loadAnimation(getApplicationContext(),R.anim.bounce);
        popupWindow.setAnimationStyle(R.style.AnimationPopup);
        popupWindow.update();
        popupWindow.showAtLocation(findViewById(R.id.rootLayout), Gravity.LEFT | Gravity.TOP, 0, contentViewTop);
        Util.fireSelectEvent(mFirebaseAnalytics, getClass().getName(),Util.BUTTON_CLICK, "SHOW_RECORD_LIST");
        // popupWindow.showAsDropDown(findViewById(R.id.subtitleView),0, 0);
        // popupWindow.showAsDropDown(findViewById(R.id.subtitleView));
    }

    private void startRegionPlay(final int mediaStart, final int startTimeMs, final int mediaEnd, final int endTimeMs, final int theoryStartPage, final int theoryStartLine, final int theoryEndPage, final int theoryEndLine, int regionIndex, String title) {

        GLamrimSectIndex = regionIndex;
        setRegionSec(mediaStart, startTimeMs, mediaEnd, endTimeMs, theoryStartPage, theoryStartLine, theoryEndPage, theoryEndLine, title);

        int start = mediaStart;
        int end = mediaEnd;
        int intentCmd[] = null;
        if (start == end) {
            if (!fsm.isFilesReady(start))
                intentCmd = new int[]{start};
        } else
            intentCmd = fsm.getUnreadyList(start, end);

        //Log.d(getClass().getName(),"Send download param "+param+" to speechMenuActivity");
        if (intentCmd != null) {
            final Intent speechMenu = new Intent(LamrimReaderActivity.this, SpeechMenuActivity.class);
            speechMenu.putExtra("index", intentCmd);
//			if (wakeLock.isHeld())wakeLock.release();
            startActivityForResult(speechMenu, SPEECH_MENU_RESULT_REGION);

            // The procedure will not return to onStart or onResume, start
            // play media from here.

            // Check file exist again, if no download, return.
            boolean isDownloaded = true;
            if (start == end)
                isDownloaded = fsm.isFilesReady(start);
            else
                isDownloaded = fsm.isFilesReady(start) && fsm.isFilesReady(end);

            if (!isDownloaded) return;
        }

        //mpController.desetPlayRegion();
        mpController.reset();
        SharedPreferences.Editor record = playRecord.edit();
        record.putInt("regionIndex", regionIndex);
        record.putInt("mediaIndex", this.GLamrimSect[regionIndex][0]);
        //editor.putInt("playPosition", startTimeMs);
        record.commit();
        mediaIndex = this.GLamrimSect[regionIndex][0];
        //GLamrimSectIndex=0;
        Crashlytics.log(Log.DEBUG, logTag, "Mark theory: start page=" + theoryHighlightRegion[0] + " start line=" + theoryHighlightRegion[1] + ", offset=" + bookViewMountPoint[1]);
        Crashlytics.log(Log.DEBUG, logTag, "Call startPlay from startRegionPlay");
        startPlay(mediaIndex);
    }

    private void setRegionSec(String speechPositionStart, String speechPositionEnd, String theoryLineStart, String theoryLineEnd, int GLamrimSectIndex, String title) {
        final int[] theoryStart = GlRecord.getTheoryStrToInt(glRecord.theoryLineStart);// {page,line}
        int[] theoryEnd = GlRecord.getTheoryStrToInt(glRecord.theoryLineEnd);// {page,line}
        int[] speechStart = GlRecord.getSpeechStrToInt(glRecord.speechPositionStart);// {speechIndex, TimeMs}
        int[] speechEnd = GlRecord.getSpeechStrToInt(glRecord.speechPositionEnd);// {speechIndex, TimeMs}

        Crashlytics.log(Log.DEBUG, logTag, "Parse result: Theory: P" + theoryStart[0] + "L" + theoryStart[1] + " ~ P" + theoryEnd[0] + "L" + theoryEnd[1]);
        Crashlytics.log(Log.DEBUG, logTag, "Parse result: Speech: " + speechStart[0] + ":" + Util.getMsToHMS(speechStart[1]) + " ~ " + speechEnd[0] + ":" + Util.getMsToHMS(speechEnd[1]));

        setRegionSec(speechStart[0], speechStart[1], speechEnd[0], speechEnd[1], theoryStart[0], theoryStart[1], theoryEnd[0], theoryEnd[1], title);
    }

    private void setRegionSec(int speechStartIndex, int speechStartMs, int speechEndIndex, int speechEndMs, final int theoryStartPage, final int theoryStartLine, int theoryEndPage, int theoryEndLine, String title) {
        Crashlytics.log(Log.DEBUG, logTag, "Set region[0]: startIndex=" + speechStartIndex + ", startMs=" + speechStartMs + ", speechEndIndex=" + speechEndIndex + ", endMs=" + speechEndMs);

        actionBarTitle = title;

        if (speechStartIndex == speechEndIndex) {
            Crashlytics.log(Log.DEBUG, logTag, "Set region[0]: startIndex=" + speechStartIndex + ", startMs=" + speechStartMs + ", endMs=" + speechEndMs + "; region[1]: -1, -1, -1");
            GLamrimSect[0][0] = speechStartIndex;
            GLamrimSect[0][1] = speechStartMs;
            GLamrimSect[0][2] = speechEndMs;
            GLamrimSect[1][0] = -1;
            GLamrimSect[1][0] = -1;
            GLamrimSect[1][0] = -1;
        } else {// difference media.
            Crashlytics.log(Log.DEBUG, logTag, "Set region[0]: startIndex=" + speechStartIndex + ", startMs=" + speechStartMs + ", endMs=-1; region[1]: endIndex=" + speechEndIndex + ", startMs=0, endMs=" + speechEndMs);
            GLamrimSect[0][0] = speechStartIndex;
            GLamrimSect[0][1] = speechStartMs;
            GLamrimSect[0][2] = -1;
            GLamrimSect[1][0] = speechEndIndex;
            GLamrimSect[1][1] = 0;
            GLamrimSect[1][2] = speechEndMs;
        }
        // Copy section data to regionSet
        regionSet[0] = speechStartIndex;
        regionSet[1] = speechStartMs;
        regionSet[2] = speechEndIndex;
        regionSet[3] = speechEndMs;

        // Set theory high light
        theoryHighlightRegion[0] = theoryStartPage;
        theoryHighlightRegion[1] = theoryStartLine;
        theoryHighlightRegion[2] = theoryEndPage;
        theoryHighlightRegion[3] = theoryEndLine;

        // Set theory mount point.
        synchronized (bookViewMountPointKey) {
            bookViewMountPoint[0] = theoryStartPage;
            bookViewMountPoint[1] = (int) bookView.setViewToPosition(theoryStartPage, theoryStartLine);
        }
        runtimeEditor.putInt("playMode", playMode);
        runtimeEditor.commit();

        SharedPreferences.Editor record = playRecord.edit();
        record.putInt("startMediaIndex", speechStartIndex);
        record.putInt("startMediaTime", speechStartMs);
        record.putInt("endMediaIndex", speechEndIndex);
        record.putInt("endMediaTime", speechEndMs);
        record.putInt("theoryStartPage", theoryStartPage);
        record.putInt("theoryStartLine", theoryStartLine);
        record.putInt("theoryEndPage", theoryEndPage);
        record.putInt("theoryEndLine", theoryEndLine);
        record.putString("title", title);
        record.commit();
    }


    // ======================= For Render mode ======================================
    private void showRenderModeFirstLevelMenu() {
        String[] menuStr = {getString(R.string.leaveRenderMode), getString(R.string.normalMenu), getString(R.string.settingMenu)};
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(LamrimReaderActivity.this);
        builderSingle.setIcon(R.mipmap.ic_launcher);
        builderSingle.setTitle(getString(R.string.menuTitle)).setItems(menuStr, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) switchMainView();
                else if (which == 1) showRenderModeNormalMenu();
                else shwoRenderModeOptMenu();
            }
        });
		/*builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		*/
        builderSingle.show();
    }

    private void showRenderModeNormalMenu() {
        //String[] menuStr = {getString(R.string.menuStrSelectSpeech), getString(R.string.globalLamrim), getString(R.string.menuStrPlayRegionRec), getString(R.string.leaveRenderMode), getString(R.string.menuStrOpenProjectWeb), getString(R.string.exitApp)};
        String[] menuStr = {getString(R.string.menuStrSelectSpeech), getString(R.string.menuStrPlayRegionRec), getString(R.string.leaveRenderMode), getString(R.string.menuAbout), getString(R.string.exitApp)};

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(LamrimReaderActivity.this);
        builderSingle.setIcon(R.mipmap.ic_launcher);
        builderSingle.setTitle(getString(R.string.menuTitle)).setItems(menuStr, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) startSpeechMenuActivity();
                    //else if (which == 1) startGlobalLamrimCalendarActivity();
                else if (which == 1) {
                    if (RegionRecord.records.size() == 0) {
                        Util.showInfoToast(LamrimReaderActivity.this, getString(R.string.recSecFirst));
                        return;
                    }
                    showRecordListPopupMenu();
                } else if (which == 2) switchMainView();
                else if (which == 3) showAboutDialog();
                else if (which == 4) onBackPressed();
                else Crashlytics.log(Log.DEBUG, logTag, "There is a non exist menu option been selected.");
            }
        });
		/*builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		*/

        AlertDialog dialog = builderSingle.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private void shwoRenderModeOptMenu() {
        String[] menuStr = getResources().getStringArray(R.array.SettingsMenuItem);
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(LamrimReaderActivity.this);
        builderSingle.setIcon(R.mipmap.ic_launcher);
        builderSingle.setTitle(getString(R.string.menuTitle)).setItems(menuStr, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) showHideSubtitleSwBtn();
                else if (which == 1) selectRenderImage();
                else if (which == 2) showScaleTypeDialog();
                else if (which == 3) showRenderImageBgColorDlg();
                else if (which == 4) showSubFgColorDlg();
                else if (which == 5) showSubBgColorDlg();
                else if (which == 6) showSubBgAlphaDlg();
                else if (which == 7) setRenderModeOptsToDefault();
            }
        });
/*		builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});*/
        AlertDialog dialog = builderSingle.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private void setRenderModeOptsToDefault() {
        renderView.setImageResource(R.drawable.master);
        renderView.setScaleType(scaleType[0]);
        renderView.setBackgroundColor(0);
        subtitleView.setTextColor(getResources().getColor(R.color.defSubtitleFGcolor));
        subtitleView.setBackgroundColor(getResources().getColor(R.color.defSubtitleBGcolor));
        modeSwBtn.setVisibility(View.VISIBLE);

        runtimeEditor.remove(getString(R.string.isShowModeSwBtnKey));
        runtimeEditor.remove(getString(R.string.renderImgFgPathKey));
        runtimeEditor.remove(getString(R.string.renderImgScaleKey));
        runtimeEditor.remove(getString(R.string.renderImgBgColorKey));
        runtimeEditor.remove(getString(R.string.subtitleFgColorKey));
        runtimeEditor.remove(getString(R.string.subtitleBgColorKey));
        runtimeEditor.remove(getString(R.string.subtitleAlphaKey));
        runtimeEditor.commit();
    }

    private void showScaleTypeDialog() {
        final String scaleStr[] = getResources().getStringArray(R.array.PicsMenuItem);
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(LamrimReaderActivity.this);
        builderSingle.setIcon(R.mipmap.ic_launcher);
        builderSingle.setTitle(getString(R.string.picsExtOpt))
                .setItems(scaleStr, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        renderView.setScaleType(scaleType[which]);
                        Crashlytics.log(Log.DEBUG, logTag, "Set image scale type: " + scaleStr[which]);
                        runtimeEditor.putInt(getString(R.string.renderImgScaleKey), which);
                        runtimeEditor.commit();
                    }
                });
/*        builderSingle.setNegativeButton("cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
*/
        AlertDialog dialog = builderSingle.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private void switchMainView() {
        if (renderView.getVisibility() == View.VISIBLE) {
            renderView.setVisibility(View.GONE);
            bookView.setVisibility(View.VISIBLE);
            subtitleView.setTextColor(getResources().getColor(R.color.defSubtitleFGcolor));
            //subtitleView.setBackgroundColor(getResources().getColor(R.color.defSubtitleBGcolor));
            if (Build.VERSION.SDK_INT >= 16)
                subtitleView.setBackground(getResources().getDrawable(R.drawable.subtitle_background));
            else
                subtitleView.setBackgroundDrawable(getResources().getDrawable(R.drawable.subtitle_background));

            modeSwBtn.setVisibility(View.VISIBLE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getSupportActionBar().show();
        } else {
            showRenderModeWarring();
            renderView.setVisibility(View.VISIBLE);
            bookView.setVisibility(View.GONE);
            int alpha = runtime.getInt(getString(R.string.subtitleAlphaKey), 255) << 24 & 0xFF000000;
            int color = runtime.getInt(getString(R.string.subtitleBgColorKey), getResources().getColor(R.color.defSubtitleBGcolor)) & 0x00FFFFFF;
            Crashlytics.log(Log.DEBUG, logTag, "Load alpha of subitlte: " + alpha);
            int bgColor = alpha | color;
            subtitleView.setTextColor(runtime.getInt(getString(R.string.subtitleFgColorKey), getResources().getColor(R.color.defSubtitleFGcolor)));
            subtitleView.setBackgroundColor(bgColor);
            boolean isShowModeSwBtn = runtime.getBoolean(getString(R.string.isShowModeSwBtnKey), true);
            if (isShowModeSwBtn) modeSwBtn.setVisibility(View.VISIBLE);
            else modeSwBtn.setVisibility(View.GONE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getSupportActionBar().hide();
        }
    }

    // ================ For option menu =====================

    private void showRenderModeWarring() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this).setTitle(getString(R.string.msgWarring)).setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(getString(R.string.msgLongPressShowMenu));
        builderSingle.setPositiveButton(getString(R.string.dlgOk), null);
        builderSingle.setCancelable(false);
        builderSingle.show();
    }

    private void showHideSubtitleSwBtn() {
        if (modeSwBtn.getVisibility() == View.VISIBLE) {
            modeSwBtn.setVisibility(View.GONE);
            runtimeEditor.putBoolean(getString(R.string.isShowModeSwBtnKey), false);
            runtimeEditor.commit();
        } else {
            modeSwBtn.setVisibility(View.VISIBLE);
            runtimeEditor.putBoolean(getString(R.string.isShowModeSwBtnKey), true);
            runtimeEditor.commit();
        }
    }

    private void selectRenderImage() {
        Intent fgIntent = new Intent(getBaseContext(), FileDialogActivity.class);
        fgIntent.putExtra(FileDialogActivity.TITLE, getString(R.string.msgSelectPic));
        fgIntent.putExtra(FileDialogActivity.START_PATH, Environment.getExternalStorageDirectory().getPath());
        //can user select directories or not
        fgIntent.putExtra(FileDialogActivity.CAN_SELECT_DIR, false);
        fgIntent.putExtra(FileDialogActivity.SELECTION_MODE, FileDialogActivity.MODE_OPEN);

        //alternatively you can set file filter
        fgIntent.putExtra(FileDialogActivity.FORMAT_FILTER, new String[]{"jpg", "gif", "png", "bmp", "webp"});
        startActivityForResult(fgIntent, SELECT_FG_PIC_RESULT);
    }

    private void showRenderImageBgColorDlg() {
        int defColor = runtime.getInt(getString(R.string.renderImgBgColorKey), R.color.defSubtitleBGcolor);

        final ColorPickerDialog colorDialog = new ColorPickerDialog(LamrimReaderActivity.this, defColor, new ColorPickerView.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                renderView.setBackgroundColor(color | 0xFF000000);
            }
        });
        colorDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                int color = colorDialog.getColor() | 0xFF000000;
                runtimeEditor.putInt(getString(R.string.renderImgBgColorKey), color);
                runtimeEditor.commit();
            }
        });

        WindowManager.LayoutParams lp = colorDialog.getWindow().getAttributes();
        lp.alpha = 0.8f;
        colorDialog.getWindow().setAttributes(lp);
        colorDialog.setCanceledOnTouchOutside(true);
        colorDialog.show();
    }

    private void showSubFgColorDlg() {
        int defFgColor = runtime.getInt(getString(R.string.subtitleFgColorKey), R.color.defSubtitleFGcolor);

        final ColorPickerDialog colorDialog = new ColorPickerDialog(LamrimReaderActivity.this, defFgColor, new ColorPickerView.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                subtitleView.setTextColor(color | 0xFF000000);
            }
        });

        colorDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                int color = subtitleView.getCurrentTextColor();
                runtimeEditor.putInt(getString(R.string.subtitleFgColorKey), color);
                runtimeEditor.commit();
            }
        });

        WindowManager.LayoutParams lp = colorDialog.getWindow().getAttributes();
        lp.alpha = 0.8f;
        colorDialog.getWindow().setAttributes(lp);
        colorDialog.setCanceledOnTouchOutside(true);
        colorDialog.show();
    }

    private void showSubBgColorDlg() {
        int defBgColor = runtime.getInt(getString(R.string.subtitleBgColorKey), R.color.defSubtitleBGcolor);

        final ColorPickerDialog colorDialog = new ColorPickerDialog(LamrimReaderActivity.this, defBgColor, new ColorPickerView.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                int alpha = runtime.getInt(getString(R.string.subtitleAlphaKey), 255);
                Crashlytics.log(Log.DEBUG, logTag, "Get alpha: " + alpha);
                int c = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
                subtitleView.setBackgroundColor(c);
            }
        });
        colorDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                int color = colorDialog.getColor();
                runtimeEditor.putInt(getString(R.string.subtitleBgColorKey), color);
                runtimeEditor.commit();
            }
        });

        WindowManager.LayoutParams lp = colorDialog.getWindow().getAttributes();
        lp.alpha = 0.8f;
        colorDialog.getWindow().setAttributes(lp);
        colorDialog.setCanceledOnTouchOutside(true);
        colorDialog.show();
    }

    private void showSubBgAlphaDlg() {
        final SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(255);
        int alpha = runtime.getInt(getString(R.string.subtitleAlphaKey), 255);
        seekBar.setProgress(alpha);
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int color = runtime.getInt(getString(R.string.subtitleBgColorKey), 255);
                subtitleView.setBackgroundColor(Color.argb(progress, Color.red(color), Color.green(color), Color.blue(color)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this).setTitle(getString(R.string.msgAlphaOpt)).setIcon(android.R.drawable.ic_dialog_info).setView(seekBar);
        builderSingle.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                int alpha = seekBar.getProgress();
                Crashlytics.log(Log.DEBUG, logTag, "Save alpha of subitlte to " + alpha);
                runtimeEditor.putInt(getString(R.string.subtitleAlphaKey), alpha);
                runtimeEditor.commit();
            }
        });
        AlertDialog dialog = builderSingle.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();


		/*.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try{
							//int alpha=Integer.parseInt(input.getText().toString());
							int alphaValue=seekBar.getProgress();
							subtitleView.getBackground().setAlpha(alphaValue);


						}catch(Exception e){}
					}})
					.setNegativeButton("取消", null).show();*/
    }

    /*
	private void hideTitle(){
		if(!getSupportActionBar().isShowing())return;
		double inch=Util.getDisplaySizeInInch(LamrimReaderActivity.this);
		Log.d(getClass().getName(),"The screen is "+inch+" inch.");
		// Do not hide title bar over 6 inch screen.
		if(inch > 6)return;

		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Log.d(getClass().getName(),"Hide action bar");
//				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			    getSupportActionBar().hide();
			}});
	}

	private void showTitle(){
		Log.d(getClass().getName(),"is action bar showing = "+getSupportActionBar().isShowing());
		Log.d(getClass().getName(),"renderView visiable = "+((renderView.getVisibility()==View.VISIBLE)?"true":"false"));
		if(getSupportActionBar().isShowing() || renderView.getVisibility()==View.VISIBLE)return;
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Log.d(getClass().getName(),"========================= Show action bar =======================");
//				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				getSupportActionBar().show();
			}});
	}
*/
    private void showMediaController() {
        mpController.showControllerView(LamrimReaderActivity.this);

        // Adjust the height of subtitleView, avoid the modeSwBtn over the window.
        int maxHeight = (int) (rootLayout.getHeight() - modeSwBtn.getHeight()) - mpController.getControllerView().getHeight();
        if (subtitleView.getHeight() > maxHeight) subtitleView.setHeight(maxHeight);

    }

    private void hideMediaController(boolean isForce) {
        if (isForce || !mpController.isShowLongTerm())
            mpController.hideMediaPlayerController();
    }

    private void highlightView(View v) {
        //Animation animation = (Animation) AnimationUtils.loadAnimation(this, R.anim.blank);
        Animation animation = (Animation) AnimationUtils.loadAnimation(this, R.anim.rotate);
        v.startAnimation(animation);
    }


    class SubtitleSearchAdapter extends SimpleAdapter {
        public SubtitleSearchAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                Crashlytics.log(Log.DEBUG, logTag, position+"th row=null, construct it.");
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.subtitle_search_result_row, parent, false);
                ((TextView) row.findViewById(R.id.text)).setTypeface(Util.getFont(LamrimReaderActivity.this, runtime));
                //((TextView)row.findViewById(R.id.info)).setTypeface(educFont);
            }
            TextView serial = (TextView) row.findViewById(R.id.serial);
            TextView text = (TextView) row.findViewById(R.id.text);
            TextView info = (TextView) row.findViewById(R.id.info);
            try {
                SubtitleSearchIndex ssi = subtitleSearchResult.get(position);
                String speechName = SpeechData.getNameId(ssi.mediaIndex);
                SubtitleElement subtitle = subtitleSearch[ssi.mediaIndex].getSubtitle(ssi.getSubtitleIndex());
                int searchTextlength = ssi.length;
                int index = ssi.getTextIndex();

                int remLen = subtitle.text.length() - index;
                if (remLen > searchTextlength) remLen = searchTextlength;

                String serialStr = " " + (position + 1) + ". ";
                SpannableString str = new SpannableString(serialStr);
                str.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, serialStr.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                serial.setText(str);

                str = new SpannableString(subtitle.text);
                int textColor = getResources().getColor(R.color.subtitleRedingModeHilightColor);
                str.setSpan(new ForegroundColorSpan(textColor), index, index + remLen, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                text.setText(str);

                str = new SpannableString(speechName + ":" + Util.getMsToHMS(subtitle.startTimeMs, ":", "", true) + " #" + (ssi.getSubtitleIndex() + 1));
                str.setSpan(new ForegroundColorSpan(Color.CYAN), 0, speechName.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                info.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) (text.getTextSize() * 0.9));
                info.setText(str);
            } catch (IndexOutOfBoundsException ioobe) {
                ioobe.printStackTrace();
            }
            return row;
        }
    }

    class SubtitleSearchIndex {
        int[] data;
        public int mediaIndex;
        public int length;

        public SubtitleSearchIndex(int mediaId, int[] data, int length) {
            this.mediaIndex = mediaId;
            this.data = data;
            this.length = length;
        }

        public int getSubtitleIndex() {
            return data[0];
        }

        public int getTextIndex() {
            return data[1];
        }
    }

    class RemoteSubtitleSearchAdapter extends SimpleAdapter {
        ArrayList<RemoteSubtitleSearchIndex> searchResult = null;

        public RemoteSubtitleSearchAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        public void setSubtitleSearchIndex(ArrayList<RemoteSubtitleSearchIndex> searchResult) {
            this.searchResult = searchResult;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                Crashlytics.log(Log.DEBUG, logTag, position+"th row=null, construct it.");
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.subtitle_search_result_row, parent, false);
                ((TextView) row.findViewById(R.id.text)).setTypeface(Util.getFont(LamrimReaderActivity.this, runtime));
                //((TextView)row.findViewById(R.id.info)).setTypeface(educFont);
            }
            TextView serial = (TextView) row.findViewById(R.id.serial);
            TextView text = (TextView) row.findViewById(R.id.text);
            TextView info = (TextView) row.findViewById(R.id.info);
            try {

                RemoteSubtitleSearchIndex ssi = searchResult.get(position);
                String speechName = SpeechData.getNameId(ssi.mediaIndex);

                // 順序數列，紫色
                String serialStr = " " + (position + 1) + ". ";
                SpannableString str = new SpannableString(serialStr);
                str.setSpan(new ForegroundColorSpan(ContextCompat.getColor(LamrimReaderActivity.this, R.color.searchResultID)), 0, serialStr.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                serial.setText(str);

                // 字幕字串
                str = new SpannableString(ssi.subtitleText);
                int textColor = ContextCompat.getColor(LamrimReaderActivity.this,R.color.searchResultKeyWord);
                str.setSpan(new ForegroundColorSpan(textColor), ssi.indexOfSubtitle, ssi.indexOfSubtitle + ssi.lengthOfSearchStr, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                text.setText(str);

                // 資訊列
                str = new SpannableString(speechName + ":" + Util.getMsToHMS(ssi.startTime, ":", "", true) + " #" + (ssi.subtitleSerialNum + 1));
                str.setSpan(new ForegroundColorSpan(ContextCompat.getColor(LamrimReaderActivity.this, R.color.searchResultSpeechName)), 0, speechName.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                info.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) (text.getTextSize() * 0.9));
                info.setText(str);
            } catch (IndexOutOfBoundsException ioobe) {
                ioobe.printStackTrace();
            }
            return row;
        }
    }



    class RemoteSubtitleSearchIndex {
        //{"searchString":"ab","count":1,"list":[{"subSerlNum":523,"startTime":1440943,"textIndex":6,"text":"如果英文叫table，那沒關係，","mediaId":294}],"spendTime":1}
        public int mediaIndex, subtitleSerialNum, startTime;
        public String subtitleText;
        public int indexOfSubtitle, lengthOfSearchStr;
        //int[] data; // 文字位於哪一個字幕([0]), 以及是該字幕文字中的第幾個字([1]).
        //public int mediaIndex;// 音檔編號
        //public int length; // 搜尋文字的長度,如[善知識]=3

        public RemoteSubtitleSearchIndex(int mediaId, int subSerialNum, int startTime, String subtitleText, int indexOfSubtitle, int lenOfSearchStr) {
            this.mediaIndex = mediaId;
            this.subtitleSerialNum = subSerialNum;
            this.startTime = startTime;
            this.subtitleText = subtitleText;
            this.indexOfSubtitle = indexOfSubtitle;
            this.lengthOfSearchStr = lenOfSearchStr;
        }
    }

    class RegionRecordAdapter extends SimpleAdapter {
        public RegionRecordAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                Crashlytics.log(Log.DEBUG, logTag, position+"th row=null, construct it.");
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.popup_record_list_row, parent, false);
            }

            final RegionRecord record = RegionRecord.getRegionRecord(LamrimReaderActivity.this, position);
            Crashlytics.log(Log.DEBUG, logTag, "Set: " + record.title);
            TextView title = (TextView) row.findViewById(R.id.regionRowTitle);
            TextView timeReg = (TextView) row.findViewById(R.id.timeRegion);
            TextView theoryIndex = (TextView) row.findViewById(R.id.theoryIndex);
            TextView info = (TextView) row.findViewById(R.id.info);

            ImageButton shareButton = (ImageButton) row.findViewById(R.id.shareButton);
            ImageButton editButton = (ImageButton) row.findViewById(R.id.editButton);
            ImageButton delButton = (ImageButton) row.findViewById(R.id.deleteButton);

            title.setText(record.title);

            if (record.theoryPageStart != -1 && record.theoryStartLine != -1 && record.theoryPageEnd != -1 && record.theoryEndLine != -1)
                theoryIndex.setText(String.format(getString(R.string.dlgRecordTheoryIndex), (record.theoryPageStart + 1), (record.theoryStartLine + 1), (record.theoryPageEnd + 1), (record.theoryEndLine + 1)));

            timeReg.setText(SpeechData.getTheoryName(record.mediaStart) + "  "
                    + Util.getMsToHMS(record.startTimeMs, "\"", "'", false) + " ~ "
                    + SpeechData.getTheoryName(record.mediaEnd) + "  " + Util.getMsToHMS(record.endTimeMs, "\"", "'", false));
            info.setText(record.info);
            Crashlytics.log(Log.DEBUG, logTag, "Info: " + record.info);

            shareButton.setFocusable(false);
            editButton.setFocusable(false);
            delButton.setFocusable(false);
            shareButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareSegment(RegionRecord.getRegionRecord(LamrimReaderActivity.this, position));
                }
            });

            editButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    RegionRecord rr = RegionRecord.getRegionRecord(LamrimReaderActivity.this, position);
                    Runnable callBack = new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    regionRecordAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    };
                    BaseDialogs.showEditRegionDialog(LamrimReaderActivity.this, rr.mediaStart, rr.startTimeMs, rr.mediaEnd, rr.endTimeMs, rr.theoryPageStart, rr.theoryStartLine, rr.theoryPageEnd, rr.theoryEndLine, record.info, position, callBack);
                }
            });

            delButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    BaseDialogs.showDelWarnDialog(LamrimReaderActivity.this, getString(R.string.record), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            RegionRecord.removeRecord(LamrimReaderActivity.this, position);
                            regionFakeList.remove(position);
                            regionRecordAdapter.notifyDataSetChanged();
                        }
                    }, null);
                }
            });
            return row;
        }
    }

    ;

    private void setMediaControllerView(int regStartMs, int regEndMs, boolean prevBtnVisiable, boolean nextBtnVisiable, OnClickListener prevListener, OnClickListener nextListener) {
        mpController.setPlayRegion(regStartMs, regEndMs);
        mpController.setPrevNextListeners(prevListener, nextListener);
        ((View) mpController.getControllerView().findViewById(R.id.prev)).setVisibility(((prevBtnVisiable) ? View.VISIBLE : View.GONE));
        ((View) mpController.getControllerView().findViewById(R.id.next)).setVisibility(((nextBtnVisiable) ? View.VISIBLE : View.GONE));
    }

    public interface PrevNextListener {
        public OnClickListener getPrevPageListener();

        public OnClickListener getNextPageListener();
    }

    PrevNextListener glModePrevNextListener = new PrevNextListener() {
        OnClickListener nextListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Crashlytics.log(Log.DEBUG, logTag, "Next button click on Global Lamrim mode.");
                GLamrimSectIndex = 1;
                startLamrimSection();
            }
        };

        OnClickListener prevListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Crashlytics.log(Log.DEBUG, logTag, "Prev button click on Global Lamrim mode.");
                GLamrimSectIndex = 0;
                startLamrimSection();
            }
        };

        private void startLamrimSection() {
            if (GLamrimSect[GLamrimSectIndex][0] == -1) return;
            hideMediaController(true);
            Crashlytics.log(Log.DEBUG, logTag, "Switch to first section of Global Lamrim.");
            mpController.reset();

            SharedPreferences.Editor record = playRecord.edit();
            record.putInt("mediaIndex", GLamrimSect[GLamrimSectIndex][0]);
            record.putInt("playPosition", GLamrimSect[GLamrimSectIndex][1]);
            record.commit();

            Crashlytics.log(Log.DEBUG, logTag, "Call startPlay from glModePrevNextListener");

            // Rebuild the title text [MediaName]-[month]/[date](117B-01/30)
            mediaIndex = GLamrimSect[GLamrimSectIndex][0];
            if (actionBarTitle != null && !actionBarTitle.isEmpty()) {
                String[] titleDate = actionBarTitle.split(":");// [全廣: 03/01 - 001A]
                if (titleDate.length >= 2) {
                    titleDate = titleDate[1].split("-");
                    if (titleDate.length >= 2)
                        actionBarTitle = getString(R.string.globalLamrimShortName) + ": " + titleDate[0].trim() + " - " + SpeechData.getSubtitleName(mediaIndex);
                }
            }

            startPlay(mediaIndex);
        }

        @Override
        public OnClickListener getPrevPageListener() {
            return prevListener;
        }

        @Override
        public OnClickListener getNextPageListener() {
            return nextListener;
        }
    };


    PrevNextListener normalModePrevNextListener = new PrevNextListener() {

        OnClickListener prevListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Crashlytics.log(Log.DEBUG, logTag, "Prev button click on Normal mode.");
                if (mediaIndex - 1 < 0) return;

                startLamrimSection(--mediaIndex);
            }
        };

        OnClickListener nextListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Crashlytics.log(Log.DEBUG, logTag, "Next button click on Normal mode.");
                if (mediaIndex + 1 >= SpeechData.name.length) return;

                startLamrimSection(++mediaIndex);
            }
        };

        private void startLamrimSection(int index) {
            Crashlytics.log(Log.DEBUG, logTag, "Switch to speech " + SpeechData.getTheoryName(index));
            hideMediaController(true);

            // File not exist.
            if (!fsm.isFilesReady(index)) {
                final Intent speechMenu = new Intent(LamrimReaderActivity.this, SpeechMenuActivity.class);
                speechMenu.putExtra("index", new int[]{index});
//				if (wakeLock.isHeld())wakeLock.release();
                startActivityForResult(speechMenu, SPEECH_MENU_RESULT);
                return;
            }

            // File exist, play it.
            SharedPreferences.Editor record = playRecord.edit();
            record.putInt("mediaIndex", index);
            record.putInt("playPosition", 0);
            record.commit();
            GLamrimSectIndex = -1;

/*			final int pageNum = SpeechData.refPage[index] - 1;
			if (pageNum < 0){
				synchronized(bookViewMountPointKey){
					bookViewMountPoint[0]=pageNum;
					bookViewMountPoint[1]=0;
				}
			}
*/
            Crashlytics.log(Log.DEBUG, logTag, "Call reset player.");
            actionBarTitle = SpeechData.getNameId(index);
            getSupportActionBar().setTitle(actionBarTitle);
            mpController.reset();
            Crashlytics.log(Log.DEBUG, logTag, "Call startPlay from normalModePrevNextListener");
            mediaIndex = index;
            startPlay(index);
        }

        @Override
        public OnClickListener getPrevPageListener() {
            return prevListener;
        }

        @Override
        public OnClickListener getNextPageListener() {
            return nextListener;
        }
    };

    public void playNormalMode(int media, int timeMs) {
        playMode = SPEECH_PLAY_MODE;
        playRecord = getSharedPreferences(getString(R.string.speechModeRecordFile), 0);
        mpController.setPlayRegion(-1, -1);
        mpController.reset();
        GLamrimSectIndex = -1;
        mediaIndex = media;

        SharedPreferences.Editor record = playRecord.edit();
        record.putInt("mediaIndex", media);
        record.putInt("playPosition", timeMs);
        Crashlytics.log(Log.DEBUG, logTag, "Save play position: " + timeMs);
        record.commit();

        SharedPreferences.Editor editor = runtime.edit();
        editor.putInt("playMode", SPEECH_PLAY_MODE);
        editor.commit();

        startPlay(media);
    }

    public static void setInfoText(int playPosition) {
        if (infoTextView == null || infoTextView.getVisibility() == View.GONE) return;
        String infoText = infoTextView.getText().toString();
        int sec = infoText.indexOf(',');
        String timeStr = Util.getMsToHMS(playPosition, "分", "秒, 字幕序: #", true);
        final String outputText = timeStr + subtitleIndex;
        infoTextView.post(new Runnable() {
            @Override
            public void run() {
                infoTextView.setText(outputText);
                //               modeSwBtn.setText(outputText);
            }
        });
    }
}
