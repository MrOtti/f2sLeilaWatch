/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.f2s.mandm.portbi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static de.f2s.mandm.portbi.NotificationHandlerService.ACTION_REPLY;
import static de.f2s.mandm.portbi.NotificationHandlerService.ACTION_SNOOZE;

/**
 * A service that runs in the background and provides responses to the incoming messages from the
 * wearable. It also keeps a record of the chat session history, which it can provide upon request.
 */
public class ResponderService extends Service {

    public static final String ACTION_WAIT = "de.f2s.mandm.portbi.WAIT";

    public static final String ACTION_RESPONSE = "de.f2s.mandm.portbi.REPLY";
    public static final String ACTION_REMIND = "de.f2s.mandm.portbi.REMIND";

    public static final String ACTION_UPDATE = "de.f2s.mandm.portbi.UPDATE";
    public static final String ACTION_DONE = "de.f2s.mandm.portbi.DONE";

    public static final String EXTRA_NOTIFICATION = "notification";
    public static final String EXTRA_REPLY = "reply";

    private static final String TAG = "ResponderService";
    private static final String QUICK_REPLY_TEXT = "quick_reply";


    private ElizaResponder mResponder;

    private String mLastResponse = null;

    private StringBuffer mCompleteConversation = new StringBuffer();

    private LocalBroadcastManager mBroadcastManager;

    private int notificationId = 0;

    String notificationChannelId = "PortBI_01";
    private NotificationManager notificationManager;
    NotificationCompat.Builder notificationCompatBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Chat Service started");
        }
        mResponder = new ElizaResponder();
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        processIncoming(null);

        notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null == intent || null == intent.getAction()) {
            return Service.START_STICKY;
        }
        String action = intent.getAction();
        if (action.equals(ACTION_UPDATE)) {
            String notificationMessage = intent.getStringExtra(EXTRA_NOTIFICATION);

            // Reply Action.
            Intent replyIntent = new Intent(this, NotificationHandlerService.class);
            replyIntent.setAction(ACTION_REPLY);

            String replyLabel = getResources().getString(R.string.reply_label);
            String[] replyChoices = getResources().getStringArray(R.array.reply_choices);

            RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_REPLY)
                    .setLabel(replyLabel)
                    .setChoices(replyChoices)
                    .build();

            PendingIntent replyPendingIntent = PendingIntent.getService(this, 0, replyIntent, 0);
            NotificationCompat.Action replyAction =
                    new NotificationCompat.Action.Builder(
                            R.drawable.ic_full_reply,
                            "Reply",
                            replyPendingIntent)
                            .addRemoteInput(remoteInput)
                            .build();

            // Snooze Action.
            Intent snoozeIntent = new Intent(this, NotificationHandlerService.class);
            snoozeIntent.setAction(ACTION_SNOOZE);
            snoozeIntent.putExtra(EXTRA_NOTIFICATION, notificationMessage);

            PendingIntent snoozePendingIntent = PendingIntent.getService(this, 0, snoozeIntent, 0);

            NotificationCompat.Action snoozeAction =
                    new NotificationCompat.Action.Builder(
                            android.R.drawable.ic_lock_idle_alarm,
                            "Remind me tomorrow",
                            snoozePendingIntent)
                            .build();

            ArrayList actions = new ArrayList();
            actions.add(replyAction);
            actions.add(snoozeAction);
            showNotification(notificationMessage, actions);
        } else if (action.equals(ACTION_DONE)) {
            String notificationMessage = intent.getStringExtra(EXTRA_NOTIFICATION);
            ArrayList actions = new ArrayList();
            showNotification(notificationMessage, actions);
        } else if (action.equals(ACTION_RESPONSE)) {
            Bundle remoteInputResults = RemoteInput.getResultsFromIntent(intent);
            CharSequence replyMessage = "";
            if (remoteInputResults != null) {
                replyMessage = remoteInputResults.getCharSequence(EXTRA_REPLY);
            }
            processIncoming(replyMessage.toString());
        } else if (action.equals(MainActivity.ACTION_GET_CONVERSATION)) {
            broadcastMessage(mCompleteConversation.toString());
        }
        return Service.START_STICKY;
    }

    private void showNotification(String notificationText, ArrayList<NotificationCompat.Action> actions) {
        if (notificationText == null || notificationText.equals("")) {
            notificationText = mLastResponse;
        } else if (!notificationText.equals(mLastResponse)) {
            mLastResponse = notificationText;
            broadcastMessage(notificationText);
            mCompleteConversation.setLength(0);
            mCompleteConversation.append(notificationText);
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Sent: " + notificationText);

        }

        // 3. Set up main Intent for notification.
        Intent mainIntent = new Intent(this, MainActivity.class);

        PendingIntent mainPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        // 4. Create additional Actions (Intents) for the Notification.

        // 5. Build and issue the notification.

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. Later, we use the same global builder to get back the notification
        // we built here for the snooze action, that is, canceling the notification and relaunching
        // it several seconds later.

        // Notification Channel Id is ignored for Android pre O (26).
        notificationCompatBuilder = new NotificationCompat.Builder(
                getApplicationContext(), notificationChannelId);

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder);


        notificationCompatBuilder
                // BIG_TEXT_STYLE sets title and content.
                .setContentTitle(getString(R.string.portBI))
                .setContentText(notificationText)
                .setSmallIcon(R.drawable.logo_small)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Set primary color (important for Wear 2.0 Notifications).
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVibrate(new long[]{1000, 1000})
                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (actions == null) {
            actions = new ArrayList<>();
        }
        for (NotificationCompat.Action action : actions) {
            if (action != null) {
                notificationCompatBuilder.addAction(action);
            }
        }

        Notification notification = notificationCompatBuilder.build();

        if (notificationManager == null) {
            notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        }

// Issue the notification with notification manager.
        notificationManager.notify(notificationId, notification);

        notificationId++;
    }

    private void processIncoming(String text) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Received: " + text);
        }
        mLastResponse = mResponder.elzTalk(text);
        String line = TextUtils.isEmpty(text) ? mLastResponse : text + "\n" + mLastResponse;

        // Send a new line of conversation to update the Activity, unless the incoming text was
        // empty.
        if (!TextUtils.isEmpty(text)) {
            broadcastMessage(line);
        }
        NotificationManagerCompat.from(this).cancelAll();
        if (!mLastResponse.toLowerCase().contains("no pending") && !mLastResponse.toLowerCase().contains("no changes"))
            showNotification("", null);

        mCompleteConversation.append("\n" + line);
    }

    private void broadcastMessage(String message) {
        Intent intent = new Intent(MainActivity.ACTION_NOTIFY);
        intent.putExtra(MainActivity.EXTRA_MESSAGE, message);
        mBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Chat Service stopped");
        }
        NotificationManagerCompat.from(this).cancel(0);
        mBroadcastManager = null;
        super.onDestroy();
    }

    public NotificationCompat.Builder getNotificationCompatBuilder() {
        return notificationCompatBuilder;
    }
}
