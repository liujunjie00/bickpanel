package com.incar.bickpanel;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    // 功率 电量  温度  压差 电流 电压
    private TextView power,electricity,temperature,pressure,current,voltage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}