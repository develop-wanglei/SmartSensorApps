package com.atplatform.yuyenchia.shcontrol;


import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.GetCallback;

public class MainActivity extends AppCompatActivity {
    public AVObject Home_Data;
    private double Distance;//超声波传感器读取距离
    private double Temperature;//温湿度传感器读取温度
    private double Humidity;//温湿度传感器读取湿度
    private boolean humanState=false;//红外传感器转态
    private boolean smokeState=false;//烟雾传感器状态
    private boolean alarm_state=false;//蜂鸣器当前状态
    private boolean alarm_switch=false;
    private long TimeTicket=0;
    private Handler LoopHandler = new Handler();
    private Button alarm;
    TextView View_temp,View_humidity,view_human,View_smoke,distanceview,View_alarm;
    private NotificationManager mManager;
    private boolean toaststate=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View_temp=findViewById(R.id.textView2);
        View_humidity=findViewById(R.id.textView3);
        View_alarm=findViewById(R.id.textView6);
        View_smoke=findViewById(R.id.textView5);
        view_human=findViewById(R.id.textView4);
        distanceview=findViewById(R.id.textView);
        alarm=findViewById(R.id.button);
        Home_Data=AVObject.createWithoutData("SmatHome","5de89812dd3c13007fcfcee5");
        LoopHandler.post(looper);

        //通知消息在一个activity只打开一次
        mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (!isTaskRoot()) {
            Intent intent = getIntent();
            String action = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(action)) {
                finish();
            }
        }
    }

    protected void onStart() {
        super.onStart();
        alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarm_switch=!alarm_switch;
                Home_Data.put("ALARM_SWITCH",alarm_switch);
                Home_Data.saveInBackground();
                if(alarm_switch==true)
                    alarm.setText("关闭报警");
                else
                    alarm.setText("开启报警");
            }
        });

        mManager.cancel(1);
    }



    private Runnable looper=new Runnable() {
        @Override
        public void run() {
            TimeTicket++;
            if(TimeTicket>9999)
            {
                TimeTicket=0;
            }

            if(TimeTicket%100==0)
            {
                Home_Data.fetchInBackground(new GetCallback<AVObject>() {
                    @Override
                    public void done(AVObject avObject, AVException e) {
                            if (avObject!=null) {
                                Distance = avObject.getDouble("DISTANCE");
                                Temperature = avObject.getDouble("TEMPERATURE");
                                Humidity = avObject.getDouble("HUMIDITY");
                                smokeState = avObject.getBoolean("SMOKE_STATE");
                                alarm_state = avObject.getBoolean("ALARM_STATE");
                                humanState = avObject.getBoolean("HUMAN_STATE");
                                Log.d("data", "renew");
                                toaststate=false;
                            }
                            else
                            {
                                if (!toaststate)
                                {
                                    Toast ts=Toast.makeText(MainActivity.this,"连接网络失败，请连接网络后重试",Toast.LENGTH_LONG);
                                    ts.show();
                                    toaststate=true;
                                }
                            }
                        }


                });
            }
            if (smokeState)
                View_smoke.setText("有烟雾");
            else
                View_smoke.setText("没有烟雾");

            if (humanState)
                view_human.setText("有人");
            else
                view_human.setText("没有人");

            if(alarm_state) {
                View_alarm.setText("报警开启");
                sendNotification();
            }
            else
                View_alarm.setText("报警关闭");

            View_humidity.setText(String.valueOf(Humidity));
            View_temp.setText(String.valueOf(Temperature));
            distanceview.setText(String.valueOf(Distance));
            LoopHandler.postDelayed(looper, 1);
        }
    };

    private void sendNotification() {
        String channelId = "channelId";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = "notification";
            createNotificationChannel(channelId, channelName);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setSmallIcon(R.mipmap.ic_launcher);

        builder.setContentTitle("SmartHome");
        builder.setContentText("家居系统报警！！");
        builder.setNumber(1);
//        builder.setTimeoutAfter(10000)
//        //大图标展开
//        android.support.v4.app.NotificationCompat.BigPictureStyle style = new android.support.v4.app.NotificationCompat.BigPictureStyle();
//        style.setBigContentTitle("摄像头已拍摄");
//        style.setSummaryText("SummaryText");
//
//        builder.setStyle(style);
        builder.setAutoCancel(true);
        //通知点击返回应用
        Intent msgIntent = getPackageManager().getLaunchIntentForPackage("com.atplatform.yuyenchia.shcontrol");//获取启动Activity
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                1,
                msgIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        Notification notification = builder.build();
        mManager.notify(1, notification);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(String channelId, String channelName) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setShowBadge(true);
        channel.enableLights(true);//是否在桌面icon右上角展示小红点
        channel.setLightColor(Color.RED);//小红点颜色
        mManager.createNotificationChannel(channel);
    }



}
