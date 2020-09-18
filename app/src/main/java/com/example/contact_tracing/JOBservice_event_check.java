package com.example.contact_tracing;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

import static com.example.contact_tracing.Activity.TAG;
import static com.example.contact_tracing.Activity.contact_time_first;
import static com.example.contact_tracing.Activity.contact_time_imei;
import static com.example.contact_tracing.Activity.contact_time_last;
import static com.example.contact_tracing.Service_scan_function.add;
import static com.example.contact_tracing.Service_scan_function.rssi_level_1;
import static com.example.contact_tracing.Service_scan_function.rssi_level_2;
import static com.example.contact_tracing.Service_scan_function.rssi_level_3;
import static com.example.contact_tracing.Service_scan_function.time_difference;


//定時檢查是否有已經結束的接觸史
public class JOBservice_event_check extends JobService {
    public JOBservice_event_check() {
        Log.e(TAG,"JOBservice_event_check");
    }

    ArrayList<Integer> list = new ArrayList<>();
    @Override
    public boolean onStartJob(JobParameters params) {
        Calendar calendar = Calendar.getInstance();

        for (int i =0 ; i < contact_time_imei.size() ; i++){
            if(!time_difference(contact_time_last.get(i),calendar)){
                Log.e(TAG,"contact_time_imei"+contact_time_imei);
                add(contact_time_imei.get(i),contact_time_first.get(i),contact_time_last.get(i)
                        ,rssi_level_1.get(i),rssi_level_2.get(i),rssi_level_3.get(i));
                list.add(i);
            }
        }

        if(!list.isEmpty()){
            for (int i =list.size()-1 ; i>=0 ; i--){
                int indexx = list.get(i);
                contact_time_imei.remove(indexx);
                contact_time_first.remove(indexx);
                contact_time_last.remove(indexx);
                rssi_level_1.remove(indexx);
                rssi_level_2.remove(indexx);
                rssi_level_3.remove(indexx);
            }
            list.clear();
        }

        this.jobFinished(params,false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
