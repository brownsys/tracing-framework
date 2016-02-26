package edu.brown.cs.systems.retro.aspects.network;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;

import edu.brown.cs.systems.retro.resources.Network;
import edu.brown.cs.systems.retro.wrappers.NetworkInputStreamWrapper;
import edu.brown.cs.systems.retro.wrappers.NetworkOutputStreamWrapper;

/**
 * Wraps input streams and output streams on the network acquired via the URLConnection class
 * 
 * @author a-jomace
 */
public aspect XURLConnection {
  
  InputStream around(URLConnection connection): target(connection) && call(InputStream URLConnection+.getInputStream()) {
    return new NetworkInputStreamWrapper(proceed(connection), thisJoinPointStaticPart);
  }
  
  OutputStream around(URLConnection connection): target(connection) && call(OutputStream URLConnection+.getOutputStream()) {
    return new NetworkOutputStreamWrapper(proceed(connection), thisJoinPointStaticPart);
  }
  
  void around(URLConnection connection): target(connection) && (call(void URLConnection+.connect(..))) {
    Network.Connect.starting(connection, thisJoinPointStaticPart);
    try {
      proceed(connection);
    } finally {
      Network.Connect.finished(connection, thisJoinPointStaticPart);
    }
  }

}