package server.com.rextension;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.bean.common.DeviceType;
import com.bean.common.MsgCommand;
import com.bean.common.ParamsModel;
import com.bean.common.STBIPAddress;
import com.bean.common.STBRequestParam;
import com.bean.common.rudp.RUdpCallBack;
import com.bean.common.rudp.RUdpListener;
import com.bean.common.rudp.RUdpManager;
import com.bean.common.utils.STBLog;
import com.google.protobuf.InvalidProtocolBufferException;
import org.linphone.RLinkPhoneListener;
import org.linphone.SLinPhoneSDK;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ExPhoneManager {

    private String sipNumber = "";
    private DeviceType deviceType;
    private String ext;
    private Context context;
    private static String hostIP = "192.168.88.253";
    private static int port = 5060;
    private RUdpManager rUdpManager;
    private ExPhoneListener exPhoneListener;
    private SLinPhoneSDK sLinPhoneSDK;

    /***
     * 初始化方法
     * @param context
     * @param serverIP 服务器iP地址
     * @param serverPort 服务器端口
     */
    public ExPhoneManager(Context context, String serverIP, int serverPort) {
        if (serverIP != null) hostIP = serverIP;
        if (serverPort != 0) port = serverPort;
        this.context = context;
        udpSetup();
        sipSetup();
        STBLog.out("IP",hostIP+":"+port);
    }

    public ExPhoneManager(Context context) {
        this(context, null, 0);
    }

    /***
     * @param exPhoneListener
     */
    public void addListener(ExPhoneListener exPhoneListener) {
        this.exPhoneListener = exPhoneListener;
    }

    /***
     * 设置麦克风增益
     * @param f 0 - 20
     */
    public void setMicrophoneGain(float f) {
        sLinPhoneSDK.setMicrophoneGain(f);
    }

    /***
     * 床头分机获取对应门口分机IP
      * @param rUdpCallBack
     */
    public void fetchDoorSideIPAddress(RUdpCallBack rUdpCallBack) {
        String msg = STBRequestParam.msgConfig(MsgCommand.fetchDoorSideIPAddress.value(), this.sipNumber, this.deviceType.value(), "");
        rUdpManager.sendMsg(msg,hostAddress(),rUdpCallBack);
    }

    /***
     * 床头向门口发送开灯指令
      * @param extMsg
     * @param remoteIPAddress
     * @param rUdpCallBack
     */
    public void bSendOpenLight(String extMsg,String remoteIPAddress, RUdpCallBack rUdpCallBack) {
        switchLight(true,extMsg,remoteIPAddress,rUdpCallBack);
    }

    /***
     * 床头向门口发送关灯指令
     * @param extMsg
     * @param remoteIPAddress
     * @param rUdpCallBack
     */
    public void bSendCloseLight(String extMsg,String remoteIPAddress, RUdpCallBack rUdpCallBack) {
        switchLight(false,extMsg,remoteIPAddress,rUdpCallBack);
    }

    /***
     * 床头向门口发送开关灯指令
     * @param isOpen
     * @param extMsg
     * @param remoteIPAddress
     * @param rUdpCallBack
     */
    private void switchLight(Boolean isOpen, String extMsg,String remoteIPAddress, RUdpCallBack rUdpCallBack) {
        MsgCommand msgCommand = isOpen ? MsgCommand.lightOpen : MsgCommand.lightClose;
        String msg = STBRequestParam.msgConfig(msgCommand.value(), this.sipNumber, this.deviceType.value(), extMsg);
        InetAddress inetAddress = STBIPAddress.iNetAddress(remoteIPAddress);
        if (inetAddress != null)
            rUdpManager.sendMsgToClient(msg,inetAddress,rUdpCallBack);
        else
            rUdpCallBack.onFailure("ERROR", new IOException("RemoteIPAddress is error."));
    }

    /***
     * 停止发送心跳
     */
    public void stopSendHeart() {
        if (handler != null) {
            this.handler.removeCallbacks(this.keepHeartRunnable);
        }
    }

    /***
     * 向信息主机注册
     * @param sipNumber
     * @param deviceType
     * @param ext
     * @param rUdpCallBack
     */
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
        rUdpManager.sendMsgToServer(msg, hostAddress(), rUdpCall, 10000);
    }

    private void responseData(String result) throws InvalidProtocolBufferException {

        ParamsModel paramsModel = ParamsModel.getParamsHeader(result);
        if (paramsModel == null) return;

        if (paramsModel.paramsHeader.msgCommand == MsgCommand.registerSuccess) {
            Log.e("SIPReg","成功");
            sLinPhoneSDK.register(sipNumber);
            ExPhoneManager.this.keepHeart();
        }
        else if (paramsModel.paramsHeader.msgCommand == MsgCommand.registerFailed) {
            Log.e("SIPReg","失败");
        }
    }

    /***
     * udp 数据接收
     * @param rUdpListener
     */
    public void addUdpListener(RUdpListener rUdpListener) {
        rUdpManager.addListener(rUdpListener);
    }

    /***
     * RUdp setup
     */
    private void udpSetup() {
        rUdpManager = new RUdpManager(RUdpManager.ServiceType.client);
    }

    /***
     * sip 初始化
      */
    private void sipSetup()
    {
        sLinPhoneSDK = new SLinPhoneSDK(this.context, hostIP, port, new RLinkPhoneListener() {
            @Override
            public void callState(String s, int i, String s1) {
                if (exPhoneListener != null)
                    exPhoneListener.callState(s,i,s1);
            }

            @Override
            public void registrationState(String s, String s1) {
                if (exPhoneListener != null)
                    exPhoneListener.registrationState(s,s1);
            }
        });

//        LinphoneManager.getLc().clearAuthInfos();
//        LinphoneManager.getLc().clearProxyConfigs();
        if (ExPhoneManager.this.sipNumber.length() > 0) {
            sLinPhoneSDK.register(ExPhoneManager.this.sipNumber);
        }
    }

    /***
     * 信息主机地址
      * @return
     */
    private static InetAddress hostAddress() {
        InetAddress serverAddress = null;
        try {
            serverAddress = InetAddress.getByName(hostIP);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return serverAddress;
    }

    /***
     * 响应收到
      * @param msgFlag
     * @param fromAddress
     */
    public void response(String msgFlag, InetAddress fromAddress) {
        rUdpManager.sendResponseMsg(msgFlag,fromAddress);
    }

    /***
     * 注册成为移动分机
     * @param rUdpCallBack
     */
    public void registerMobile(RUdpCallBack rUdpCallBack)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.registerOfDoctor.value(), this.sipNumber, this.deviceType.value(), this.ext);
        rUdpManager.sendMsg(msg,hostAddress(), rUdpCallBack);
    }

    /***
     * 取消注册为非移动分机
     * @param rUdpCallBack
     */
    public void unregisterMobile(RUdpCallBack rUdpCallBack)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.unregisterOfDoctor.value(), this.sipNumber, this.deviceType.value(), this.ext);
        rUdpManager.sendMsg(msg,hostAddress(), rUdpCallBack);
    }

    /***
     * 卫浴分机呼叫
      * @param rUdpCallBack
     */
    public void bathroomCall(RUdpCallBack rUdpCallBack)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.call.value(), this.sipNumber.substring(0, 4) + "999", DeviceType.bathroom.value(), "0");
        rUdpManager.sendMsg(msg,hostAddress(), rUdpCallBack);
    }

    /***
     * 取消呼叫
     * @param sessionID
     * @param rUdpCallBack
     */
    public void cancelCall(String sessionID, RUdpCallBack rUdpCallBack)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.cancelCall.value(), this.sipNumber.substring(0, 4) + "999", DeviceType.bathroom.value(), this.ext, sessionID, null);
        rUdpManager.sendMsg(msg,hostAddress(), rUdpCallBack);
    }

    /***
     * 床头发起呼叫
     * @param nurseLevel
     * @param rUdpCallBack
     */
    public void call(String nurseLevel, RUdpCallBack rUdpCallBack)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.call.value(), this.sipNumber, this.deviceType.value(), nurseLevel);
        rUdpManager.sendMsg(msg,hostAddress(), rUdpCallBack);
    }

    /***
     * 医护分机主动呼叫
     * @param sipNumber
     */
    public void  sipCall(String sipNumber) {
        sLinPhoneSDK.callOutgoing(sipNumber);
    }

    /***
     * 拒绝通话
     * @param sessionID
     * @param rUdpCallBack
     */
    public void refusingToAnswer(String sessionID, RUdpCallBack rUdpCallBack)
    {
        String msg = STBRequestParam.msgConfig(MsgCommand.refusingToanswer.value(), this.sipNumber, this.deviceType.value(), this.ext, sessionID, null);
        rUdpManager.sendMsg(msg,hostAddress(), rUdpCallBack);
    }

    /***
     *  接听
     * @param sessionID
     * @param rUdpCallBack
     */
    public final void acceptCall(String sessionID, final RUdpCallBack rUdpCallBack)
    {
        final String sipNumber = getReceiveSipNumber(sessionID);
        if (sipNumber.contains("999")) return;
        RUdpCallBack udpCallBack = new RUdpCallBack() {
            @Override
            public void onFailure(String callMsg, IOException e) {
                rUdpCallBack.onFailure(callMsg,e);
            }

            @Override
            public void onResponse(String CallMsg, String response) throws IOException {
                rUdpCallBack.onResponse(CallMsg,response);
            }
        };

        sLinPhoneSDK.callOutgoing(sipNumber);

        String msg = STBRequestParam.msgConfig(MsgCommand.receiveCall.value(), sipNumber, this.deviceType.value(), this.ext, sessionID, null);
        rUdpManager.sendMsg(msg,hostAddress(),udpCallBack);
    }

    /***
     * 挂断
     * @param sessionID
     * @param rUdpCallBack
     */
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

    /***
     * sip 通话挂断
     */
    public final void sipHangup() {
        SLinPhoneSDK.hangup();
    }

    /***
     * 从会话ID内获取SIP号码
     * @param sessionID
     * @return
     */
    private String getReceiveSipNumber(String sessionID)
    {
        return sessionID.substring(0, 8);
    }


    private HandlerThread thread = new HandlerThread("KeepHeart");
    private Handler handler;

    /***
     * 开启心跳
     */
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
        if (sLinPhoneSDK != null)
            sLinPhoneSDK.destroy();

        stopSendHeart();
        thread.quit();
        thread = null;
        handler = null;
        rUdpManager.destroy();
        rUdpManager = null;
        Log.e("THREAD", "clear");
    }

}
