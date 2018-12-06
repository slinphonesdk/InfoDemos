package com.udp.infodemo;

import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.udp.master.MasterListener;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static java.lang.Thread.sleep;

public class BootActivity extends AppCompatActivity {

    private boolean alwaysTrue = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boot);
        setNavigationBarStatusBarTranslucent();
        new Thread(new Runnable() {
            @Override
            public void run() {

                while (alwaysTrue) {
                    if (getLocalHostIp().contains("192.168.")) {
                        Intent i = new Intent(BootActivity.this, MainActivityDemo.class);
                        BootActivity.this.startActivity(i);
                        alwaysTrue = false;
                        finish();
                        return;
                    }

                    try { sleep(300); }
                    catch ( InterruptedException e) {e.printStackTrace();}
                }
            }
        }).start();
    }

    // TODO: 隐藏导航栏
    public final void setNavigationBarStatusBarTranslucent(){

        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(option);
        }

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        getWindow().setAttributes(params);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();
    }

    private static String getLocalHostIp() {
        String hostIp = "";
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia;
            while (nis.hasMoreElements()) {

                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                if (ni != null) {
                    Enumeration<InetAddress> ias = ni.getInetAddresses();
                    while (ias.hasMoreElements()) {
                        ia = ias.nextElement();
                        if (ia instanceof Inet6Address) {
                            continue;// skip ipv6
                        }
                        String ip = ia.getHostAddress();
                        if (!"127.0.0.1".equals(ip)) {
                            hostIp = ia.getHostAddress();
                            break;
                        }
                    }

                }
            }
        } catch (SocketException e) {
            Log.i("yao", "SocketException");
            e.printStackTrace();
        }
        return hostIp;
    }
}
