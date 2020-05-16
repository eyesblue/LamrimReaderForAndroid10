package eyes.blue.RemoteDataSource;

import android.content.Context;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import eyes.blue.R;
import eyes.blue.SpeechData;

/**
 * Created by eyesblue on 2018/4/15.
 */

public class VultrRemoteSource implements RemoteSource {
    static String name="Vultr";
    public final static String baseURL="http://lamrimreader.eyes-blue.com/appresources/";
    Context context;
    String audioDirName;

    public VultrRemoteSource(Context context) {
        this.context=context;
        this.audioDirName=context.getResources().getString(R.string.audioDirName).toLowerCase();
    }

    public  String getName(){return name;}
    public  String getMediaFileAddress(int i){
        String url=null;
        try {
            url = baseURL+audioDirName+"/"+ URLEncoder.encode(SpeechData.name[i],"UTF-8");
        } catch (UnsupportedEncodingException e) {e.printStackTrace();}
        return url;
    }

    // ============ Not used ==============
    public String getSubtitleFileAddress(int i){return null;}
    public String getTheoryFileAddress(int i){return null;}
    public String getGlobalLamrimSchedule(){return null;}
}
