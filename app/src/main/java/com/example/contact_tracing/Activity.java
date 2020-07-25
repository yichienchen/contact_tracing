package com.example.contact_tracing;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.example.contact_tracing.Service_Adv.stopAdvertising;
import static com.example.contact_tracing.Service_Scan.stopScanning;


public class Activity extends AppCompatActivity {
    static int ManufacturerData_size = 24;  //ManufacturerData長度
    static String TAG = "chien";
    static long contant_time_limit = 5*60; //5分鐘

    //static String Data_adv = "CHENYICHIENCHENYI123456sdbjfksdfjsbvjkabksdafs";
    static String Data_adv;
    static boolean version = false;  //true: 4.0 , false:5.0
    static byte[][] adv_seg_packet;
    static byte[] id_byte = new byte[]{0x22, 0x6c, 0x74, 0x52, 0x04a, 0x5f, 0x2d};
    static int pdu_size;  //純data，不包含id跟manufacturer specific data的flags及第幾個packet


    static List<String> list_device = new ArrayList<>();
    static List<String> list_device_detail = new ArrayList<>();


    static ArrayList<ArrayList<Object>> matrix = new ArrayList<>();
    static ArrayList<ArrayList<Object>> time_interval = new ArrayList<>();
    static ArrayList<Integer> num_total = new ArrayList<>();
    static ArrayList<Long> time_previous = new ArrayList<>();
    static ArrayList<Long> mean_total = new ArrayList<>();

    static ArrayList<String> contact_time_imei = new ArrayList<>();
    static ArrayList<Calendar> contact_time_first = new ArrayList<>();
    static ArrayList<Calendar> contact_time_last = new ArrayList<>();

    static Map<Integer, AdvertiseCallback> AdvertiseCallbacks_map;
    static Map<Integer, AdvertisingSetCallback> extendedAdvertiseCallbacks_map;


    static BluetoothManager mBluetoothManager;
    static BluetoothAdapter mBluetoothAdapter;
    static BluetoothLeScanner mBluetoothLeScanner;
    static AdvertiseCallback mAdvertiseCallback;
    static BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    @SuppressLint("StaticFieldLeak")
    static ImageButton btn_service_stop;
    @SuppressLint("StaticFieldLeak")
    static ImageButton btn_service;
    @SuppressLint("StaticFieldLeak")
    static ImageButton btn_list;
    @SuppressLint("StaticFieldLeak")
    static TextView textview_status;
    @SuppressLint("StaticFieldLeak")
    static androidx.appcompat.widget.Toolbar toolbar;



    private Receiver_BLE mBLEReceiver;

    static NotificationManager notificationManager;
    static NotificationChannel mChannel;
    Intent intentMainActivity;
    static PendingIntent pendingIntent;
    Notification notification;
    static Intent received_id;

    Intent adv_service;
    Intent scan_service;

    public static String mDeviceIMEI = "0";
    TelephonyManager mTelephonyManager = null;

    public static DBHelper DH=null;

    static Calendar time_bluetooth_off;
    static Calendar time_bluetooth_on;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DH = new DBHelper(this,"MYDB",null,2);
        initialize();
        permission();
        element();
        notice();
        mjobScheduler();
        getDeviceImei();
        Data_adv = mDeviceIMEI;


        @SuppressLint("WrongThread") String token = FirebaseInstanceId.getInstance().getId();
        Log.e("FCM", "Token:"+token);
    }




    @Override
    public void onDestroy() {
        notificationManager.notify(1000, notification);
        unregisterReceiver(mBLEReceiver);
        stopService(adv_service);
        stopService(scan_service);
        Log.e(TAG, "onDestroy() called");
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
//        Log.e(TAG, "onResume() called");
        permission();
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }

    public void notice() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mChannel = new NotificationChannel("Activity", "主畫面", NotificationManager.IMPORTANCE_HIGH);
        intentMainActivity = new Intent(this, Activity.class);
        pendingIntent = PendingIntent.getActivity(this, 0, intentMainActivity, PendingIntent.FLAG_UPDATE_CURRENT);
        notification = new Notification.Builder(this, "Activity")
                .setSmallIcon(R.drawable.ble)
//                .setContentTitle("CONTACT TRACING")
                .setContentText("app已關閉 請重新開啟")
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .build();
//        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        mChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(mChannel);
        notificationManager.cancel(1);

        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mBLEReceiver = new Receiver_BLE();
        registerReceiver(mBLEReceiver, filter1);


    }

    private void initialize() {
        if (mBluetoothLeScanner == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager != null) {
                BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
                if (bluetoothAdapter != null) {
                    mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                }
            }
        }
        if (mBluetoothLeAdvertiser == null) {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
                if (bluetoothAdapter != null) {
                    mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
                }
            }
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isLeExtendedAdvertisingSupported()){
            version =true;
        }
    }

    public void permission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.VIBRATE}, 1);
        }

    }

    private void element() {
        toolbar = findViewById(R.id.toolbar);
//        toolbar.setSubtitle("CONTACT  TRACING");
        toolbar.setNavigationIcon(R.mipmap.home);
        setSupportActionBar(toolbar);
//        ActionBar actionBar = getSupportActionBar();
//        actionBar.setDisplayHomeAsUpEnabled(true);
//

        btn_service = findViewById(R.id.btn_service);
        btn_service_stop = findViewById(R.id.btn_service_stop);
        btn_list = findViewById(R.id.btn_list);

        btn_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG,"service is started");
                permission();
                String msg = "Service has started!";
                textview_status.setText("");
                textview_status.append(msg);
                startService(scan_service);
                startService(adv_service);
                btn_service.setVisibility(View.INVISIBLE);
                btn_service_stop.setVisibility(View.VISIBLE);
            }
        });

        btn_service.setVisibility(View.VISIBLE);
        btn_service_stop.setVisibility(View.INVISIBLE);

        btn_service_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(scan_service);
                stopService(adv_service);
                stopAdvertising();
                stopScanning();
                Log.e(TAG,"service is stopped");
                String msg = "Service has stopped!";
                textview_status.setText("");
                textview_status.setText(msg);


                btn_service.setVisibility(View.VISIBLE);
                btn_service_stop.setVisibility(View.INVISIBLE);
            }
        });

        btn_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(Activity.this, Activity_history.class);
                startActivity(intent);
            }
        });


        textview_status = findViewById(R.id.textview_status);
        textview_status.setText("");


        /*--------------------------------------intent----------------------------------------*/
        adv_service = new Intent(Activity.this, Service_Adv.class);
        scan_service = new Intent(Activity.this, Service_Scan.class);

        /*-------------------------------------Receiver---------------------------------------*/
        received_id = new Intent();

        /*--------------------------------------others----------------------------------------*/
        AdvertiseCallbacks_map = new TreeMap<>();
        extendedAdvertiseCallbacks_map = new TreeMap<>();
    }


    @SuppressLint("HardwareIds")
    private void getDeviceImei() {

        Log.e(TAG,"getDeviceImei ");
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//        Log.e(TAG, "mTelephonyManager: "+mTelephonyManager.getLine1Number());
        String IMSI;
//        Log.e(TAG, "IMSI: "+IMSI);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                assert mTelephonyManager != null;
                mDeviceIMEI = mTelephonyManager.getImei();
                IMSI = mTelephonyManager.getSubscriberId();
                Log.e(TAG, "IMEI: "+mDeviceIMEI);
                Log.e(TAG, "IMSI: "+IMSI);
            } else {
                assert mTelephonyManager != null;
                mDeviceIMEI = mTelephonyManager.getDeviceId();
                Log.e(TAG, "IMEI: "+mDeviceIMEI);
            }
        } catch (SecurityException e) {
            // expected
            if (Build.VERSION.SDK_INT >= 26) {
                Log.e(TAG, "SecurityException e "+e);

            }
        }
    }

    public void mjobScheduler(){
        JobScheduler scheduler = (JobScheduler)getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ComponentName componentName = new ComponentName(this, JOBservice_http.class);
        ComponentName componentName1 = new ComponentName(this, JOBservice_status_check.class);
        ComponentName componentName2 = new ComponentName(this, JOBservice_event_check.class);

        JobInfo job = new JobInfo.Builder(1, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true) // 重開機後是否執行
                .setPeriodic(1000*60*60*8)
                .build();
        JobInfo job1 = new JobInfo.Builder(2, componentName1)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true) // 重開機後是否執行
                .setPeriodic(1000*60*15)
                .build();
        JobInfo job2 = new JobInfo.Builder(3, componentName2)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true) // 重開機後是否執行
                .setPeriodic(1000*60*60)
                .build();
//调用schedule
        assert scheduler != null;
        scheduler.schedule(job);
//        scheduler.schedule(job1);
        scheduler.schedule(job2);
    }



}
