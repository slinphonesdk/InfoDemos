package com.udp.master;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.bean.common.DeviceType;
import com.bean.common.FunCode;
import com.bean.common.MsgCommand;
import com.bean.common.ParamsHeader;
import com.bean.common.ReceiverDatagramSocket;
import com.bean.common.ReceiverDatagramSocketListener;
import com.bean.common.STBIPAddress;
import com.bean.common.STBRequestParam;
import com.bean.common.SenderDatagramSocket;
import com.bean.common.SimpleDate;

import com.udp.master.model.ExPhoneModel;
import com.udp.master.model.FailedRequestJsonModel;
import com.udp.master.model.SessionIDModel;
import com.udp.master.model.SessionIDState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Handler;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import src.CMC;

import static java.lang.Thread.sleep;

public class MasterManager {

    private final static int receivePort = 8888;
    private final static int sendPort = 10888;
    private static Context context = null;
    private MasterListener masterListener;

    private SharedPreferences.Editor dateEditor() {
        SharedPreferences.Editor dataEditor = this.context.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE).edit();
        return dataEditor;
    }

    public void setDefaultHost(String host) {

        SharedPreferences.Editor dataEditor = dateEditor();
        dataEditor.putString("default_host", host);
        dataEditor.apply();
    }

    public void setDefaultPort(int port) {

        SharedPreferences.Editor dataEditor = dateEditor();
        dataEditor.putInt("default_port", port);
        dataEditor.apply();
    }

    private int getDefaultWardID() {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt("default_wardid", 14);
    }

    public String getDefaultHost() {
        return context.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE).getString("default_host", "192.168.88.3");
    }

    public int getDefaultPort() {
        return context.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE).getInt("default_port", 9600);
    }

    // TODO: 初始化
    public MasterManager(Context act, final MasterListener masterListener) {
        context = act;
        this.masterListener = masterListener;
        this.loadCacheData();
        registerToServer();// 向服务器注册
        this.setupSocket();// 初始化udp
        this.startThreads(); // 发送配对床头、门口分机线程
    }

    // TODO: 线程移除
    public void threadClear() {
        mRunning = false;
        if (this.handlers == null || this.runnableArrayList == null) return;
        if (this.handlers.size() == this.runnableArrayList.size()) {
            for (int i = 0; i < this.handlers.size(); i++) {
                handlers.get(i).removeCallbacks(runnableArrayList.get(i));
            }
        }
        receiverDatagramSocket.clear();
        senderDatagramSocket.clear();
        Log.e("THREAD", "clear");
    }

    // TODO: 线程开启
    private ArrayList<Handler> handlers;
    private ArrayList<Runnable> runnableArrayList;
    private boolean mRunning = true;
    private void startThreads() {

        handlers = new ArrayList<>();
        runnableArrayList = new ArrayList<>();

        runnableArrayList.add(mWaitSessionRunnable);
        runnableArrayList.add(mCheckHeartRunnable);
        runnableArrayList.add(mCheckFailedRequestRunnable);
        runnableArrayList.add(mCheckFailedBroadcastRunnable);
        runnableArrayList.add(mCheckDateTime);
        runnableArrayList.add(mFiveMinutesSynchronousRunnable);

        // 检查等待会话 检查心跳 检查失败请求 检查发送广播失败线程
        String[] threadNames = {
                "WaitSessionThread",
                "CheckHeartThread",
                "CheckFailedRequestThread",
                "CheckFailedBroadcast",
                "CheckDateTime",
                "CheckFiveMinutesSynchronous"};

        for (int i = 0; i < threadNames.length; i ++) {
            HandlerThread thread = new HandlerThread(threadNames[i]);
            thread.start();//创建一个HandlerThread并启动它
            Handler handler = new Handler(thread.getLooper());
            handler.post(runnableArrayList.get(i));
            handlers.add(handler);
        }

    }

    // TODO: 5分钟同步一次心跳列表
    private Runnable mFiveMinutesSynchronousRunnable = new Runnable()
    {
        public void run()
        {

            while (mRunning) {

                if (registerPhones.size() > 0) {

                    JSONObject reqObject = new JSONObject();
                    JSONObject headerObject = new JSONObject();
                    JSONArray listArray = new JSONArray();

                    try {

                        for (ExPhoneModel exM: registerPhones) {
                            listArray.put(exM.jsonObject());
                        }

                        headerObject.put("funcode", FunCode.heartList.value());
                        headerObject.put("reqtime", SimpleDate.getCurrentDate());
                        headerObject.put("deviceip", STBIPAddress.getLocalHostIp());

                        reqObject.put("header", headerObject);
                        reqObject.put("body", listArray);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    MasterHttpHelper.Post(reqObject.toString());
                }

                long l = 300000;// 5分钟
                try
                {
                    Thread.sleep(l);
                }
                catch (InterruptedException localInterruptedException) {}
            }
        }
    };

    // TODO: 获取服务器时间日期
    private void fetchDateTimeFromServer() {

        JSONObject header = new JSONObject();
        JSONObject reqJson = new JSONObject();
        try {
            header.put("funcode", FunCode.updateDateTime.value());
            reqJson.put("header", header);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String requestJsonString = reqJson.toString();
        MasterHttpHelper.Post(requestJsonString, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                MasterHttpHelper.saveFailedInfoToDB(requestJsonString);
            }

            @Override
            public void onResponse(@Nullable Call call, @Nullable Response response) throws IOException {

                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    final String responseStr = responseBody.string();
                    Log.e("UPDATE DATE AND TIME", "result: "+responseStr);

                    try {
                        MaJson jsonObject = new MaJson(responseStr);
                        final String resultCode = jsonObject.resultCode();
                        final String successCode = "0";
                        if (TextUtils.equals(resultCode, successCode)) {
                            // 获取信息成功
                            String dateTime = jsonObject.resultDateTime();
                            // 设置当前机器系统时间日期
                            try {
                                SimpleDate.Sys.setDateTime(context, Long.valueOf(SimpleDate.dateToStamp(dateTime)));
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            // 保存
                            LastUpdateDateTime = dateTime;
                            LastCheckTime = dateTime;
                        }
                        else {
                            MasterHttpHelper.saveFailedInfoToDB(requestJsonString);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    // TODO: 检查日期时间
    private String LastUpdateDateTime = "";
    private String LastCheckTime = "";
    private Runnable mCheckDateTime = new Runnable() {
        @Override
        public void run() {
            while (mRunning) {

                if (TextUtils.equals("",LastUpdateDateTime)) {
                    // 获取服务器时间并更新本地时间
                    fetchDateTimeFromServer();
                }
                else {
                    // 检查上次更新时间
                    try {

                        int halfAMonth = 1000*60*60*24*1;//一天的时间
                        Long lastCheckTime = Long.valueOf(SimpleDate.dateToStamp(LastCheckTime));
                        Long lastUpdateTime = Long.valueOf(SimpleDate.dateToStamp(LastUpdateDateTime));
                        long overTime = lastCheckTime - lastUpdateTime;
                        Log.e("UPDATE DATE AND TIME", String.format("LastCheckTime: %s - LastUpdateTime : %s  = %d , halfTime:%d", lastCheckTime, lastUpdateTime, overTime,halfAMonth));

                        if (lastCheckTime - lastUpdateTime >= halfAMonth) {
                            fetchDateTimeFromServer();// 更新
                        }
                        else {
                            LastCheckTime = SimpleDate.getCurrentDate();
                            Log.e("UPDATE DATE AND TIME", String.format("当前时间: %s",LastCheckTime));
                        }

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                int sleepTime = 1000*60*60*1;
                try {sleep(sleepTime);}
                catch (InterruptedException e){e.printStackTrace();}
            }
        }
    };


    // TODO: 广播会话列表检查线程
    private CheckFailedBroadcastRunnable mCheckFailedBroadcastRunnable = new CheckFailedBroadcastRunnable();
    private class CheckFailedBroadcastRunnable implements Runnable {
        int setupCount = 10;// 初始次数
        int checkCount = 10;// 重发次数
        int checkTime = 3000;// 3秒检查

        long oldDate = 0;
        boolean isNeedCheck = false;// 是否需要检查重发

        private long currentDate() {
            return System.currentTimeMillis();
        }
        @Override
        public void run() {
            while(mRunning){
                Log.i("THREAD", "mCheckFailedBroadcastRunnable running!");

                if (isNeedCheck) {
                    checkFailed();
                }

                try { sleep(1000); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
        }

        // 已经收到成功发送广播消息，不需要重发
        void isDonTNeedCheck() {
            Log.e("RESEND", "停止重发");
            isNeedCheck = false;
            this.oldDate = 0;
        }

        /*
         * 设置发送时间*/
        void setSendDate() {
            this.oldDate = currentDate();
            isNeedCheck = true;
            Log.e("RESEND", "设置广播发送时间" + this.oldDate);
        }

        private void checkFailed() {
            Log.e("RESEND", "checkFailed");

            if (sessionIDModels.size() == 0) {
                checkCount = setupCount;
                isDonTNeedCheck();
                return;
            }

            if (checkCount == 0) {
                isDonTNeedCheck();
                // 通知：医护分机不可用
                Log.e("RESEND", "未收到医护分机确认消息");
                return;
            }

            if (currentDate() - oldDate > checkTime) {
                sendMsgToTANPhones();
                checkCount--;
                Log.e("RESEND", "重发广播列表");
            }
        }
    }

    // TODO: 请求检查失败线程
    private Runnable mCheckFailedRequestRunnable = new Runnable() {
        private final int checkTime = 30000;//30秒检查一次

        @Override
        public void run() {
            while(mRunning){
                Log.i("THREAD", "mCheckFailedRequestRunnable running!");

                checkReqFailed();

                try {
                    sleep(checkTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // TODO: 检查失败请求
        private void checkReqFailed() {

            List<FailedRequestJsonModel> lists = MasterDBDao.getInstance().queryFailedModels();
            int listSize = lists.size();
            if (listSize > 0) {

                for (int i = 0; i < listSize; i++) {

                    FailedRequestJsonModel failedRequestJsonModel = lists.get(i);
                    MasterDBDao.getInstance().removeFailed(failedRequestJsonModel);
                    MasterHttpHelper.Post(failedRequestJsonModel.getJsonStr());
                    try { sleep(300); }
                    catch (InterruptedException e) { e.printStackTrace(); }
                }
            }
        }
    };

    // TODO: 心跳的线程
    private Runnable mCheckHeartRunnable = new Runnable() {
        private final int checkTime = 30000;//30秒检查一次
        @Override
        public void run() {
            while(mRunning){
                Log.i("THREAD", "mCheckHeartRunnable running!");

                checkOffline();

                try {
                    sleep(checkTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // TODO: 检查离线分机
        private void checkOffline() {
            int i = 0;
            for (ExPhoneModel exPhoneModel : registerPhones) {

                long currentTime = System.currentTimeMillis();
                if (currentTime - exPhoneModel.getTheLastHeartTime() > checkTime) {
                    Log.e("PPT", "第"+i+"次：设备："+exPhoneModel.getDeviceType()+exPhoneModel.getSip()+"已离线");
                    i++;
                    exPhoneModel.setOffLineTimes(exPhoneModel.getOffLineTimes()+1);
                    if (exPhoneModel.getState() == ExPhoneModel.State.online) {
                        if (exPhoneModel.getOffLineTimes() >= 3) {

                            exPhoneModel.setState(ExPhoneModel.State.offline);

                            Log.e("PPT", "设备："+exPhoneModel.getDeviceType()+exPhoneModel.getSip()+"已离线"+"3,将上传至服务器,重置次数.");
                            final String msg = STBRequestParam.msgConfig(MsgCommand.offline.value(), exPhoneModel.getSip(), exPhoneModel.getDeviceType().value(), "");
                            senderDatagramSocket.sendBroadcast(msg);
                            exPhoneModel.setOffLineTimes(0);
                            exPhoneModel.setState(ExPhoneModel.State.offline);
                            MasterDBDao.getInstance().insertExModel(exPhoneModel);// 插入数据库
                            try { sleep(300); }
                            catch (InterruptedException e) { e.printStackTrace(); }
                        }
                    }
                }
            }
        }
    };

    // TODO: 等待发送会话线程
    private Runnable mWaitSessionRunnable = new Runnable() {
        private int millis = 9000;// 下次给配对分机发送时间

        @Override
        public void run() {
            while(mRunning){
                Log.i("THREAD", "mWaitSessionRunnable running!");

                sendMsgToMobilePair();

                try {  sleep(300); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
        }

        // TODO: 给配对分机发送会话
        private void sendMsgToMobilePair() {

            if (sessionIDs.size() > 0 && mobilesPhones.size() > 0)
            {
                theLastSessionID = sessionIDs.remove(0);//取出第一次

                final String msg = STBRequestParam.msgConfig(MsgCommand.receiveBroadcastSingle.value(), "000000", DeviceType.master.value(), "master", theLastSessionID, null);
                // 1.发送广播 -> 移动->医护分机
                senderDatagramSocket.sendBroadcast(msg);

                sessionIDs.add(theLastSessionID);//发送完毕后，添加

                try { sleep(millis); }
                catch (InterruptedException o) { o.printStackTrace(); }
            }
        }
    };

    // TODO: 向服务器注册
    public void registerToServer() {
        JSONObject body = new JSONObject();

        try {
            body.put("swardid", getDefaultWardID());
            body.put("deviceip",STBIPAddress.getLocalHostIp());
            body.put("devicetypeid",DeviceType.master.value());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MasterHttpHelper.Post(body, FunCode.register);
    }

    // TODO: 加载离线数据
    private void loadCacheData() {
        List<ExPhoneModel> lists = MasterDBDao.getInstance().queryExModels();
        if (lists.size() > 0) {
            MasterDBDao.getInstance().clearExModels();

            for (ExPhoneModel em : lists) {
                em.setState(ExPhoneModel.State.online); // 重新检测是否在线
                em.setOffLineTimes(3);
                addRegister(em, false);
            }
        }
    }

    private List<ExPhoneModel> registerPhones = Collections.synchronizedList(new ArrayList());// 床头 门口
    private List<ExPhoneModel> mobilesPhones =  Collections.synchronizedList(new ArrayList());// 注册床头、门口
    private List<ExPhoneModel> refusePhones = Collections.synchronizedList(new ArrayList());// 拒绝心跳
    // TODO: Context给数据库用
    public static Context getContext() {
        return context;
    }

    // TODO: 添加配对分机 -> 移动分机配对
    private void mobilePair(ExPhoneModel exphoneModel) {

        if (mobilesPhones.size() > 0) {
            Boolean isContains = false;
            for (ExPhoneModel mo : mobilesPhones) {
                if (mo.isEqual(exphoneModel)) {
                    isContains = true;
                    break;
                }
            }
            if (!isContains)
                mobilesPhones.add(exphoneModel);
        } else {
            mobilesPhones.add(exphoneModel);
        }
        Log.e("ppt","mobilesPhones: size = "+mobilesPhones.size());
    }

    // TODO: 移除配对分机 -> 移动分机配对
    private void mobileRemove(ExPhoneModel exphoneModel) {

        if (mobilesPhones.size() == 0) return;

        for (int index = 0; index < mobilesPhones.size(); index++) {
            ExPhoneModel currentExPhoneModel = mobilesPhones.get(index);
            if (currentExPhoneModel.isEqual(exphoneModel)) {
                mobilesPhones.remove(currentExPhoneModel);
                break;
            }
        }
    }

    // TODO: 心跳在线处理
    private void keepHeartAline(ExPhoneModel exPhoneModel) {

        for (ExPhoneModel em : registerPhones) {
            if (em.isEqual(exPhoneModel)) {

                if (em.getState() == ExPhoneModel.State.offline) { // 如果是已经离线，发送在线消息
                    Log.e("PPT", "设备: " + exPhoneModel.getDeviceType() + " " + exPhoneModel.getSip() + "已经在线");
                    final String msg = STBRequestParam.msgConfig(MsgCommand.online.value(), exPhoneModel.getSip(), exPhoneModel.getDeviceType().value(), "");
                    senderDatagramSocket.sendBroadcast(msg);
                    em.setState(ExPhoneModel.State.online);
                }
                em.setTheLastHeartTime();
                em.setState(ExPhoneModel.State.online);
                break;
            }
        }

        List<ExPhoneModel> exPhoneModels = MasterDBDao.getInstance().queryExModels();
        if (exPhoneModels.size() > 0) {
            for (ExPhoneModel em : exPhoneModels) {
                if (em.isEqual(exPhoneModel)) {
                    MasterDBDao.getInstance().removeExPhoneModel(em);
                    break;
                }
            }
        }

    }

    // TODO: 上传呼叫记录
    private void updateRecord(SessionIDModel sessionIDModel) {

        try {
            MaJson bodyJson = new MaJson(sessionIDModel);
            MasterHttpHelper.Post(bodyJson,getDefaultHost(), getDefaultPort(), FunCode.updateRecode);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private SenderDatagramSocket senderDatagramSocket;
    private ReceiverDatagramSocket receiverDatagramSocket;
    // TODO: SOCKET 初始化
    private void setupSocket() {

        try {
            senderDatagramSocket = new SenderDatagramSocket(sendPort);
            receiverDatagramSocket = new ReceiverDatagramSocket(receivePort);

        } catch (SocketException e) {
            e.printStackTrace();
        }

        receiverDatagramSocket.addListener(new ReceiverDatagramSocketListener() {

            // return ExPhoneModel
            private ExPhoneModel getExModel(ParamsHeader paramsHeader, InetAddress address, ExPhoneModel.State state) {
                return new ExPhoneModel(paramsHeader.sip, paramsHeader.deviceType, address.getHostAddress(), state);
            }

            final static String needRefreshList = "01|1";
            @Override
            public void msgFromServer(String jsonStr, InetAddress inetAddress, int port) {

                String firstChar = jsonStr.substring(0,1);
                String responseChar = "0";
                String resultMsg = "";
                String flagMsg = "";
                if (jsonStr.length() > 17) {
                    flagMsg = jsonStr.substring(1, 17);
                    resultMsg = jsonStr.substring(17,jsonStr.length());
                }

                if (TextUtils.equals(firstChar,responseChar)) {
                    if (inetAddress != null)
                        senderDatagramSocket.sendMsgToClient(flagMsg,inetAddress,port);
                }

                if (TextUtils.equals(resultMsg,needRefreshList)) {
                    if (masterListener != null) {
                        masterListener.msgFromServerIsNeedReload();
                    }
                }
            }

            @Override
            public void msgCallBack(ParamsHeader paramsHeader, CMC.CMCRequestParam.Body body, InetAddress fromAddress) {

                Log.e("ppt", "receive: \n"
                        + paramsHeader.msgCommand.toString() + " " + "\n"
                        + fromAddress.getHostAddress());

                if (paramsHeader.msgCommand == MsgCommand.register) {

                    ExPhoneModel exPhoneModel = getExModel(paramsHeader, fromAddress, ExPhoneModel.State.online);
                    addRegister(exPhoneModel, true);
                }
                else if (paramsHeader.msgCommand == MsgCommand.registerOfDoctor) {
                    ExPhoneModel exPhoneModel = getExModel(paramsHeader, fromAddress, ExPhoneModel.State.online);
                    mobilePair(exPhoneModel);
                }
                else if (paramsHeader.msgCommand == MsgCommand.unregisterOfDoctor) {
                    ExPhoneModel exPhoneModel = getExModel(paramsHeader, fromAddress, ExPhoneModel.State.online);
                    mobileRemove(exPhoneModel);
                }
                else if (paramsHeader.msgCommand == MsgCommand.heart) {
                    ExPhoneModel exPhoneModel = getExModel(paramsHeader, fromAddress, ExPhoneModel.State.online);

                    if (refusePhones.size() > 0) { // 如果心跳分机与拒绝列表内ip重复则不再存入心跳列表
                        for (int i = 0; i < refusePhones.size(); i++) {
                            ExPhoneModel exM = refusePhones.get(i);
                            if (exM.isEqual(exPhoneModel) && TextUtils.equals(exM.getSip(),exPhoneModel.getSip())) {
                                return;
                            }
                        }
                    }

                    addRegister(exPhoneModel,false);
                    keepHeartAline(exPhoneModel);
                } else if (paramsHeader.msgCommand == MsgCommand.receiveHeartStop) {
                    ExPhoneModel exPhoneModel = getExModel(paramsHeader, fromAddress, ExPhoneModel.State.online);
                    Log.e("HeartStop",exPhoneModel.getIPAddress() + " " + exPhoneModel.getSip());
                }

                else if (paramsHeader.msgCommand == MsgCommand.receiveSessionIDList) {
                    // 收到医护分机确定 收到会话列表消息，停止重发线程
                    mCheckFailedBroadcastRunnable.isDonTNeedCheck();
                }

                else if (paramsHeader.msgCommand == MsgCommand.call) { // from 床头
                    Log.e("ppt","收到"+paramsHeader.deviceType.toString()+"呼叫指令, 生成sessionID，转发给医护分机集合");
                    final String sessionID = generatingSessionID(paramsHeader.sip, fromAddress, paramsHeader.uuid);

                    if (sessionID == null) return;
                    final String msg = STBRequestParam.msgConfig(MsgCommand.callRecSessionID.value(), "000000", DeviceType.master.value(), "master", sessionID, null);
                    senderDatagramSocket.sendMsgToClient(msg, fromAddress);

                    try { sleep(300); }
                    catch (InterruptedException e) { e.printStackTrace(); }
                    sendMsgToTANPhones();
                }
                else if (paramsHeader.msgCommand == MsgCommand.receiveCall) { // from 医护分机
                    Log.e("ppt","有医护分机发起接听");
                    //向其它分机发送取消接听
                    //向此分机发送确定接听 <发送接听通话分机收到的列表内不包含此条会话则认为已经成功收到>
                    //更新此条sessionID状态
                    update(body.getSessionID(), SessionIDState.response, fromAddress, paramsHeader.sip);
                    sendMsgToTANPhones();
                }

                else if (paramsHeader.msgCommand == MsgCommand.hangup) { // from all
                    update(body.getSessionID(), SessionIDState.callEnd, fromAddress, paramsHeader.sip);
                }

                else if (paramsHeader.msgCommand == MsgCommand.cancelCall) { // from 床头

                    update(body.getSessionID(), SessionIDState.cancel, fromAddress, paramsHeader.sip);

                    final String msg = STBRequestParam.msgConfig(MsgCommand.cancelCallSuccess.value(), "000000", DeviceType.master.value(), "master", null, null);
                    senderDatagramSocket.sendMsgToClient(msg, fromAddress);

                    try {
                        sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    sendMsgToTANPhones();
                }
                else if (paramsHeader.msgCommand == MsgCommand.refusingToanswer) {// from 医护
                    //发送拒绝给指定床头
                    SessionIDModel sessionIDModel = getSessionIDModel(body.getSessionID());
                    final String msg = STBRequestParam.msgConfig(MsgCommand.refusingToanswer.value(), "000000", DeviceType.master.value(), "master", body.getSessionID(), null);
                    if (sessionIDModel != null)
                        senderDatagramSocket.sendMsgToClient(msg, sessionIDModel.getFromIPAddress());

                    try {
                        sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    update(body.getSessionID(), SessionIDState.refuse, fromAddress, paramsHeader.sip);
                    sendMsgToTANPhones();
                }
            }
        });
    }

    // TODO: 注册分机 初始化注册调用
    int index = 0;
    private void addRegister(ExPhoneModel exPhoneModel, boolean isRegister) {

        if (exPhoneModel.isPass()) {// 判断类型与sip是否正确

            if (isRegister) {
                registerStateToClient(true, exPhoneModel);
            }

            if (registerPhones.size() == 0 && refusePhones.size() == 0 && isRegister) {
                registerPhones.add(exPhoneModel);
            }

            // 如果是注册指令
            else if (isRegister) {
                 // 从拒绝列表移除
                if (refusePhones.size() > 0) {
                    for (int i = 0; i < refusePhones.size(); i++) {
                        ExPhoneModel ex = refusePhones.get(i);
                        if (ex.isEqual(exPhoneModel)) {
                            refusePhones.remove(i);
                        }
                    }
                }

                // 注册列表移除旧设备
                if (registerPhones.size() > 0) {

                    boolean isNotContains = true;
                    for (int i = 0; i < registerPhones.size(); i++) {
                        ExPhoneModel oldExPhoneModel = registerPhones.get(i);

                        //sip相同 ip不同
                        if (TextUtils.equals(oldExPhoneModel.getSip(), exPhoneModel.getSip()) && !oldExPhoneModel.isEqual(exPhoneModel)) {
                            // 如果旧设备在线 通知旧设备
                            if (oldExPhoneModel.getState() == ExPhoneModel.State.online && !oldExPhoneModel.isEqual(exPhoneModel)) {

                                String str = STBRequestParam.msgConfig(MsgCommand.SIPConflict.value(), "000000", DeviceType.master.value(), "master");
                                this.senderDatagramSocket.sendMsgToClient(str, oldExPhoneModel.IPAddress());
                                refusePhones.add(oldExPhoneModel);
                            }
                            registerPhones.remove(i);
                            Log.e("CREG", "SIP相同 IP不同 "+registerPhones.size()+" 当前删除设备: "+oldExPhoneModel.getState().toString()+" "+oldExPhoneModel.getIPAddress() + " "+oldExPhoneModel.getSip()+" "+oldExPhoneModel.getDeviceType().toString());
                            continue;
                        }

                        //sip不同 ip相同
                        if (oldExPhoneModel.isEqual(exPhoneModel) && !TextUtils.equals(oldExPhoneModel.getSip(), exPhoneModel.getSip())) {//IP相同
                            registerPhones.remove(i);
                            Log.e("CREG", "IP相同 ： "+registerPhones.size()+" 当前删除设备: "+oldExPhoneModel.getState().toString()+" "+oldExPhoneModel.getIPAddress() + " "+oldExPhoneModel.getSip()+" "+oldExPhoneModel.getDeviceType().toString());
                            continue;
                        }

                        //sip相同 ip相同
                        if (oldExPhoneModel.isEqual(exPhoneModel) && TextUtils.equals(oldExPhoneModel.getSip(), exPhoneModel.getSip())) {
                            isNotContains = false;
                            break;
                        }

                        try {
                            sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (isNotContains) {
                        registerPhones.add(exPhoneModel);
                    }
                    Log.e("CREG", "- "+registerPhones.size()+" 当前注册设备: "+exPhoneModel.getState().toString()+" "+exPhoneModel.getIPAddress() + " "+exPhoneModel.getSip()+" "+exPhoneModel.getDeviceType().toString());
                    for (ExPhoneModel e: registerPhones) {
                        Log.e("CREG", "register phone: "+e.getState().toString()+" "+e.getIPAddress() + " "+e.getSip()+" "+e.getDeviceType().toString());
                    }

                }

            }
            // 心跳过来
            else {
                checkRepeat(exPhoneModel);
                Log.e("CHEART", "<start print COUNT: "+registerPhones.size()+"\n");
                for (ExPhoneModel e: registerPhones) {
                    Log.e("CHEART", "phone: "+e.getState().toString()+" "+e.getIPAddress() + " "+e.getSip()+" "+e.getDeviceType().toString());
                }
                Log.e("CHEART", "end print>\n");
            }

        } else {

            // 返回注册失败
            registerStateToClient(false,exPhoneModel);
        }



        Log.e("INDEX", String.valueOf(index) + "  " + registerPhones.size());
        index++;
    }

    // TODO: 心跳过滤
    private void checkRepeat(ExPhoneModel newExPhoneModel) {

        // 注册设备列表内过滤重复
        boolean isNotAdd = true;
        for (int i = 0; i < registerPhones.size(); i++) {
            ExPhoneModel oldExPhoneModel = registerPhones.get(i);
            // 如果旧设备与新设备sip相同，且ip不同
            if (TextUtils.equals(newExPhoneModel.getSip(), oldExPhoneModel.getSip()) && !newExPhoneModel.isEqual(oldExPhoneModel)) {

                // 旧设备心跳被拒绝 除非重新注册
                this.refusePhones.add(oldExPhoneModel);

                // 旧设备从注册列表移除
                registerPhones.remove(i);
                continue;
            }
            // 如果旧设备与新注册设备ip相同 sip不同
            if (oldExPhoneModel.isEqual(newExPhoneModel) && !TextUtils.equals(newExPhoneModel.getSip(), oldExPhoneModel.getSip())) {

                registerPhones.remove(i);// 从注册列表移除旧设备
                continue;
            }

            // 如果旧设备与新设备ip相同 ，sip相同
            if ((TextUtils.equals(oldExPhoneModel.getSip(), newExPhoneModel.getSip()) && oldExPhoneModel.isEqual(newExPhoneModel))) {
                isNotAdd = false;
                break;
            }
        }

        if (isNotAdd) {// 如果不包含添加新设备
            registerPhones.add(newExPhoneModel);
        }
    }


    // TODO: 返回注册状态 失败原因类型与sipNumber不符合
    private void registerStateToClient(Boolean isSuccess, ExPhoneModel exPhoneModel) {

        try {

            InetAddress fromAddress = InetAddress.getByName(exPhoneModel.getIPAddress());
            final String msg = STBRequestParam.msgConfig(isSuccess ? MsgCommand.registerSuccess.value() : MsgCommand.registerFailed.value(), "000000", DeviceType.master.value(), "master");
            senderDatagramSocket.sendMsgToClient(msg, fromAddress);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


    }

    // TODO: 更新会话组各会话状态
    private void update(String sessionID, SessionIDState state, InetAddress toIPAddress, String toSip) {

        for (int i = 0; i < sessionIDModels.size(); i++) {
            SessionIDModel sessionIDModel = sessionIDModels.get(i);
            if (TextUtils.equals(sessionIDModel.getSessionID(), sessionID)) {
                sessionIDModel.setSessionIDState(state);
                sessionIDModel.setToIPAddress(toIPAddress);
                sessionIDModel.setTosip(toSip);
                Log.e("ppt", sessionIDModel.getCallsip() + " "+ sessionIDModel.getSessionID() + " "+sessionIDModel.getSessionIDState().toString());
                if (state == SessionIDState.callEnd) {
                    sessionIDModel.setCallEndDate(SimpleDate.getCurrentDate());
                } else if (state == SessionIDState.response){//响应不上传
                    sessionIDModel.setSureDate(SimpleDate.getCurrentDate());
                    return;
                } else if (state == SessionIDState.cancel) {//主动断开
                    sessionIDModel.setCancelDate(SimpleDate.getCurrentDate());
                } else if (state == SessionIDState.refuse) {//被拒绝
                    sessionIDModel.setRefuseData(SimpleDate.getCurrentDate());
                }else if (state == SessionIDState.accept) {//医护分机已经接听
                    sessionIDModel.setSureDate(SimpleDate.getCurrentDate());
                }

                // 1.上传呼叫记录
                updateRecord(sessionIDModel);
                // 2.移除
                sessionIDModels.remove(i);
                break;
            }
        }
    }

    // TODO: 根据会话id获得会话模型
    private SessionIDModel getSessionIDModel(String sessionID) {
        for (SessionIDModel m: sessionIDModels) {
            if (TextUtils.equals(m.getSessionID(), sessionID)) {
                return m;
            }
        }
        return null;
    }

    // TODO: 生成会话id
    private ArrayList<SessionIDModel> sessionIDModels = new ArrayList<>();
    private String generatingSessionID(String sip, InetAddress IPAddress, String nurseLevel) {

        String oldSessionID = waitSessionIsContains(sip);
        if (oldSessionID != null)
            return oldSessionID;

        String sessionID = sip + SimpleDate.getCurrentDateNu();
        if (nurseLevel.length() > 0) {
            sessionID += "$" + nurseLevel;
        }

        final String createDate = SimpleDate.getCurrentDate();
        SessionIDModel sessionIDModel = new SessionIDModel();
        sessionIDModel.setFromIPAddress(IPAddress);
        sessionIDModel.setCallsip(sip);
        sessionIDModel.setSessionID(sessionID);
        sessionIDModel.setGeneratingDate(createDate);
        sessionIDModel.setSessionIDState(SessionIDState.waitSend);
        sessionIDModels.add(sessionIDModel);

        return sessionID;
    }

    // TODO: 有新呼叫、取消就刷新列表
    private void sendMsgToTANPhones() {

        ArrayList<SessionIDModel> waitSendModels = waitSendSession();
        ArrayList<String> sessionIDs = new ArrayList<>();
        for (int i = 0; i < waitSendModels.size(); i++) {
            SessionIDModel sessionIDModel = waitSendModels.get(i);
            sessionIDs.add(sessionIDModel.getSessionID());
        }

        refreshSessionIDs();
        final String msg = STBRequestParam.msgConfig(MsgCommand.receiveBroadcast.value(), "000000", DeviceType.master.value(), "master", null, sessionIDs);

        // 1.发送广播 -> 医护分机
        senderDatagramSocket.sendBroadcast(msg);

        mCheckFailedBroadcastRunnable.setSendDate(); // 设置当前广播 发送时间，并开启3秒自动重发
    }

    // TODO: 确定所有会话都是有效的
    private void refreshSessionIDs() {

        ArrayList<String> waitSendSessionIDs = waitSendSessionIDs();

        handlers.get(0).removeCallbacks(mWaitSessionRunnable);// 防止队列正在操作sessionIDs 先移除队列
        if (waitSendSessionIDs.contains(theLastSessionID)) {
            waitSendSessionIDs.remove(theLastSessionID);
            sessionIDs = waitSendSessionIDs;
            sessionIDs.add(theLastSessionID);
        }
        else {
            sessionIDs = waitSendSessionIDs;
        }
        handlers.get(0).post(mWaitSessionRunnable);// 添加队列
    }

    private ArrayList<String> sessionIDs = new ArrayList<>();
    private String theLastSessionID = "";


    // TODO: 等待转发的会话models
    private ArrayList<SessionIDModel> waitSendSession() {

        ArrayList<SessionIDModel> ms = new ArrayList<>();
        ArrayList<String> sipArr = new ArrayList<>();
        for(int index = 0; index < sessionIDModels.size(); index ++) {
            SessionIDModel currentModel = sessionIDModels.get(index);
            if (currentModel.getSessionIDState() == SessionIDState.waitSend) {
                if (!sipArr.contains(currentModel.getCallsip())) {
                    ms.add(currentModel);
                    sipArr.add(currentModel.getCallsip());
                }
            }
        }
        return ms;
    }

    // TODO: 检查是否有重复sip
    private String waitSessionIsContains(String sip) {

        ArrayList<SessionIDModel> ms = waitSendSession();
        String oldSessionID = null;
        for (SessionIDModel mo: ms) {
            if (TextUtils.equals(mo.getCallsip(), sip)) {
                oldSessionID = mo.getSessionID();
                break;
            }
        }
        return oldSessionID;
    }

    // TODO: 等待转发的会话数组
    private ArrayList<String> waitSendSessionIDs() {
        ArrayList<String> ms = new ArrayList<>();
        ArrayList<String> sipArr = new ArrayList<>();
        for(int index = 0; index < sessionIDModels.size(); index ++) {
            SessionIDModel currentModel = sessionIDModels.get(index);
            if (currentModel.getSessionIDState() == SessionIDState.waitSend) {
                if (!sipArr.contains(currentModel.getCallsip())) {
                    ms.add(currentModel.getSessionID());
                    sipArr.add(currentModel.getCallsip());
                }
            }
        }
        return ms;
    }

}