package com.chenpeng.xiaopengweather.util;

import android.text.TextUtils;

import com.chenpeng.xiaopengweather.db.City;
import com.chenpeng.xiaopengweather.db.County;
import com.chenpeng.xiaopengweather.db.Province;
import com.chenpeng.xiaopengweather.gson.AQI;
import com.chenpeng.xiaopengweather.gson.Basic;
import com.chenpeng.xiaopengweather.gson.Forecast;
import com.chenpeng.xiaopengweather.gson.Now;
import com.chenpeng.xiaopengweather.gson.Suggestion;
import com.chenpeng.xiaopengweather.gson.Weather;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import interfaces.heweather.com.interfacesmodule.bean.weather.forecast.ForecastBase;
import interfaces.heweather.com.interfacesmodule.bean.weather.lifestyle.LifestyleBase;

public class Utility {

    /**
     * [{"id":1,"name":"北京"},...]
     * @param response
     * @return
     */
    public static boolean handleProvinceResponse(String response) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allProvinces = new JSONArray(response);
                for (int i = 0; i < allProvinces.length(); i++) {
                    JSONObject provinceObject = allProvinces.getJSONObject(i);
                    // 解析province
                    Province province = new Province();
                    province.setProvinceName(provinceObject.getString("name"));
                    province.setProvinceCode(provinceObject.getInt("id"));
                    // 保存province
                    province.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    /**
     * [{"id":113,"name":"南京"},...]
     * @param response
     * @param provinceId
     * @return
     */
    public static boolean handleCityResponse(String response, int provinceId){
        if(!TextUtils.isEmpty(response)){
            try {
                JSONArray allCites = new JSONArray(response);
                for (int i = 0; i < allCites.length(); i++) {
                    JSONObject cityObject = allCites.getJSONObject(i);
                    City city = new City();
                    city.setCityName(cityObject.getString("name"));
                    city.setCityCode(cityObject.getInt("id"));
                    city.setProvinceId(provinceId);
                    // 保存city数据
                    city.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    /**
     * [{"id":921,"name":"南京","weather_id":"CN101190101"},...]
     * @param response
     * @param cityId
     * @return
     */
    public static boolean handleCountyResponse(String response, int cityId){
        if(!TextUtils.isEmpty(response)){
            try {
                JSONArray allCounties = new JSONArray(response);
                for (int i = 0; i < allCounties.length(); i++) {
                    JSONObject countyOjbect = allCounties.getJSONObject(i);
                    County county = new County();
                    county.setCountyName(countyOjbect.getString("name"));
                    county.setWeatherId(countyOjbect.getString("weather_id"));
                    county.setCityId(cityId);
                    // 保存区的信息
                    county.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    /**
     * 将返回的json字符串解析成Weather实体类
     * @param response
     * @return
     */
    public static Weather handleWeatherResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather");
            String weatherContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherContent, Weather.class);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将接口中的weather映射为本地的weather
     * @param _weather
     * @return
     */
    public static Weather handleMyResponse(interfaces.heweather.com.interfacesmodule.bean.weather.Weather _weather){
        com.chenpeng.xiaopengweather.gson.Weather l_weather = new com.chenpeng.xiaopengweather.gson.Weather();
        // 解析数据
        // 1.status
        l_weather.status = _weather.getStatus();
        // 2.basic
        Basic l_basic = new Basic();
        l_basic.cityName = _weather.getBasic().getLocation();
        l_basic.weatherId = _weather.getBasic().getCid();
        Basic.Update _update = l_basic.new Update();
        _update.updateTime = _weather.getUpdate().getLoc();
        l_basic.update = _update;
        l_weather.basic = l_basic;
        // 3.aqi
        AQI l_aqi = new AQI();
        l_aqi.hum = _weather.getNow().getHum();
        l_aqi.vis = _weather.getNow().getVis();
        l_weather.aqi = l_aqi;
        // 4.now
        Now l_now = new Now();
        l_now.temperature = _weather.getNow().getTmp();
        Now.More _more = l_now.new More();
        _more.info = _weather.getNow().getCond_txt();
        l_now.more = _more;
        l_weather.now = l_now;
        // 5.suggestion
        Suggestion l_suggestion = new Suggestion();
        Suggestion.Comfort _comfort = l_suggestion.new Comfort();
        Suggestion.CarWash _carwash = l_suggestion.new CarWash();
        Suggestion.Sport _sport = l_suggestion.new Sport();
        String type_str = "";
        for (LifestyleBase e : _weather.getLifestyle()) {
            type_str = e.getType();
            if ("comf".equals(type_str)) { // 舒适度指数
                _comfort.info = e.getTxt();
                l_suggestion.comfort = _comfort;
            } else if ("cw".equals(type_str)) { // 洗车指数
                _carwash.info = e.getTxt();
                l_suggestion.carWash = _carwash;
            } else if ("sport".equals(type_str)) { // 运动指数
                _sport.info = e.getTxt();
                l_suggestion.sport = _sport;
            }
        }
        l_weather.suggestion = l_suggestion;
        // 6.daily_forecast
        List<Forecast> l_forecastlist = new ArrayList<>();
        for (ForecastBase f : _weather.getDaily_forecast()) {
            Forecast _forecast = new Forecast();
            // date
            _forecast.date = f.getDate();
            // cond
            Forecast.More _fmore = _forecast.new More();
            _fmore.info = f.getCond_txt_d();
            _forecast.more = _fmore;
            // tmp
            Forecast.Temperature _ftemp = _forecast.new Temperature();
            _ftemp.max = f.getTmp_max();
            _ftemp.min = f.getTmp_min();
            _forecast.temperature = _ftemp;
            l_forecastlist.add(_forecast);
        }
        l_weather.forecastList = l_forecastlist;
        return l_weather;
    }

}
