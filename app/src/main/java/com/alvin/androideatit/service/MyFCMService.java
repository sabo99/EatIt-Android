package com.alvin.androideatit.service;

import androidx.annotation.NonNull;

import com.alvin.androideatit.Common.Common;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;

public class MyFCMService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> dataRecv = remoteMessage.getData();

        if (dataRecv != null)
        {
            Common.showNotification(this, new Random().nextInt(),
                    dataRecv.get(Common.NOTIF_TITLE),
                    dataRecv.get(Common.NOTIF_CONTENT),
                    null);
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);

        Common.updateToken(this,s);
    }
}
