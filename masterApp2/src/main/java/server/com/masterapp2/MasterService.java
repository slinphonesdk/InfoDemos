package server.com.masterapp2;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bean.common.STBIPAddress;
import com.bean.common.utils.STBLog;
import com.bean.common.utils.ThreadPoolManager;
import com.udp.master2.MasterListener;
import com.udp.master2.MasterManager;

import org.joinsip.usipserver.USipServerService;

import java.util.concurrent.FutureTask;

public class MasterService extends Service {

    private MasterListener masterListener;
    private MasterManager masterManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Thread udpThread = new Thread(new Runnable() {
            @Override
            public void run() {
                startUDPService();
            }
        });
        ThreadPoolManager.getInstance().execute(new FutureTask<>(udpThread,null),null);


        Thread sipThread = new Thread(new Runnable() {
            @Override
            public void run() {
                startUSipServerService();
            }
        });
        ThreadPoolManager.getInstance().execute(new FutureTask<>(sipThread,null),null);
        Log.e("onCreate","onCreate");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        masterManager.threadClear();
        if (this.uSipServerIntent != null) {
            stopService(this.uSipServerIntent);
        }

        super.onDestroy();
        STBLog.out("onDestroy","onDestroy");
    }

    private void startUDPService() {

        masterListener = new MasterListener() {

            @Override
            public void notificationWith(String title, String content) {
                sendNotification(title,content);
            }
        };

        masterManager = new MasterManager(this, masterListener);
    }

    private Intent uSipServerIntent;
    private void startUSipServerService() {

        String pref_domain = "pref_domain";
        String pref_localIp = "pref_localip";
        String pref_register_data_persistence = "pref_register_data_persistence";
        String pref_localPort = "pref_localport";

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String domain = sharedPreferences.getString(pref_domain, "");
        String localIp = sharedPreferences.getString(pref_localIp, STBIPAddress.getLocalHostIp());
        boolean regDataPersistence = sharedPreferences.getBoolean(pref_register_data_persistence, false);
        int localPort = Integer.parseInt(sharedPreferences.getString(pref_localPort, "5090"));

        uSipServerIntent = new Intent(getBaseContext(), USipServerService.class);
        uSipServerIntent.putExtra(pref_domain, domain);
        uSipServerIntent.putExtra(pref_localIp, localIp);
        uSipServerIntent.putExtra(pref_localPort, localPort);
        uSipServerIntent.putExtra(pref_register_data_persistence, regDataPersistence);
        startService(uSipServerIntent);

        STBLog.out("ppt", "sip server start: "+localIp+":"+localPort);
    }

    private void sendNotification(String title, String content) {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(org.joinsip.usipserver.R.drawable.ic_launcher);
        builder.setTicker("Master Service");
        builder.setContentTitle(title);
        builder.setContentText(content);
        builder.setWhen(System.currentTimeMillis()); //发送时间
        builder.setDefaults(Notification.DEFAULT_ALL);
        Notification notification = builder.build();
        startForeground(1, notification);
    }

}