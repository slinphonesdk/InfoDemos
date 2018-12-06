package com.bean.common;

import java.util.Vector;

public class MsgCommand {
    private static Vector<MsgCommand> values = new Vector();
    private final int mValue;
    private final String mStringValue;
    public static final MsgCommand call = new MsgCommand(10001, "Call to Info. Master");
    public static final MsgCommand called = new MsgCommand(10002, "Call receive to Info. Master");
    public static final MsgCommand register = new MsgCommand(10003, "Register self to Info. Master");
    public static final MsgCommand registerFailed = new MsgCommand(108, "Register Failed to Info. Master");
    public static final MsgCommand registerSuccess = new MsgCommand(109, "Register Success to Info. Master");
    public static final MsgCommand unregister = new MsgCommand(10004, "Unregister self to Info. Master");
    public static final MsgCommand receiveCall = new MsgCommand(10005, "Receive Call from Info. Master");
    public static final MsgCommand receiveBroadcast = new MsgCommand(10007, "Receive Broadcast from Info. Master");
    public static final MsgCommand receiveBroadcastSingle = new MsgCommand(10008, "Receive Broadcast Single from Info. Master");
    public static final MsgCommand hangup = new MsgCommand(10009, "Hangup receive call info to Info. Master");
    public static final MsgCommand hanguped = new MsgCommand(10010, "Hangup ed receive call info to Info. Master");
    public static final MsgCommand refusingToanswer = new MsgCommand(10011, "Refusing to answer");
    public static final MsgCommand cancelCall = new MsgCommand(10013, "Cancel a call");
    public static final MsgCommand callRecSessionID = new MsgCommand(10014, "Call a call get sessionID");
    public static final MsgCommand receiveCancel = new MsgCommand(10015, "Receive a cancel call");
    public static final MsgCommand receiveRefusing = new MsgCommand(10017, "Receive a RefusingToanswer call");
    public static final MsgCommand registerOfDoctor = new MsgCommand(10019, "Register of doctor");
    public static final MsgCommand registerOfDoctorSuccess = new MsgCommand(10020, "Register of doctor success");
    public static final MsgCommand unregisterOfDoctor = new MsgCommand(10021, "Unregister of doctor");
    public static final MsgCommand unregisterOfDoctorSuccess = new MsgCommand(10022, "Unregister of doctor success");
    public static final MsgCommand callEnd = new MsgCommand(10023, "Call end");
    public static final MsgCommand heart = new MsgCommand(103, "Keep heart");
    public static final MsgCommand online = new MsgCommand(104, "Keep heart - Online");
    public static final MsgCommand offline = new MsgCommand(105, "Keep heart - Offline");
    public static final MsgCommand cmp = new MsgCommand(106, "Change a medical prescription");
    public static final MsgCommand receiveSessionIDList = new MsgCommand(107, "Receive sessionID list");
    public static final MsgCommand cancelCallSuccess = new MsgCommand(120, "Call Call Success");
    public static final MsgCommand SIPConflict = new MsgCommand(122, "SIP Address Conflict, will stop heart.");
    public static final MsgCommand receiveHeartStop = new MsgCommand(124, "Receive heart stop from extension.");
    public static final MsgCommand fetchDoorSideIPAddress = new MsgCommand(126, "Bed header extension phone fetch door side IPAddress from info master.");
    public static final MsgCommand lightOpen = new MsgCommand(128, "Light open");
    public static final MsgCommand lightClose = new MsgCommand(130, "Light close");


    public final int value() {
        return this.mValue;
    }

    private MsgCommand(int value, String stringValue) {
        this.mValue = value;
        values.addElement(this);
        this.mStringValue = stringValue;
    }

    public static MsgCommand fromInt(int value) {
        for(int i = 0; i < values.size(); ++i) {
            MsgCommand state = values.elementAt(i);
            if (state.mValue == value) {
                return state;
            }
        }

        throw new RuntimeException("state not found [" + value + "]");
    }

    public String toString() {
        return this.mStringValue;
    }
}