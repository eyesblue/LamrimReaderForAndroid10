package eyes.blue;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpDownloader {
    static boolean cancelled = false;
    static String logTag="HttpDownloader";
    public static void stopRun(){cancelled=true;}
    public static boolean download(Context cont, String url, String outputPath, DownloadProgressListener listener){
        Crashlytics.log(Log.DEBUG,cont.getClass().getName(),"Download file from "+url);

        cancelled = false;
        File tmpFile=new File(outputPath+cont.getString(R.string.downloadTmpPostfix));
        int readLen=-1, counter=0, bufLen=cont.getResources().getInteger(R.integer.downloadBufferSize);
        long startTime=System.currentTimeMillis();
        FileOutputStream fos=null;
        HttpURLConnection conn = null;

        try {
            /*
            conn=(HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setReadTimeout(8000);
            conn.setConnectTimeout(8000);
            conn.setInstanceFollowRedirects(false);// FollowRedirects 不會導向不同協定如 http -> https，不可用！
            conn.setDoOutput(false);
            conn.setDoInput(true);
            */
            conn=connFollowRedirect(url);
            if (conn.getResponseCode() != 200){
                if(conn!=null)conn.disconnect();
                Crashlytics.log(Log.ERROR, HttpDownloader.class.getName(), "Http connection return "+conn.getResponseCode()+", connection failure.");
                Log.d( HttpDownloader.class.getName(), "Http connection return "+conn.getResponseCode()+", connection failure.");
                return false;
            }
        }catch(MalformedURLException mue){
            mue.printStackTrace();
            if(conn!=null)conn.disconnect();
            return false;
        }catch (ProtocolException pe){
            pe.printStackTrace();
            if(conn!=null)conn.disconnect();
            return false;
        }catch(IOException ioe){
            ioe.printStackTrace();
            if(conn!=null)conn.disconnect();
            return false;
        }

        if(cancelled){
            Crashlytics.log(Log.DEBUG,cont.getClass().getName(),"User canceled, download procedure skip!");
            if(conn!=null)conn.disconnect();
            return false;
        }

        Crashlytics.log(Log.ERROR, HttpDownloader.class.getName(), "Http connected, loading data...");
        Crashlytics.setDouble("ResponseTimeOfDownload", (System.currentTimeMillis()-startTime));
        InputStream is=null;
        try {
            is = conn.getInputStream();
        } catch (IllegalStateException e2) {
            try {   is.close();     } catch (IOException e) {e.printStackTrace();}
            if(conn!=null)conn.disconnect();
            tmpFile.delete();
            e2.printStackTrace();
            return false;
        } catch (IOException e2) {
            if(conn!=null)conn.disconnect();
            tmpFile.delete();
            e2.printStackTrace();
            return false;
        }

        if(cancelled){
            Crashlytics.log(Log.DEBUG,cont.getClass().getName(),"User canceled, download procedure skip!");
            try {   is.close();     } catch (IOException e) {e.printStackTrace();}
            if(conn!=null)conn.disconnect();
            tmpFile.delete();
            return false;
        }

        final long contentLength=conn.getContentLength();
        if(listener!=null) listener.setMax((int)contentLength);
        try {
            fos=new FileOutputStream(tmpFile);
        } catch (FileNotFoundException e1) {
            Crashlytics.log(Log.DEBUG,cont.getClass().getName(),"File Not Found Exception happen while create output temp file ["+tmpFile.getName()+"] !");
            if(conn!=null)conn.disconnect();
            try {   is.close();     } catch (IOException e) {e.printStackTrace();}
            tmpFile.delete();
            e1.printStackTrace();
            return false;
        }

        if(cancelled){
            if(conn!=null)conn.disconnect();
            try {   is.close();     } catch (IOException e) {e.printStackTrace();}
            try {   fos.close();    } catch (IOException e) {e.printStackTrace();}
            tmpFile.delete();
            Crashlytics.log(Log.DEBUG, logTag,"User canceled, download procedure skip!");
            return false;
        }

        try {
            byte[] buf=new byte[bufLen];
            Crashlytics.log(Log.DEBUG,cont.getClass().getName(),Thread.currentThread().getName()+": Start read stream from remote site, is="+((is==null)?"NULL":"exist")+", buf="+((buf==null)?"NULL":"exist"));
            while((readLen=is.read(buf))!=-1){
                counter+=readLen;
                fos.write(buf,0,readLen);
                if(listener!=null)listener.setProgress(counter);

                if(cancelled){
                    conn.disconnect();
                    try {   is.close();     } catch (IOException e) {e.printStackTrace();}
                    try {   fos.close();    } catch (IOException e) {e.printStackTrace();}
                    tmpFile.delete();
                    Crashlytics.log(Log.DEBUG,cont.getClass().getName(),"User canceled, download procedure skip!");
                    return false;
                }
            }
            is.close();
            fos.flush();
            fos.close();
            if(conn!=null)conn.disconnect();
        } catch (IOException e) {
            if(conn!=null)conn.disconnect();
            try {   is.close();     } catch (IOException e2) {e2.printStackTrace();}
            try {   fos.close();    } catch (IOException e2) {e2.printStackTrace();}
            tmpFile.delete();
            e.printStackTrace();
            Crashlytics.log(Log.DEBUG,cont.getClass().getName(),Thread.currentThread().getName()+": IOException happen while download media.");
            return false;
        }

        if(counter!=contentLength || cancelled){
            tmpFile.delete();
            return false;
        }

        Crashlytics.setDouble("SpendTimeOfDownload", (System.currentTimeMillis()-startTime));

        // rename the protected file name to correct file name
        if(!tmpFile.renameTo(new File(outputPath))){
            tmpFile.delete();
            return false;
        }

        Crashlytics.log(Log.DEBUG,cont.getClass().getName(),Thread.currentThread().getName()+": Download finish, return true.");
        return true;
    }

    private static HttpURLConnection connFollowRedirect(String urlStr) throws IOException {
        URL resourceUrl, base, next;
        Map<String, Integer> visited;
        HttpURLConnection conn;
        String url=urlStr;
        String location;
        int times=0;
        visited = new HashMap<>();
        visited.put(url, 0);

        while (true)
        {
            if(visited.containsKey(url)){
                times=visited.get(url);
                visited.put(url,++times);
            }
            else visited.put(url,1);
            //times = visited.compute(url, (key, count) -> count == null ? 1 : count + 1);

            if (times > 3)
                throw new IOException("Stuck in redirect loop");

            Crashlytics.log(Log.DEBUG, logTag,"Connect to "+url);
            resourceUrl = new URL(url);
            conn        = (HttpURLConnection) resourceUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setDoOutput(false);
            conn.setDoInput(true);
            conn.setInstanceFollowRedirects(false);   // Make the logic below easier to detect redirections
            conn.setRequestProperty("User-Agent", "Mozilla/5.0...");

            switch (conn.getResponseCode())
            {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                    Crashlytics.log(Log.DEBUG, logTag,"Find redirect response.");
                    location = conn.getHeaderField("Location");
                    Crashlytics.log(Log.DEBUG, logTag,"Location: "+location);
                    //location = URLDecoder.decode(location, "UTF-8");
                    base     = new URL(url);
                    next     = new URL(base, location);  // Deal with relative URLs
                    url      = next.toExternalForm();
                    Crashlytics.log(Log.DEBUG, logTag,"Next URL: "+url);
                    listHeaders(conn);
                    continue;
            }
            break;
        }
        return conn;
    }

    private static void listHeaders(HttpURLConnection conn){
        for (Map.Entry<String, List<String>> entries : conn.getHeaderFields().entrySet()) {
            String values = "";
            for (String value : entries.getValue()) {
                values += value + ",";
            }
            Log.d("Response", entries.getKey() + " - " +  values );
        }
    }
}
