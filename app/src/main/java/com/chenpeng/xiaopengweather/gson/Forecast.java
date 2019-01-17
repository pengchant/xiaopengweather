package com.chenpeng.xiaopengweather.gson;

import com.google.gson.annotations.SerializedName;

public class Forecast {

    /**
     * 日期
     */
    public String date;

    /**
     * 温度
     */
    @SerializedName("tmp")
    public Temperature temperature;

    /**
     * 详细
     */
    @SerializedName("cond")
    public More more;

    public class Temperature {

        public String max;

        public String min;

    }

    public class More {

        @SerializedName("txt_d")
        public String info;

    }
}
