package io.nms.notification.common;

import java.util.ArrayList;
import java.util.List;
import io.nms.notification.syslog.SysLogListener;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;



/*
 * Agent Verticle with AMQP and base operations
 * Extends AmqpVerticle 
 * Deployment sequence:
 * - create local Capabilities
 * - create AMQP connection
 * - publish Capabilities
 * - listen to Specifications
 */
public class AmqpAgentVerticle extends AmqpVerticle {
	List<SysLogListener> sysLogListeners = new ArrayList<SysLogListener>();
	DatagramSocket socket =  null;
	public void start(Promise<Void> fut) {
		super.start();

		agentName = config().getJsonObject("agent").getString("name", "");
		
	    moduleName = config().getJsonObject("module").getString("name", "[Unnamed]");
		capClasses = config().getJsonObject("module").getJsonArray("capabilities");
		
		host = config().getJsonObject("amqp").getString("host", "");
		port = config().getJsonObject("amqp").getInteger("port", 0);
		
		moduleConfig = config().getJsonObject("config");
		
		Future<Void> futConn = Future.future(promise -> initModule(promise));
		Future<Void> futInit = futConn
			.compose(v -> {
				return Future.<Void>future(promise -> createCapabilities(promise));
			})
			.compose(v -> {
				return Future.<Void>future(promise -> createAmqpConnection(promise));
			})
			.compose(v -> {
				return Future.<Void>future(promise -> publishCapabilities(promise));
			})
			.compose(v -> {
				return Future.<Void>future(promise -> subscribeToSpecifications2(promise));
			})
			.compose(v -> {
				return Future.<Void>future(promise -> startSysLogServer(promise));
			});
			futInit.onComplete(res -> {
				if (res.failed()) {
					fut.fail(res.cause());
				} else {
					vertx.setPeriodic(heartbeatMs, id -> {
				    	Future<Void> capsFut = Future.future(promise -> publishCapabilities(promise));
				    	capsFut.onComplete(r -> {
					        LOG.info("Capabilities updated.");
					    });
					});
					fut.complete();
				}
			});
	}
	public void registerListener(SysLogListener sysLogListener){
		sysLogListeners.add(sysLogListener);
	}

	public void startSysLogServer(Promise<Void> promise){
		socket = vertx.createDatagramSocket(new DatagramSocketOptions());
		socket.listen(1234, "0.0.0.0", asyncResult -> {
			if (asyncResult.succeeded()) {
				socket.handler(packet -> {
					String commandData = packet.data().getString(0, packet.data().length());
					// System.out.println("[UDP] Command received from {}:{}, length: {}, data: {}", packet.sender().host(), packet.sender().port(), packet.data().length(), commandData);
					System.out.println(commandData);
					for (SysLogListener sysLogListener: sysLogListeners) {
						sysLogListener.onSyslogMSG(commandData);
					}	
				});
			} else {
				System.out.println("Listen failed" + asyncResult.cause());
			}
		});
	}

	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		socket.close();
			try {
				super.stop(stopFuture);
			} catch (Exception e) {
				LOG.error("Error on stopping", e.getMessage());
			}
	}

}
