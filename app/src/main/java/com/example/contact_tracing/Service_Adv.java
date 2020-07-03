package com.example.contact_tracing;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import static com.example.contact_tracing.Activity.AdvertiseCallbacks_map;
import static com.example.contact_tracing.Activity.Data_adv;
import static com.example.contact_tracing.Activity.TAG;
import static com.example.contact_tracing.Activity.adv_seg_packet;
import static com.example.contact_tracing.Activity.btn_service;
import static com.example.contact_tracing.Activity.btn_service_stop;
import static com.example.contact_tracing.Activity.extendedAdvertiseCallbacks_map;
import static com.example.contact_tracing.Activity.id_byte;
import static com.example.contact_tracing.Activity.mAdvertiseCallback;
import static com.example.contact_tracing.Activity.mBluetoothLeAdvertiser;
import static com.example.contact_tracing.Activity.pdu_size;
import static com.example.contact_tracing.Activity.version;
import static com.example.contact_tracing.Function.intToByte;


public class Service_Adv extends Service {
    static int num_packet;
    public Service_Adv() {
        startAdvertising();
//        btn_service_stop.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                stopAdvertising();
//                stopSelf();
//            }
//        });
//        btn_service.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                startAdvertising();
//            }
//        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void startAdvertising(){
//        Log.e(TAG, "Service: Starting Advertising");
        if (!version) {
//            pdu_size = 255-3-4-1-id_byte.length;
//            adv_seg_packet=data_seg(255);  //5.0:255
            pdu_size = 31-3-4-1-id_byte.length;
            adv_seg_packet = data_seg(31);
        }else {
            pdu_size = 31-3-4-1-id_byte.length;
            adv_seg_packet = data_seg(31);
        }
        num_packet=(Data_adv.length()/pdu_size)+1;
        if (mAdvertiseCallback == null) {
            if (mBluetoothLeAdvertiser != null) {
                for (int q=1;q<num_packet;q++){  //x
                    startBroadcast(q);
                }
            }
        }


        btn_service.setVisibility(View.INVISIBLE);
        btn_service_stop.setVisibility(View.VISIBLE);
    }

    public void startBroadcast(Integer order) {
        String localName =  String.valueOf(order) ;
        BluetoothAdapter.getDefaultAdapter().setName(localName);

        //BLE4.0
        if (version) {
            AdvertiseSettings settings = buildAdvertiseSettings();
            AdvertiseData advertiseData = buildAdvertiseData(1);  //order
            //Log.e(TAG,"buildAdvertiseData: " + buildAdvertiseData(order));
            AdvertiseData scanResponse = buildAdvertiseData_scan_response(order);
            mBluetoothLeAdvertiser.startAdvertising(settings, advertiseData, new Service_Adv.MyAdvertiseCallback(order));  //包含 scan response  BLE4.0

        } else {
            //BLE 5.0
            AdvertiseData advertiseData = buildAdvertiseData(1);  //order
//            Log.e(TAG,"advertiseData"+advertiseData.toString().length());
            AdvertiseData advertiseData_extended = buildAdvertiseData_extended();
            AdvertiseData periodicData = buildAdvertiseData_periodicData();
            AdvertisingSetParameters parameters = buildAdvertisingSetParameters();
            PeriodicAdvertisingParameters periodicParameters = buildperiodicParameters();
            mBluetoothLeAdvertiser.startAdvertisingSet(parameters,advertiseData,null,null,
                    null,0,0,new ExtendedAdvertiseCallback(order));
//            mBluetoothLeAdvertiser.startAdvertisingSet(parameters,advertiseData_extended,null,
//                    null,null,0,0,new ExtendedAdvertiseCallback(order));
        }

    }

    public static void stopAdvertising(){
        if (mBluetoothLeAdvertiser != null) {
            for (int q=1;q<num_packet;q++){
                stopBroadcast(q);
            }
            mAdvertiseCallback = null;
        }
        btn_service_stop.setVisibility(View.INVISIBLE);
        btn_service.setVisibility(View.VISIBLE);
    }

    public static void stopBroadcast(Integer order) {
        final AdvertiseCallback adCallback = AdvertiseCallbacks_map.get(order);
        final AdvertisingSetCallback exadCallback = extendedAdvertiseCallbacks_map.get(order);
        if (!version) {
            //BLE 5.0
            if (exadCallback != null) {
                try {
                    if (mBluetoothLeAdvertiser != null) {
                        mBluetoothLeAdvertiser.stopAdvertisingSet(exadCallback);
                    }
                    else {
                        Log.w(TAG,"Not able to stop broadcast; mBtAdvertiser is null");
                    }
                }
                catch(RuntimeException e) { // Can happen if BT adapter is not in ON state
                    Log.w(TAG,"Not able to stop broadcast; BT state: {}");
                }
                extendedAdvertiseCallbacks_map.remove(order);
            }
            //Log.e(TAG,order +" Advertising successfully stopped.");
        }else {
            //BLE 4.0
            if (adCallback != null) {
                try {
                    if (mBluetoothLeAdvertiser != null) {
                        mBluetoothLeAdvertiser.stopAdvertising(adCallback);
                    }
                    else {
                        Log.w(TAG,"Not able to stop broadcast; mBtAdvertiser is null");
                    }
                }
                catch(RuntimeException e) { // Can happen if BT adapter is not in ON state
                    Log.w(TAG,"Not able to stop broadcast; BT state: {}");
                }
                AdvertiseCallbacks_map.remove(order);
            }
            Log.e(TAG,order +" Advertising successfully stopped");
        }
    }

    public static byte[][] data_seg(int X){
        //Log.e(TAG,"Data : "+Data_adv.length());
        StringBuilder data = new StringBuilder(Data_adv);
        for(int c=data.length();c%pdu_size!=0;c++){
            //Data=Data + "0";
            data.append("0");
        }
        Data_adv = data.toString();
        //Log.e(TAG,"Data : "+Data_adv.length());

        byte[] byte_data = Data_adv.getBytes();
        int pack_num = 1;
        int coun = 0;
        num_packet =(byte_data.length/pdu_size)+1;
        byte[][] adv_byte = new byte[num_packet][pdu_size+id_byte.length+1];
        for (int counter = byte_data.length; counter >0; counter = counter-pdu_size) {
            if (counter>=pdu_size){
                adv_byte[pack_num][0] = intToByte(pack_num);
                System.arraycopy(id_byte,0,adv_byte[pack_num],1,id_byte.length);
                System.arraycopy(byte_data,coun,adv_byte[pack_num],id_byte.length+1,pdu_size);
                pack_num++;
                coun=coun+pdu_size;
            }else {
                adv_byte[pack_num][0] = intToByte(pack_num);
                //Log.e(TAG,"pack_num="+pack_num);
                System.arraycopy(id_byte,0,adv_byte[pack_num],1,id_byte.length);
                System.arraycopy(byte_data,coun,adv_byte[pack_num],id_byte.length+1,pdu_size);
//                Log.e(TAG,"adv_byte: "+byte2HexStr(adv_byte[pack_num])+";  counter: "+counter + ";  length: "+adv_byte[pack_num].length);
//                Log.e(TAG,"coco"+byte_len+" pack_num: "+pack_num);
            }
        }
//        Log.e(TAG, "pack_num = " + pack_num);
//        for(int xx= 0;xx<pack_num;xx++) {
//            Log.e(TAG, xx + " adv_byte.length  = " + adv_byte[xx].length);
//        }
        return adv_byte;
    }

    //BLE 4.0
    public static class MyAdvertiseCallback extends AdvertiseCallback {
        private final Integer _order;
        public MyAdvertiseCallback(Integer order) {
            _order = order;
        }
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "Advertising failed errorCode: "+errorCode);
            switch (errorCode) {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    Log.e(TAG,"ADVERTISE_FAILED_ALREADY_STARTED");
                    break;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    Log.e(TAG,"ADVERTISE_FAILED_DATA_TOO_LARGE");
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(TAG,"ADVERTISE_FAILED_FEATURE_UNSUPPORTED");
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    Log.e(TAG,"ADVERTISE_FAILED_INTERNAL_ERROR");
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    Log.e(TAG,"ADVERTISE_FAILED_TOO_MANY_ADVERTISERS");
                    break;
                default:
                    Log.e(TAG,"Unhandled error : "+errorCode);
            }
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.e(TAG, _order +" Advertising successfully started");
            AdvertiseCallbacks_map.put(_order, this);
        }
    }

    static AdvertiseData buildAdvertiseData(Integer order) {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeDeviceName(true);
        dataBuilder.setIncludeTxPowerLevel(false);
        dataBuilder.addManufacturerData(0xffff,adv_seg_packet[order]);
        return dataBuilder.build();
    }

    static AdvertiseData buildAdvertiseData_scan_response(Integer order) {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addManufacturerData(0xffff,adv_seg_packet[1]);  //order
        return dataBuilder.build();
    }

    //BLE 5.0
    public class ExtendedAdvertiseCallback extends AdvertisingSetCallback {
        private final Integer _order;
        public ExtendedAdvertiseCallback(Integer order) {
            _order = order;
        }

        @Override
        public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
            if (status==AdvertisingSetCallback.ADVERTISE_FAILED_ALREADY_STARTED)
                Log.e(TAG, "ADVERTISE_FAILED_ALREADY_STARTED");
            else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED)
                Log.e(TAG, "ADVERTISE_FAILED_FEATURE_UNSUPPORTED");
            else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_DATA_TOO_LARGE)
                Log.e(TAG, "ADVERTISE_FAILED_DATA_TOO_LARGE");
            else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_INTERNAL_ERROR)
                Log.e(TAG, "ADVERTISE_FAILED_INTERNAL_ERROR");
            else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS)
                Log.e(TAG, "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS");
            else if (status==AdvertisingSetCallback.ADVERTISE_SUCCESS) {
//                Log.e(TAG,   "ADVERTISE_SUCCESS" + "(" + _order + ")");
                btn_service.setVisibility(View.INVISIBLE);
                btn_service_stop.setVisibility(View.VISIBLE);
                extendedAdvertiseCallbacks_map.put(_order,this);
            }
        }
        @Override
        public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
//            Log.e(TAG, "onAdvertisingSetStopped:" + "("+ _order +")");
        }

        @Override
        public void onAdvertisingEnabled (AdvertisingSet advertisingSet, boolean enable, int status) {
            Log.e(TAG,"onAdvertisingEnabled: " + enable + "("+ _order +")");
            btn_service_stop.setVisibility(View.INVISIBLE);
            btn_service.setVisibility(View.VISIBLE);
        }
    }

    public static AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .setTimeout(0);
        return settingsBuilder.build();
    }

    public static AdvertisingSetParameters buildAdvertisingSetParameters() {
        AdvertisingSetParameters.Builder parametersBuilder = new AdvertisingSetParameters.Builder()
                .setConnectable(false)
                .setInterval(400)  //balanced mode (400*0.625) 1600/160
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                .setLegacyMode(true);
        return parametersBuilder.build();
    }

    public static PeriodicAdvertisingParameters buildperiodicParameters() {
        PeriodicAdvertisingParameters.Builder periodicparametersBuilder = new PeriodicAdvertisingParameters.Builder()
                .setInterval(200);
        return periodicparametersBuilder.build();
    }

    static AdvertiseData buildAdvertiseData_extended() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeDeviceName(true);
//        Log.e(TAG,"data: "+Data_adv.getBytes().length);
        dataBuilder.addManufacturerData(0xffff,Data_adv.getBytes());
        return dataBuilder.build();
    }

    //TODO data要改
    static AdvertiseData buildAdvertiseData_periodicData() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        byte[] data = {0x00,0x11,0xf,0x1a};
        dataBuilder.addManufacturerData(0xffff,data);
        return dataBuilder.build();
    }
}
