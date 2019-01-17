package com.chenpeng.xiaopengweather.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.chenpeng.xiaopengweather.MainActivity;
import com.chenpeng.xiaopengweather.R;
import com.chenpeng.xiaopengweather.WeatherAcitivy;
import com.chenpeng.xiaopengweather.db.City;
import com.chenpeng.xiaopengweather.db.County;
import com.chenpeng.xiaopengweather.db.Province;
import com.chenpeng.xiaopengweather.gson.Weather;
import com.chenpeng.xiaopengweather.util.HttpUtil;
import com.chenpeng.xiaopengweather.util.Utility;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    // 页面元素
    private ProgressDialog progressDialog;

    private TextView titleText;

    private Button backButton;

    private ListView listView;

    // 数据适配器
    private ArrayAdapter<String> adapter;

    // 数据列表
    private List<String> dataList = new ArrayList<>();

    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 城市列表
     */
    private List<City> cityList;

    /**
     * 县列表
     */
    private List<County> countyList;

    /**
     * 选中的省
     */
    private Province selectedProvince;

    /**
     * 选中的城市
     */
    private City selectedCity;

    /**
     * 当前选中的级别
     */
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        listView = view.findViewById(R.id.list_view);

        // 设置数据适配器
        adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1,
                dataList);
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // 为listView设置监听事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    //查询城市
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    //查询县
                    queryCounties();
                } else if (currentLevel == LEVEL_COUNTY) {
                    // 启动新的天气详情activity
                    String locationname = countyList.get(position).getWeatherId();
                    if(getActivity() instanceof MainActivity){ // 如果是主活动
                        Intent intent = new Intent(getActivity(), WeatherAcitivy.class);
                        intent.putExtra("locationname", locationname);
                        startActivity(intent);
                        getActivity().finish();
                    } else if (getActivity() instanceof WeatherAcitivy){ // 如果已经是在天气的活动上
                        WeatherAcitivy activity = (WeatherAcitivy) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefreshLayout.setRefreshing(true);
                        activity.requestWeather(locationname);
                    }

                }
            }
        });
        // 为返回按钮添加事件
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) {
                    //如果当前为县，则查询城市
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    //如果当前为城市，则查询省
                    queryProvinces();
                }
            }
        });
        // 默认查询所有的省份
        queryProvinces();
    }

    /**
     * 查询所有的省份
     */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        // 从数据库查询所有的省份
        provinceList = LitePal.findAll(Province.class);
        if (provinceList.size() > 0) { // 如果数据库中存在数据
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else { // 如果没有就从服务器抓取数据
            String address = "http://guolin.tech/api/china";
            //从服务器查找数据
            queryFromServer(address, "province");
        }
    }

    /**
     * 查询所有的市
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceId=?",
                String.valueOf(selectedProvince.getId()))
                .find(City.class);
        if (cityList.size() > 0) { // 查询所有的城市列表
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            //从服务器抓取城市数据
            queryFromServer(address, "city");
        }
    }

    /**
     * 查询选中市内所有的县
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityId=?",
                String.valueOf(selectedCity.getId()))
                .find(County.class);
        if (countyList.size() > 0) { // 如果数据库中存在数据
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else { // 如果没有从服务器抓取
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            // 从服务器抓取县数据
            queryFromServer(address, "county");

        }
    }

    /**
     * 从服务器抓取数据
     *
     * @param address
     * @param type
     */
    private void queryFromServer(String address, final String type) {
        // 开启对话框
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address,
                new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // 通过runOnUiThread()方法回到主线程处理逻辑
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 关闭dialog
                                closeProgressDialog();
                                Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseText = response.body().string();
                        boolean result = false;
                        // 调用不同的解析json的方法来获取对应的数据，然后保存到本地的数据库中
                        if ("province".equals(type)) {
                            result = Utility.handleProvinceResponse(responseText);
                        } else if ("city".equals(type)) {
                            result = Utility.handleCityResponse(responseText,
                                    selectedProvince.getId());
                        } else if ("county".equals(type)) {
                            result = Utility.handleCountyResponse(responseText,
                                    selectedCity.getId());
                        }
                        if (result) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // 关闭对话框
                                    closeProgressDialog();
                                    // 重新调用查询的方法进行页面更新
                                    if ("province".equals(type)) {
                                        queryProvinces();
                                    } else if ("city".equals(type)) {
                                        queryCities();
                                    } else if ("county".equals(type)) {
                                        queryCounties();
                                    }
                                }
                            });
                        }

                    }
                });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
    }

    /**
     * 隐藏对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
