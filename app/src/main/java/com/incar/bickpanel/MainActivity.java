package com.incar.bickpanel;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.TextView;

public class MainActivity extends Activity {
    // 功率 电量  温度  压差 电流 电压
    private TextView power,electricity,temperature,pressure,current,voltage;
    private static MySerialPortService mySerialPortService;
    private static final Handler handler = new Handler();
    private final int FLUSH_TIME = 300;
    private final MySerialPortService.StatusChange statusChange = new MySerialPortService.StatusChange() {
        @Override
        public void dateSet(int t0, int t1, int t2, int t3, int t4, int t5) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateData(t0,t1,t2,t3,t4,t5);
                }
            });


        }
    };

    private void updateData(int t0, int t1, int t2, int t3, int t4, int t5) {
        power.setText(t0);
        electricity.setText(t1);
        temperature.setText(t2);
        pressure.setText(t3);
        current.setText(t4);
        voltage.setText(t5);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        power = findViewById(R.id.power);
        electricity = findViewById(R.id.electricity);
        temperature = findViewById(R.id.temperature);
        pressure = findViewById(R.id.pressure);
        current = findViewById(R.id.current);
        voltage = findViewById(R.id.voltage);
        initData();
    }

    private void initData() {
        Intent intent = new Intent(this, MySerialPortService.class);
        getApplicationContext().bindService(intent,mConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MySerialPortService.MyBinder binder = (MySerialPortService.MyBinder) service;
            mySerialPortService = binder.getService();
            mySerialPortService.setStatusChange(statusChange);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mySerialPortService.queryState();
                    handler.postDelayed(this, FLUSH_TIME);
                }
            },FLUSH_TIME);


        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mySerialPortService = null;

        }
    };


}