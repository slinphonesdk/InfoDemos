package org.linphone.telpovoip;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.bean.common.DeviceType;
import com.bean.common.FunCode;
import com.bean.common.MsgCommand;
import com.bean.common.ParamsModel;
import com.bean.common.STBIPAddress;
import com.bean.common.STBRequestParam;
import com.bean.common.SimpleDate;
import com.bean.common.rudp.RUdpCallBack;
import com.bean.common.rudp.RUdpListener;
import com.bean.common.rudp.RUdpManager;
import com.google.protobuf.InvalidProtocolBufferException;
import com.telpo.sdk.SipAccountHelper;
import com.telpo.sdk.SipCallHelper;
import com.telpo.sdk.SipDeviceHelper;
import com.telpo.sdk.VoIPManager;
import com.telpo.sdk.VoIPService;
import com.telpo.sdk.v2.SipListener;
import com.telpo.sdk.v2.model.GlobalState;
import com.telpo.sdk.v2.model.RegistrationState;
import com.telpo.sdk.v2.model.SipCall;
import com.telpo.telephony.project.model.CallLog;


import org.json.JSONException;
import org.json.JSONObject;
import org.linphone.LinphoneManager;
import org.linphone.LinphonePreferences;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.util.AccountUtil;
import org.linphone.util.XUtil;


import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ExPhoneManager {

    private String sipNumber = "";
    private DeviceType deviceType;
    private String ext;
    private Context context;
    private static String hostIP = "192.168.88.253";
    private static int port = 5060;
    private RUdpManager rUdpManager;
    private ExPhoneListener exPhoneListener;

    // TODO: 初始化方法
    public ExPhoneManager(Context context, String serverIP, int serverPort) {
        if (serverIP != null) hostIP = serverIP;
        if (serverPort != 0) port = serverPort;
        this.context = context;
        udpSetup();
        sipSetup();
    }

    public ExPhoneManager(Context context) {
        this(context, null, 0);
    }

    public void addListener(ExPhoneListener exPhoneListener) {
        this.exPhoneListener = exPhoneListener;
    }

    // TODO: 床头分机获取对应门口分机IP
    public void fetchDoorSideIPAddress(RUdpCallBack rUdpCallBack) {
        String msg = STBRequestParam.msgConfig(MsgCommand.fetchDoorSideIPAddress.value(), this.sipNumber, this.deviceType.value(), "");
        rUdpManager.sendMsg(msg,hostAddress(),rUdpCallBack);
    }

    // TODO: 床头向门口发送开灯指令
    public void bSendOpenLight(String extMsg,String remoteIPAddress, RUdpCallBack rUdpCallBack) {
        switchLight(true,extMsg,remoteIPAddress,rUdpCallBack);
    }

    // TODO: 床头向门口发送关灯指令
    public void bSendCloseLight(String extMsg,String remoteIPAddress, RUdpCallBack rUdpCallBack) {
        switchLight(false,extMsg,remoteIPAddress,rUdpCallBack);
    }

    private void switchLight(Boolean isOpen, String extMsg,String remoteIPAddress, RUdpCallBack rUdpCallBack) {
        MsgCommand msgCommand = isOpen ? MsgCommand.lightOpen : MsgCommand.lightClose;
        String msg = STBRequestParam.msgConfig(msgCommand.value(), this.sipNumber, this.deviceType.value(), extMsg);
        InetAddress inetAddress = STBIPAddress.inetAddress(remoteIPAddress);
        if (inetAddress != null)
            rUdpManager.sendMsg(msg,inetAddress,rUdpCallBack);
        else
            rUdpCallBack.onFailure("ERROR", new IOException("RemoteIPAddress is error."));
    }

    // TODO: 停止发送心跳
    public void stopSendHeart() {
        if (handler != null) {
            this.handler.removeCallbacks(this.keepHeartRunnable);
        }
    }

    // TODO: 向信息主机注册
    public void registerToInfoMaster(final String sipNumber, DeviceType deviceType, String ext, final RUdpCallBack rUdpCallBack)
    {
        stopSendHeart();
        this.sipNumber = sipNumber;
        this.deviceType = deviceType;
        this.ext = ext;
        RUdpCallBack rUdpCall = new RUdpCallBack() {
            @Override
            public void onFailure(String callMsg, IOException e) {
                rUdpCallBack.onFailure(callMsg,e);
            }

            @Override
            public void onResponse(String CallMsg, String response) throws IOException {
                rUdpCallBack.onResponse(CallMsg,response);
                responseData(response);
            }
        };
        String msg = STBRequestParam.msgConfig(MsgCommand.register.value(), this.sipNumber, this.deviceType.value(), this.ext);
        rUdpManager.sendMsg(msg, hostAddress(), rUdpCall);
    }

    private void responseData(String result) throws InvalidProtocolBufferException {

        ParamsModel paramsModel = ParamsModel.getParamsHeader(result);
        if (paramsModel == null) return;

        if (paramsModel.paramsHeader.msgCommand == MsgCommand.registerSuccess) {
            Log.e("SIPReg","成功");
            registerSipNumber();
            ExPhoneManager.this.keepHeart();
        }
        else if (paramsModel.paramsHeader.msgCommand == MsgCommand.registerFailed) {
            Log.e("SIPReg","失败");
        }
    }

    // TODO: udp 数据接收
    public void addUdpListener(RUdpListener rUdpListener) {
        rUdpManager.addListener(rUdpListener);
    }

    // TODO: RUdp setup
    private void udpSetup() {
        rUdpManager = new RUdpManager();
    }

    public boolean autoAnswer = false;

    // TODO: sip 初始化
    private void sipSetup()
    {

        VoIPManager.createManager(context);
        LinphonePreferences.instance().setSipPort(5060);
        SipDeviceHelper.muteMic(true);


        LinphoneManager.getLc().addListener(new LinphoneCoreListenerBase() {
            public void registrationState(LinphoneCore linphoneCore, LinphoneProxyConfig linphoneProxyConfig, LinphoneCore.RegistrationState registrationState, String s) {
                if (exPhoneListener != null) {
                    exPhoneListener.registrationState(registrationState.toString(), s);
                }

            }

            public void callState(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCall.State state, String s) {
                if (state == LinphoneCall.State.IncomingReceived && autoAnswer) {
                    acceptCall();
                }

                if (exPhoneListener != null) {
                    exPhoneListener.callState(linphoneCall.getRemoteAddress().asStringUriOnly(), state.value(), s);
                }

            }
        });
    }

    public void acceptCall() {
        LinphoneCall[] calls = LinphoneManager.getLc().getCalls();
        if (calls.length > 0) {

            for (int i = 0; i < calls.length; i++) {
                LinphoneCall call = calls[i];
                if (LinphoneCall.State.IncomingReceived == call.getState()) {
                    try {
                        LinphoneManager.getLc().acceptCall(call);
                    } catch (LinphoneCoreException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    private void registerSipNumber() {

        if (ExPhoneManager.this.sipNumber.length() > 0) {
            LinphoneManager.getLc().clearAuthInfos();
            LinphoneManager.getLc().clearProxyConfigs();

            AccountUtil.AccountBuilder builder = (new AccountUtil.AccountBuilder(LinphoneManager.getLc()))
                    .setUsername(sipNumber)
                    .setDomain(hostIP + ":" + this.port)
                    .setPassword("123456")
                    .setTransport(LinphoneAddress.TransportType.LinphoneTransportUdp);

            try {
                builder.saveNewAccount();
            } catch (LinphoneCoreException e) {
                e.printStackTrace();
            }
        }
    }

    // TODO: 信息主机地址
    private static InetAddress hostAddress() {
        InetAddress serverAddress = null;
        try {
            serverAddress = InetAddress.getByName(hostIP);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return serverAddress;
    }

    // TODO: 响应收到
    public void response(String msgFlag, InetAddress fromAddress) {
        rUdpManager.sendResponseMsg(msgFlag,fromAddress);
    }

    // TODO: 注册成为移动分机
    public void registerMobile(RUdpCallBack rUdpCallBack)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.registerOfDoctor.value(), this.sipNumber, this.deviceType.value(), this.ext);
        rUdpManager.sendMsg(msg,hostAddress(), rUdpCallBack);
    }

    // TODO: 取消注册为非移动分机
    public void unregisterMobile(RUdpCallBack rUdpCallBack)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.unregisterOfDoctor.value(), this.sipNumber, this.deviceType.value(), this.ext);
        rUdpManager.sendMsg(msg,hostAddress(), rUdpCallBack);
    }

    // TODO: 卫浴分机呼叫
    public void bathroomCall(RUdpCallBack rUdpCallBack)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.call.value(), this.sipNumber.substring(0, 4) + "999", DeviceType.bathroom.value(), "0");
        rUdpManager.sendMsg(msg,hostAddress(), rUdpCallBack);
    }

    // TODO: 取消呼叫
    public void cancelCall(String sessionID, RUdpCallBack rUdpCallBack)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.cancelCall.value(), this.sipNumber.substring(0, 4) + "999", DeviceType.bathroom.value(), this.ext, sessionID, null);
        rUdpManager.sendMsg(msg,hostAddress(), rUdpCallBack);
    }

    // TODO: 床头发起呼叫
    public void call(String nurseLevel, RUdpCallBack rUdpCallBack)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.call.value(), this.sipNumber, this.deviceType.value(), nurseLevel);
        rUdpManager.sendMsg(msg,hostAddress(), rUdpCallBack);
    }

    // TODO: 医护分机主动呼叫
    public void  sipCall(String sipNumber) {
        outCallNumber(sipNumber);
    }

    private void outCallNumber(String sipNumber) {
        XUtil.newOutgoingCall(String.format("sip:%s@%s",sipNumber,hostIP+":"+port),"",false);
    }

    // TODO: 拒绝通话
    public void refusingToAnswer(String sessionID, RUdpCallBack rUdpCallBack)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.refusingToanswer.value(), this.sipNumber, this.deviceType.value(), this.ext, sessionID, null);
        rUdpManager.sendMsg(msg,hostAddress(), rUdpCallBack);
    }

    // TODO: 接听
    public final void acceptCall(String sessionID, final RUdpCallBack rUdpCallBack)
    {
        final String sipNumber = getReceiveSipNumber(sessionID);
        if (!TextUtils.equals("999", sipNumber.substring(4, 7)))
        {
            if (LinphoneManager.getLc().isIncall()) {
                LinphoneManager.getLc().terminateAllCalls();
            }
        }
        RUdpCallBack udpCallBack = new RUdpCallBack() {
            @Override
            public void onFailure(String callMsg, IOException e) {
                rUdpCallBack.onFailure(callMsg,e);
            }

            @Override
            public void onResponse(String CallMsg, String response) throws IOException {
                rUdpCallBack.onResponse(CallMsg,response);
                outCallNumber(sipNumber);
            }
        };

        String msg = STBRequestParam.msgConfig(MsgCommand.receiveCall.value(), sipNumber, this.deviceType.value(), this.ext, sessionID, null);
        rUdpManager.sendMsg(msg,hostAddress(),udpCallBack);
    }

    // TODO: 挂断
    public final void hangup(String sessionID, final RUdpCallBack rUdpCallBack) {

        RUdpCallBack udpCallBack = new RUdpCallBack() {
            @Override
            public void onFailure(String callMsg, IOException e) {
                rUdpCallBack.onFailure(callMsg,e);
            }

            @Override
            public void onResponse(String CallMsg, String response) throws IOException {
                rUdpCallBack.onResponse(CallMsg,response);
                sipHangup();
            }
        };

        String msg = STBRequestParam.msgConfig(MsgCommand.hangup.value(), this.sipNumber, this.deviceType.value(), this.ext, sessionID, null);
        rUdpManager.sendMsg(msg,hostAddress(), udpCallBack);
    }

    // TODO: sip 通话挂断
    public final void sipHangup() {
        LinphoneManager.getLc().terminateAllCalls();
    }

    // TODO: 从会话ID内获取SIP号码
    private String getReceiveSipNumber(String sessionID)
    {
        return sessionID.substring(0, 7);
    }

    // TODO: 向服务器注册
    private int serverPort = 9600;
    private static final MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
    public void registerToServer()
    {
        JSONObject header = new JSONObject();
        JSONObject body = new JSONObject();
        JSONObject requestB = new JSONObject();
        try
        {
            header.put("funcode", FunCode.register.value());
            header.put("reqtime", SimpleDate.getCurrentDate());
            header.put("deviceip", STBIPAddress.getLocalHostIp());

            body.put("deviceip", STBIPAddress.getLocalHostIp());
            body.put("devicesip", this.sipNumber);
            if (this.sipNumber.length() > 6) {
                body.put("swardid", this.sipNumber.substring(0, 2));
            }
            if (this.deviceType != DeviceType.treatAndNurse)
            {
                body.put("sroomid", this.sipNumber.substring(2, 4));
                body.put("sbedno", this.sipNumber.substring(4, 7));
            }
            body.put("devicetypeid", this.deviceType.value());

            requestB.put("header", header);
            requestB.put("body", body);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(mediaType, requestB.toString());

        Request request = new Request.Builder().url(FunCode.urlString(FunCode.host, serverPort)).post(requestBody).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback()
        {
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("EX", "向服务器注册异常");
            }

            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException
            {
                ResponseBody responseBody = response.body();
                if (responseBody != null)
                {
                    String responseStr = responseBody.string();
                    Log.e("EX", "向服务器注册:" + responseStr);
                }
            }
        });
    }

    private HandlerThread thread = new HandlerThread("KeepHeart");
    private Handler handler;

    // TODO: 开启心跳
    private void keepHeart()
    {
        if (!this.thread.isAlive()) {
            this.thread.start();
        }
        if (this.handler == null) {
            this.handler = new Handler(this.thread.getLooper());
        }
        this.handler.postDelayed(keepHeartRunnable,10000L);
    }

    private Runnable keepHeartRunnable = new Runnable()
    {
        public void run()
        {
            String msg = STBRequestParam.msgConfig(MsgCommand.heart.value(), ExPhoneManager.this.sipNumber, ExPhoneManager.this.deviceType.value(), ExPhoneManager.this.ext);
            rUdpManager.sendHeartMsg(msg,hostAddress());
            handler.postDelayed(keepHeartRunnable,10000L);
        }
    };

    public void destroy()
    {
        VoIPManager.destroyManager();

        stopSendHeart();
        thread.quit();
        thread = null;
        handler = null;
        rUdpManager.destroy();
        rUdpManager = null;
        Log.e("THREAD", "clear");
    }

}
