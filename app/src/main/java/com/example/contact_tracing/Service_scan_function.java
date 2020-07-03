package com.example.contact_tracing;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static com.example.contact_tracing.Activity.DH;
import static com.example.contact_tracing.Activity.TAG;
import static com.example.contact_tracing.Activity.contact_time_first;
import static com.example.contact_tracing.Activity.contact_time_imei;
import static com.example.contact_tracing.Activity.contact_time_last;
import static com.example.contact_tracing.Activity.contant_time_limit;
import static com.example.contact_tracing.Activity.list_device;
import static com.example.contact_tracing.Activity.list_device_detail;
import static com.example.contact_tracing.Activity.matrix;
import static com.example.contact_tracing.Activity.mean_total;
import static com.example.contact_tracing.Activity.num_total;
import static com.example.contact_tracing.Activity.textview_status;
import static com.example.contact_tracing.Activity.time_interval;
import static com.example.contact_tracing.Activity.time_previous;
import static com.example.contact_tracing.DBHelper.TB1;
import static com.example.contact_tracing.Function.byte2HexStr;
import static com.example.contact_tracing.Function.hexToAscii;


//TODO 都是利用收到下一個封包當作觸發裝置
public class Service_scan_function {
    private static SimpleDateFormat f = new SimpleDateFormat("YYYY-MM-dd,HH:mm:ss.SS");
    static SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd,HH:mm");
    //static SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss.SS");

    //限單一裝置
    static ArrayList<Calendar> received_time_Calendar = new ArrayList<>();
    static ArrayList<String> received_time = new ArrayList<>();
    static ArrayList<Long> received_time_interval = new ArrayList<>();

    static ArrayList<Integer> rssi_level_1 = new ArrayList<>();  //rssi>-70
    static ArrayList<Integer> rssi_level_2 = new ArrayList<>();  //-70>rssi>-90
    static ArrayList<Integer> rssi_level_3 = new ArrayList<>();  //-90<>ssi

    static ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result){
            /*----------------------------------------------------------------contact event----------------------------------------------------------------*/
            String received_imei = byte2HexStr(result.getScanRecord().getManufacturerSpecificData(0xffff));
            received_imei = hexToAscii(received_imei).subSequence(8,23).toString();

            Calendar a = Calendar.getInstance();
            String currentTime = f.format(a.getTime());

            int rssi = result.getRssi();
            int level;
            if(rssi>-70){
                level = 1;
            }else if(rssi>-90){
                level = 2;
            }else {
                level = 3;
            }

            int indexx = 0;
            if(!contact_time_imei.contains(received_imei)){
                switch (level){
                    case 1:
                        rssi_level_1.add(1);
                        rssi_level_2.add(0);
                        rssi_level_3.add(0);
                        break;
                    case 2:
                        rssi_level_1.add(0);
                        rssi_level_2.add(1);
                        rssi_level_3.add(0);
                        break;
                    case 3:
                        rssi_level_1.add(0);
                        rssi_level_2.add(0);
                        rssi_level_3.add(1);
                        break;
                }
                contact_time_imei.add(received_imei);
                contact_time_first.add(a);
                contact_time_last.add(a);
            }else {
                indexx = contact_time_imei.indexOf(received_imei);
                switch (level){
                    case 1:
                        int i = rssi_level_1.get(indexx);
                        rssi_level_1.set(indexx,i+1);
                        break;
                    case 2:
                        i = rssi_level_2.get(indexx);
                        rssi_level_2.set(indexx,i+1);
                        break;
                    case 3:
                        i = rssi_level_3.get(indexx);
                        rssi_level_3.set(indexx,i+1);
                        break;
                }

                if(time_difference(contact_time_last.get(indexx),a)){  //判斷事件是否結束
                    contact_time_last.set(indexx,a);
//                    Log.e(TAG,"繼續");
                } else {
                        if(rssi_level_1.get(indexx)+rssi_level_2.get(indexx)+rssi_level_3.get(indexx)>2){
                            Log.e(TAG,"事件結束，儲存起來 " + contact_time_imei.get(indexx)+contact_time_first.get(indexx).getTime()+contact_time_last.get(indexx).getTime());
                            add(contact_time_imei.get(indexx),contact_time_first.get(indexx),contact_time_last.get(indexx)
                                    ,rssi_level_1.get(indexx),rssi_level_2.get(indexx),rssi_level_3.get(indexx));
                        }
                        contact_time_imei.remove(indexx);
                        contact_time_first.remove(indexx);
                        contact_time_last.remove(indexx);
                        rssi_level_1.remove(indexx);
                        rssi_level_2.remove(indexx);
                        rssi_level_3.remove(indexx);
                        Log.e(TAG,"事件結束");
                }
                for(int i = 0; i< contact_time_imei.size(); i++){
                    if(!time_difference(contact_time_last.get(i),a)) {  //是否超過10分鐘
                        if(rssi_level_1.get(i)+rssi_level_2.get(i)+rssi_level_3.get(i)>2){
                            Log.e(TAG,"事件結束，儲存起來!!" + contact_time_imei.get(i)+contact_time_first.get(i).getTime()+contact_time_last.get(i).getTime());
                            add(contact_time_imei.get(i),contact_time_first.get(i),contact_time_last.get(i)
                            ,rssi_level_1.get(i),rssi_level_2.get(i),rssi_level_3.get(i));
                        }
                        contact_time_imei.remove(i);
                        contact_time_first.remove(i);
                        contact_time_last.remove(i);
                        rssi_level_1.remove(i);
                        rssi_level_2.remove(i);
                        rssi_level_3.remove(i);
                        Log.e(TAG,"事件結束!!");
                    }
                }
            }
            if(received_imei.equals("354702090194260")||received_imei.equals("355944092498120")){
                Log.e(TAG,"contact_list: "+"\n" +contact_time_imei +"\n");
            }

            /*--------------------------------------------------------------contact event END--------------------------------------------------------------*/


            /*------------------------------------------------------------message-------------------------------------------------------------------------*/
            String msg;
//            msg="Device Name: " + result.getDevice().getName() +"\n"+ "rssi: " + result.getRssi() +"\n" + "add: " + result.getDevice().getAddress() +"\n"
//                    + "time: " + currentTime +"\n" + "imei: " + received_imei +"\n\n" ;

            msg = "Service has started." + "\n" + "There are "+contact_time_imei.size()+" devices beside you!";

            textview_status.setText("");
            textview_status.setText(msg);
            Log.e(TAG,"contact_time_imei: "+contact_time_imei);
            /*----------------------------------------------------------message END-----------------------------------------------------------------------*/



            /*-------------------------------------------------------interval-----------------------------------------------------------------------------*/

            String address = result.getDevice().getAddress();
            if(!list_device.contains(address)){
                list_device.add(address);
                list_device_detail.add(msg);
            }

        /*
        -----------|---------------------
        address    |
        -----------|---------------------
        time_pre   |
        -----------|---------------------
        time_now   |
        -----------|---------------------
        interval   |
        -----------|---------------------
        num        |
        -----------|---------------------
        mean_total |
        -----------|---------------------
        mean       |
        -----------|---------------------
        */
            long TimestampMillis = result.getTimestampNanos()/1000000; //單位:ms
//            int index = list_device.indexOf(address);
            int index = indexx;
            int initial = 0;
//            if(!matrix.get(0).contains(index)){
            if(matrix.get(0).size()<contact_time_imei.size()){
                matrix.get(0).add(index);                 //address
                matrix.get(1).add(initial);               //time_pre
                matrix.get(2).add(TimestampMillis);       //time_now
                matrix.get(3).add(initial);               //interval
                matrix.get(4).add(num_total.get(index));  //num
                matrix.get(5).add(mean_total.get(index));                  //mean_total
                matrix.get(6).add(mean_total.get(index)/num_total.get(index));     //mean
                time_previous.set(index,TimestampMillis);
                num_total.set(index,num_total.get(index)+1);
                mean_total.set(index,TimestampMillis-time_previous.get(index));
            }else {
                long interval = TimestampMillis-time_previous.get(index);
//                Log.e(TAG, "interval:"+interval);
//                Log.e(TAG, "time_previous:"+time_previous);
                mean_total.set(index,mean_total.get(index)+interval);
                matrix.get(1).set(index,time_previous.get(index));
                matrix.get(2).set(index,TimestampMillis);
                matrix.get(3).set(index,interval);
                matrix.get(4).set(index,num_total.get(index));
                matrix.get(5).set(index,mean_total.get(index));
                matrix.get(6).set(index,mean_total.get(index)/num_total.get(index));
                time_previous.set(index,TimestampMillis);
                num_total.set(index,num_total.get(index)+1);
            }

//            Log.e(TAG, "index:"+index+matrix.get(3));

            //每個不同imei or address的time interval
            if(contact_time_imei.size()>time_interval.size()){  //list_device or imei
                time_interval.add(new ArrayList<>());
//                Log.e(TAG, "time_interval size:"+time_interval.size());
            }

            if(matrix.get(3).get(index).toString().length()<10 && !matrix.get(3).get(index).equals(0)){
                time_interval.get(index).add(matrix.get(3).get(index));
//                if(received_imei.equals("354702090194260")){
//                    Log.e(TAG, index+" time_interval:"+time_interval.get(contact_time_imei.indexOf("354702090194260"))+" , size: "+time_interval.get(contact_time_imei.indexOf("354702090194260")).size());
//                }
//                if(received_imei.equals("355944092498120")){
//                    Log.e(TAG, index+" time_interval:"+time_interval.get(contact_time_imei.indexOf("355944092498120"))+" , size: "+time_interval.get(contact_time_imei.indexOf("355944092498120")).size());
//                }
                for(int i =0; i<time_interval.size();i++){
//                    Log.e(TAG, "time_interval:"+time_interval.get(i)+" , size: "+time_interval.get(i).size());
                }
            }


//            Log.e(TAG,"matrix: "+"\n"
//                    +matrix.get(0).toString()+"\n"
//                    +matrix.get(1).toString()+"\n"
//                    +matrix.get(2).toString()+"\n"
//                    +matrix.get(3).toString()+"\n"
//                    +matrix.get(4).toString()+"\n"
//                    +matrix.get(5).toString()+"\n"
//                    +matrix.get(6).toString()+"\n");

            //單一裝置time interval
            received_time_Calendar.add(a);
            received_time_interval.clear();
            for(int i = 0;i<received_time_Calendar.size();i++){
                received_time.add(f.format(received_time_Calendar.get(i).getTime()));
                if(i>0){
                    received_time_interval.add(time_difference_(received_time_Calendar.get(i-1),received_time_Calendar.get(i)));
                }
            }
//            Log.e(TAG,"received_time_interval.length: "+received_time_interval.size());
//            Log.e(TAG,"received_time_interval"+received_time_interval);

            /*-------------------------------------------------------interval END--------------------------------------------------------------------------*/

            SQLiteDatabase db = DH.getReadableDatabase();
            show(db);
        }


        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d("onScanFailed: " , String.valueOf(errorCode));
        }
    };

    public static void add(String s,Calendar first,Calendar last,int rssi_1,int rssi_2,int rssi_3) {
        SQLiteDatabase db = DH.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id",s);
        values.put("time_first",format.format(first.getTime()));
        values.put("time_last",format.format(last.getTime()));
        values.put("rssi_level_1",rssi_1);
        values.put("rssi_level_2",rssi_2);
        values.put("rssi_level_3",rssi_3);
        values.put("is_contact",0);
        db.insert(TB1,null,values);
        show(db);
    }

    public static void delete(String _id){
        SQLiteDatabase db = DH.getWritableDatabase();
        db.delete(TB1,"_id=?",new String[]{_id});
        show(db);
    }

    private static void show(SQLiteDatabase db){
        Cursor cursor = db.query(TB1,new String[]{"_id","user_id","time_first","time_last","rssi_level_1","rssi_level_2","rssi_level_3","is_contact"},
                null,null,null,null,null);

        StringBuilder resultData = new StringBuilder("RESULT: \n");
        while(cursor.moveToNext()){
            int id = cursor.getInt(0);
            String user_id = cursor.getString(1);
            String time_first = cursor.getString(2);
            String time_last = cursor.getString(3);
            int rssi_1 = cursor.getInt(4);
            int rssi_2 = cursor.getInt(5);
            int rssi_3 = cursor.getInt(6);
            int is_contact = cursor.getInt(7);

            resultData.append(id).append(": ");
            resultData.append(user_id).append("\n ");
            resultData.append(time_first).append(", ");
            resultData.append(time_last).append("\n");
            resultData.append("RSSI level: ").append(rssi_1).append(", ");
            resultData.append(rssi_2).append(", ");
            resultData.append(rssi_3).append("\n ");
            resultData.append("is check: ").append(is_contact);
            resultData.append("\n");
        }
//        sql_Text.setText(resultData);
//        sql_Text.setMovementMethod(new ScrollingMovementMethod()); //垂直滾動
        cursor.close();
    }

    public static long time_difference_(Calendar first, Calendar last){
        Date first_time = first.getTime();
        Date last_time = last.getTime();

        long different = last_time.getTime() - first_time.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;
        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;
        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;
        long elapsedSeconds = different / secondsInMilli;
//        Log.e(TAG,"different: "+elapsedDays +"days, " + elapsedHours + "hours, " + elapsedMinutes +"minutes, " + elapsedSeconds +"seconds. ");
        return different;
    }

    public static boolean time_difference(Calendar first, Calendar last){
        Date first_time = first.getTime();
        Date last_time = last.getTime();

        long different = last_time.getTime() - first_time.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

//        long elapsedDays = different / daysInMilli;
//        different = different % daysInMilli;
//        long elapsedHours = different / hoursInMilli;
//        different = different % hoursInMilli;
//        long elapsedMinutes = different / minutesInMilli;
//        different = different % minutesInMilli;
//        long elapsedSeconds = different / secondsInMilli;
//        Log.e(TAG,"different: "+elapsedDays +"days, " + elapsedHours + "hours, " + elapsedMinutes +"minutes, " + elapsedSeconds +"seconds. ");

        return different/1000<contant_time_limit;
    }

    public static void clearTable(){
        SQLiteDatabase db = DH.getWritableDatabase();
        db.execSQL("drop table " + TB1);
        show(db);
    }

}
