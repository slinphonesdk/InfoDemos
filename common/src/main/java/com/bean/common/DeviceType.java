package com.bean.common;

import java.util.Vector;

public class DeviceType
{
    private static Vector<DeviceType> values = new Vector();
    private final int mValue;
    private final String mStringValue;
    public static final DeviceType bedHeader = new DeviceType(101, "Bed Header");
    public static final DeviceType doorSide = new DeviceType(102, "Door Side");
    public static final DeviceType bathroom = new DeviceType(104, "Treat And Nurse");
    public static final DeviceType treatAndNurse = new DeviceType(106, "Treat And Nurse");
    public static final DeviceType master = new DeviceType(109, "Master");

    public final int value() {
        return this.mValue;
    }

    private DeviceType(int value, String stringValue) {
        this.mValue = value;
        values.addElement(this);
        this.mStringValue = stringValue;
    }

    public static DeviceType fromInt(int value) {
        for(int i = 0; i < values.size(); ++i) {
            DeviceType state = values.elementAt(i);
            if (state.mValue == value) {
                return state;
            }
        }

        throw new RuntimeException("type not found [" + value + "]");
    }

    public String toString() {
        return this.mStringValue;
    }
}
