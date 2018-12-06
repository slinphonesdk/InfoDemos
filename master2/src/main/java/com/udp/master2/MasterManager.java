package com.udp.master2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.HandlerThread;
import android.text.TextUtils;

import com.bean.common.DeviceType;
import com.bean.common.FunCode;
import com.bean.common.MsgCommand;
import com.bean.common.ParamsModel;
import com.bean.common.STBIPAddress;
import com.bean.common.STBRequestParam;
import com.bean.common.SimpleDate;
import com.bean.common.utils.STBLog;
import com.bean.common.utils.ThreadPoolManager;
import com.google.protobuf.InvalidProtocolBufferException;
import com.bean.common.rudp.RUdpCallBack;
import com.bean.common.rudp.RUdpListener;
import com.bean.common.rudp.RUdpManager;
import com.udp.master2.model.ExPhoneModel;
import com.udp.master2.model.FailedRequestJsonModel;
import com.udp.master2.model.SessionIDModel;
import com.udp.master2.model.SessionIDState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.FutureTask;

import android.os.Handler;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import src.CMC;

import static java.lang.Thread.sleep;

public class MasterManager {

    private static String BASE_URL = "http://192.168.1.41:8080/v1/";
    private static int successCode = 200;
    private static Context context = null;
    private RUdpManager rUdpManager;
    private ArrayList<ExPhoneModel> treatAndNurseModels = new ArrayList<>(10);
    private SharedPreferences sharedPreferences;
    private MasterListener masterListener;

    /**
     * 后台服务器地址 10.9.0.12
     * @return
     */
    public String getDefaultHost() {//
        if (sharedPreferences == null) return "";
        return sharedPreferences.getString("default_host", "10.9.0.5");
    }

    public int getDefaultPort() {
        if (sharedPreferences == null) return 8080;
        return sharedPreferences.getInt("default_port", 9600);
    }

    public int getDefaultWardID() {
        if (sharedPreferences == null) return 9;
        return sharedPreferences.getInt("default_wardid", 9);
    }

    /***
     * 初始化
     * @param act
     * @param masterListener
     */
    public MasterManager(Context act, final MasterListener masterListener) {
        context = act;
        this.masterListener = masterListener;
        loadCacheData();

        this.startThreads(); // 发送配对床头、门口分机线程
        STBLog.isDebug = true;

        Thread registerThread = new Thread(new Runnable() {
            @Override
            public void run() {

                Call<PostRequestCallModel> call = registerToServer(null);
                call.enqueue(new Callback<PostRequestCallModel>() {
                    @Override
                    public void onResponse(Call<PostRequestCallModel> call, Response<PostRequestCallModel> response) {

                        if (response.body().getCode() == successCode) {
                            STBLog.out("Register","向服务器注册成功"+response.body().toString());
                        }
                        else {
                            masterListener.notificationWith("信息主机", "注册:失败"+response.body().toString());
                        }
                    }

                    @Override
                    public void onFailure(Call<PostRequestCallModel> call, Throwable t) {
                        masterListener.notificationWith("信息主机", "注册:失败");
                    }
                });
            }
        });
        ThreadPoolManager.getInstance().execute(new FutureTask<>(registerThread,null),null);

        rUdpManager = new RUdpManager(RUdpManager.ServiceType.server);
        rUdpManager.addListener(new RUdpListener() {

            private ExPhoneModel exPhoneModel;
            private String msgFlag;
            private InetAddress fromAddress;
            private ParamsModel paramsModel = null;

            /**
             * 注册
             */
            private  void register() {

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (exPhoneModel.isPass()) {// 校验通过返回注册成功

                            Call<PostRequestCallModel> call = registerToServer(exPhoneModel);
                            call.enqueue(new Callback<PostRequestCallModel>() {
                                @Override
                                public void onResponse(Call<PostRequestCallModel> call, Response<PostRequestCallModel> response) {
                                    // 向服务器注册
                                    if (response.code() == successCode) { // 服务器注册成功

                                        addRegister(exPhoneModel, true);
                                        final String msg = STBRequestParam.msgConfig( MsgCommand.registerSuccess.value(), "000000", DeviceType.master.value(), "master");
                                        rUdpManager.sendResponseMsg(msgFlag, msg, fromAddress);

                                        if (exPhoneModel.getDeviceType() == DeviceType.treatAndNurse) {
                                            addTreatAndNurseExPhone(exPhoneModel);
                                        }
                                        else if (exPhoneModel.getDeviceType() == DeviceType.doorSide) {
                                            mobilePair(exPhoneModel); // 门口分机默认都是注册
                                        }

                                        STBLog.out("FROM","来自"+exPhoneModel.getIPAddress()+" "+exPhoneModel.getSip()+" 注册成功");
                                    }
                                    else { // 服务器注册失败
                                        final String msg = STBRequestParam.msgConfig( MsgCommand.registerFailed.value(), "", DeviceType.master.value(), "Request to server failed, please try again.");
                                        rUdpManager.sendResponseMsg(msgFlag, msg, fromAddress);
                                        STBLog.out("FROM","来自"+exPhoneModel.getIPAddress()+" "+exPhoneModel.getSip()+" 服务器注册失败");
                                    }
                                }

                                @Override
                                public void onFailure(Call<PostRequestCallModel> call, Throwable t) {
                                    final String msg = STBRequestParam.msgConfig( MsgCommand.registerFailed.value(), "", DeviceType.master.value(), "Request to server failed, please try again.");
                                    rUdpManager.sendResponseMsg(msgFlag, msg, fromAddress);
                                    STBLog.out("FROM","来自"+exPhoneModel.getIPAddress()+" "+exPhoneModel.getSip()+" 服务器注册失败");
                                }
                            });


                        }
                        else {// 校验失败返回注册失败
                            final String msg = STBRequestParam.msgConfig( MsgCommand.registerFailed.value(), "", DeviceType.master.value(), "Please check sip number and try.");
                            rUdpManager.sendResponseMsg(msgFlag, msg, fromAddress);
                            STBLog.out("FROM","来自"+exPhoneModel.getIPAddress()+" "+exPhoneModel.getSip()+" 注册失败");
                        }
                    }
                });
                ThreadPoolManager.getInstance().execute(new FutureTask<>(thread,null),null);
            }

            /**
             * 呼叫
             */
            private void call() {

                Thread thread =new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        final String sessionID = generatingSessionID(exPhoneModel.getSip(), fromAddress,paramsModel.paramsHeader.uuid);
                        final String msg = STBRequestParam.msgConfig(MsgCommand.callRecSessionID.value(), "", DeviceType.master.value(), "master", sessionID, null);
                        // 1.给发起呼叫分机返回会话id
                        rUdpManager.sendResponseMsg(msgFlag, msg, fromAddress);

                        handlers.get(1).post(refreshRunnable);
                        // 2.发送给医护分机会话列表
                        sendNursePhone();
                        STBLog.out("FROM","来自"+fromAddress.getHostAddress()+"呼叫"+"\nID:"+sessionID);
                    }
                };
                ThreadPoolManager.getInstance().execute(new FutureTask<>(thread,null),null);
            }

            /**
             * 接收呼叫
             */
            private void receiveCall() {
                Thread thread =new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        rUdpManager.sendResponseMsg(msgFlag,fromAddress);
                        refreshWaitSessionID(paramsModel.body.getSessionID());
                        handlers.get(1).post(refreshRunnable);
                        sendNursePhone();
                        update(paramsModel.body.getSessionID(), SessionIDState.response, fromAddress, paramsModel.paramsHeader.sip);
                        STBLog.out("FROM","来自"+fromAddress.getHostAddress()+"接听会话："+paramsModel.body.getSessionID()+" 告诉发起会话分机，电话将被接起");
                    }
                };
                ThreadPoolManager.getInstance().execute(new FutureTask<>(thread,null),null);
            }

            /**
             * 取消呼叫
             */
            private void cancelCall() {
                Thread thread =new Thread(){
                    @Override
                    public void run() {
                        super.run();

                        final String msg = STBRequestParam.msgConfig(MsgCommand.cancelCallSuccess.value(), "", DeviceType.master.value(), "master", null, null);
                        // 1.给发起取消分机返回取消成功
                        rUdpManager.sendResponseMsg(msgFlag,msg,fromAddress);

                        refreshWaitSessionID(paramsModel.body.getSessionID());
                        // 2.发送给医护分机会话列表
                        handlers.get(1).post(refreshRunnable);
                        sendNursePhone();

                        update(paramsModel.body.getSessionID(), SessionIDState.cancel, fromAddress, paramsModel.paramsHeader.sip);

                        STBLog.out("FROM",fromAddress.getHostAddress()+"主动取消呼叫"+paramsModel.body.getSessionID());
                    }
                };
                ThreadPoolManager.getInstance().execute(new FutureTask<>(thread,null),null);

            }

            /**
             * 挂断
             */
            private void hangup() {
                Thread thread =new Thread(){
                    @Override
                    public void run() {
                        super.run();

                        rUdpManager.sendResponseMsg(msgFlag,fromAddress);

                        final SessionIDModel sessionIDModel = getSessionIDModel(paramsModel.body.getSessionID());
                        if (sessionIDModel != null) {
                            final String msg = STBRequestParam.msgConfig(MsgCommand.hanguped.value(), "", DeviceType.master.value(), "master", null, null);
                            rUdpManager.sendMsg(msg, sessionIDModel.getFromIPAddress(), new RUdpCallBack() {
                                @Override
                                public void onFailure(String callMsg, IOException e) {
                                    STBLog.out("SRUdp","给发起呼叫分机发送"+MsgCommand.hanguped+"命令超时");
                                }

                                @Override
                                public void onResponse(String CallMsg, String response) throws IOException {
                                    STBLog.out("SRUdp","给发起呼叫分机发送"+MsgCommand.hanguped+"命令OK");
                                }
                            });
                        }

                        refreshWaitSessionID(paramsModel.body.getSessionID());
                        handlers.get(1).post(refreshRunnable);
                        sendNursePhone();

                        update(paramsModel.body.getSessionID(), SessionIDState.callEnd, fromAddress, paramsModel.paramsHeader.sip);

                        STBLog.out("FROM",fromAddress.getHostAddress()+"主动挂断"+paramsModel.body.getSessionID());
                    }
                };
                ThreadPoolManager.getInstance().execute(new FutureTask<>(thread,null),null);

            }

            /**
             * 心跳
             */
            private void heart() {

                Thread thread =new Thread(){
                    @Override
                    public void run() {
                        super.run();

                        STBLog.out("CHEART", ""+refusePhones.size()+exPhoneModel.getState());
                        if (refusePhones.size() > 0) { // 如果心跳分机与拒绝列表内ip重复则不再存入心跳列表
                            for (int i = 0; i < refusePhones.size(); i++) {
                                ExPhoneModel exM = refusePhones.get(i);
                                if (exM.isEqual(exPhoneModel) && TextUtils.equals(exM.getSip(),exPhoneModel.getSip())) {
                                    return;
                                }
                            }
                        }

                        if (exPhoneModel.getDeviceType() == DeviceType.treatAndNurse) {
                            addTreatAndNurseExPhone(exPhoneModel);
                        }else if (exPhoneModel.getDeviceType() == DeviceType.doorSide) {
                            mobilePair(exPhoneModel); // 门口分机默认都是注册
                        }

                        keepHeartAline(exPhoneModel);
                        addRegister(exPhoneModel,false);

                    }
                };
                ThreadPoolManager.getInstance().execute(new FutureTask<>(thread,null),null);
            }

            /**
             * 注册配对分机
             */
            private void registerOfDoctor() {

                Thread thread =new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        mobilePair(exPhoneModel);
                        final String msg = STBRequestParam.msgConfig(MsgCommand.registerOfDoctorSuccess.value(), "", DeviceType.master.value(), "master", null, null);
                        rUdpManager.sendResponseMsg(msgFlag,msg,fromAddress);
                        STBLog.out("FROM",fromAddress.getHostAddress()+"配对移动分机");
                    }
                };
                ThreadPoolManager.getInstance().execute(new FutureTask<>(thread,null),null);
            }

            /**
             * 取消配对
             */
            private void unregisterOfDoctor() {
                Thread thread =new Thread(){
                    @Override
                    public void run() {
                        super.run();

                        mobileRemove(exPhoneModel);
                        final String msg = STBRequestParam.msgConfig(MsgCommand.unregisterOfDoctorSuccess.value(), "", DeviceType.master.value(), "master", null, null);
                        rUdpManager.sendResponseMsg(msgFlag,msg,fromAddress);
                        STBLog.out("FROM",fromAddress.getHostAddress()+"取消移动分机");
                    }
                };
                ThreadPoolManager.getInstance().execute(new FutureTask<>(thread,null),null);
            }

            /**
             * 拒绝
             */
            private void refusingToanswer() {
                Thread thread =new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        rUdpManager.sendResponseMsg(msgFlag,fromAddress);// 响应发起拒绝分机

                        //发送拒绝给发起呼叫床头
                        final SessionIDModel sessionIDModel = getSessionIDModel(paramsModel.body.getSessionID());
                        final String msg = STBRequestParam.msgConfig(MsgCommand.refusingToanswer.value(), "", DeviceType.master.value(), "master", paramsModel.body.getSessionID(), null);
                        if (sessionIDModel != null)
                            rUdpManager.sendMsg(msg, sessionIDModel.getFromIPAddress(), new RUdpCallBack() {
                                @Override
                                public void onFailure(String callMsg, IOException e) {
                                    STBLog.out("SRUdp",sessionIDModel.getCallsip() + "发起呼叫被拒绝超时");
                                }

                                @Override
                                public void onResponse(String CallMsg, String response) throws IOException {
                                    STBLog.out("SRUdp",sessionIDModel.getCallsip() + "发起呼叫被拒绝成功");
                                }
                            });
                        // 更新医护与配对状态
                        refreshWaitSessionID(paramsModel.body.getSessionID());
                        handlers.get(1).post(refreshRunnable);
                        sendNursePhone();
                        update(paramsModel.body.getSessionID(), SessionIDState.refuse, fromAddress, paramsModel.paramsHeader.sip);
                    }
                };
                ThreadPoolManager.getInstance().execute(new FutureTask<>(thread,null),null);
            }

            /**
             * 获取门口分机
             */
            private void fetchDoorSideIPAddress() {

                Thread thread =new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        STBLog.out("FetchIP",paramsModel.paramsHeader.sip+"@"+fromAddress);
                        String remoteIPAddress = getDoorSideIPAddress(exPhoneModel.getSip());
                        STBLog.out("FetchIP","IP: "+remoteIPAddress);
                        if (remoteIPAddress == null) {
                            rUdpManager.sendResponseMsg(msgFlag,fromAddress);
                        } else {
                            final String msg = STBRequestParam.msgConfig(MsgCommand.cmp.value(), "", DeviceType.master.value(), remoteIPAddress);
                            rUdpManager.sendResponseMsg(msgFlag, msg, fromAddress);
                        }
                    }
                };
                ThreadPoolManager.getInstance().execute(new FutureTask<>(thread,null),null);
            }

            /**
             * 数据处理
             * @param msgFlag
             * @param result
             * @param fromAddress
             */
            @Override
            public void dataResponse(String msgFlag, String result, InetAddress fromAddress) {

                STBLog.out("SRUdp", result);

                if (TextUtils.equals(msgFlag,"refresh")) {
                    // 服务器要求刷新
                    return;
                }

                try {
                    paramsModel = ParamsModel.getParamsHeader(result);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                if (paramsModel == null) return;

                exPhoneModel = new ExPhoneModel(paramsModel.paramsHeader.sip,paramsModel.paramsHeader.deviceType,fromAddress.getHostAddress(), ExPhoneModel.State.online);
                this.msgFlag = msgFlag;
                this.fromAddress = fromAddress;

                switch (paramsModel.paramsHeader.msgCommand.value()) {

                    case 10003:
                        register();
                        break;// 注册
                    case 10001:
                        call();
                        break;// 呼叫 from 床头
                    case 10005:
                        receiveCall();
                        break;// from 配对分机 与医护分机
                    case 10013:
                        cancelCall();
                        break;// from 床头主动取消
                    case 10009:
                        hangup();
                        break;// 挂断
                    case 103:
                        heart();
                        break;// 心跳
                    case 10019:
                        registerOfDoctor();
                        break;// 注册成为移动分机
                    case 10021:
                        unregisterOfDoctor();
                        break;// 取消注册
                    case 10011:
                        refusingToanswer();
                        break;// from 医护分机 配对分机
                    case 126:
                        fetchDoorSideIPAddress();
                        break;// 获取对应门口分机地址
                }
            }
        });
    }

    /**
     * 获取床头对应门口分机
      * @param sipNumber
     * @return
     */
    private String getDoorSideIPAddress(String sipNumber) {

        if (mobilesPhones.size() > 0) {
            for (int i = 0; i< mobilesPhones.size(); i++) {
                ExPhoneModel mobileModel = mobilesPhones.get(i);
                if (TextUtils.equals(mobileModel.getSip().substring(0,4), sipNumber.substring(0,4))) {
                    return mobileModel.getIPAddress();
                }
            }
        }

        return null;
    }

    /**
     * 添加医护分机
     * @param exPhoneModel
     */
    private void addTreatAndNurseExPhone(ExPhoneModel exPhoneModel) {

        if (treatAndNurseModels.size() == 0)
            treatAndNurseModels.add(exPhoneModel);
        else {
            boolean isNotContains = true;
            for (int i = 0; i < treatAndNurseModels.size(); i++) {
                ExPhoneModel exModel = treatAndNurseModels.get(i);
                if (exModel.isEqual(exPhoneModel)) {
                    isNotContains = false;
                    break;
                }
            }
            if (isNotContains)
                treatAndNurseModels.add(exPhoneModel);
        }
    }

    /**
     * 发送给医护分机
     */
    private void sendNursePhone() {

        CMC.CMCRequestParam.Body.Builder cmcBodyBuilder = CMC.CMCRequestParam.Body.newBuilder();

        // 发送给医护分机
        for (int i = 0; i < waitSendSessionArray.size(); i++) {
            SessionIDModel strModel = waitSendSessionArray.get(i);
            cmcBodyBuilder.addLists(strModel == null ? theLastSessionIDModel.getSessionID() : strModel.getSessionID());

            // sip:号码 strModel.getCallsip();
        }

        final String msg = STBRequestParam.rMsgConfig(MsgCommand.receiveBroadcast.value(), "", DeviceType.master.value(), "master", null, cmcBodyBuilder);

        if (treatAndNurseModels.size() == 1) {
            sendSingleNursePhone(msg,treatAndNurseModels.get(0));
        }
        else {
            for (int i = 0; i < treatAndNurseModels.size(); i++) {
                ExPhoneModel exPhoneModel = treatAndNurseModels.get(i);
                sendSingleNursePhone(msg,exPhoneModel);
            }
        }

        // 发送给服务器

    }

    /**
     * 发送单个医护分机
     * @param msg
     * @param exPhoneModel
     */
    private void sendSingleNursePhone(String msg, final ExPhoneModel exPhoneModel) {
        InetAddress inetAddress = STBIPAddress.iNetAddress(exPhoneModel.getIPAddress());
        if (inetAddress != null) {
            rUdpManager.sendMsg(msg, inetAddress, new RUdpCallBack() {
                @Override
                public void onFailure(String callMsg, IOException e) {
                    STBLog.out("SRUdp", "发送医护分机超时" + exPhoneModel.getIPAddress());
                }

                @Override
                public void onResponse(String CallMsg, String response) throws IOException {
                    STBLog.out("SRUdp", "发送医护分机成功" + exPhoneModel.getIPAddress());
                }
            });
        }
    }

    /**
     * 线程移除
     */
    public void threadClear() {
        mRunning = false;
        if (this.handlers == null || this.runnableArrayList == null) return;
        if (this.handlers.size() == this.runnableArrayList.size()) {
            for (int i = 0; i < this.handlers.size(); i++) {
                handlers.get(i).removeCallbacks(runnableArrayList.get(i));
                handlerThreads.get(i).quit();
            }
        }

        handlerThreads.clear();
        handlers.clear();
        runnableArrayList.clear();

        STBLog.out("threadClear", "threadClear");
        rUdpManager.destroy();
    }



    /**
     *   线程开启
      */
    private ArrayList<Handler> handlers;
    private ArrayList<Runnable> runnableArrayList;
    private ArrayList<HandlerThread> handlerThreads;
    private boolean mRunning = true;
    private void startThreads() {

        handlers = new ArrayList<>(4);
        runnableArrayList = new ArrayList<>(4);
        handlerThreads = new ArrayList<>(4);

        runnableArrayList.add(mWaitSessionRunnable);
        runnableArrayList.add(refreshRunnable);
        runnableArrayList.add(syncHeartRunnable);
        runnableArrayList.add(syncOffLineRunnable);

        // 检查等待会话 检查心跳 检查失败请求 检查发送广播失败线程
        String[] threadNames = {
                "WaitSessionThread",
                "RefreshHandlerThread",
                "RoundThread",
                "SyncHeartThread"};

        for (int i = 0; i < threadNames.length; i ++) {
            HandlerThread thread = new HandlerThread(threadNames[i]);
            handlerThreads.add(thread);
            thread.start();//创建一个HandlerThread并启动它

            Handler handler = new Handler(thread.getLooper());
            handler.post(runnableArrayList.get(i));
            handlers.add(handler);
        }
    }

    /**
     * 5分钟同步一次心跳列表
     */
    private void syncHeart() {

        STBLog.out("updateStatus","----------");

        if (registerPhones.size() > 0) {

            UpdateStatusModel updateStatusModel = new UpdateStatusModel();
            updateStatusModel.list = new ArrayList<>(registerPhones.size());
            for (int i = 0; i < registerPhones.size(); i++) {

                ExPhoneModel exPhoneModel = registerPhones.get(i);
                UpdateStatusModel.StatusModel statusModel = new UpdateStatusModel.StatusModel();
                statusModel.device_ip = exPhoneModel.getIPAddress();
                statusModel.device_sip = exPhoneModel.getSip();
                statusModel.device_status = String.valueOf(exPhoneModel.getState().value());
                statusModel.device_type = String.valueOf(exPhoneModel.getDeviceType().value());
                updateStatusModel.list.add(statusModel);
            }

            STBLog.out("updateStatus",updateStatusModel.toString());

            requestInterfaceWith(getInterface().updateStatus(updateStatusModel));
        }
    }

    /**
     * 获取服务器时间日期
     */
//    private void fetchDateTimeFromServer() {
//
//        JSONObject header = new JSONObject();
//        JSONObject reqJson = new JSONObject();
//        try {
//            header.put("funcode", FunCode.updateDateTime.value());
//            reqJson.put("header", header);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        final String requestJsonString = reqJson.toString();
//        MasterHttpHelper.Post(requestJsonString, new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                MasterHttpHelper.saveFailedInfoToDB(requestJsonString);
//            }
//
//            @Override
//            public void onResponse(@Nullable Call call, @Nullable Response response) throws IOException {
//
//                ResponseBody responseBody = response.body();
//                if (responseBody != null) {
//                    final String responseStr = responseBody.string();
//                    STBLog.out("UPDATE DATE AND TIME", "result: "+responseStr);
//
//                    try {
//                        UpdateRecordModel jsonObject = new UpdateRecordModel(responseStr);
//                        final String resultCode = jsonObject.resultCode();
//                        final String successCode = "0";
//                        if (TextUtils.equals(resultCode, successCode)) {
//                            // 获取信息成功
//                            String dateTime = jsonObject.resultDateTime();
//                            // 设置当前机器系统时间日期
//                            try {
//                                SimpleDate.Sys.setDateTime(context, Long.valueOf(SimpleDate.dateToStamp(dateTime)));
//                            } catch (ParseException e) {
//                                e.printStackTrace();
//                            }
//                            // 保存
//                            LastUpdateDateTime = dateTime;
//                            LastCheckTime = dateTime;
//                        }
//                        else {
//                            MasterHttpHelper.saveFailedInfoToDB(requestJsonString);
//                        }
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//    }

    // TODO: 检查日期时间
    private String LastUpdateDateTime = "";
    private String LastCheckTime = "";
    public void checkDateTime() {
        if (TextUtils.equals("",LastUpdateDateTime)) {
            // 获取服务器时间并更新本地时间
//            fetchDateTimeFromServer();
        }
        else {
            // 检查上次更新时间
            try {

                int halfAMonth = 1000*60*60*24*1;//一天的时间
                Long lastCheckTime = Long.valueOf(SimpleDate.dateToStamp(LastCheckTime));
                Long lastUpdateTime = Long.valueOf(SimpleDate.dateToStamp(LastUpdateDateTime));
                long overTime = lastCheckTime - lastUpdateTime;
                STBLog.out("UPDATE DATE AND TIME", String.format("LastCheckTime: %s - LastUpdateTime : %s  = %d , halfTime:%d", lastCheckTime, lastUpdateTime, overTime,halfAMonth));

                if (lastCheckTime - lastUpdateTime >= halfAMonth) {
//                    fetchDateTimeFromServer();// 更新
                }
                else {
                    LastCheckTime = SimpleDate.getCurrentDate();
                    STBLog.out("UPDATE DATE AND TIME", String.format("当前时间: %s",LastCheckTime));
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查失败请求
     */
    private void checkReqFailed() {

        List<FailedRequestJsonModel> lists = MasterDBDao.getInstance().queryFailedModels();
        int listSize = lists.size();
        if (listSize > 0) {

            for (int i = 0; i < listSize; i++) {

                FailedRequestJsonModel failedRequestJsonModel = lists.get(i);
                MasterDBDao.getInstance().removeFailed(failedRequestJsonModel);

                try { sleep(300); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
        }


    }

    /**
     * 检查离线分机
     */
    private void checkOffline() {

        int checkTime = 3000;

        for (int i = 0; i < registerPhones.size(); i ++) {
            final ExPhoneModel exPhoneModel = registerPhones.get(i);
            long overTime = System.currentTimeMillis() - exPhoneModel.getTheLastHeartTime();
            if (overTime > checkTime) {
                STBLog.out("PPT", "第"+i+"次：设备："+exPhoneModel.getState()+" "+exPhoneModel.getDeviceType()+exPhoneModel.getSip()+"已离线"+overTime);
                exPhoneModel.setOffLineTimes(exPhoneModel.getOffLineTimes()+1);
                if (exPhoneModel.getState() == ExPhoneModel.State.online) {
                    if (exPhoneModel.getOffLineTimes() >= 3) {

                        exPhoneModel.setState(ExPhoneModel.State.offline);
                        exPhoneModel.setOffLineTimes(0);
                        MasterDBDao.getInstance().insertExModel(exPhoneModel);// 插入数据库
                        STBLog.out("PPT", "设备："+exPhoneModel.getState()+exPhoneModel.getDeviceType()+exPhoneModel.getSip()+"已离线"+"3,将上传至服务器,重置次数.");

                        // 2.发送给服务器
                        Thread sendToServerThread = new Thread(new Runnable() {
                            @Override
                            public void run() {

                                UpdateStatusModel updateStatusModel = new UpdateStatusModel();
                                updateStatusModel.list = new ArrayList<>(registerPhones.size());
                                for (int i = 0; i < registerPhones.size(); i++) {

                                    UpdateStatusModel.StatusModel statusModel = new UpdateStatusModel.StatusModel();
                                    statusModel.device_ip = exPhoneModel.getIPAddress();
                                    statusModel.device_sip = exPhoneModel.getSip();
                                    statusModel.device_status = String.valueOf(exPhoneModel.getState().value());
                                    statusModel.device_type = String.valueOf(exPhoneModel.getDeviceType().value());
                                    updateStatusModel.list.add(statusModel);
                                }

                                STBLog.out("updateStatus","checkOffline: "+updateStatusModel.toString());

                                requestInterfaceWith(getInterface().updateStatus(updateStatusModel));

                            }
                        });
                        ThreadPoolManager.getInstance().execute(new FutureTask<>(sendToServerThread,null),null);



                        try { sleep(300); }
                        catch (InterruptedException e) { e.printStackTrace(); }
                    }
                }
            }
        }
    }

    // 同步心跳
    private Runnable syncHeartRunnable = new Runnable() {
        @Override
        public void run() {
            syncHeart();
            handlers.get(2).postDelayed(syncHeartRunnable,60 * 1 * 1000);
        }
    };

    // 检测离线
    private Runnable syncOffLineRunnable = new Runnable() {
        @Override
        public void run() {
            checkOffline();
            handlers.get(3).postDelayed(syncOffLineRunnable, 60 * 1 * 1000);
        }
    };

    /**
     * 等待发送会话线程
     */
    private Runnable mWaitSessionRunnable = new Runnable() {
        private int millis = 9000;// 下次给配对分机发送时间

        @Override
        public void run() {
            while(mRunning){

                sendMsgToMobilePair();

                try {  sleep(30); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
        }

        /**
         * 给配对分机发送会话
         */
        private void sendMsgToMobilePair() {

            if (waitSendSessionArray.size() > 0 && mobilesPhones.size() > 0)
            {
                theLastSessionIDModel = waitSendSessionArray.remove(0);//取出第一次
                if (!theLastSessionIDModel.getSessionID().contains("999")) {
                    sendMobiles(theLastSessionIDModel.getSessionID());
                    waitSendSessionArray.add(theLastSessionIDModel);//完毕后，添加
                }
                else {
                    waitSendSessionArray.add(theLastSessionIDModel);//完毕后，添加
                }

                try { sleep(millis); }
                catch (InterruptedException o) { o.printStackTrace(); }
            }
        }
    };

    /**
     * sessionIDs列表为空通知所有门口
      * @param theLastSessionID
     */
    public void sendMobiles(String theLastSessionID) {

        final String msg = STBRequestParam.msgConfig(MsgCommand.receiveBroadcastSingle.value(), "000000", DeviceType.master.value(), "master", theLastSessionID, null);
        // 1.发送广播 -> 移动->医护分机

        for (int i = 0; i < mobilesPhones.size(); i++) {
            final ExPhoneModel exModel = mobilesPhones.get(i);
            rUdpManager.sendMsg(msg, STBIPAddress.iNetAddress(exModel.getIPAddress()), new RUdpCallBack() {
                @Override
                public void onFailure(String callMsg, IOException e) {
                    STBLog.out("SRUdp","发送配对消息: "+exModel.getIPAddress()+" 超时");
                }

                @Override
                public void onResponse(String CallMsg, String response) {
                    STBLog.out("SRUdp","发送配对消息: "+exModel.getIPAddress()+" OK");
                }
            });
            try { sleep(30); }
            catch (InterruptedException o) { o.printStackTrace(); }
        }
    }

    /**
     * 向服务器注册 -> 需要回调注册结果　｜｜　如果是本身注册，结果直接通知
     * @param exPhoneModel
     */
    public Call<PostRequestCallModel> registerToServer(final ExPhoneModel exPhoneModel) {

        RegisterModel registerModel = new RegisterModel();
        if (exPhoneModel == null) {
            registerModel.device_ip = STBIPAddress.getLocalHostIp();
            registerModel.device_type = String.valueOf(DeviceType.master.value());
        }
        else {
            registerModel.device_type = String.valueOf(exPhoneModel.getDeviceType().value());
            registerModel.device_ip = exPhoneModel.getIPAddress();
            registerModel.device_sip = exPhoneModel.getSip();
            registerModel.ward_id = exPhoneModel.getSip().substring(0,2);
            registerModel.room_no = exPhoneModel.getSip().substring(2,5);
            registerModel.bed_no = exPhoneModel.getSip().substring(5,8);
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        PostRequestInterface requestInterface = retrofit.create(PostRequestInterface.class);
        Call<PostRequestCallModel> call = requestInterface.register(registerModel);
        return call;
    }


    /**
     * 加载离线数据
     */
    private void loadCacheData() {
        List<ExPhoneModel> lists = MasterDBDao.getInstance().queryExModels();
        if (lists.size() > 0) {
            MasterDBDao.getInstance().clearExModels();

            for (int i = 0; i < lists.size(); i++) {
                ExPhoneModel em = lists.get(i);
                em.setState(ExPhoneModel.State.online); // 重新检测是否在线
                em.setTheLastHeartTime();
                em.setOffLineTimes(3);
                addRegister(em, false);
            }
        }
    }

    private List<ExPhoneModel> registerPhones = Collections.synchronizedList(new ArrayList(500));// 床头 门口
    private List<ExPhoneModel> mobilesPhones  = Collections.synchronizedList(new ArrayList(200));// 注册床头、门口
    private List<ExPhoneModel> refusePhones   = Collections.synchronizedList(new ArrayList(500));// 拒绝心跳

    /**
     * Context给数据库用
     * @return
     */
    public static Context getContext() {
        return context;
    }

    /**
     * 添加配对分机 -> 移动分机配对
     * @param exphoneModel
     */
    private void mobilePair(ExPhoneModel exphoneModel) {

        if (mobilesPhones.size() > 0) {
            Boolean isContains = false;
            for (int i = 0; i < mobilesPhones.size(); i++) {
                ExPhoneModel mo = mobilesPhones.get(i);
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
        STBLog.out("ppt","mobilesPhones: size = "+mobilesPhones.size());
    }

    /**
     * 移除配对分机 -> 移动分机配对
     * @param exphoneModel
     */
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

    /**
     * 心跳在线处理 - 只改状态
     * @param exPhoneModel
     */
    private void keepHeartAline(final ExPhoneModel exPhoneModel) {

        for (int i = 0; i < registerPhones.size(); i++) {

            ExPhoneModel em = registerPhones.get(i);
            if (em.isEqual(exPhoneModel) && TextUtils.equals(em.getSip(),exPhoneModel.getSip())) {// 同ip 同sip

                if (em.getState() == ExPhoneModel.State.offline) { // 如果是已经离线，发送在线消息
                    STBLog.out("PPT", "设备: " + exPhoneModel.getDeviceType() + " " + exPhoneModel.getSip() + "已经在线");
                    em.setState(ExPhoneModel.State.online);

                    // 2.发送至服务器
                    Thread sendToServerThread = new Thread(new Runnable() {
                        @Override
                        public void run() {

                            UpdateStatusModel updateStatusModel = new UpdateStatusModel();
                            updateStatusModel.list = new ArrayList<>(registerPhones.size());
                            for (int i = 0; i < registerPhones.size(); i++) {

                                UpdateStatusModel.StatusModel statusModel = new UpdateStatusModel.StatusModel();
                                statusModel.device_ip = exPhoneModel.getIPAddress();
                                statusModel.device_sip = exPhoneModel.getSip();
                                statusModel.device_status = String.valueOf(exPhoneModel.getState().value());
                                statusModel.device_type = String.valueOf(exPhoneModel.getDeviceType().value());
                                updateStatusModel.list.add(statusModel);
                            }

                            STBLog.out("updateStatus","keepHeartAline: "+updateStatusModel.toString());

                            requestInterfaceWith(getInterface().updateStatus(updateStatusModel));
                        }
                    });
                    ThreadPoolManager.getInstance().execute(new FutureTask<>(sendToServerThread,null),null);

                }
                em.setTheLastHeartTime();
                break;
            }
        }

        // 从数据库移除
        List<ExPhoneModel> exPhoneModels = MasterDBDao.getInstance().queryExModels();
        if (exPhoneModels.size() > 0) {
            for (int i = 0; i < exPhoneModels.size(); i++) {
                ExPhoneModel em = exPhoneModels.get(i);
                if (em.isEqual(exPhoneModel)) {
                    MasterDBDao.getInstance().removeExPhoneModel(em);
                    break;
                }
            }
        }

    }

    /**
     * 上传呼叫记录
     * @param sessionIDModel
     */
    private void updateRecord(SessionIDModel sessionIDModel) {

        UpdateRecordModel updateRecordModel = new UpdateRecordModel();
        updateRecordModel.device_sip = sessionIDModel.getCallsip();
        updateRecordModel.call_ip = sessionIDModel.getFromIPAddress().getHostAddress();
        updateRecordModel.to_call_ip = sessionIDModel.getToIPAddress().getHostAddress();
        updateRecordModel.call_begin_time = sessionIDModel.getGeneratingDate();
        updateRecordModel.call_receive_time = sessionIDModel.getSureDate();
        updateRecordModel.call_end_time = sessionIDModel.getCallEndDate();

        STBLog.out("updateRecord",sessionIDModel.toString());

        requestInterfaceWith(getInterface().updateCallRecord(updateRecordModel));

    }

    private PostRequestInterface getInterface() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        PostRequestInterface requestInterface = retrofit.create(PostRequestInterface.class);
        return requestInterface;
    }

    private void requestInterfaceWith(Call<PostRequestCallModel> call) {
        call.enqueue(new Callback<PostRequestCallModel>() {
            @Override
            public void onResponse(Call<PostRequestCallModel> call, Response<PostRequestCallModel> response) {
                if (response.body().getCode() == successCode) {
                    STBLog.out("requestInterfaceWith","success: code:"+response.body().toString());
                }
                else {
                    STBLog.out("requestInterfaceWith","failed: "+response.body().toString());
                }
            }

            @Override
            public void onFailure(Call<PostRequestCallModel> call, Throwable t) {
                STBLog.out("requestInterfaceWith","error: "+t.toString());
            }
        });
    }


    /**
     * 注册分机 初始化注册调用
     */
    private int index = 0;
    private void addRegister(ExPhoneModel exPhoneModel, boolean isRegister) {

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
                    final ExPhoneModel oldExPhoneModel = registerPhones.get(i);

                    //sip相同 ip不同
                    if (TextUtils.equals(oldExPhoneModel.getSip(), exPhoneModel.getSip()) && !oldExPhoneModel.isEqual(exPhoneModel)) {
                        // 如果旧设备在线
                        if (oldExPhoneModel.getState() == ExPhoneModel.State.online) {

                            // 通知旧设备
                            String str = STBRequestParam.msgConfig(MsgCommand.SIPConflict.value(), "", DeviceType.master.value(), "");
                            rUdpManager.sendMsg(str, oldExPhoneModel.IPAddress(), new RUdpCallBack() {// 发送冲突消息给旧设备
                                @Override
                                public void onFailure(String callMsg, IOException e) {
                                    STBLog.out("SIPConflict", "SIP注册有冲突"+oldExPhoneModel.getIPAddress()+" 超时");
                                }

                                @Override
                                public void onResponse(String CallMsg, String response) throws IOException {
                                    STBLog.out("SIPConflict", "SIP注册有冲突"+oldExPhoneModel.getIPAddress()+" OK");
                                }
                            });
                            // 添加到拒绝列表
                            refusePhones.add(oldExPhoneModel);
                        }
                        registerPhones.remove(i);
                        STBLog.out("CREG", "SIP相同 IP不同 "+registerPhones.size()+" 当前删除设备: "+oldExPhoneModel.getState().toString()+" "+oldExPhoneModel.getIPAddress() + " "+oldExPhoneModel.getSip()+" "+oldExPhoneModel.getDeviceType().toString());
                        continue;
                    }

                    //sip不同 ip相同
                    if (oldExPhoneModel.isEqual(exPhoneModel) && !TextUtils.equals(oldExPhoneModel.getSip(), exPhoneModel.getSip())) {//IP相同
                        registerPhones.remove(i);
                        STBLog.out("CREG", "IP相同 ： "+registerPhones.size()+" 当前删除设备: "+oldExPhoneModel.getState().toString()+" "+oldExPhoneModel.getIPAddress() + " "+oldExPhoneModel.getSip()+" "+oldExPhoneModel.getDeviceType().toString());
                        continue;
                    }

                    //sip相同 ip相同
                    if (oldExPhoneModel.isEqual(exPhoneModel) && TextUtils.equals(oldExPhoneModel.getSip(), exPhoneModel.getSip())) {
                        isNotContains = false;
                        break;
                    }
                }

                if (isNotContains) {
                    registerPhones.add(exPhoneModel);
                }
                STBLog.out("CREG", "- "+registerPhones.size()+" 当前注册设备: "+exPhoneModel.getState().toString()+" "+exPhoneModel.getIPAddress() + " "+exPhoneModel.getSip()+" "+exPhoneModel.getDeviceType().toString());
                for (int i = 0; i < registerPhones.size(); i++) {
                    ExPhoneModel e = registerPhones.get(i);
                    STBLog.out("CREG", "register phone: "+e.getState().toString()+" "+e.getIPAddress() + " "+e.getSip()+" "+e.getDeviceType().toString());
                }

            }

        }
        // 心跳过来
        else {
            checkRepeat(exPhoneModel);
            STBLog.out("CHEART", "<start print COUNT: "+registerPhones.size()+"\n");
            for (int i = 0; i < registerPhones.size(); i++) {
                ExPhoneModel e = registerPhones.get(i);
                STBLog.out("CHEART", "phone: "+e.getState().toString()+" "+e.getIPAddress() + " "+e.getSip()+" "+e.getDeviceType().toString());
            }
            STBLog.out("CHEART", "end print>\n");
        }

        STBLog.out("INDEX", String.valueOf(index) + "  " + registerPhones.size());
        index++;
    }

    /**
     * 心跳过滤
     * @param newExPhoneModel
     */
    private void checkRepeat(ExPhoneModel newExPhoneModel) {

        // 注册设备列表内过滤重复
        Boolean isNotContains = true;
        if (registerPhones.size() > 0) {
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
                    oldExPhoneModel.setTheLastHeartTime();
                    oldExPhoneModel.setOffLineTimes(0);
                    oldExPhoneModel.setState(ExPhoneModel.State.online);
                    isNotContains = false;
                    break;
                }
            }
        }

        if (isNotContains)
            registerPhones.add(newExPhoneModel);
    }

    /**
     * 更新等待发送列表
     * @param sessionID
     */
    private void refreshWaitSessionID(String sessionID) {
        // 只要进入更新状态都等于需要在等待发送列表内移除
        if (waitSendSessionArray.size() > 0) {
            for (int i = 0; i < waitSendSessionArray.size(); i++) {
                SessionIDModel curModel = waitSendSessionArray.get(i);
                if (curModel != null) {
                    if (TextUtils.equals(curModel.getSessionID(), sessionID)) {
                        waitSendSessionArray.remove(i);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 更新会话组各会话状态
     * @param sessionID
     * @param state
     * @param toIPAddress
     * @param toSip
     */
    private void update(String sessionID, SessionIDState state, InetAddress toIPAddress, String toSip) {

        // 所有的会话对象列表状态更新
        for (int i = 0; i < sessionIDModels.size(); i++) {
            SessionIDModel sessionIDModel = sessionIDModels.get(i);
            if (TextUtils.equals(sessionIDModel.getSessionID(), sessionID)) {
                sessionIDModel.setSessionIDState(state);
                sessionIDModel.setToIPAddress(toIPAddress);
                sessionIDModel.setTosip(toSip);
                STBLog.out("ppt", sessionIDModel.getCallsip() + " "+ sessionIDModel.getSessionID() + " "+sessionIDModel.getSessionIDState().toString());
                if (state == SessionIDState.callEnd) {
                    sessionIDModel.setCallEndDate(SimpleDate.getCurrentDate());
                    // 1.上传呼叫记录
                    updateRecord(sessionIDModel);
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

                // 2.移除
                sessionIDModels.remove(i);
                STBLog.out("Length","size: "+sessionIDModels.size());
                break;
            }
        }
    }

    /**
     * 根据会话id获得会话模型
     * @param sessionID
     * @return
     */
    private SessionIDModel getSessionIDModel(String sessionID) {
        for (int i = 0; i < sessionIDModels.size(); i++) {
            SessionIDModel m = sessionIDModels.get(i);
            if (TextUtils.equals(m.getSessionID(), sessionID)) {
                return m;
            }
        }
        return null;
    }

    /**
     * 生成会话id
     */
    private ArrayList<SessionIDModel> sessionIDModels = new ArrayList<>(1000);
    private String generatingSessionID(String sip, InetAddress IPAddress, String nurseLevel) {

        String oldSessionID = waitSessionIsContains(sip); // 从等待发送的列表内查找是否已经有呼叫
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
        waitSendSessionArray.add(sessionIDModel);

        return sessionID;
    }

    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshSessionIDs();
        }
    };

    /**
     * 确定所有会话都是有效的
     */
    private void refreshSessionIDs() {

        handlers.get(0).removeCallbacks(mWaitSessionRunnable);// 防止队列正在操作sessionIDs 先移除队列

        if (waitSendSessionArray.size() > 0) {
            handlers.get(0).post(mWaitSessionRunnable);// 添加队列
        } else {
            sendMobiles("");
            theLastSessionIDModel = null;
        }
    }

    private SessionIDModel theLastSessionIDModel;

    /*
    等待转发的会话models
     */
    public ArrayList<SessionIDModel> waitSendSessionArray = new ArrayList<>(100);


    /*
     检查是否有重复sip
     */
    private String waitSessionIsContains(String sip) {

        String oldSessionID = null;
        if (waitSendSessionArray.size() == 0) {
            return oldSessionID;
        }

        for(int index = 0; index < waitSendSessionArray.size(); index ++) {
            SessionIDModel currentModel = waitSendSessionArray.get(index);
            if (TextUtils.equals(currentModel.getCallsip(), sip)) {
                oldSessionID = currentModel.getSessionID();
                return oldSessionID;
            }
        }

        return oldSessionID;
    }
}