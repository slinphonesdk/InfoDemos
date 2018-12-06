package rudp.com.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.bean.common.rudp.RUdpCallBack;
import com.bean.common.rudp.RUdpListener;
import com.bean.common.rudp.RUdpManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class RUdpMainActivity extends AppCompatActivity {

    private EditText editText;
    private TextView textView;
    private RUdpManager rUdpManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rudp_main);

        editText = findViewById(R.id.editText);
        textView = findViewById(R.id.textView);

        rUdpManager = new RUdpManager(9999);
        rUdpManager.addListener(new RUdpListener() {
            @Override
            public void dataResponse(String msgFlag, String result, InetAddress fromAddress) {

            }
        });
    }


    public void send(View view) {
        String msg = editText.getText().toString().trim();
        if (msg.length() > 0) {
            responseString += msg;
            updateUI();
            rUdpManager.sendMsg(msg, remoteAddress("192.168.88.253"), new RUdpCallBack() {
                @Override
                public void onFailure(String callMsg, IOException e) {
                    Log.e("error",callMsg);
                    responseString = callMsg;
                    updateUI();
                }

                @Override
                public void onResponse(String CallMsg, String response) throws IOException {
                    Log.e(CallMsg,response);
                    responseString += "\n"+response;
                    updateUI();
                }
            });
            editText.setText("");
        }
    }

    private String responseString;
    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(responseString);
            }
        });
    }

    private InetAddress remoteAddress(String host) {
        InetAddress serverAddress = null;
        try {
            serverAddress = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return serverAddress;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rUdpManager.destroy();
        rUdpManager = null;
    }

    public void clear(View view) {
        responseString = "";
        updateUI();
    }
}
