package io.nms.notification.syslog;

public interface SysLogListener {
    void onSyslogMSG(String logEntry); 
}
