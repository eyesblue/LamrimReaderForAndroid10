package eyes.blue;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.api.Api;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.w3c.dom.Text;

import java.util.Locale;

public class AboutActivity extends Activity {
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        TextView aboutVerText=findViewById(R.id.aboutVerText);
        String versionStr=getString(R.string.version)+"："+ BuildConfig.VERSION_NAME+", "+ getString(R.string.resNum)+"："+ BuildConfig.VERSION_CODE;
        aboutVerText.setText(versionStr);

        TextView aboutMailToMe=findViewById(R.id.aboutMailToMe);
        String mailToStr="%1$s：<a href=\"mailto:eyesblue@eyes-blue.com?&subject=%2$s&body=\n\n\n\n%3$s: %4$s(%5$d)\n%6$s: %7$s, Android%8$s, %9$s: %10$s\">eyesblue@eyes-blue.com</a>";
        mailToStr=String.format(Locale.US, mailToStr,getString(R.string.contactAuther), getString(R.string.app_name)+getString(R.string.contactAuther), getString(R.string.version), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, getString(R.string.OSver), android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL, Build.VERSION.RELEASE+"("+Build.VERSION.SDK_INT+")", getString(R.string.locale), ApiLevelAdaptor.getLocale(AboutActivity.this));
        mailToStr=mailToStr.replaceAll("\n", "%0d%0a");
        Crashlytics.log(Log.DEBUG, getClass().getName(),"Mail to string: "+mailToStr);
        aboutMailToMe.setText(ApiLevelAdaptor.fromHtml(mailToStr));
        aboutMailToMe.setMovementMethod(LinkMovementMethod.getInstance());
    }

    /*
    protected void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span)
    {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);
        ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {
                // Do something with span.getURL() to handle the link click...
            }
        };
        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
    }


    protected void setTextViewHTML(TextView text, String html)
    {
        CharSequence sequence = ApiLevelAdaptor.fromHtml(html);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for(URLSpan span : urls) {
            makeLinkClickable(strBuilder, span);
        }
        text.setText(strBuilder);
        text.setMovementMethod(LinkMovementMethod.getInstance());
    }
    */
}
