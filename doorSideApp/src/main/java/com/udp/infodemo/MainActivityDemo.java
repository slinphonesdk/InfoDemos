package com.udp.infodemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.bean.common.AutoWifiManager;
import com.bean.common.DeviceType;
import com.bean.common.MsgCommand;
import com.bean.common.ParamsHeader;


import java.util.ArrayList;

import src.CMC;

public class MainActivityDemo extends AppCompatActivity {

    private TextView textView;

    private String sessionID = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_demo);
        setupUI();
//        connectWIFI();
        exPhoneSetup();
    }

    private void connectWIFI() {

        new AutoWifiManager(this, 14, 7, new AutoWifiManager.Listener() {
            @Override
            public void connectState(Boolean state, String err) {
                Log.e("WIFI", String.format("State: %s, Error Message: %s", state.toString(), err));
            }

            @Override
            public void connectEnd(String err) {
                Log.e("WIFI", String.format("Error Message: %s", err));

            }
        });
    }
    private void setupUI() {
        textView = findViewById(R.id.textView);
    }

//    private ExPhoneManager exPhoneManager;
    private void exPhoneSetup() {
//        exPhoneManager = new ExPhoneManager(this,"1109000", DeviceType.doorSide, "cbo");
//        exPhoneManager.addListener(new ExPhoneListener() {
//            @Override
//            public void msgCallBack(ParamsHeader paramsHeader, MsgCommand msgCommand, final CMC.CMCRequestParam.Body body) { }
//
//            @Override
//            public void sessionStatus(final String sessionID) {
//                Log.e("ppt","sessionID: "+sessionID);
//                MainActivityDemo.this.sessionID = sessionID;
//                textView.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        textView.setText("sessionID: "+sessionID);
//                    }
//                });
//            }
//
//            @Override
//            public void sipCallIncomingReceived() { }
//
//            @Override
//            public void sipCallEnd(){
//            }
//
//            @Override
//            public void keepLineState(PhoneLineState state, String sip) {
//
//            }
//        });
    }

    public void register(View v) {
//        exPhoneManager.registerOfTreatAndNurse();
        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.setText("移动-医护分机-已注册");
            }
        });
    }

    public void unregister(View v) {
//        exPhoneManager.unregister();
        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.setText("移动-医护分机-已取消");
            }
        });
    }


//    public void startCall(View v) {
//        exPhoneManager.call();
//    }

    public void receive(View v) {
        if (sessionID != null) {
//            exPhoneManager.acceptCall(sessionID);
        }
    }

    public void hangup(View v) {
        if (sessionID != null) {
//            exPhoneManager.hangupOrCancelCall(sessionID);
        }
    }

    public void refuse(View v) {
        if (sessionID != null) {
//            exPhoneManager.refusingToAnswer(sessionID);
        }

    }

    /*
    * {"head":"{\"deviceType\":0,\"msgCommand\":\"10005\",\"sip\":\"110801\",\"uuid\":\"ccbio\",\"date\":\"ccbio\"}","body":"{\"sessionID\":\"1111\"}"}
    * */
}
