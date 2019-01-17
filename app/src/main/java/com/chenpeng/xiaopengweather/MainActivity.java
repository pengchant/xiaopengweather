package com.chenpeng.xiaopengweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 查询是否有缓存，如果有就直接跳转到天气详情页面
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if(pref.getString("weather", null) != null){
            Intent intent = new Intent(this, WeatherAcitivy.class);
            startActivity(intent);
        }
    }
}
