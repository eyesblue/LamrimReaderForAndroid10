package eyes.blue;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import eyes.blue.RemoteDataSource.GoogleRemoteSource;
import eyes.blue.RemoteDataSource.RemoteSource;
import eyes.blue.RemoteDataSource.VultrRemoteSource;

public class Util {
    static ArrayList<HashMap<String, String>> regionFakeList = new ArrayList<HashMap<String, String>>();
    static HashMap<String, String> fakeSample = new HashMap<String, String>();
    static Handler toastHandler=new Handler();
    static Typeface educFont=null;
    static Hashtable<Character,Character> tsCharTable=null;
    static Toast toast=null;
    static long subtitleLastShowTime = -1;
    static String logTag = "Util";

    public static String[] getRegionInfo(FileSysManager fsm, int[] speechData) {
        // startMediaIndex, startTimeMs, endMediaIndex, endTimeMs, theoryStartPage, theoryStartLine, theoryEndPage, theoryEndLine, startSubtitle, endSubtitle
        int startMediaIndex = speechData[0];
        int startTimeMs = speechData[1];
        int endMediaIndex = speechData[2];
        int endTimeMs = speechData[3];

        // ==================== Get text of subtitle ==============

        String startSubtitle = null, endSubtitle = null;

        SubtitleElement[] seArray = Util.loadSubtitle(fsm.context, startMediaIndex);
        int index = subtitleBSearch(seArray, startTimeMs);
        startSubtitle = seArray[index].text;

        if (startMediaIndex != endMediaIndex)
            seArray = Util.loadSubtitle(fsm.context, endMediaIndex);
        index = subtitleBSearch(seArray, endTimeMs);
        endSubtitle = seArray[index].text;


        return new String[]{startSubtitle, endSubtitle};
    }

    /*
     * Show the information PopupWindow on the center of root view of activity with delay time.
     * */
    public static void showInfoToast(final Context context, final String s) {
        showToast((Activity) context,s, R.mipmap.ic_info, 0);
    }

    /*
     * Show the information PopupWindow on the center of the specified view of activity.
     * */
    public static void showInfoToast(final Context context, final String s, int delay) {
        showToast((Activity) context,s, R.mipmap.ic_info, delay);
    }

    /*
     * Show the error PopupWindow on the center of root view of activity with delay time.
     * */
    public static void showErrorToast(final Context context, final String s, int delay) {
        showToast((Activity) context,  s, R.mipmap.ic_error, delay);
    }

    /*
     * Show the error PopupWindow on the center of the specified view of activity.
     * */
    public static void showErrorToast(final Context context, final String s) {
        showToast((Activity) context,  s, R.mipmap.ic_error, 0);
    }

    public synchronized static void showToast(final Activity activity, final String s, final int icon, int delay) {
        if(educFont==null)educFont = Typeface.createFromAsset(activity.getAssets(), "EUDC.TTF");

        toastHandler.postDelayed(new Runnable() {
            public void run() {
                LayoutInflater inflater = activity.getLayoutInflater();
                View toastView = inflater.inflate(R.layout.toast_text_view, (ViewGroup) activity.findViewById(R.id.toastLayout));
                ImageView image = (ImageView) toastView.findViewById(R.id.image);
                image.setImageResource(icon);
                TextView toastTextView = (TextView) toastView.findViewById(R.id.text);
                toastTextView.setTypeface(educFont);
                toastTextView.setText(s);

                if(toast!=null)toast.cancel();

                try {
                    toast = new Toast(activity);
                    toast.setView(toastView);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.show();
                } catch (Exception e) {
//					AnalyticsApplication.sendException("SHOW_TOAST", e, true);
                    e.printStackTrace();
                    return;
                }
            }
        },delay);
    }

    /**
     * Enables/Disables all child views in a view group.
     *
     * @param viewGroup the view group
     * @param enabled   <code>true</code> to enable, <code>false</code> to disable
     *                  the views.
     */
    public static void enableDisableViewGroup(ViewGroup viewGroup, boolean enabled) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = viewGroup.getChildAt(i);
            view.setEnabled(enabled);
            if (view instanceof ViewGroup) {
                enableDisableViewGroup((ViewGroup) view, enabled);
            }
        }
    }


    /*
     * While start playing, there may not have subtitle yet, it will return -1, except array index n.
     * */
    public static int subtitleBSearch(SubtitleElement[] a, int key) {
        int mid = a.length / 2;
        int low = 0;
        int hi = a.length;
        while (low <= hi) {
            mid = (low + hi) >>> 1;
            // final int d = Collections.compare(a[mid], key, c);
            int d = 0;
            if (mid == 0) {
//					System.out.println("Shift to the index 0, Find out is -1(no subtitle start yet) or 0 or 1");
                if (key < a[0].startTimeMs) return 0;
                if (a[1].startTimeMs <= key) return 1;
                return 0;
            }
            if (mid == a.length - 1) {
//					System.out.println("Shift to the last element, check is the key < last element.");
                if (key < a[a.length - 1].startTimeMs) return a.length - 2;
                return a.length - 1;
            }
            if (a[mid].startTimeMs > key && key <= a[mid + 1].startTimeMs) {
                d = 1;
//					System.out.println("MID=" + mid + ", Compare " + a[mid].startTimeMs + " > " + key + " > " + a[mid + 1].startTimeMs + ", set -1, shift to smaller");
            } else if (a[mid].startTimeMs <= key && key < a[mid + 1].startTimeMs) {
                d = 0;
//					System.out.println("This should find it! MID=" + mid + ", "						+ a[mid].startTimeMs + " < " + key + " > "						+ a[mid + 1].startTimeMs + ", set 0, this should be.");
            } else {
                d = -1;
//					System.out.println("MID=" + mid + ", Compare "						+ a[mid].startTimeMs + " < " + key + " < "						+ a[mid + 1].startTimeMs + ", set -1, shift to bigger");
            }
            if (d == 0)
                return mid;
            else if (d > 0)
                hi = mid - 1;
            else
                // This gets the insertion point right on the last loop
                low = ++mid;
        }
        String msg = "Binary search state error, shouldn't go to the unknow stage. this may cause by a not sorted subtitle: MID="
                + mid + ", Compare " + a[mid].startTimeMs + " <> " + key + " <> " + a[mid + 1].startTimeMs + " into unknow state.";
        Log.e("Util", msg, new Exception(msg));
        return -1;
    }

    public static SubtitleElement[] loadSubtitle(File file) throws FileNotFoundException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        return loadSubtitle(br);
    }


    public static SubtitleElement[] loadSubtitle(Context c, int index) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getAssets().open(c.getString(R.string.subtitleDirName) + File.separator + SpeechData.getSubtitleName(index) + "." + c.getString(R.string.defSubtitleType))));
            return loadSubtitle(br);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // impassable.
    }

    public static SubtitleElement[] loadSubtitle(BufferedReader br) {
        ArrayList<SubtitleElement> subtitleList = new ArrayList<SubtitleElement>();
        try {
            String stemp;
            int lineCounter = 0;
            int step = 0; // 0: Find the serial number, 1: Get the serial
            // number, 2: Get the time description, 3: Get
            // Subtitle
            int serial = 0;
            SubtitleElement se = null;

            while ((stemp = br.readLine()) != null) {
                lineCounter++;

                // This may find the serial number
                if (step == 0) {
                    if (stemp.matches("[0-9]+")) {
                        // System.out.println("Find a subtitle start: "+stemp);
                        se = new SubtitleElement();
                        serial = Integer.parseInt(stemp);
                        step = 1;
                    }
                }

                // This may find the time description
                else if (step == 1) {
                    if (stemp.matches("[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} +-+> +[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}")) {
                        String[] region = stemp.split(" +-+> +");
                        region[0] = region[0].trim();
                        region[1] = region[1].trim();
                        // System.out.println("Get time string: "+stemp);
                        int timeMs;

                        String ts = region[0].substring(0, 2);
                        // System.out.println("Hour: "+ts);
                        timeMs = Integer.parseInt(ts) * 3600000;
                        ts = region[0].substring(3, 5);
                        // System.out.println("Min: "+ts);
                        timeMs += Integer.parseInt(ts) * 60000;
                        ts = region[0].substring(6, 8);
                        // System.out.println("Sec: "+ts);
                        timeMs += Integer.parseInt(ts) * 1000;
                        ts = region[0].substring(9, 12);
                        // System.out.println("Sub: "+ts);
                        timeMs += Integer.parseInt(ts);
                        // System.out.println("Set time: "+startTimeMs);
                        se.startTimeMs = timeMs;

                        ts = region[1].substring(0, 2);
                        // System.out.println("Hour: "+ts);
                        timeMs = Integer.parseInt(ts) * 3600000;
                        ts = region[1].substring(3, 5);
                        // System.out.println("Min: "+ts);
                        timeMs += Integer.parseInt(ts) * 60000;
                        ts = region[1].substring(6, 8);
                        // System.out.println("Sec: "+ts);
                        timeMs += Integer.parseInt(ts) * 1000;
                        ts = region[1].substring(9, 12);
                        // System.out.println("Sub: "+ts);
                        timeMs += Integer.parseInt(ts);
                        se.endTimeMs = timeMs;
                        step = 2;
                    } else {
                        // System.err.println("Find a bad format subtitle element at line "+lineCounter+": Serial: "+serial+", Time: "+stemp);
                        step = 0;
                    }
                } else if (step == 2) {
                    se.text = stemp;
                    step = 0;
                    subtitleList.add(se);
//						System.out.println("get Subtitle: " + stemp);
                    if (stemp.length() == 0)
                        System.err.println("Load Subtitle: Warring: Get a Subtitle with no content at line " + lineCounter);
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (SubtitleElement[]) subtitleList.toArray(new SubtitleElement[0]);
    }

    public static String getMsToHMS(int ms) {
        return getMsToHMS(ms, "'", "\"", true);
    }

    public static String getMsToHMS(int ms, String minuteSign, String secSign, boolean hasDecimal) {
        String sub = "" + (ms % 1000);
        if (sub.length() == 1) sub = "00" + sub;
        else if (sub.length() == 2) sub = "0" + sub;

        int second = ms / 1000;
        int ht = second / 3600;
        second = second % 3600;
        int mt = second / 60;
        second = second % 60;

        String hs = "" + ht;
        if (hs.length() == 1) hs = "0" + hs;
        String mst = "" + mt;
        if (mst.length() == 1) mst = "0" + mst;
        String ss = "" + second;
        if (ss.length() == 1) ss = "0" + ss;

//	System.out.println("getMSToHMS: input="+ms+"ms, ht="+ht+", mt="+mt+", sec="+second+", HMS="+hs+":"+ms+":"+ss+"."+sub);
        return mst + minuteSign + ss + ((hasDecimal) ? "." + sub : "") + secSign;
    }

    public static double getDisplaySizeInInch(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
        double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
        double screenInches = Math.sqrt(x + y);
        return screenInches;
    }

    /*
        public static int getMaxFontSize(Activity activity){
            Point p=getResolution(activity);
            int ref=Math.min(p.x, p.y);
            float rate=(float)activity.getResources().getInteger(R.integer.textMaxSize)/100;

            return Math.round(ref*rate);
        }

        public static int getMinFontSize(Activity activity){
            Point p=getResolution(activity);
            int ref=Math.min(p.x, p.y);
            float rate=(float)activity.getResources().getInteger(R.integer.textMinSize)/100;

            return Math.round(ref*rate);
        }

        public static int getDefFontSize(Activity activity){
            Point p=getResolution(activity);
            int ref=Math.min(p.x, p.y);
            float rate=(float)activity.getResources().getInteger(R.integer.defFontSize)/100;
            Log.d("Util","ref="+ref+", rate="+rate);
            return Math.round(ref*rate);
        }
    */
    @SuppressLint("NewApi")
    public static Point getResolution(Activity activity) {
        Point screenDim = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(screenDim);
        return screenDim;
    }

    public static boolean unZip(String zipname, String extractTo) {
        InputStream is;
        ZipInputStream zis;
        try {
            is = new FileInputStream(extractTo + zipname);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;

            while ((ze = zis.getNextEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int count;

                // zapis do souboru
                String filename = ze.getName();
                FileOutputStream fout = new FileOutputStream(extractTo + filename);

                // cteni zipu a zapis
                while ((count = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, count);
                    byte[] bytes = baos.toByteArray();
                    fout.write(bytes);
                    baos.reset();
                }
                fout.flush();
                fout.close();
                zis.closeEntry();
            }
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    static Typeface defFont = null, bktFont = null;

    public static Typeface getFont(Context context, SharedPreferences runtime) {
        int defFontOpt = context.getResources().getInteger(R.integer.defFontProp);
        int bktFontOpt = context.getResources().getInteger(R.integer.bktFontProp);
        int fontOpt = runtime.getInt(context.getString(R.string.fontKey), defFontOpt);
        if (fontOpt == defFontOpt) {
            Log.d(logTag, "Get default font.");
            if (defFont == null)
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      defFont = Typeface.createFromAsset(context.getAssets(), "EUDC.TTF");
            return defFont;
        }
        if (fontOpt == bktFontOpt) {
            Log.d(logTag, "Get BKT font.");
            if (bktFont == null)
                bktFont = Typeface.createFromAsset(context.getAssets(), "BKT_Lamrim.ttf");
            return bktFont;
        }
        return null;
    }

    public static boolean isFileCorrect(String fileName, long crc32) throws Exception {
        return isFileCorrect(new File(fileName), crc32);
    }

    public static boolean isFileCorrect(File file, long crc32) throws Exception {
        long startTime = System.currentTimeMillis();
        Checksum checksum = new CRC32();
        InputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[16384];
        int readLen = -1;

        while ((readLen = fis.read(buffer)) != -1)
            checksum.update(buffer, 0, readLen);
        fis.close();

        long sum = checksum.getValue();
        boolean isCorrect = (crc32 == sum);
        int spend = (int) (System.currentTimeMillis() - startTime);
        Log.d("Util", "CRC Check result: " + ((isCorrect) ? "Correct!" : "Incorrect!") + ", ( Sum=" + sum + ", record=" + crc32 + "), length: " + file.length() + ", spend time: " + spend + "ms, File path: " + file.getAbsolutePath());
        return isCorrect;
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer))
            return capitalize(model);
        else
            return capitalize(manufacturer) + " " + model;
    }


    private static String capitalize(String s) {
        if (s == null || s.length() == 0) return "";
        char first = s.charAt(0);
        if (Character.isUpperCase(first))
            return s;
        else
            return Character.toUpperCase(first) + s.substring(1);
    }

    public static View getRootView(Activity activity) {
        return activity.getWindow().getDecorView().findViewById(android.R.id.content);
    }

    /*
    * Check is the device tablet.
    * */
    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.isTablet);
    }

    /*
    * Change the size of Drawable image.
    * */
    public static Drawable resize(Context c, Drawable image, int rate) {
        Bitmap b = ((BitmapDrawable)image).getBitmap();
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, rate, rate, false);
        return new BitmapDrawable(c.getResources(), bitmapResized);
    }

    public static Drawable resizeImage(Context c, int resId, int w, int h)
    {
        // load the origial Bitmap
        Bitmap BitmapOrg = BitmapFactory.decodeResource(c.getResources(), resId);
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        int newWidth = w;
        int newHeight = h;
        // calculate the scale
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0,width, height, matrix, true);
        return new BitmapDrawable(resizedBitmap);
    }

    public static void restartApp(Activity activity) {
        Intent i = activity.getBaseContext().getPackageManager().getLaunchIntentForPackage(activity.getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(i);
    }

    public static long getMemInfo(Activity activity) {
        MemoryInfo mi = new MemoryInfo();
        ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long availableMegs = mi.availMem / 1048576L;
        return availableMegs;
    }

    public static int getNumberOfCores() {
        if (Build.VERSION.SDK_INT >= 17) {
            return Runtime.getRuntime().availableProcessors();
        } else {
            // Use saurabh64's answer
            return getNumCoresOldPhones();
        }
    }

    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     *
     * @return The number of cores, or 1 if failed to get result
     */
    private static int getNumCoresOldPhones() {
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                if (Pattern.matches("cpu[0-9]+", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            //Default to return 1 core
            return 1;
        }
    }

    public static String MENU_CLICK = "MENU_CLICK";
    public static String BUTTON_CLICK = "BUTTON_CLICK";
    public static String SPEND_TIME = "SPEND_TIME";
    public static String STATISTICS = "STATISTICS";

    public static void fireSelectEvent(FirebaseAnalytics mFirebaseAnalytics, String activity, String type, String name) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, activity);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, type);
//		bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, name);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }


    //@TargetApi(Build.VERSION_CODES.N)
    public static void fireTimming(Context context, FirebaseAnalytics mFirebaseAnalytics, String logTag, String type, String name, int value) {

        Bundle bundle = new Bundle();
		bundle.putString("Activity", logTag);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, type);
		bundle.putString("MANUFACTURER", android.os.Build.MANUFACTURER);
		bundle.putString("MODEL", Build.MODEL);
		bundle.putString("OS_VERSION", android.os.Build.VERSION.RELEASE);
		//bundle.putString("LOCATE", current.getDisplayName());
        //bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, name);
        bundle.putString(FirebaseAnalytics.Param.VALUE, "" + value);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

/*
    public static void fireException(String msg, Throwable e) {
        String repMsg = msg + ", V" + Build.VERSION.RELEASE + ", " + Util.getDeviceName() + ", " + "Thread: {" + Thread.currentThread().getName() + "}";//, Exception: " + ExceptionUtils.getStackTrace(throwable)
        FirebaseCrash.log(repMsg);
        FirebaseCrash.report(e);
    }
*/

    public static void multiThreadExec(Runnable[] tasks) {
        Executer[] executer = new Executer[tasks.length];
        Log.d("Util", "================ There are " + tasks.length + " tasks.");
        for (int i = 0; i < tasks.length; i++)
            executer[i] = new Executer(tasks[i], executer, i);

        for (Executer exe : executer)
            exe.start();

        try {
            synchronized (executer) {
                executer.wait();
            }
        } catch (InterruptedException ie) {
            Log.e("Util", "The caller thread has been interrupt!");
        }
    }

    public static class Executer extends Thread {
        public Executer[] tasks;
        public Runnable runnable;
        public int index;
        public long spendTime;

        public Executer(Runnable runnable, Executer[] tasks, int index) {
//			super(runnable);
            this.runnable = runnable;
            this.tasks = tasks;
            this.index = index;
        }

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            runnable.run();
            spendTime = System.currentTimeMillis() - startTime;
            synchronized (tasks) {
                for (int i = 0; i < tasks.length; i++) {
                    if (i == index) continue;
                    if (tasks[i].isAlive()) return;
                }
                tasks.notify();
            }

            int i = 1;
            String res = "";
            for (Executer exe : tasks)
                res += "Task" + i++ + " execute spend " + exe.spendTime + " MS.\n";
            Log.d("MultiThreadExec", res);
        }
    }

    public static String simpToTradChar(Context c, String str) {
        // Check is the system is simply
        //String locale = c.getResources().getConfiguration().locale.getCountry();
        String locale = ApiLevelAdaptor.getLocale(c).getCountry();
        if(!locale.contains("CN")) // 非簡體中文系統，不轉換。
            return str;


        // Build the char map hash table.
        if (tsCharTable == null) {
            try {
                Log.d(logTag,"Rebuild the simple to trad char table.");
                tsCharTable = new Hashtable<>();
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getAssets().open("simpChars.txt"),"UTF-8"));
                char[] s =  br.readLine().toCharArray();
                br.close();
                br = new BufferedReader(new InputStreamReader(c.getAssets().open("tradChars.txt"),"UTF-8"));
                char[] t = br.readLine().toCharArray();
                br.close();

                for(int i=0;i<s.length;i++)
                    tsCharTable.put(s[i], t[i]);

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        long startTime=System.currentTimeMillis();
            StringBuffer sb = new StringBuffer();
            char[] chars=str.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                Character  value = tsCharTable.get(chars[i]);
                if (value == null) {
                    Log.d(logTag,"Not found \""+chars[i]+"\"");
                    sb.append(chars[i]);
                }
                else sb.append(value);
            }

            String trans=sb.toString();
            //Util.showInfoToast(c, "轉換為 "+trans+", spend: "+(System.currentTimeMillis()-startTime));
            return trans;
    }

    public static MediaBrowserCompat.ConnectionCallback getCallback(final Activity c, final MediaBrowserCompat mMediaBrowserCompat){
       return new MediaBrowserCompat.ConnectionCallback() {
            @Override
            public void onConnected() {
                super.onConnected();
                try {
                    MediaControllerCompat mMediaControllerCompat = new MediaControllerCompat(c, mMediaBrowserCompat.getSessionToken());
                    //mMediaControllerCompat.registerCallback(mMediaControllerCompatCallback);
                    mMediaControllerCompat.registerCallback(new MediaControllerCompat.Callback(){});

                    //setSupportMediaController(mMediaControllerCompat);
                    MediaControllerCompat.setMediaController(c, mMediaControllerCompat);
                    //getSupportMediaController().getTransportControls().playFromMediaId(String.valueOf(R.raw.warner_tautz_off_broadway), null);
                    MediaControllerCompat.getMediaController(c).getTransportControls().playFromMediaId(String.valueOf(R.raw.warner_tautz_off_broadway), null);
                } catch( RemoteException e ) {
                    e.printStackTrace();
                }
            }
        };
    }

    public static RemoteSource[] getRemoteSource(Context ctx){
        //String locale = ctx.getResources().getConfiguration().locale.getCountry();
        String locale = ApiLevelAdaptor.getLocale(ctx).getCountry();
        //Util.showInfoToast((Activity)ctx, "Locale: "+locale);
        if(locale.contains("CN")){
            Crashlytics.log(Log.DEBUG, logTag,"**** Downlaod resource from VULTR site ****");
            return new RemoteSource[]{new VultrRemoteSource(ctx)};
        }
        Crashlytics.log(Log.DEBUG, logTag,"**** Downlaod resource from Google site ****");
        return new RemoteSource[]{new GoogleRemoteSource(ctx, GoogleRemoteSource.PROJECT_URL)};
    }

}
