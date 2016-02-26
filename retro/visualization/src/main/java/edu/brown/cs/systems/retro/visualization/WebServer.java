package edu.brown.cs.systems.retro.visualization;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.ConfigFactory;

import edu.brown.cs.systems.retro.aggregation.Callback;
import edu.brown.cs.systems.retro.aggregation.ClusterResources;
import edu.brown.cs.systems.retro.aggregation.JSON;
import edu.brown.cs.systems.retro.aggregation.Report.ResourceReport;

/**
 * A simple server that provides html and websocket connections to both
 * subscribe to and visualize resource usage
 * 
 * @author a-jomace
 */
public class WebServer {

    private static final Logger log = LoggerFactory.getLogger(WebServer.class);

    private static Map<PubSubProxyWebSocket, Boolean> sockets = new ConcurrentHashMap<PubSubProxyWebSocket, Boolean>();

    private static Callback callback = new Callback() {
        @Override
        protected void OnMessage(ResourceReport report) {
            String json = JSON.reportToJson(report);
            for (PubSubProxyWebSocket socket : sockets.keySet()) {
                socket.sendReport(json);
            }
        }
    };

    private static Server setupServer() throws Exception {
        // String webDir = "target/classes/webui";
        // String webDir = "src/main/resources/webui";
        String webDir = WebServer.class.getClassLoader().getResource("webui").toExternalForm();
        log.info("Base webdir is {}", webDir);

        int httpPort = ConfigFactory.load().getInt("resource-reporting.visualization.webui-port");
        log.info("Resource reporting web ui port is ", httpPort);

        // Create Jetty server
        Server server = new Server(httpPort);

        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed(true);
        resource_handler.setWelcomeFiles(new String[] { "filter.html" });
        resource_handler.setResourceBase(webDir);

        WebSocketHandler wsHandler = new WebSocketHandler.Simple(PubSubProxyWebSocket.class);

        ContextHandler context = new ContextHandler();
        context.setContextPath("/ws");
        context.setHandler(wsHandler);

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { context, resource_handler, new DefaultHandler() });

        server.setHandler(handlers);

        ClusterResources.subscribeToAll(callback);

        return server;
    }

    public static void main(String[] args) throws Exception {
        // Configure console logging
        BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
        org.apache.log4j.Logger.getLogger(WebServer.class).setLevel(Level.ALL);

        log.info("Starting web interface");
        Server server = setupServer();
        server.start();
        server.join();
    }

    @WebSocket
    public static class PubSubProxyWebSocket {
        private Session connection;

        public void sendReport(String json) {
            if (this.connection != null && this.connection.isOpen()) {
                // this.connection.write(arg0, arg1, arg2);sendMessage(json);
                // this.connection.write(json);
                try {
                    connection.getRemote().sendString(json);
                } catch (IOException e) {
                    log.warn("Exception sending JSON to remote");
                    e.printStackTrace();
                }
            }
        }

        @OnWebSocketClose
        public void onWebSocketClose(Session connection, int a, String s) {
            this.connection = null;
            sockets.remove(this);
        }

        @OnWebSocketConnect
        public void onWebSocketConnect(Session connection) {
            log.info("Incoming web connection from {}", connection.getRemoteAddress());
            this.connection = connection;
            sockets.put(this, true);
        }

    }

}
