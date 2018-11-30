package de.f2s.mandm.portbi;

import android.app.IntentService;
import android.app.Notification;
import android.app.RemoteInput;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static de.f2s.mandm.portbi.ResponderService.ACTION_DONE;
import static de.f2s.mandm.portbi.ResponderService.ACTION_REMIND;
import static de.f2s.mandm.portbi.ResponderService.ACTION_UPDATE;
import static de.f2s.mandm.portbi.ResponderService.EXTRA_NOTIFICATION;
import static de.f2s.mandm.portbi.ResponderService.EXTRA_REPLY;

public class NotificationHandlerService extends IntentService {

    public static final String ACTION_REPLY =
            "de.f2s.mandm.portbi.action.REPLY";
    public static final String ACTION_SNOOZE =
            "de.f2s.mandm.portbi.action.SNOOZE";
    private static final String TAG = "NotificationHandlerService";
    TextToSpeech textToSpeech;
    private boolean speechInitialized;
    private int speechId;


    public NotificationHandlerService() {
        super("NotificationHandlerService");
        speechId = 0;
        speechInitialized = false;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent(): " + intent);
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_REPLY.equals(action)) {
                handleActionReply(getMessage(intent));
            } else if (ACTION_SNOOZE.equals(action)) {
                String stringExtra = intent.getStringExtra(EXTRA_NOTIFICATION);
                handleActionSnooze(stringExtra);
            }
        }
    }

    /*
     * Extracts CharSequence created from the RemoteInput associated with the Notification.
     */
    private CharSequence getMessage(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence(EXTRA_REPLY);
        }
        return null;
    }

    /**
     * Handles action Dismiss in the provided background thread.
     */
    private void handleActionReply(CharSequence replyCharSequence) {
        String response = replyCharSequence.toString().toLowerCase();
        Log.d(TAG, "Replied: " + response);

        cancelNotifications();
        Intent serviceIntent = new Intent(this, ResponderService.class);
        serviceIntent.setAction(ACTION_DONE);
        String textResponse = "Ok";
        if (response.contains("order")) {
            Log.d(TAG, "Order this!!!");
            textResponse = "Done! Have a great day";
        } else if (response.contains("ask") || response.contains("clarification")) {
            Log.d(TAG, "Clarify! Send an email!!!");
            textResponse = "I will send an email for you.";
        } else if (response.contains("remind")) {
            textResponse = "Of course. Enjoy your day!";
        }
        textToSpeechFunction(textResponse);
        serviceIntent.putExtra(EXTRA_NOTIFICATION, textResponse);
        startService(serviceIntent);
    }

    public void textToSpeechFunction(final String text) {
        if (!speechInitialized) {
            textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        textToSpeech.setLanguage(Locale.UK);
                        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speech" + speechId);
                        speechId++;
                    }
                }
            });
        }
    }


    private void cancelNotifications() {
        NotificationManagerCompat notificationManagerCompat =
                NotificationManagerCompat.from(getApplicationContext());
        notificationManagerCompat.cancelAll();
    }

    /**
     * Handles action Snooze in the provided background thread.
     */
    private void handleActionSnooze(String oldNotification) {
        Log.d(TAG, "Snooze " + oldNotification);

        NotificationManagerCompat notificationManagerCompat =
                NotificationManagerCompat.from(getApplicationContext());
        notificationManagerCompat.cancelAll();


        String notificationMessage = oldNotification;
        Timer scheduleTimer = new Timer();

        final Intent serviceIntent;
        serviceIntent = new Intent(this, ResponderService.class);
        serviceIntent.setAction(ACTION_REMIND);
        serviceIntent.putExtra(EXTRA_NOTIFICATION, "");
        TimerTask scheduledTask = new TimerTask() {
            @Override
            public void run() {
                startService(serviceIntent);
            }
        };
        Calendar calNow = Calendar.getInstance();
        Calendar calSet = (Calendar) calNow.clone();
        calSet.set(Calendar.HOUR_OF_DAY, 10);
        calSet.add(Calendar.DATE, 1);
        long delay = calSet.getTimeInMillis() - calNow.getTimeInMillis();

        Log.d(TAG, "Will notify at " + calSet.getTime());
        Log.d(TAG, "This is in " + delay / 60000 + " minutes");

        scheduleTimer.schedule(scheduledTask, delay);

    }
}

