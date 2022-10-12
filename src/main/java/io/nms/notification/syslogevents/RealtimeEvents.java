package io.nms.notification.syslogevents;

// import com.tailf.jnc.Device;
// import com.tailf.jnc.DeviceUser;
// import com.tailf.jnc.Element;
// import com.tailf.jnc.JNCException;
// import com.tailf.jnc.NetconfSession;
// import com.tailf.jnc.YangException;
// import java.net.InetAddress;
// import java.net.SocketException;
// import java.net.SocketTimeoutException;
// import java.util.Scanner;

import java.io.IOException;
import java.util.Date;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.sql.Timestamp;
import io.nms.notification.taskmanager.AbstractAgentTask;
import io.nms.notification.taskmanager.AgentTaskListener;
import io.nms.notification.common.AmqpAgentVerticle;
import io.nms.notification.constants.Errors;
import io.nms.notification.message.Message;
import io.nms.notification.syslog.SysLogListener;
import io.vertx.core.json.JsonObject;

public class RealtimeEvents extends AbstractAgentTask implements SysLogListener{

	private AgentTaskListener syslogServer = null;

	public RealtimeEvents(Message spec, JsonObject context) {
		super(spec, context); 
		verb = "collect";
		name = "oxc";
		label = "Probe collecting OXC events";		
		resultColumns = Arrays.asList("logEntry");
		role = "admin"; 
		taskPeriodMs = 0;
	}
	// implementation of Specification exec
	protected short executeSpec(AgentTaskListener t) {
		syslogServer = t;
		LOG.info("Real Time Events...");
		syslogServer.registerListener(this);
		return Errors.TASK_SUCCESS_NO_RESULT;
	}

	public void onSyslogMSG(String logEntry) {
		putSyslogResultValues(logEntry);
		Timestamp ts = new Timestamp(new Date().getTime());
		syslogServer.onResult(taskId, (short)Errors.TASK_SUCCESS, ts);
	}

    private void putSyslogResultValues(String logEntry) {
		resultValues.clear();
		List<String> resValRow = new ArrayList<String>();
		resValRow.addAll(specification.getResults());
		int ri = resValRow.indexOf("logEntry");
		if (ri >= 0) {
			resValRow.set(ri, logEntry);	
		}
		resultValues.add(resValRow);
	}

	//  private void putOXCNetConfNotificationResultValues(String mgmtIp, int port) throws YangException, JNCException, IOException {
	// 	List<String> resValRow = new ArrayList<String>();
	// 	resValRow.addAll(specification.getResults());
	// 	DeviceUser deviceUser = new DeviceUser("mvs", "admin", "root");
	// 	Device device = new Device("admin", deviceUser, mgmtIp, port);
	// 	device.connect("mvs");
	// 	device.newSession("mySession");
	// 	NetconfSession session = device.getSession("mySession");
	// 	session.createSubscription("Polatis");
	// 	while (true) {
	// 		Element notification = session.receiveNotification();
	// 		System.out.println(notification.toXMLString());
	// 	}
	// }
}
