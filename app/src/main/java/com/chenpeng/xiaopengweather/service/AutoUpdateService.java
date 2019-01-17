package com.chenpeng.xiaopengweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.chenpeng.xiaopengweather.util.HttpUtil;
import com.chenpeng.xiaopengweather.util.MyApplication;
import com.chenpeng.xiaopengweather.util.Utility;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

import interfaces.heweather.com.interfacesmodule.bean.Lang;
import interfaces.heweather.com.interfacesmodule.bean.Unit;
import interfaces.heweather.com.interfacesmodule.bean.weather.Weather;
import interfaces.heweather.com.interfacesmodule.view.HeWeather;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeataher();
        updateBingPic();
        // 使用alarm机制设置定时任务
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 8 * 60 * 60 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this, AutoUpdateService.class);
        // 使用pendingIntent的目的主要是用于所包含的intent执行是否满足某些条件。
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);

    }

    /**
     * 更新天气（sharedpreferences）
     */
    private void updateWeataher(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if(weatherString !=null){
            // 有缓存直接解析数据
            final com.chenpeng.xiaopengweather.gson.Weather mweather = new Gson().fromJson(
                    weatherString,
                    com.chenpeng.xiaopengweather.gson.Weather.class);
            String locationname = mweather.basic.weatherId;
            HeWeather.getWeather(
                    MyApplication.getContext(),
                    locationname,
                    Lang.CHINESE_SIMPLIFIED,
                    Unit.METRIC,
                    new HeWeather.OnResultWeatherDataListBeansListener() {

                        @Override
                        public void onError(Throwable throwable) {
                            throwable.printStackTrace();
                        }

                        @Override
                        public void onSuccess(List<Weather> list) {
                            if(list!=null & list.size()>0){
                                com.chenpeng.xiaopengweather.gson.Weather result
                                        = Utility.handleMyResponse(list.get(0));
                                // 保存到本地
                                SharedPreferences.Editor editor = PreferenceManager
                                        .getDefaultSharedPreferences(AutoUpdateService.this).edit();
                                editor.putString("weather", new Gson().toJson(result));
                                editor.apply();
                            }
                        }
                    });
        }
    }


    /**
     * 更新每日一图
     */
    private void updateBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                // 放入缓存
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                        AutoUpdateService.this)
                        .edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
            }
        });
    }
}
