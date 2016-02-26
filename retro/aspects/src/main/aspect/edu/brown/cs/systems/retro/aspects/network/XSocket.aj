package edu.brown.cs.systems.retro.aspects.network;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;

import edu.brown.cs.systems.retro.resources.Network;
import edu.brown.cs.systems.retro.wrappers.NetworkInputStreamWrapper;
import edu.brown.cs.systems.retro.wrappers.NetworkOutputStreamWrapper;

/**
 * Wraps input streams and output streams on the network 
 * 
 * @author a-jomace
 */
public aspect XSocket {
  
  InputStream around(Socket socket): target(socket) && call(InputStream Socket+.getInputStream()) {
    return new NetworkInputStreamWrapper(proceed(socket), thisJoinPointStaticPart, Network.isLoopback(socket));
  }
  
  OutputStream around(Socket socket): target(socket) && call(OutputStream Socket+.getOutputStream()) {
    return new NetworkOutputStreamWrapper(proceed(socket), thisJoinPointStaticPart, Network.isLoopback(socket));
  }
  
  void around(Socket socket): target(socket) && (call(void Socket+.connect(..)) || call(* AutoCloseable+.close())) {
    Network.Connect.starting(socket, thisJoinPointStaticPart);
    try {
      proceed(socket);
    } finally {
      Network.Connect.finished(socket, thisJoinPointStaticPart);
    }
  }

}