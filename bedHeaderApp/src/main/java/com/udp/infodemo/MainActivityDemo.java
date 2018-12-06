package com.udp.infodemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.bean.common.AutoWifiManager;
import com.bean.common.MsgCommand;
import com.bean.common.SSIDUtils;
import com.bean.common.STBRequestParam;
import com.bean.common.STBUntils;
import com.bean.common.WifiUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import com.udp.extension.ExPhoneListener;
import com.udp.extension.ExPhoneManager;
import com.bean.common.DeviceType;
import com.bean.common.ParamsHeader;
import com.udp.extension.PhoneLineState;

import java.io.IOException;
import java.util.List;

import src.CMC;

public class MainActivityDemo extends AppCompatActivity {


    private TextView textView;

    private String sessionID = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_demo);
        setupUI();
        exPhoneSetup();
    }

    @Override
    protected void onDestroy() {
        exPhoneManager.destroy();
        exPhoneManager = null;
        super.onDestroy();
    }

    private void setupUI() {
        textView = findViewById(R.id.textView);
    }

    public void reRegister(View v) {
        exPhoneManager.reConfig("6608001", DeviceType.bedHeader, "bed14", new ExPhoneManager.RegisterListener() {
            @Override
            public void state(Boolean paramBoolean) {
                Log.e("ppt", paramBoolean.toString());
            }
        });
    }

    public void testCall(View view) {

    }

    private String testSessionID = "";
    // TODO: 响应数据处理
    private void responseModel(String result) throws InvalidProtocolBufferException {

        String firstChar = result.substring(0, 1);
        String lastChar = result.substring(result.length() - 1, result.length());
        String insideFirstChar = "[";
        String insideLastChar = "]";

        if (TextUtils.equals(firstChar, insideFirstChar) && TextUtils.equals(lastChar, insideLastChar)) {
            byte[] bytes = STBUntils.stringToByte(result);

            CMC.CMCRequestParam cmcRequestParam = CMC.CMCRequestParam.parseFrom(bytes);
            CMC.CMCRequestParam.Header header = cmcRequestParam.getHeader();
            CMC.CMCRequestParam.Body body = cmcRequestParam.getBody();
            ParamsHeader paramsHeader = new ParamsHeader(DeviceType.fromInt(header.getType()), header.getUuid(), header.getSip(), MsgCommand.fromInt(header.getCommand()));

            if (paramsHeader.msgCommand == MsgCommand.callRecSessionID) { // from 信息主机
                testSessionID = body.getSessionID();
                Log.e("RUDP", "会话ID: "+testSessionID);
            }
            else if (paramsHeader.msgCommand == MsgCommand.cancelCallSuccess) {
                Log.e("RUDP", "取消会话成功");
            }
        }
    }

    public void testCancelCall(View view) {
    }

    public void register(View v) {
        exPhoneManager.registerOfTreatAndNurse();
        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.setText("移动-医护分机-已注册");
            }
        });
    }

    public void unregister(View v) {
        exPhoneManager.unregister();
        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.setText("移动-医护分机-已取消");
            }
        });
    }

    public void bathroomCall(View v) {
        exPhoneManager.bathroomCall();
        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.setText("卫浴分机-呼叫");
            }
        });
    }

    public void bathroomCancel(View v) {
        if (sessionID != null)
             exPhoneManager.bathroomCancel(sessionID);
        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.setText("卫浴分机-取消");
            }
        });
    }

    ExPhoneManager exPhoneManager;
    private void exPhoneSetup() {
        exPhoneManager = new ExPhoneManager(this, "0907021", DeviceType.bedHeader, "bed1", new ExPhoneManager.RegisterListener() {
            @Override
            public void state(Boolean paramBoolean) {
                Log.e("ppt", paramBoolean.toString());
            }
        });
        exPhoneManager.addListener(new ExPhoneListener() {
            @Override
            public void msgCallBack(ParamsHeader paramsHeader, MsgCommand msgCommand, final CMC.CMCRequestParam.Body body) {
                Log.e("ppt", "exPhone ----");
                if (paramsHeader.msgCommand == MsgCommand.refusingToanswer) {
                    sessionID = "";
                    Log.e("ppt","此会话被拒绝：sessionID:"+body.getSessionID());
                    textView.post(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText("此会话被拒绝：sessionID:"+body.getSessionID());
                        }
                    });
                }
                if (paramsHeader.msgCommand == MsgCommand.callRecSessionID) {//from info. master
                    sessionID = body.getSessionID();
                    Log.e("ppt","呼叫得到的：sessionID:"+body.getSessionID());
                    textView.post(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText("呼叫得到的：sessionID:"+body.getSessionID());
                        }
                    });
                }
            }

            @Override
            public void sessionStatus(final String sessionID) {
                Log.e("ppt","sessionID: "+sessionID);
                MainActivityDemo.this.sessionID = sessionID;
                textView.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText("sessionID: "+sessionID);
                    }
                });
            }

            @Override
            public void registrationState(String s, String s1) {

            }

            @Override
            public void sipCallIncomingReceived() { }

            @Override
            public void sipCallEnd() { }

            @Override
            public void sipCallState(String s, int i, String s1) {

            }

            // 医护分机
            @Override
            public void keepLineState(PhoneLineState state, String sip) { }
        });
    }

    public void startCall(View v) {
        exPhoneManager.bedHeaderCallWith("1");
    }

    public void receive(View v) {
        if (sessionID != null) {
            exPhoneManager.acceptCall(sessionID);
        }
    }

    public void hangup(View v) {
        if (sessionID != null) {
            exPhoneManager.hangupOrCancelCall(sessionID);
            textView.post(new Runnable() {
                @Override
                public void run() {
                    textView.setText("");
                }
            });
        }
    }

    public void refuse(View v) {
        if (sessionID != null) {
            exPhoneManager.refusingToAnswer(sessionID);
        }
    }

    /*
    * {"head":"{\"deviceType\":0,\"msgCommand\":\"10005\",\"sip\":\"110801\",\"uuid\":\"ccbio\",\"date\":\"ccbio\"}","body":"{\"sessionID\":\"1111\"}"}
    * */
}
