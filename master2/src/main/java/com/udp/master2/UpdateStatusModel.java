package com.udp.master2;

import java.util.List;

public class UpdateStatusModel {

    public List<StatusModel> list;

    public static class StatusModel {
        public String device_ip;//设备IP
        public String device_sip;//设备SIP
        public String device_status;//设备状态
        public String device_type;//设备类型
        public String remark;//备注
    }


}
