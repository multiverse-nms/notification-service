package io.nms.notification.taskmanager;

import java.sql.Timestamp;

import io.nms.notification.syslog.SysLogListener;

public interface AgentTaskListener { 
  void onResult(String taskId, short resultCode, Timestamp ts); 
  void onFinished(String taskId); 
  void registerListener(SysLogListener sysLogListener);
}
