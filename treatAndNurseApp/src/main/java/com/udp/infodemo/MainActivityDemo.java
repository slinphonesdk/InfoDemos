package com.udp.infodemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.bean.common.DeviceType;
import com.bean.common.MsgCommand;
import com.bean.common.ParamsModel;
import com.bean.common.rudp.RUdpCallBack;
import com.bean.common.rudp.RUdpListener;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

public class MainActivityDemo extends AppCompatActivity
{

    private TextView textView;

    private String sessionID = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_demo);
        setupUI();
        exPhoneSetup();
    }

    private void setupUI() {
        textView = findViewById(R.id.textView);
    }

    private ArrayList<String> list = new ArrayList<>();
    private ExPhoneManager exPhoneManager;
    private void exPhoneSetup() {

        exPhoneManager = new ExPhoneManager(this);
        exPhoneManager.autoAnswer = true;
        exPhoneManager.addUdpListener(new RUdpListener() {
            @Override
            public void dataResponse(String s, String s1, InetAddress inetAddress) {
                try {
                    ParamsModel paramsModel = ParamsModel.getParamsHeader(s1);

                    if (paramsModel != null) {

                        if (paramsModel.paramsHeader.msgCommand == MsgCommand.receiveSessionIDList) {
                            exPhoneManager.response(s,inetAddress);
                            sessionID = paramsModel.body.getLists(0);
                            Log.e("s",paramsModel.body.getListsList().toString());
                        }

                    }

                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            }
        });

        exPhoneManager.addListener(new ExPhoneListener() {
            @Override
            public void registrationState(String s, String s1) {
                Log.e("reg",s+s1);
            }

            @Override
            public void callState(String s, int i, String s1) {
                Log.e("call",s+s1+i);
            }
        });

        exPhoneManager.registerToInfoMaster("1500000", DeviceType.treatAndNurse, "", new RUdpCallBack() {
            @Override
            public void onFailure(String callMsg, IOException e) {
                Log.e("F","注册超时");
            }

            @Override
            public void onResponse(String CallMsg, String response) throws IOException {
                Log.e("F","注册成功");
            }
        });
    }



    public void startCall(View v) {
       exPhoneManager.sipCall("10099");
    }

    public void receive(View v) {

        if (sessionID != null) {
            exPhoneManager.acceptCall(sessionID, new RUdpCallBack() {
                @Override
                public void onFailure(String callMsg, IOException e) {

                }

                @Override
                public void onResponse(String CallMsg, String response) throws IOException {

                }
            });
        }
    }

    public void hangup(View v) {
        if (sessionID != null) {
            exPhoneManager.hangup(sessionID, new RUdpCallBack() {
                @Override
                public void onFailure(String callMsg, IOException e) {

                }

                @Override
                public void onResponse(String CallMsg, String response) throws IOException {

                }
            });
            exPhoneManager.sipHangup();
            removeFromList();
        }

        textView.post(new Runnable() {
            @Override
            public void run() {
                if (list != null) {
                    textView.setText(list.toString());
                }
            }
        });
    }

    public void refuse(View v) {
        if (sessionID != null) {
            exPhoneManager.refusingToAnswer(sessionID, new RUdpCallBack() {
                @Override
                public void onFailure(String callMsg, IOException e) {

                }

                @Override
                public void onResponse(String CallMsg, String response) throws IOException {

                }
            });
            removeFromList();
        }

        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.setText(list.toString());
            }
        });
    }

    private void removeFromList() {
        if (sessionID != null && list != null) {

            if (list.size() > 0) {
                if (list.contains(sessionID)) {
                    list.remove(sessionID);
                }
                if (list.size() > 0) {
                    sessionID = list.get(0);
                }
                else {
                    sessionID = "";
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        exPhoneManager.destroy();
        exPhoneManager = null;
        super.onDestroy();
    }

    /*
    * {"head":"{\"deviceType\":0,\"msgCommand\":\"10005\",\"sip\":\"110801\",\"uuid\":\"ccbio\",\"date\":\"ccbio\"}","body":"{\"sessionID\":\"1111\"}"}
    * */
}
