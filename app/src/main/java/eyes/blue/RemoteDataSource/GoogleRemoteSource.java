package eyes.blue.RemoteDataSource;

import android.content.Context;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import eyes.blue.R;
import eyes.blue.SpeechData;

public class GoogleRemoteSource implements RemoteSource {
	Context context=null;
	//https://sites.google.com/a/eyes-blue.com/lamrimreader/appresources/audio/LR-001A-%E5%BA%8F.mp3?attredirects=0&d=1
	public final static String PROJECT_URL="https://sites.google.com/a/eyes-blue.com/lamrimreader/appresources/";

	public final static String GEBIS_URL="http://lamrimreader.gebis.global/appresources/";
	String baseURL=null;
	String audioDirName=null;
	String subtitleDirName=null;
	String theoryDirName=null;
	String globalLamrimDirName=null;
	
	public GoogleRemoteSource(Context context, String srcUrl) {
		this.context=context;
		this.audioDirName=context.getResources().getString(R.string.audioDirName).toLowerCase();
		this.subtitleDirName=context.getResources().getString(R.string.subtitleDirName).toLowerCase();
		this.theoryDirName=context.getResources().getString(R.string.theoryDirName).toLowerCase();
		this.globalLamrimDirName=context.getResources().getString(R.string.globalLamrimDirName).toLowerCase();
		baseURL=srcUrl;
	}
	
	@Override
	public String getMediaFileAddress(int i){
		String url=null;
		try {
			//url = baseURL+audioDirName+"/"+URLEncoder.encode(SpeechData.name[i],"UTF-8");
			url = baseURL+audioDirName+"/"+URLEncoder.encode(SpeechData.name[i],"UTF-8")+"?attredirects=0&d=1";
		} catch (UnsupportedEncodingException e) {e.printStackTrace();}
		return url;
	}
	@Override
	public String getSubtitleFileAddress(int i){
		//return baseURL+subtitleDirName+"/"+SpeechData.getNameId(i)+"."+subtitleSubName;
		String url=null;
		try {
			url = baseURL+subtitleDirName+"/"+URLEncoder.encode(SpeechData.getSubtitleName(i),"UTF-8")+"."+context.getResources().getString(R.string.defSubtitleType);
		} catch (UnsupportedEncodingException e) {e.printStackTrace();}
		return url;
	}
	@Override
	public String getTheoryFileAddress(int i){return baseURL+theoryDirName+"/"+ SpeechData.getNameId(i)+"."+context.getResources().getString(R.string.defTheoryType);}
	@Override
	public String getName(){return "Google";}

	@Override
	public String getGlobalLamrimSchedule() {
		String url=null;
		url = baseURL+globalLamrimDirName+"/"+context.getResources().getString(R.string.globalLamrimScheduleFile)+"."+context.getResources().getString(R.string.globalLamrimScheduleFileFormat)+"?attredirects=0&d=1";
		return url;
	}
}
