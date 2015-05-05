
package net.qb9.hades;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.app.NotificationManager;
import android.os.Bundle;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import android.app.PendingIntent;
import org.cocos2dx.cpp.AppActivity;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import net.qb9.notifsample.R;

public class GCMIntentService extends IntentService {

	public static final int NOTIFICATION_ID = 1;
	private NotificationManager mNotificationManager;
	NotificationCompat.Builder builder;

	public GCMIntentService() {
		super("GCMIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		String messageType = gcm.getMessageType(intent);

		if ( ! extras.isEmpty() ) {
			if ( GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType) ) {
				String title = extras.getString("title", "");
				String msg = extras.getString("msg", "");
				sendNotification(title, msg, extras);
			}
		}

		GCMBroadcastReceiver.completeWakefulIntent(intent);
	}

	private void sendNotification(String title, String msg, Bundle extras) {
			mNotificationManager = (NotificationManager)
					this.getSystemService(Context.NOTIFICATION_SERVICE);

			Intent notifIntent = new Intent(this, AppActivity.class);
			notifIntent.putExtras(extras);
			notifIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			NotificationCompat.Builder mBuilder =
					new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.icon)
				.setContentTitle(title)
				.setStyle(new NotificationCompat.BigTextStyle()
				.bigText(msg))
				.setContentText(msg);

			mBuilder.setContentIntent(contentIntent);
			mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

}
