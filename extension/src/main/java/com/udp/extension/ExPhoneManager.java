package com.udp.extension;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.json.JSONException;
import org.json.JSONObject;
import org.linphone.LinphoneService;
import org.linphone.SLinPhoneSDK;
import org.linphone.SLinPhoneSDKListener;

import static java.lang.Thread.sleep;

public class ExPhoneManager
{

    private static String hostIP = "192.168.88.253";
    private static String port = "5060";
    private RegisterListener registerListener;
    private static final int receivePort = 10888;
    private static final int sendPort = 8888;
    private SenderDatagramSocket senderDatagramSocket;
    private ReceiverDatagramSocket receiverDatagramSocket;
    private String sipNumber;
    private DeviceType deviceType;
    private String uuid;
    private Context context;
    private boolean isUseDefaultSipServer = true;
    private HashMap<MsgCommand,String> waitSendMaps = new HashMap<>();

    private SharedPreferences.Editor dateEditor()
    {
        if (this.context == null) {
            return null;
        }
        SharedPreferences.Editor dataEditor = PreferenceManager.getDefaultSharedPreferences(this.context).edit();
        return dataEditor;
    }

    public void setDefaultHost(String host)
    {
        SharedPreferences.Editor dataEditor = dateEditor();
        if (dataEditor == null) {
            return;
        }
        dataEditor.putString("default_host", host);
        dataEditor.apply();
    }

    public void adjustVolume(int vol) {
        SLinPhoneSDK.getInstance().adjustVolume(vol);
    }

    public void adjustSoftwareVolume(int vol) {
        SLinPhoneSDK.getInstance().adjustSoftwareVolume(vol);
    }

    public void enableEchoCancellation(boolean isUse) {
        SLinPhoneSDK.getInstance().enableEchoCancellation(isUse);
    }

    public void setDefaultPort(int port)
    {
        SharedPreferences.Editor dataEditor = dateEditor();
        if (dataEditor == null) {
            return;
        }
        dataEditor.putInt("default_port", port);
        dataEditor.apply();
    }

    public String getDefaultHost()
    {
        return PreferenceManager.getDefaultSharedPreferences(this.context).getString("default_host", "192.168.88.3");
    }

    public int getDefaultPort()
    {
        return PreferenceManager.getDefaultSharedPreferences(this.context).getInt("default_port", 9600);
    }

    // TODO: 打开扬声器
    public void closeSpeaker() {
        SLinPhoneSDK.routeAudioToReceiver();
    }
    // TODO: 关闭扬声器
    public void openSpeaker() {
        SLinPhoneSDK.routeAudioToSpeaker();
    }

    // TODO: 卫浴分机呼叫
    public void bathroomCall()
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.call.value(), this.sipNumber.substring(0, 4) + "999", DeviceType.bathroom.value(), "0");
        this.senderDatagramSocket.sendMsgToHost(msg);
    }

    // TODO: 卫浴分机取消呼叫
    public void bathroomCancel(String sessionID)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.cancelCall.value(), this.sipNumber.substring(0, 4) + "999", DeviceType.bathroom.value(), this.uuid, sessionID, null);
        this.senderDatagramSocket.sendMsgToHost(msg);
    }

    // TODO: 医护分机直接呼叫床头
    public void call(String sipNumber)
    {
        SLinPhoneSDK.callOutgoing(sipNumber);
    }

    // TODO: 床头发起呼叫
    public void call()
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.call.value(), this.sipNumber, this.deviceType.value(), this.uuid);
        this.senderDatagramSocket.sendMsgToHost(msg);
    }

    // TODO: 床头发起呼叫 添加护理级别
    public void bedHeaderCallWith(String nurseLevel)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.call.value(), this.sipNumber, this.deviceType.value(), nurseLevel);
        this.senderDatagramSocket.sendMsgToHost(msg);
    }

    // TODO: sessionID 为空 or null -> sip电话挂断 如果当前分机为通话状态 挂断电话并发送挂断命令 else 发送取消呼叫命令
    public void hangupOrCancelCall(String sessionID)
    {
        if ((sessionID == null) || (TextUtils.equals(sessionID, ""))) {
            SLinPhoneSDK.hangup();
        }
        if (this.isInCalling.booleanValue())
        {
            SLinPhoneSDK.hangup();

            cancelOrHangup(MsgCommand.hangup, sessionID);
        }
        else
        {
            cancelOrHangup(MsgCommand.cancelCall, sessionID);
        }
    }

    // TODO: 发送取消或挂断命令
    private void cancelOrHangup(MsgCommand msgCommand, String sessionID)
    {
        String msg = STBRequestParam.msgConfig(msgCommand.value(), this.sipNumber, this.deviceType.value(), this.uuid, sessionID, null);
        this.senderDatagramSocket.sendMsgToHost(msg);
    }

    // TODO: 主动取消呼叫
    public void cancel(String sessionID)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.cancelCall.value(), this.sipNumber, this.deviceType.value(), this.uuid, sessionID, null);
        this.senderDatagramSocket.sendMsgToHost(msg);
    }

    private Boolean isRegister = Boolean.valueOf(false);
    private ExPhoneListener exPhoneListener;

    // TODO: 主动注册为移动配对分机
    public void registerOfTreatAndNurse()
    {
        this.isRegister = Boolean.valueOf(true);
        String msg = STBRequestParam.msgConfig(MsgCommand.registerOfDoctor.value(), this.sipNumber, this.deviceType.value(), this.uuid);
        this.senderDatagramSocket.sendMsgToHost(msg);
    }

    // TODO: 主动断开移动配对分机
    public void unregister()
    {
        this.isRegister = Boolean.valueOf(false);
        String msg = STBRequestParam.msgConfig(MsgCommand.unregisterOfDoctor.value(), this.sipNumber, this.deviceType.value(), this.uuid);
        this.senderDatagramSocket.sendMsgToHost(msg);
    }

    // TODO: 从会话ID内获取SIP号码
    private String getReceiveSipNumber(String sessionID)
    {
        return sessionID.substring(0, 7);
    }

    // TODO: 接听sessionID
    public final void acceptCall(String sessionID)
    {
        String sipNumber = getReceiveSipNumber(sessionID);
        if (!TextUtils.equals("999", sipNumber.substring(4, 7)))
        {
            SLinPhoneSDK.callOutgoing(sipNumber);
            this.isInCalling = Boolean.valueOf(true);
        }
        String msg = STBRequestParam.msgConfig(MsgCommand.receiveCall.value(), sipNumber, this.deviceType.value(), this.uuid, sessionID, null);
        this.senderDatagramSocket.sendMsgToHost(msg);
    }

    // TODO: 拒绝sessionID
    public void refusingToAnswer(String sessionID)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.refusingToanswer.value(), this.sipNumber, this.deviceType.value(), this.uuid, sessionID, null);
        this.senderDatagramSocket.sendMsgToHost(msg);
    }

    // TODO: 添加监听
    public void addListener(ExPhoneListener exPhoneListener)
    {
        this.exPhoneListener = exPhoneListener;
    }

    // TODO: 初始化方法
    public ExPhoneManager(Context context,Boolean isUseDefaultSipServer, String sipNumber, DeviceType deviceType, String uuid, String hostIP, String port, RegisterListener registerListener)
    {
        if (hostIP != null) {
            this.hostIP = hostIP;
        }
        if (port != null) {
            this.port = port;
        }
        this.isUseDefaultSipServer = isUseDefaultSipServer;
        this.registerListener = registerListener;
        this.deviceType = deviceType;
        this.sipNumber = sipNumber;
        this.uuid = uuid;
        this.context = context;
        setupSocket();
        if (isUseDefaultSipServer) {
            sipSetup();
        }
        registerToInfoMaster();
    }

    // TODO: 重新注册方法
    public void reConfig(String sipNumber, DeviceType deviceType, String uuid, RegisterListener registerListener)
    {
        if (handler != null) {
            this.handler.removeCallbacks(this.keepHeartRunnable);
        }
        this.registerListener = registerListener;
        this.deviceType = deviceType;
        this.sipNumber = sipNumber;
        this.uuid = uuid;
        registerToInfoMaster();
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
        this.handler.post(this.keepHeartRunnable);
    }

    private Boolean alwaysTrue = Boolean.valueOf(true);
    private Runnable keepHeartRunnable = new Runnable()
    {
        public void run()
        {
            while (ExPhoneManager.this.alwaysTrue.booleanValue())
            {
                String msg = STBRequestParam.msgConfig(MsgCommand.heart.value(), ExPhoneManager.this.sipNumber, ExPhoneManager.this.deviceType.value(), ExPhoneManager.this.uuid);
                ExPhoneManager.this.senderDatagramSocket.sendMsgToHost(msg);
                try
                {
                    sleep(10000L);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    };

    public void destroy()
    {
        this.alwaysTrue = Boolean.valueOf(false);
        this.handler.removeCallbacks(this.keepHeartRunnable);
        this.senderDatagramSocket.clear();
        this.receiverDatagramSocket.clear();
        Log.e("THREAD", "clear");
    }

    // TODO: 发送确定收到会话列表
    private void sendReceiveSessionIDs()
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.receiveSessionIDList.value(), this.sipNumber, this.deviceType.value(), this.uuid);
        this.senderDatagramSocket.sendMsgToHost(msg);
    }

    // TODO: 向信息主机注册
    private void registerToInfoMaster()
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.register.value(), this.sipNumber, this.deviceType.value(), this.uuid);
        this.senderDatagramSocket.sendMsgToHost(msg);

        HandlerThread thread = new HandlerThread("RegisterThread");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        handler.postDelayed(registerRunnable,3000);
    }

    private boolean registerIsNoneResponse = true;
    private Runnable registerRunnable = new Runnable() {
        @Override
        public void run() {
            if (registerIsNoneResponse) {
                if (ExPhoneManager.this.registerListener != null) {
                    ExPhoneManager.this.registerListener.state(false);
                }
            }
        }
    };

    private static final MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
    // TODO: 向服务器注册
    private void registerToServer()
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
        Log.e("REQ", requestB.toString());
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(mediaType, requestB.toString());

        Request request = new Request.Builder().url(FunCode.urlString(getDefaultHost(), getDefaultPort())).post(requestBody).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback()
        {
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException
            {
                ResponseBody responseBody = response.body();
                if (responseBody != null)
                {
                    String responseStr = responseBody.string();
                    Log.e("REQ", "REQ:" + responseStr);
                }
            }
        });
    }


    // TODO: Socket 初始化
    private void setupSocket()
    {
        try
        {
            this.senderDatagramSocket = new SenderDatagramSocket(8888);
            this.receiverDatagramSocket = new ReceiverDatagramSocket(10888);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        this.receiverDatagramSocket.addListener(new ReceiverDatagramSocketListener()
        {
            public void msgFromServer(String jsonStr) {}

            public void msgCallBack(ParamsHeader paramsHeader, src.CMC.CMCRequestParam.Body body, InetAddress fromAddress)
            {
                Log.e("ppt", "receive: \n" + paramsHeader.msgCommand
                        .toString() + " " + body + "\n" + fromAddress
                        .getHostAddress());
                if (body.getSessionID() == null) {
                    return;
                }
                if (ExPhoneManager.this.exPhoneListener != null) {
                    if (paramsHeader.msgCommand == MsgCommand.receiveBroadcastSingle)
                    {
                        if ((ExPhoneManager.this.isRegister.booleanValue()) && ((ExPhoneManager.this.deviceType == DeviceType.bedHeader) || (ExPhoneManager.this.deviceType == DeviceType.doorSide))) {
                            ExPhoneManager.this.exPhoneListener.sessionStatus(body.getSessionID());
                        }
                    }
                    else
                    {
                        ExPhoneManager.this.exPhoneListener.msgCallBack(paramsHeader, paramsHeader.msgCommand, body);
                        if ((paramsHeader.msgCommand == MsgCommand.receiveBroadcast) && (ExPhoneManager.this.deviceType == DeviceType.treatAndNurse)) {
                            ExPhoneManager.this.sendReceiveSessionIDs();
                        }
                    }
                }
                if (paramsHeader.msgCommand == MsgCommand.offline)
                {
                    if (ExPhoneManager.this.exPhoneListener != null) {
                        ExPhoneManager.this.exPhoneListener.keepLineState(PhoneLineState.offline, paramsHeader.sip);
                    }
                }
                else if (paramsHeader.msgCommand == MsgCommand.online)
                {
                    if (ExPhoneManager.this.exPhoneListener != null) {
                        ExPhoneManager.this.exPhoneListener.keepLineState(PhoneLineState.online, paramsHeader.sip);
                    }
                }
                else if (paramsHeader.msgCommand == MsgCommand.refusingToanswer) {
                    ExPhoneManager.this.isInCalling = Boolean.valueOf(false);
                }
                if (paramsHeader.msgCommand == MsgCommand.cancelCallSuccess)
                {
                    Log.e("ppt", "取消呼叫成功");
                }
                else if (paramsHeader.msgCommand == MsgCommand.registerSuccess)
                {

                    if (ExPhoneManager.this.registerListener != null) {
                        ExPhoneManager.this.registerListener.state(Boolean.valueOf(true));
                    }

                    ExPhoneManager.this.registerIsNoneResponse = false;
                    Log.e("ppt", "注册成功");
                    if (ExPhoneManager.this.isUseDefaultSipServer) {// 如果不是医护分机才注册sip服务
                        if (LinphoneService.isReady()) {
                            SLinPhoneSDK.register(ExPhoneManager.this.sipNumber);
                        }
                    }
                    ExPhoneManager.this.keepHeart();
                    ExPhoneManager.this.registerToServer();
                }
                else if (paramsHeader.msgCommand == MsgCommand.registerFailed)
                {
                    ExPhoneManager.this.registerIsNoneResponse = false;
                    Log.e("ppt", "注册失败");
                    if (ExPhoneManager.this.registerListener != null) {
                        ExPhoneManager.this.registerListener.state(Boolean.valueOf(false));
                    }
                }
                else if (paramsHeader.msgCommand == MsgCommand.SIPConflict)
                {
                    ExPhoneManager.this.handler.removeCallbacks(ExPhoneManager.this.keepHeartRunnable);
                    String msg = STBRequestParam.msgConfig(MsgCommand.receiveHeartStop.value(), ExPhoneManager.this.sipNumber, ExPhoneManager.this.deviceType.value(), ExPhoneManager.this.uuid);
                    ExPhoneManager.this.senderDatagramSocket.sendMsgToHost(msg);
                }
            }
        });
    }

    private Boolean isInCalling = Boolean.valueOf(false);
    private final String incomingReceived = "IncomingReceived";
    private final String callFailed = "Released";
    private final String callEnd = "CallEnd";

    // TODO: sip 初始化
    private void sipSetup()
    {
        SLinPhoneSDK.init(this.context, hostIP, port);
        SLinPhoneSDK.getInstance().addSDKListener(new SLinPhoneSDKListener()
        {
            public void serviceIsReady()
            {
                SLinPhoneSDK.register(ExPhoneManager.this.sipNumber);
            }

            public void callState(String s, String s1, String s2)
            {
                Log.e("ppt", "callState: " + s + "-" + s1 + "-" + s2);
                if (TextUtils.equals(s1, "IncomingReceived"))
                {
                    if (ExPhoneManager.this.exPhoneListener != null) {
                        ExPhoneManager.this.exPhoneListener.sipCallIncomingReceived();
                    }
                    ExPhoneManager.this.isInCalling = Boolean.valueOf(true);
                }
                else if (TextUtils.equals(s1, "CallEnd"))
                {
                    if (ExPhoneManager.this.exPhoneListener != null) {
                        ExPhoneManager.this.exPhoneListener.sipCallEnd();
                    }
                    ExPhoneManager.this.isInCalling = Boolean.valueOf(false);
                }
            }

            public void callState(String s, int i, String s1)
            {
                if (ExPhoneManager.this.exPhoneListener != null) {
                    ExPhoneManager.this.exPhoneListener.sipCallState(s, i, s1);
                }
            }

            public void registrationState(String s, String s1)
            {
                if (ExPhoneManager.this.exPhoneListener != null) {
                    ExPhoneManager.this.exPhoneListener.registrationState(s,s1);
                }
            }
        });
    }

    public interface RegisterListener
    {
        void state(Boolean paramBoolean);
    }
}
