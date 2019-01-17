package com.chenpeng.xiaopengweather;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.chenpeng.xiaopengweather.db.City;
import com.chenpeng.xiaopengweather.gson.AQI;
import com.chenpeng.xiaopengweather.gson.Basic;
import com.chenpeng.xiaopengweather.gson.Forecast;
import com.chenpeng.xiaopengweather.gson.Now;
import com.chenpeng.xiaopengweather.gson.Suggestion;
import com.chenpeng.xiaopengweather.gson.Weather;
import com.chenpeng.xiaopengweather.service.AutoUpdateService;
import com.chenpeng.xiaopengweather.util.HttpUtil;
import com.chenpeng.xiaopengweather.util.Utility;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import interfaces.heweather.com.interfacesmodule.bean.Lang;
import interfaces.heweather.com.interfacesmodule.bean.Unit;
import interfaces.heweather.com.interfacesmodule.bean.air.now.AirNow;
import interfaces.heweather.com.interfacesmodule.bean.air.now.AirNowCity;
import interfaces.heweather.com.interfacesmodule.bean.weather.forecast.ForecastBase;
import interfaces.heweather.com.interfacesmodule.bean.weather.lifestyle.LifestyleBase;
import interfaces.heweather.com.interfacesmodule.view.HeWeather;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherAcitivy extends AppCompatActivity {

    // 页面上的元素
    private ScrollView weatherLayout;

    // 城市
    private TextView titleCity;

    // 标题更新时间
    private TextView titleUpdateTime;

    // 当前城市的温度
    private TextView degreeText;

    // 天气的详细描述
    private TextView weatherInfoText;

    // 天气预报数据
    private LinearLayout forecastLayout;

    // 空气质量数据
    private TextView aqiText;

    // pm2.5数据
    private TextView pm25Text;

    // 舒适度数据
    private TextView comfortText;

    // 洗车建议
    private TextView carWashText;

    // 户外运动建议
    private TextView sportText;

    // 每日背景图
    private ImageView bingPicImg;

    // 下拉刷新
    public SwipeRefreshLayout swipeRefreshLayout;

    // 当前城市的编号
    private String weatherId;

    // drawerlayout
    public DrawerLayout drawerLayout;

    // 主页按钮
    private Button navButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置全屏效果
        if(Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        // 初始化各控件
        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comfortText = findViewById(R.id.comfort_text);
        carWashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        bingPicImg = findViewById(R.id.bing_pic_img);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        drawerLayout = findViewById(R.id.drawer_layout);
        navButton = findViewById(R.id.nav_button);

        // 读取weather的缓存数据
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        String bingPic = prefs.getString("bing_pic", null);
        // 加载背景图
        if(bingPic != null){ // 如果有缓存
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }
        // 加载天气信息
        if (weatherString != null) { // 如果有缓存
            Weather weather = new Gson().fromJson(weatherString, Weather.class);
            // 记录下当前的城市编号
            weatherId = weather.basic.weatherId;
            // 直接更新UI
            showWeatherInfo(weather);
        } else { // 如果没有缓存
            String locationname = getIntent().getStringExtra("locationname");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(locationname);
        }
        // 为刷新编写逻辑
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });
        // 为返回主页按钮绑定事件
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    /**
     * 加载每日一图
     */
    private void loadBingPic(){
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
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherAcitivy.this)
                        .edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                // 回到主线程更新页面
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherAcitivy.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }

    /**
     * 请求城市天气的信息
     *
     * @param locationname(如果不添加此参数,SDK会根据GPS联网定位,根据当前经纬度查询)所查询的地区， 可通过该地区名称、ID、Adcode、IP和经纬度进行查询经纬度格式：纬度,经度
     */
    public void requestWeather(final String locationname) {
        final Context _mcurrent = this;
        // 获取天气信息
        HeWeather.getWeather(
                _mcurrent,
                locationname,
                Lang.CHINESE_SIMPLIFIED,
                Unit.METRIC,
                new HeWeather.OnResultWeatherDataListBeansListener() {

                    @Override
                    public void onError(Throwable throwable) {
                        throwable.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(WeatherAcitivy.this, "获取天气信息失败!", Toast.LENGTH_SHORT).show();
                                // 停止显示加载动画
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        });
                    }

                    @Override
                    public void onSuccess(List<interfaces.heweather.com.interfacesmodule.bean.weather.Weather> list) {
                        // 获取结果数据
                        if (list != null && list.size() > 0) {
                            // 解析结果
                            interfaces.heweather.com.interfacesmodule.bean.weather.Weather _weather = list.get(0);
                            final Weather l_weather = Utility.handleMyResponse(_weather);

                            // 放到缓存中
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherAcitivy.this).edit();
                            editor.putString("weather", new Gson().toJson(l_weather));
                            editor.apply();

                            // 记录当前的 weatherid/locationname
                            weatherId = _weather.getBasic().getCid();

                            // 更新UI
                            if (l_weather != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // 更新页面
                                        showWeatherInfo(l_weather);
                                        // 停止显示加载动画
                                        swipeRefreshLayout.setRefreshing(false);
                                    }
                                });
                            }

                        }
                    }
                });
        loadBingPic();
    }


    /**
     * 渲染天气页面
     *
     * @param weather
     */
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        // 更新页面
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        // 清空预测天气
        forecastLayout.removeAllViews();
        // 遍历预测结果
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this)
                    .inflate(R.layout.forecast_item,
                            forecastLayout,
                            false);
            // 获取页面元素并跟新
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.hum);
            pm25Text.setText(weather.aqi.vis);
        }
        String comfort = "舒适度:" + weather.suggestion.comfort.info;
        String carWash = "洗车指数:" + weather.suggestion.carWash.info;
        String sport = "运动指数:" + weather.suggestion.sport.info;
        // 更新页面
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        // 显示weatherLAYOUT
        weatherLayout.setVisibility(View.VISIBLE);

        // 后台启动一个自动更新的服务(每隔8小时更新一次数据)
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }


}
