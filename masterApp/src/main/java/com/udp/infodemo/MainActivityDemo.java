package com.udp.infodemo;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.HttpAuthHandler;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.udp.master.MasterDBDao;
import com.udp.master.MasterHttpHelper;
import com.udp.master.MasterListener;
import com.udp.master.MasterManager;

import net.steamcrafted.loadtoast.LoadToast;

import org.joinsip.usipserver.USipServerService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import static java.lang.Thread.sleep;


public class MainActivityDemo extends AppCompatActivity
{

    private String TAG = "MainActivity";

    private TextView displayTitle;
    private ImageView errImage;
    private ViewPager viewPager;
    private int viewPageIndex = 0;
    private LoadToast lt = null;
    private LinearLayout bottomLayout;
    private RelativeLayout errLayout;

    private MasterListener masterListener;
    private int dpID = 0;
    private Handler handler = new Handler();
    // TODO: 页面滚动
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {

            MainActivityDemo.this.viewPager.setCurrentItem(viewPageIndex);
            MainActivityDemo.this.setBottomIndex(viewPageIndex);
            viewPageIndex++;

            if (viewPageIndex >= fragments.size()) {
                viewPageIndex = 0;
            }
            handler.postDelayed(runnable, 3000);
        }
    };

    public ArrayList<ArrayList<MPatient>> pubPatients = new ArrayList<>();

    private MasterManager masterManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_demo);
        setNavigationBarStatusBarTranslucent();
        setupUI();

        masterListener = new MasterListener() {
            @Override
            public void msgFromServerIsNeedReload() { requestData(); }

            @Override
            public void msgFromServerListData(String jsonBody) { dealResponseData(jsonBody); }

            @Override
            public void msgFromServerErr(String err) { showErrMsg(""); }
        };
        masterManager = new MasterManager(this, masterListener);
        startUSipServerService();
        requestData();
        networkCheck();
    }

    // TODO: 网络监测
    private void networkCheck() {

        if (checkNetworkHandler == null) {
            HandlerThread handlerThread = new HandlerThread("CheckNetwork");
            handlerThread.start();
            checkNetworkHandler = new Handler(handlerThread.getLooper());
            checkNetworkHandler.post(checkNetworkRunnable);
        }
    }

    private Handler checkNetworkHandler = null;
    private Boolean mRunning = true;
    // TODO: 网络检测
    private Runnable checkNetworkRunnable = new Runnable() {
        @Override
        public void run() {
            while (mRunning) {

                if (!getLocalHostIp().contains("192.168.")) {
                    Intent i = new Intent(MainActivityDemo.this, BootActivity.class);
                    MainActivityDemo.this.startActivity(i);
                    mRunning = false;
                    finish();
                    return;
                }

                try { sleep(300); }
                catch (InterruptedException e){e.printStackTrace();}
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        masterManager.threadClear();
        removeHandler();
        if (checkNetworkHandler != null) {
            checkNetworkHandler.removeCallbacks(checkNetworkRunnable);
        }
        masterManager = null;
        MasterHttpHelper.msListener = null;

        if (this.uSipServerIntent != null) {
            stopService(this.uSipServerIntent);
        }
    }

    // TODO: 展示错误信息
    private void showErrMsg(String err) {
        Log.e("E", err);
        xHandler.removeCallbacks(delayRunnable);//移除超时重试

        if (lt != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    lt.error();
                    lt.hide();
                    displayTitle.setText("");
                    bottomLayout.removeAllViews();
                    viewPager.setVisibility(View.GONE);
                    errLayout.setVisibility(View.VISIBLE);
                    errImage.setBackgroundResource(R.drawable.fetch_data);
                }
            });
        }
    }

    // TODO: 重新加载
    public void retryQuest(View v) {
        requestData();
    }

    // TODO: 处理请求数据
    private int totalNumber = 0;
    private String ward_name = "";
    private synchronized void dealResponseData(String jsonBody) {

        xHandler.removeCallbacks(delayRunnable);//移除超时重试

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (lt != null)
                    lt.success();
                bottomLayout.removeAllViews();
                displayTitle.setText("");
                if (pubPatients.size() > 1)
                    addGroupImage(pubPatients.size());
            }
        });

        try {
            MdJson jsonObject = new MdJson(jsonBody);
            final String resultCode = jsonObject.getResultCode();
            final String successCode = "0";
            if (TextUtils.equals(resultCode, successCode)) {

                pubPatients.clear();
                totalNumber = jsonObject.getTotalNumber();

                JSONArray lists = jsonObject.getJSONBody();
                if (lists.length() > 0) {
                    ArrayList<MPatient> pts = new ArrayList<>();
                    for (int i = 0; i < lists.length(); i++) {

                        JSONObject patJson = lists.getJSONObject(i);

                        if (i % 32 == 0 && i != 0) {

                            pubPatients.add((ArrayList<MPatient>)pts.clone());
                            pts.clear();
                        }

                        MPatient mPatient = new MPatient(patJson);
                        pts.add(mPatient);

                        if (i == 0)
                            ward_name = mPatient.getWardname();
                    }
                    if (pts.size() > 0)
                        pubPatients.add(pts);

                    updateUI();
                }
                else {
                    MainActivityDemo.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            errLayout.setVisibility(View.VISIBLE);
                            viewPager.setVisibility(View.GONE);
                            errImage.setBackgroundResource(R.drawable.none_data);
                        }
                    });
                }

            } else {
                showErrMsg("result error");
                if (requestJsonString.length() > 0) {
                    MasterHttpHelper.saveFailedInfoToDB(requestJsonString);
                    requestJsonString = "";
                }
            }
        }
        catch (JSONException e) {
            showErrMsg("result error");
            if (requestJsonString.length() > 0) {
                MasterHttpHelper.saveFailedInfoToDB(requestJsonString);
                requestJsonString = "";
            }
            e.printStackTrace();
        }
    }

    // TODO: 数据请求
    private String requestJsonString;
    private void requestData() {

        removeHandler();

        if (dpID == 0) {
            settingWardID();
            return;
        }
        JSONObject bodyJson = new JSONObject();

        try {
           bodyJson.put("wardid",dpID);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (lt == null) {
            lt = new LoadToast(this)
                    .setTranslationY(300)
                    .setText("正在加载...")
                    .setBackgroundColor(Color.GRAY)
                    .setProgressColor(Color.WHITE)
                    .setTextColor(Color.WHITE);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lt.show();
            }
        });

        requestJsonString = MasterHttpHelper.PostInt(bodyJson, 5011, masterListener);

        // 30秒超时
        if (xHandler == null)
            xHandler = new Handler();

        xHandler.postDelayed(delayRunnable,30000);

    }

    private Handler xHandler;
    private Runnable delayRunnable = new Runnable() {
        @Override
        public synchronized void run() {
            MainActivityDemo.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    lt.hide();
                    viewPager.setVisibility(View.GONE);
                    errLayout.setVisibility(View.VISIBLE);
                    MasterHttpHelper.saveFailedInfoToDB(requestJsonString);
                    xHandler.removeCallbacks(delayRunnable);
                }
            });
        }
    };

    // TODO: 初始化SIP服务器
    private static String default_wardid = "default_wardid";
    private Intent uSipServerIntent;
    private void startUSipServerService() {

         String pref_domain = "pref_domain";
         String pref_localIp = "pref_localip";
         String pref_register_data_persistence = "pref_register_data_persistence";
         String pref_localPort = "pref_localport";

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String domain = sharedPreferences.getString(pref_domain, "");
        String localIp = sharedPreferences.getString(pref_localIp, getLocalHostIp());
        boolean regDataPersistence = sharedPreferences.getBoolean(pref_register_data_persistence, false);
        int localPort = Integer.parseInt(sharedPreferences.getString(pref_localPort, "5060"));

        uSipServerIntent = new Intent(getBaseContext(), USipServerService.class);
        uSipServerIntent.putExtra(pref_domain, domain);
        uSipServerIntent.putExtra(pref_localIp, localIp);
        uSipServerIntent.putExtra(pref_localPort, localPort);
        uSipServerIntent.putExtra(pref_register_data_persistence, regDataPersistence);
        startService(uSipServerIntent);

        Log.e("ppt", "sip server start: "+localIp+":"+localPort);
    }

    // TODO: 绑定UI控件
    private void setupUI() {

        dpID = PreferenceManager.getDefaultSharedPreferences(MainActivityDemo.this).getInt(default_wardid, 0);

        errLayout = findViewById(R.id.err_layout);
        displayTitle = findViewById(R.id.title_tv);
        viewPager = findViewById(R.id.view_pager);
        bottomLayout = findViewById(R.id.bottom_view);
        errImage = findViewById(R.id.err_image);

    }

    // TODO: 添加底部圆圈
    private void addGroupImage(int size){
        bottomLayout.removeAllViews();  //clear linearLayout
        for (int i = 0; i < size; i++) {
            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));  //设置图片宽高
            imageView.setImageResource(R.drawable.moren_bg); //图片资源
            imageView.setPadding(4, 4, 4, 4);
            bottomLayout.addView(imageView); //动态添加图片
        }
    }

    // TODO: 设置底部圆圈
    private void setBottomIndex(int index) {

        int childMax = bottomLayout.getChildCount();
        if (childMax == 0) return;
        for (int i = 0; i < childMax; i++) {
            ImageView imageView = (ImageView)bottomLayout.getChildAt(i);
            imageView.setImageResource(index == i ? R.drawable.xuanzhong_bg : R.drawable.moren_bg);
        }
    }

    // TODO: 更新UI
    private ArrayList fragments = new ArrayList();
    private void updateUI() {

        Log.e(TAG, "UPDATE UI");

        fragments.clear();
        for (int i = 0; i < pubPatients.size() ; i++) {
            MSFragment msFragment = MSFragment.newInstance(i);
            fragments.add(msFragment);
        }

        final PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager(), fragments);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (lt != null)
                    lt.success();

                if (pubPatients.size() > 0) {
                    viewPager.setVisibility(View.VISIBLE);
                    errLayout.setVisibility(View.GONE);
                    errImage.setBackgroundResource(R.drawable.fetch_data);
                    String title = ward_name + " 床位总数: " + String.valueOf(totalNumber);
                    displayTitle.setText(title);
                }

                viewPageIndex = 0;
                viewPager.setAdapter(adapter);
                viewPager.setCurrentItem(viewPageIndex);

                if (pubPatients.size() > 1) {
                    addGroupImage(pubPatients.size());
                    handlerStart();
                }
            }
        });

    }

    private void removeHandler() {
        handler.removeCallbacks(runnable);
    }
    private void handlerStart() {
        handler.postDelayed(runnable, 3000);
    }

    // TODO: 设置按钮点击
    public void setting(View v) {
       displayAlert();
    }

    // TODO: 设置病区
    private SettingDialog settingDialog;
    private void settingWardID() {

        SettingDialog.Builder builder = new SettingDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.setting_ward_layout, null);
        final EditText editText = view.findViewById(R.id.ward_id_et);
        editText.setText(String.valueOf(dpID));

        settingDialog =
                builder.cancelTouchout(false)
                        .view(view)
                        .heightdp(400)
                        .widthdp(600)
                        .addViewOnclick(R.id.cancel,new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                settingDialog.dismiss();
                            }
                        })
                        .addViewOnclick(R.id.sure, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int dpId = Integer.parseInt(editText.getText().toString());
                                SharedPreferences.Editor dataEditor = PreferenceManager.getDefaultSharedPreferences(MainActivityDemo.this).edit();
                                dataEditor.putInt(default_wardid, dpId);
                                dataEditor.apply();
                                MainActivityDemo.this.dpID = dpId;
                                masterManager.registerToServer();// 向服务器注册
                                requestData();
                                settingDialog.dismiss();
                            }
                        })
                        .build();

        settingDialog.show();
    }

    // TODO: 配置服务器地址
    private SettingDialog serverDialog;
    private void settingServer() {

        SettingDialog.Builder builder = new SettingDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.setting_server_layout, null);
        final EditText serverEditText = view.findViewById(R.id.server_ip);
        serverEditText.setText(masterManager.getDefaultHost());
        final EditText portEditText = view.findViewById(R.id.server_port);
        portEditText.setText(String.valueOf(masterManager.getDefaultPort()));

        serverDialog =
                builder.cancelTouchout(false)
                        .view(view)
                        .heightdp(600)
                        .widthdp(600)
                        .addViewOnclick(R.id.cancel,new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                serverDialog.dismiss();
                            }
                        })
                        .addViewOnclick(R.id.sure, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String a = serverEditText.getText().toString().trim();
                                String b = portEditText.getText().toString().trim();
                                //    将输入的用户名和密码打印出来
                                Toast.makeText(MainActivityDemo.this, "IP地址: " + a + ", 端口: " + b, Toast.LENGTH_SHORT).show();
                                if (a.split(".").length == 4)
                                    masterManager.setDefaultHost(a);
                                if (b.length() > 2)
                                    masterManager.setDefaultPort(Integer.parseInt(b));
                                serverDialog.dismiss();
                            }
                        })
                        .build();

        serverDialog.show();
    }

    // TODO: 弹出提示框
    private SettingDialog dialog;
    private void displayAlert() {

        SettingDialog.Builder builder = new SettingDialog.Builder(this);
        dialog =
                builder.cancelTouchout(false)
                        .view(R.layout.setting_layout)
                        .heightdp(600)
                        .widthdp(500)
                        .addViewOnclick(R.id.setting_ward_button,new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.dismiss();
                                try { sleep(30);}
                                catch (InterruptedException e){}
                                settingWardID();
                            }
                        })
                        .addViewOnclick(R.id.setting_server_button, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                                try { sleep(30);}
                                catch (InterruptedException e){}
                                settingServer();
                            }
                        })
                        .addViewOnclick(R.id.cancel_button, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        })
                        .build();

        dialog.show();
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

    // TODO: 获取本机IP
    public static String getLocalHostIp() {
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
