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

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static de.f2s.mandm.portbi.ResponderService.ACTION_RESPONSE;
import static de.f2s.mandm.portbi.ResponderService.EXTRA_NOTIFICATION;


public class MainActivity extends Activity {
    @SuppressWarnings("unused")
    private static final String TAG = "MainActivity";

    public static final String EXTRA_MESSAGE = "message";

    public static final String ACTION_NOTIFY = "de.f2s.mandm.portbi.NOTIFY";

    public static final String ACTION_GET_CONVERSATION = "de.f2s.mandm.portbi.CONVERSATION";

    private BroadcastReceiver mReceiver;

    ProgressDialog prgDialog;

    private TextView mHistoryView;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.main);
        cont = true;
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                processMessage(intent);
            }
        };

        mHistoryView = (TextView) findViewById(R.id.history);
        startResponderService(ResponderService.ACTION_WAIT);
        getNotifications();
        ImageView mPullButton = findViewById(R.id.pullButton);
        mPullButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkStatus();
            }
        });

        prgDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
        prgDialog.setMessage("Checking...");

        // Set Cancelable as True
        prgDialog.setCancelable(true);
        prgDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                prgDialog.hide();
            }
        });
    }

    private void startResponderService(String action) {
        Intent serviceIntent = new Intent(this, ResponderService.class);
        serviceIntent.setAction(action);
        startService(serviceIntent);
    }

    private void startResponderService(String action, String extra) {
        Intent serviceIntent = new Intent(this, ResponderService.class);
        serviceIntent.setAction(action);
        serviceIntent.putExtra(EXTRA_NOTIFICATION, extra);
        startService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                new IntentFilter(ACTION_NOTIFY));
        mHistoryView.setText("");
        startResponderService(ACTION_GET_CONVERSATION);
        cont = true;
        getNotifications();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onPause();
        cont = false;
    }

    private void processMessage(Intent intent) {
        String text = intent.getStringExtra(EXTRA_MESSAGE);
        if (!TextUtils.isEmpty(text)) {
            mHistoryView.append("\n" + text);
        }
    }

    private String baseUrl = "localhost/api/v1/getNotifications";
    boolean cont;

    public void checkStatus() {
        prgDialog.show();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                    mHistoryView.setText("");
                    startResponderService(ACTION_RESPONSE, "No changes");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                prgDialog.cancel();
            }
        });

    }

    public void getNotifications() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(20000);
                    startResponderService(ResponderService.ACTION_UPDATE, "There's a new order!");
                    prgDialog.cancel();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                try {
                    URL url = new URL(baseUrl);
                    while (cont) {
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        if (conn.getResponseCode() == 200) {
                            InputStream responseBody = conn.getInputStream();
                            InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");
                            JsonReader jsonReader = new JsonReader(responseBodyReader);

                            jsonReader.beginObject();

                            while (jsonReader.hasNext()) {
                                String key = jsonReader.nextName();
                                //final int oldStatus = statusId;
                                if (key.equals("id")) {
                                    //statusId = jsonReader.nextInt();
                                } else {
                                    jsonReader.skipValue();
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        startResponderService(ResponderService.ACTION_RESPONSE, "Test!");
                                    }
                                });
                            }

                            conn.disconnect();
                            jsonReader.close();
                            responseBodyReader.close();
                            responseBody.close();
                            Thread.sleep(2000);
                        } else {
                            throw new Exception("Request failed");
                        }
                    }
                } catch (Exception e) {
                }
            }
        });
    }
}
