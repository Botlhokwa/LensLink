package com.example.lenslink;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class LensLinkApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
    }
}
