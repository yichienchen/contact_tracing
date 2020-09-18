package com.example.contact_tracing;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
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
import static com.example.contact_tracing.Activity.id_byte;
import static com.example.contact_tracing.Activity.mAdvertiseCallback;
import static com.example.contact_tracing.Activity.mBluetoothLeAdvertiser;
import static com.example.contact_tracing.Activity.pdu_size;
import static com.example.contact_tracing.Function.intToByte;


public class Service_Adv extends Service {
    static int num_packet;
    public Service_Adv() {
        startAdvertising();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void startAdvertising(){
        pdu_size = 31-3-4-1-id_byte.length;
        adv_seg_packet = data_seg();

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
        AdvertiseSettings settings = buildAdvertiseSettings();
        AdvertiseData advertiseData = buildAdvertiseData(order);  //order
        mBluetoothLeAdvertiser.startAdvertising(settings, advertiseData, new Service_Adv.MyAdvertiseCallback(order));
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

    public static byte[][] data_seg(){
        StringBuilder data = new StringBuilder(Data_adv);
        for(int c=data.length();c%pdu_size!=0;c++){
            data.append("0");
        }
        Data_adv = data.toString();

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
                System.arraycopy(id_byte,0,adv_byte[pack_num],1,id_byte.length);
                System.arraycopy(byte_data,coun,adv_byte[pack_num],id_byte.length+1,pdu_size);
            }
        }

        return adv_byte;
    }

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

    public static AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .setTimeout(0);
        return settingsBuilder.build();
    }

}
