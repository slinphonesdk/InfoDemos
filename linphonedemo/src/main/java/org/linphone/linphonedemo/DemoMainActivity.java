package org.linphone.linphonedemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.linphone.RLinkPhoneListener;
import org.linphone.SLinPhoneSDK;

public class DemoMainActivity extends AppCompatActivity {

    EditText callEt;
    EditText editText;
    SLinPhoneSDK sLinPhoneSDK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_main);
        editText = findViewById(R.id.editText);
        callEt = findViewById(R.id.call_editText);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.reg:
                String[] args = editText.getText().toString().split("@");
                if (args.length != 2) return;
                String username = args[0];
                String doMain = args[1];
                sLinPhoneSDK = new SLinPhoneSDK(this, doMain, 5060, new RLinkPhoneListener() {
                    @Override
                    public void callState(String from, int state, String s) {
                        Log.e("Call State",from+" state:"+state+" s:"+s);
                    }

                    @Override
                    public void registrationState(String state, String s) {
                        Log.e("Reg State","state:"+state+" s:"+s);
                    }
                });
                sLinPhoneSDK.register(username);
                break;
            case R.id.call:
                if (sLinPhoneSDK == null) return;
                String callNumber = callEt.getText().toString();
                sLinPhoneSDK.callOutgoing(callNumber);
                break;
            case R.id.hangup:
                if (sLinPhoneSDK == null) return;
                SLinPhoneSDK.hangup();
                break;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        sLinPhoneSDK.destroy();
        sLinPhoneSDK = null;
    }
}
