package com.udp.extension;

import java.util.Vector;

public class PhoneLineState
{
    @SuppressWarnings("unchecked")
    private static Vector<PhoneLineState> values = new Vector();
    private final int mValue;
    private final String mStringValue;
    public static final PhoneLineState online = new PhoneLineState(104, "Keep heart - Online");
    public static final PhoneLineState offline = new PhoneLineState(105, "Keep heart - Offline");
    public final int value() {
        return this.mValue;
    }

    private PhoneLineState(int value, String stringValue) {
        this.mValue = value;
        values.addElement(this);
        this.mStringValue = stringValue;
    }

    public static PhoneLineState fromInt(int value) {
        for(int i = 0; i < values.size(); ++i) {
            PhoneLineState state = values.elementAt(i);
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
