package com.example.contact_tracing;

import android.app.Service;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import static com.example.contact_tracing.Activity.ManufacturerData_size;
import static com.example.contact_tracing.Activity.TAG;
import static com.example.contact_tracing.Activity.btn_service;
import static com.example.contact_tracing.Activity.btn_service_stop;
import static com.example.contact_tracing.Activity.contact_time_imei;
import static com.example.contact_tracing.Activity.id_byte;
import static com.example.contact_tracing.Activity.list_device;
import static com.example.contact_tracing.Activity.list_device_detail;
import static com.example.contact_tracing.Activity.mBluetoothLeScanner;
import static com.example.contact_tracing.Activity.matrix;
import static com.example.contact_tracing.Activity.mean_total;
import static com.example.contact_tracing.Activity.num_total;
import static com.example.contact_tracing.Activity.time_interval;
import static com.example.contact_tracing.Activity.time_previous;
import static com.example.contact_tracing.Function.byte2HexStr;
import static com.example.contact_tracing.Service_scan_function.leScanCallback;
import static com.example.contact_tracing.Service_scan_function.received_time;
import static com.example.contact_tracing.Service_scan_function.received_time_Calendar;
import static com.example.contact_tracing.Service_scan_function.received_time_interval;


public class Service_Scan extends Service {

    public Service_Scan() {
//        Log.e(TAG,"Service_Scan start");
        startScanning();
//        btn_service_stop.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                stopScanning();
//                stopSelf();
//            }
//        });
//        btn_service.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                startScanning();
//            }
//        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void startScanning() {
        received_time.clear();
        received_time_interval.clear();
        received_time_Calendar.clear();

//        Log.e(TAG,"start scanning");


        list_device.clear();
        list_device_detail.clear();

        num_total.clear();
        time_previous.clear();
        mean_total.clear();
        matrix.clear();
        time_interval.clear();
        contact_time_imei.clear();

        long zero=0;
        for (int j=0;j<100;j++){  //100 : mac address數量上限
            num_total.add(1);
            time_previous.add(zero);
            mean_total.add(zero);
        }



        //add six row
        matrix.add(new ArrayList<>());
        matrix.add(new ArrayList<>());
        matrix.add(new ArrayList<>());
        matrix.add(new ArrayList<>());
        matrix.add(new ArrayList<>());
        matrix.add(new ArrayList<>());
        matrix.add(new ArrayList<>());


        btn_service.setVisibility(View.INVISIBLE);
        btn_service_stop.setVisibility(View.VISIBLE);


//        0x0209311BFFFFFF01226C74524A5F2D33353539343430393234393831323030

        byte[] data_all = new byte[ManufacturerData_size];
        System.arraycopy(id_byte, 0, data_all, 1, id_byte.length);

//        System.arraycopy(data.toString().getBytes(), 0, data_all, id_byte.length, data.length());
        // ManufacturerData : packet編號(1) + id(4) + data(19)

        byte[] data_mask = new byte[] {0x00,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
        Log.e(TAG,"data_all: "+ byte2HexStr(data_all)+"\n"
                +"data_mask: "+byte2HexStr(data_mask));

        ScanFilter UUID_Filter_M = new ScanFilter.Builder().setManufacturerData(0xffff,data_all,data_mask).build();
        ArrayList<ScanFilter> filters = new ArrayList<>();
        filters.add(UUID_Filter_M);


        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
//                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)  //Fails to start power optimized scan as this feature is not supported
//                .setMatchMode()
//                .setNumOfMatches(1)
//                .setReportDelay()
                .build();
//        btScanner.flushPendingScanResults(leScanCallback);
        mBluetoothLeScanner.startScan(filters, settings, leScanCallback);
    }

    public static void stopScanning() {
        Log.e(TAG,"stopping scanning");
        btn_service.setVisibility(View.VISIBLE);
        btn_service_stop.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mBluetoothLeScanner.stopScan(leScanCallback);
            }
        });
    }
}
