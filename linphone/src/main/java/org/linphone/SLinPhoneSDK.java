package org.linphone;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.Version;

import java.util.List;

import static android.content.Intent.ACTION_MAIN;
import static java.lang.Thread.sleep;

public class SLinPhoneSDK
{
    private RLinkPhoneListener rLinkPhoneListener;
    private String Host;
    private int port;
    private final LinphoneAddress.TransportType transportType = LinphoneAddress.TransportType.LinphoneTransportUdp;
    private Context mContext;

    public SLinPhoneSDK(Context context, String Host, int port, RLinkPhoneListener rLinkPhoneListener) {

        mContext = context;
        this.rLinkPhoneListener = rLinkPhoneListener;
        this.Host = Host;
        this.port = port;
        start(context);
    }

    // 设置麦克风增益
    public static void setMicrophoneGain(float f) {
        LinphoneManager.getLc().setMicrophoneGain(f);
    }

    private synchronized void start(Context context) {


        mHandler = new Handler();

        if (LinphoneService.isReady()) {
            onServiceReady();
        } else {
            // start linphone as background
            context.startService(new Intent(ACTION_MAIN).setClass(context, LinphoneService.class));
            mThread = new ServiceWaitThread();
            mThread.start();
        }
    }

    private Boolean autoAnswer = true;
    public void isAutoAnswer(Boolean bol) {
        autoAnswer = bol;
    }

    private static String defaultPwd = "123456";
    private String sipNumber = "";
    public void register(String phoneNumber) {

        sipNumber = phoneNumber;
        if (!LinphoneService.isReady()) return;

        LinphonePreferences.AccountBuilder builder = new LinphonePreferences.AccountBuilder(LinphoneManager.getLc())
                .setUsername(phoneNumber)
                .setDomain(Host+":"+port)
                .setPassword(defaultPwd)
                .setTransport(transportType);
        try {
            LinphoneManager.getLc().clearProxyConfigs();
            LinphoneManager.getLc().clearAuthInfos();
            builder.saveNewAccount();
        } catch (LinphoneCoreException e) {
            Log.e(e);
        }
    }

    // 回声消除开关
    public void enableEchoCancellation(Boolean bol) {
        LinphoneManager.getLc().enableEchoCancellation(bol);
    }

    public void callOutgoing(String number) {
        if (number == null || number.length() < 2)
            return;

        try {
            if (!LinphoneManager.getInstance().acceptCallIfIncomingPending()) {
                String to = String.format("sip:%s@%s", number, Host+":"+port);
                LinphoneManager.getInstance().newOutgoingCall(to, "");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                          sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        } catch (LinphoneCoreException e) {
            LinphoneManager.getInstance().terminateCall();
        }
    }

    // TODO: 调整音频
    public void adjustVolume(int vol) {

        LinphoneManager.getInstance().adjustVolume(vol);
    }

    public void adjustSoftwareVolume(int vol) {
        LinphoneManager.getLc().adjustSoftwareVolume(vol);
    }

    // TODO: 关闭扬声器
    public static void routeAudioToReceiver() {
        LinphoneManager.getInstance().routeAudioToReceiver();
    }

    // TODO: 打开扬声器
    public static void routeAudioToSpeaker() {
        LinphoneManager.getInstance().routeAudioToSpeaker();
    }

    public void callOutgoing(String number, String HOST) {
        if (number == null || number.length() < 2)
            return;

        try {
            if (!LinphoneManager.getInstance().acceptCallIfIncomingPending()) {
                String to = String.format("sip:%s@%s", number, HOST+":"+port);
                LinphoneManager.getInstance().newOutgoingCall(to, "");
            }
        } catch (LinphoneCoreException e) {
            LinphoneManager.getInstance().terminateCall();
        }
    }

    public static void hangup() {
        LinphoneManager.getInstance().terminateCall();
    }

    private LinphoneCall mCall;
    public void acceptCall() {

        List<LinphoneCall> calls = LinphoneUtils.getLinphoneCalls(LinphoneManager.getLc());
        if (calls.size() == 0)
            return;

        for (int i = 0; i < calls.size(); i++) {
            LinphoneCall call = calls.get(i);
            if (LinphoneCall.State.IncomingReceived == call.getState()) {
                mCall = call;
                break;
            }
        }

        LinphoneManager.getInstance().acceptCall(mCall);
    }

    // TODO: 添加会议
    public static void addCall() {

        List<LinphoneCall> calls = LinphoneUtils.getLinphoneCalls(LinphoneManager.getLc());
        if (calls.size() == 0)
            return;

        for (LinphoneCall call : calls) {

            if (LinphoneCall.State.IncomingReceived == call.getState()) {
                LinphoneManager.getInstance().acceptCall(call);
                LinphoneManager.getLc().addToConference(call);
            }
        }
    }

    public static synchronized Boolean isDestroyedOrNull() {
        return LinphoneManager.getLcIfManagerNotDestroyedOrNull() == null ? true : false;
    }

    public void destroy() {
        System.out.print("destroy  --------- ");
        mContext.stopService(new Intent(ACTION_MAIN).setClass(mContext, LinphoneService.class));
    }


    private Handler mHandler;
    private ServiceWaitThread mThread;

    protected void onServiceReady() {

        // We need LinphoneService to start bluetoothManager
        if (Version.sdkAboveOrEqual(Version.API11_HONEYCOMB_30)) {
            BluetoothManager.getInstance().initBluetooth();
        }

        // 如果本地用户是一个，查看sipNumber是否相同，不相同则重新注册
        // 如果本地用户不是一个，则重新注册
        LinphoneAuthInfo[] authInfos = LinphoneManager.getLc().getAuthInfosList();
        if (authInfos.length == 1) {
            LinphoneAuthInfo linphoneAuthInfo = authInfos[0];
            if (!TextUtils.equals(linphoneAuthInfo.getUsername(),sipNumber)) {
                register(sipNumber);
            }
        }else {
            register(sipNumber);
        }

//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                // 跳至主activity
//            }
//        }, 1000);

        LinphoneManager.getLc().addListener(new LinphoneCoreListenerBase() {

            @Override
            public void registrationState(LinphoneCore linphoneCore, LinphoneProxyConfig linphoneProxyConfig, LinphoneCore.RegistrationState registrationState, String s) {
                if (rLinkPhoneListener != null) {
                    rLinkPhoneListener.registrationState(registrationState.toString(), s);
                }
            }
            @Override
            public void callState(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCall.State state, String s) {

                if (state == LinphoneCall.State.IncomingReceived) {
                    if (autoAnswer)
                        acceptCall();
                }
                if (rLinkPhoneListener != null) {
                    rLinkPhoneListener.callState(linphoneCall.getRemoteAddress().getUserName(), state.value(), s);
                }
            }
        });
    }

    private class ServiceWaitThread extends Thread {
        public void run() {
            while (!LinphoneService.isReady()) {
                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException("waiting thread sleep() has been interrupted");
                }
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onServiceReady();
                }
            });
            mThread = null;
        }
    }

}
