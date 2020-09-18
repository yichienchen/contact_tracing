package com.example.contact_tracing;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static com.example.contact_tracing.Activity.DH;
import static com.example.contact_tracing.Activity.TAG;
import static com.example.contact_tracing.Activity.mBluetoothAdapter;
import static com.example.contact_tracing.DBHelper.TB3;
import static com.example.contact_tracing.Service_scan_function.format;

//定時檢查app狀態
public class JOBservice_status_check extends JobService {
    Calendar a = Calendar.getInstance();
    ArrayList<String> time_first = new ArrayList<>();
    ArrayList<String> time_last = new ArrayList<>();

    public JOBservice_status_check() {
        Log.e(TAG,"JOBservice_status_check");
    }
    @Override
    public boolean onStartJob(JobParameters params) {
        String time = format.format(a.getTime());

        SQLiteDatabase db = DH.getWritableDatabase();
        Cursor cursor = db.query(TB3,new String[]{"_id","time_first","time_last"},
                null,null,null,null,null);
        if (cursor.moveToFirst()) {
            for (int i = 0; i < cursor.getCount(); i++) {
                time_first.add(cursor.getString(1));
                time_last.add(cursor.getString(2));
                cursor.moveToNext();
            }
        }

        cursor.close();



        int index = time_first.size()-1;
        if(mBluetoothAdapter.isEnabled()){
            if(time_first.isEmpty()){
                add(time,"0");
            }else {
//            Log.e(TAG,"size: "+time_last.size()+time_first.size());
                if(time_last.get(index).equals("123")){
                    if(time_difference(time_first.get(index),time)){
                        update(time_first.get(index),time);
                    }else {
                        add(time,"0");
                    }
                }else {
                    if(time_difference(time_last.get(index),time)){
                        update(time_first.get(index),time);
                    }else {
                        add(time,"0");
                    }
                }
            }
        }

        this.jobFinished(params,true);
        JobScheduler scheduler = (JobScheduler)getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ComponentName componentName1 = new ComponentName(this, JOBservice_status_check.class);
        JobInfo job1 = new JobInfo.Builder(2, componentName1)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .setPersisted(true) // 重開機後是否執行
                .setMinimumLatency(1000*60*5)
                .build();
//调用schedule
        scheduler.schedule(job1);
        this.jobFinished(params,false);
        time_first.clear();
        time_last.clear();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    private void add(String first, String last) {
        SQLiteDatabase db = DH.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("time_first",first);
        values.put("time_last","123");
        db.insert(TB3,null,values);
        show(db);
    }

    private static void show(SQLiteDatabase db){
        Cursor cursor = db.query(TB3,new String[]{"_id","time_first","time_last"},
                null,null,null,null,null);

        while(cursor.moveToNext()){
            String _id = cursor.getString(0);
            String time_first = cursor.getString(1);
            String time_last = cursor.getString(2);
            Log.e(TAG,"TB3 id: " + _id + "\n" + "time_first: " + time_first + "\n" + "time_last: " + time_last);
        }
        cursor.close();
    }

    private static void clearTable3(){
        SQLiteDatabase db = DH.getWritableDatabase();
        db.execSQL("delete from " + TB3);
        show(db);
    }

    private static boolean time_difference(String time_first, String time_last){
        //時間差小於10分鐘，return true；相反則return false
        Date first;
        Date last;
        long different = 0;
        try {
            first = format.parse(time_first);
            last = format.parse(time_last);
            different = (last.getTime() - first.getTime())/(1000*60);
//            Log.e(TAG,"different: "+different);
        }catch (ParseException e){
            e.printStackTrace();
        }
        return different<20; //單位 分鐘
    }

    private void update(String time_first, String time_last) { // 更新指定的資料
        SQLiteDatabase db = DH.getReadableDatabase();
        ContentValues values = new ContentValues();

        values.put("time_last ", time_last);
        db.update(TB3,values,"time_first=? " , new String[]{time_first});
        show(db);
    }

    private void delete() {

    }
}
