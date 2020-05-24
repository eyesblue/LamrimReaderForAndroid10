package eyes.blue.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

//import androidx.appcompat.app.NotificationCompat;
//import androidx.media.app.NotificationCompat;

/**
 * Helper APIs for constructing MediaStyle notifications
 */
public class MediaStyleHelper {
    public static final String CHANNEL_ID="LamrimReaderPlayer";
    public static final int NOTIFY_ID = 1;
    public static String logTag="MediaStyleHelper";
    /**
     * Build a notification using the information from the given media session. Makes heavy use
     * of {@link MediaMetadataCompat#getDescription()} to extract the appropriate information.
     * @param context Context used to construct the notification.
     * @param mediaSession Media session to get information.
     * @return A pre-built notification with information from the given media session.
     */

    public static NotificationManager notifManager;
    public static NotificationCompat.Builder from(Context context, MediaSessionCompat mediaSession) {
        //final int NOTIFY_ID = 0; // ID of notification
        //String id = MediaStyleHelper.CHANNEL_ID; // default_channel_id
        String title = "Title Text"; // Default Channel
        Intent intent;
        PendingIntent pendingIntent;
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        NotificationCompat.Builder builder;
        if (notifManager == null) {
            notifManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = notifManager.getNotificationChannel(CHANNEL_ID);
            if (mChannel == null) {
                mChannel = new NotificationChannel(CHANNEL_ID, title, importance);


                //
                //mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                notifManager.createNotificationChannel(mChannel);
            }
            Log.d(logTag,"Turn sound off.");
            mChannel.setSound(null, null);
            mChannel.setDescription("description");
            mChannel.enableLights(true);
            mChannel.enableVibration(false);
            mChannel.setLightColor(Color.BLUE);
            notifManager.createNotificationChannel(mChannel);

            builder = new NotificationCompat.Builder(context, CHANNEL_ID);
            builder.setChannelId(mChannel.getId());
            intent = new Intent(context, BgPlayerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        }
        else {
            builder = new NotificationCompat.Builder(context, CHANNEL_ID);
            intent = new Intent(context, BgPlayerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        }



        builder .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setLargeIcon(description.getIconBitmap())
                .setContentIntent(controller.getSessionActivity())
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(Notification.DEFAULT_LIGHTS);

        Log.d(logTag,"Return builder.");
        return builder;

    }

    public static NotificationCompat.Builder from1(
            Context context, MediaSessionCompat mediaSession) {
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setLargeIcon(description.getIconBitmap())
                .setContentIntent(controller.getSessionActivity())
                .setDeleteIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        return builder;
    }
}