package com.example.contact_tracing;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.example.contact_tracing.Activity.DH;
import static com.example.contact_tracing.Activity.mDeviceIMEI;
import static com.example.contact_tracing.Activity.toolbar;
import static com.example.contact_tracing.DBHelper.TB1;
import static com.example.contact_tracing.JOBservice_http.db;
import static com.example.contact_tracing.Activity.TAG;


public class Activity_history extends AppCompatActivity {

    static ListView SQL_list;

    static final ArrayList<String> check_list_id = new ArrayList<>();
    static final ArrayList<String> check_list_time = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);


        toolbar = findViewById(R.id.toolbar);
//        toolbar.setSubtitle("CONTACT  TRACING");
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);




//        actionBar.setDisplayShowHomeEnabled(true);

        Log.e(TAG,"Activity_history");
//        check_list_id.clear();
//        check_list_time.clear();

        SQL_list = findViewById(R.id.SQL_list);

        initiate(db);

        listview();

    }


    private void listview(){
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,check_list_time);
        SQL_list.setAdapter(arrayAdapter);
        SQL_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
//                Toast.makeText(Activity_history.this , display(db , check_list_id.get(position)),Toast.LENGTH_SHORT).show();
                AlertDialog.Builder dialog = new AlertDialog.Builder(Activity_history.this);
                dialog.setTitle("接觸史");
                dialog.setMessage(display(db ,check_list_id.get(position)));

                if(Integer.parseInt(get_detail(db,check_list_id.get(position),4))==1){
                    dialog.setPositiveButton("update", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            http_header(check_list_id.get(position));
                            update(check_list_id.get(position));
                        }
                    });
                }

                dialog.setNegativeButton("close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                dialog.show();
            }
        });

    }

    private static void initiate(SQLiteDatabase db) {
        check_list_id.clear();
        check_list_time.clear();

        Cursor cursor = db.query(TB1,new String[]{"_id","user_id","time_first","time_last","rssi_level_1","rssi_level_2","rssi_level_3","is_contact"},
                null,null,null,null,null);
        while(cursor.moveToNext()){
            String id = cursor.getString(0);
            String time_first = cursor.getString(2);
            int is_contact = cursor.getInt(7);

            if(is_contact!=0){
                check_list_id.add(id);
                check_list_time.add(time_first);
//                check_list_.add(false);
            }
        }
        cursor.close();
    }

    private static String display(SQLiteDatabase db,String ID) {
        Cursor cursor = db.query(TB1,new String[]{"_id","user_id","time_first","time_last","rssi_level_1","rssi_level_2","rssi_level_3","is_contact"},
                null,null,null,null,null);
        String data = "";
        String check="";
        while(cursor.moveToNext()){
            String id = cursor.getString(0);
            String user_id = cursor.getString(1);
            String time_first = cursor.getString(2);
            String time_last = cursor.getString(3);
            int rssi_1 = cursor.getInt(4);
            int rssi_2 = cursor.getInt(5);
            int rssi_3 = cursor.getInt(6);
            int is_contact = cursor.getInt(7);

            if (is_contact==1){
                check = "had contacted";
            }else if(is_contact==2) {
                check = "had returned to server";
            }

            if(ID.equals(id)){
                data =  "USER ID: "+ user_id + "\n" +
                        "TIME: "+time_first +" ~ " + time_last + "\n" +
                        "RSSI: " + rssi_1 + "," + rssi_2 + "," + rssi_3 +  "\n" +
                        "STATUS: "+ check;
            }
        }
        cursor.close();
        return data;
    }

    private void update(String id) { // 更新指定的資料
        SQLiteDatabase db = DH.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_contact ", 2);
        db.update(TB1,values,"_id=? " , new String[]{id});
        initiate(db);
    }

    private void http_header(final String id){
//        Log.e(TAG,"http_header:" + id);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://140.114.26.89/")
                .header("userid",mDeviceIMEI)
                .header("contactid", get_detail(db,id,0))
                .header("starttime",get_detail(db,id,1))
                .header("endtime",get_detail(db,id,2))
                .header("rssi",get_detail(db,id,3))
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG,"http_header" + " 失敗" + e.getLocalizedMessage());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
//                Log.e(TAG,"http_header result" + json);
                Log.e(TAG,"http_header 成功 "+id);
            }
        });
    }

    private static String get_detail(SQLiteDatabase db,String ID,int x) {
        //x=0 他人的id; x=1 starttime; x=2 endtime; x=3 rssi;  x=4 is_contact
        Cursor cursor = db.query(TB1,new String[]{"_id","user_id","time_first","time_last","rssi_level_1","rssi_level_2","rssi_level_3","is_contact"},
                null,null,null,null,null);
        String data = "";
        while(cursor.moveToNext()){
            String id = cursor.getString(0);
            String user_id = cursor.getString(1);
            String time_first = cursor.getString(2);
            String time_last = cursor.getString(3);
            String rssi_1 = cursor.getString(4);
            String rssi_2 = cursor.getString(5);
            String rssi_3 = cursor.getString(6);
            String is_contact = cursor.getString(7);

            if(ID.equals(id)){
                switch (x){
                    case 0:
                        data=user_id;
                        break;
                    case 1:
                        data = time_first;
                        break;
                    case 2:
                        data = time_last;
                        break;
                    case 3:
                        data = rssi_1 + " , " +rssi_2 + " , " + rssi_3 ;
                        break;
                    case 4:
                        data = is_contact;
                }

            }
        }
        cursor.close();
        return data;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
