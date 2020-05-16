package eyes.blue;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import androidx.core.app.NotificationCompat;

import static eyes.blue.DownloadAllService.notificationId;

/**
 * Created by eyesblue on 2017/9/5.
 */

public class ApiLevelAdaptor {


    public static void notifyMsg(Context context, String title, String contentText) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // ====================================================
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "DownloadService";
            CharSequence channelName = "LamrimReader";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
//            notificationChannel.enableVibration(true);
//            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mNotificationManager.createNotificationChannel(notificationChannel);

            // Creates an explicit intent for an Activity in your app
            ComponentName c=new ComponentName(context, DownloadAllServiceHandler.class);
            Intent intent = new Intent();
            intent.setComponent(c);

            // Sets the Activity to start in a new, empty task
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // Creates the PendingIntent
            PendingIntent notifyIntent =
                    PendingIntent.getActivity(
                            context,
                            0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

//            int notifyId = 1;
            Notification notification = new Notification.Builder(context, channelId)
                    .setContentTitle(title)
                    .setContentText(contentText)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(notifyIntent)
                    .build();


            mNotificationManager.notify(notificationId, notification);
        }

        else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(contentText);
            // Creates an explicit intent for an Activity in your app
            ComponentName c=new ComponentName(context, DownloadAllServiceHandler.class);
            Intent intent = new Intent();
            intent.setComponent(c);

            // Sets the Activity to start in a new, empty task
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // Creates the PendingIntent
            PendingIntent notifyIntent =
                    PendingIntent.getActivity(
                            context,
                            0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );


            // Puts the PendingIntent into the notification builder
            builder.setContentIntent(notifyIntent);
            // Notifications are issued by sending them to the

            // mId allows you to update the notification later on.
            mNotificationManager.notify(notificationId, builder.build());
        }
    }

    public static void removeNotification(Context context){
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notificationId);
    }

    public static void setBackground(Context c, View v, int resId){
        if (Build.VERSION.SDK_INT >= 16)
            v.setBackground(c.getResources().getDrawable(resId));
        else
            v.setBackgroundDrawable(c.getResources().getDrawable(resId));
    }
    public static void setBackground(View v, Drawable d){
        if (Build.VERSION.SDK_INT >= 16)
            v.setBackground(d);
        else
            v.setBackgroundDrawable(d);
    }
}
