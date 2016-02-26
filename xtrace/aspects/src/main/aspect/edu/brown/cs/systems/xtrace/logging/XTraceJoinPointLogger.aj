package edu.brown.cs.systems.xtrace.logging;

/**
 * Just contains some useful extensions to the X-Trace API
 */
public aspect XTraceJoinPointLogger {
  
  // Just changes the logging call to the one that takes a join point, in order to include information about source of the call
  void around(XTraceLogger xtrace, String message, Object[] labels): target(xtrace) && args(message, labels) && call(void XTraceLogger+.log(String, Object...)) {
      xtrace.log(thisJoinPointStaticPart, message, labels);
  }
  
}
