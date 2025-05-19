package com.example.rma_2_sakib_avdibasic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!(context instanceof MoviesActivity)) {
            new MoviesActivity();
        }
    }
}