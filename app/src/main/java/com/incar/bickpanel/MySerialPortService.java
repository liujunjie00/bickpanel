package com.incar.bickpanel;

import android.app.Service;
import android.content.Intent;
import android.hardware.SerialPort;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MySerialPortService extends Service {

    public static final String port = "/dev/ttyS2";
    private SerialPort serialPort;
    private ParcelFileDescriptor parcelFileDescriptor;
    private ByteBuffer dates = ByteBuffer.allocate(50);
    public static final String TAG = "MySerialPortService";
    private FileOutputStream mFileOutputStream;
    private final IBinder mBinder = new MyBinder();
    private StatusChange statusChange;

    public void setStatusChange(StatusChange statusChange) {
        this.statusChange = statusChange;
    }



    public MySerialPortService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "启动service");
        thread.start();

    }

    public byte[] getRealData(byte[] src){
        byte[] res = new byte[src.length+1];
        byte checkSum = 0x0 & 0xff;
        for (int i = 0; i < src.length; i++) {
            byte ui = src[i];
            checkSum +=(ui&0xff);
            res[i] = src[i];
        }
        checkSum = (byte) (checkSum%0x100);
        res[src.length] = checkSum;
        Log.d(TAG, "getRealData: checkSun" +checkSum);
        return res;
    }

    //获取每节的电压

    public  void getEveryoneVoltage(){
        byte[] setui = new byte[]{(byte) 0xDD,(byte) 0xA5,0x04,0x00, (byte) 0xFF, (byte) 0xFD} ;
        setCommd(setui);
    }

    /**
     * 查询每一节的电压 DD A5 04 00 FF FC 77
     * 查询状态       DD A5 03 00 FF FD 77
     * DD 是开始码
     * A5是读 ，5A是写
     * 03是读取基本状态,04是读取单体电压，05是读取保护板的信息
     * 00是正确码，08是错误码
     * */
    public void queryState(){ // DD A5 03 00 FF FD 77
        byte[] setui = new byte[]{(byte) 0xDD,(byte) 0xA5,0x03,0x00, (byte) 0xFF, (byte) 0xFD} ;
        setCommd(setui);


    }

    private void setCommd(byte[] su){
        try {
            if (mFileOutputStream != null) {
                mFileOutputStream.write(su);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }




    private final Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            serialPort = new SerialPort(port);
            try {
                parcelFileDescriptor = ParcelFileDescriptor.open(new File(port), ParcelFileDescriptor.MODE_READ_WRITE);
                serialPort.open(parcelFileDescriptor, 9600);
                mFileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
                Log.d(TAG,"main Thread start "+mFileOutputStream);
                writeThread.start();
                while (true) {
                    Log.d(TAG, "run: 读取线程阻塞:");
                    int read = serialPort.read(dates);
                    readDate();
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    });

    private final Thread writeThread = new Thread(new Runnable() {
        @Override
        public void run() {
            Log.d(TAG,"writeThread start "+mFileOutputStream);
            if (mFileOutputStream != null){
                //setColorTemperature(10);
                //queryState();
            }
        }
    });

    /**         0
     *解析数据  DD 03 00 1B    17 00 00 00 02 D0 03 E8 00 00 20 78 00 00 00 00 00 00 10 48 03 0F 02 0B 76 0B 82    FB FF 77
     * 0,1                    DD  03  不需要解
     *  2                   00   表示没有错误
     *  3                   1B 表示27 位数据段位不包括本身  17 00 00 00 02 D0 03 E8 00 00 20 78 00 00 00 00 00 00 10 48 03 0F 02 0B 76 0B 82
     * 4,5                  17 00  表示总电压 58v
     * 6,7                   00 00  表示电流0
     * 8,9                    02 D0  表示剩余容量 7.27AH
     * 10,11                    03 E8  表示标称容量 10.00AH
     * 12,13                    00 00  表示循环零次
     * 14,15                    20 78  表示生产日期
     * 16,17                   00 00 均衡低
     * 18,19                    00 00 均衡高
     * 20,21                    00 00 保护状态
     * 22,23                    10  软件版本
     *  24                   48   电量72%
     * 25                    03   mos 管状态
     * 26,27                    0F   15 串电池
     * 28                   02   温度个数
     *  29 30                  0B 76 第1个温度
     *   31  32                0B 82 第2个温度
     *                     FB FF 数据校验  00 1B 17 00 00 00 02 D0 03 E8 00 00 20 78 00 00 00 00 00 00 10 48 03 0F 02 0B 76 0B 82
     *                     77   不需要
     * */
    private void readDate() {
        byte[] by = dates.array();
        if (by[2] == 0x00 && by[1] == 0x03){
            setdianya(by[4],by[5]);
            setronlian(by[24]);
            setdianliu(by[6],by[7]);
            setwendu(by[29],by[30],by[31],by[32]);
            getEveryoneVoltage();
        }else if (by[2] == 0x00 && by[1] == 0x04){
            // 获取电池串数
            int cs = by[3];
            int  index = 0;
            List<Integer> list = new ArrayList();
            for (int i = 4; i < cs; i+=2) {
                int ui = BytesUtil.addByte(by[i],by[i+1]);
                list.add(ui);
                index++;
                Log.d(TAG, "readDate: 第"+index+"串电池电压"+ui+"mV");
            }
            Integer max = Collections.max(list);
            Integer min = Collections.min(list);
            t2 = max -min;
            Log.d(TAG, "readDate: 最大压差为："+t2);
            if (statusChange != null){
                statusChange.dateSet(t0,t1,t2,t3,t4,t5);
            }
        }
    }

    private void setwendu(byte b, byte b1, byte b2, byte b3) {
        int wd1 = BytesUtil.addByte(b,b1);
        int wd2 = BytesUtil.addByte(b2,b3);
        t3 = Math.max(wd2, wd1);
        Log.d(TAG, "setwendu: 温度为："+t3);

    }

    private void setdianliu(byte b, byte b1) {
       t4 =  BytesUtil.addByte(b,b1);
       t0 = t5*t4;
        Log.d(TAG, "setdianliu: 电流为："+t4+", 电压为："+t5 +" , 功率为:"+t0);
    }

    private void setronlian(byte b) {
        t1 = b&0xff;
    }

    private void setdianya(byte b, byte b1) {
        t5 = BytesUtil.addByte(b,b1);

    }


    // 功率 电量  温度  压差 电流 电压
    int t0,t1,t2,t3,t4,t5;


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }
    public class MyBinder extends Binder {
       public MySerialPortService getService() {
            return MySerialPortService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mFileOutputStream.close();
            serialPort.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, START_STICKY, startId);
    }
    //功率 电量  温度  压差 电流 电压
    public interface StatusChange{
        void dateSet(int t0,int t1,int t2,int t3,int t4,int t5);
    }


}
