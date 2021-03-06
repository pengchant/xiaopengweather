package com.chenpeng.xiaopengweather.util;

import android.app.Application;
import android.content.Context;

import org.litepal.LitePal;

import interfaces.heweather.com.interfacesmodule.view.HeConfig;

/**
 * 我的application
 */
public class MyApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        LitePal.initialize(this);
        HeConfig.init("HE1901151617421874", "dc4a315689c14f409b8ff4836166ba82");
        HeConfig.switchToFreeServerNode();
        context = getApplicationContext();
    }

    public static Context getContext(){
        return context;
    }
}
