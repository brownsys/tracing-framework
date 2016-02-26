package edu.brown.cs.systems.pivottracing.examples;

import edu.brown.cs.systems.pivottracing.PivotTracingClient;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.MethodTracepointSpec.Where;
import edu.brown.cs.systems.pivottracing.tracepoint.MethodTracepoint;
import edu.brown.cs.systems.pivottracing.tracepoint.MultiTracepoint;

public class SOSPPaperExamplesTracepoints {
    
    /** Get a client loaded with the example tracepoints */
    public static PivotTracingClient client() {
        PivotTracingClient pt = new PivotTracingClient();
        pt.addTracepoint(hdfs_datanode_incrBytesRead);
        pt.addTracepoint(hdfs_datanode_addReadBlockOp);
        pt.addTracepoint(hdfs_datanode_dataTransferProtocol);
        pt.addTracepoint(hdfs_client_open);
        pt.addTracepoint(hdfs_namenode_getBlockLocations);
        pt.addTracepoint(hdfs_namenode_getBlockLocations_return);
        pt.addTracepoint(client_protocols);
        pt.addTracepoint(stresstest_donextop);
        return pt;
    }

    public static final MethodTracepoint hdfs_datanode_incrBytesRead;
    public static final MethodTracepoint hdfs_datanode_addReadBlockOp;
    public static final MethodTracepoint hdfs_datanode_dataTransferProtocol;
    public static final MethodTracepoint hdfs_client_open;
    public static final MethodTracepoint hdfs_namenode_getBlockLocations;
    public static final MethodTracepoint hdfs_namenode_getBlockLocations_return;
    public static final MultiTracepoint client_protocols;
    public static final MethodTracepoint stresstest_donextop;
    
    static {
        hdfs_datanode_incrBytesRead = new MethodTracepoint(
                "DataNodeMetrics.incrBytesRead", // name for the tracepoint
                Where.ENTRY, "org.apache.hadoop.hdfs.server.datanode.metrics.DataNodeMetrics", "incrBytesRead", // entry point, class name, method 
                "int"); // arg types
        hdfs_datanode_incrBytesRead.addExport("delta", "new Integer(delta)");
        
        hdfs_datanode_addReadBlockOp = new MethodTracepoint(
                "DataNodeMetrics.addReadBlockOps",
                Where.ENTRY, "org.apache.hadoop.hdfs.server.datanode.metrics.DataNodeMetrics", "addReadBlockOp", // entry point, class name, method 
                "long"); // arg types
        hdfs_datanode_addReadBlockOp.addExport("latency", "new Long(latency)");
        
        hdfs_datanode_dataTransferProtocol = new MethodTracepoint(
                "DN.DataTransferProtocol", // name for the tracepoint
                Where.ENTRY, "org.apache.hadoop.hdfs.server.datanode.DataXceiver", "readBlock", // entry point, class name, method 
                "org.apache.hadoop.hdfs.protocol.ExtendedBlock", "org.apache.hadoop.security.token.Token", "java.lang.String", "long", "long", "boolean"); // arg types
        
        hdfs_client_open = new MethodTracepoint(
                "DN.ClientOpen", // name for the tracepoint
                Where.ENTRY, "org.apache.hadoop.hdfs.DFSClient", "open", // entry point, class name, method 
                "java.lang.String", "int", "boolean"); // arg types
        
        hdfs_namenode_getBlockLocations = new MethodTracepoint(
                "NN.GetBlockLocations", // name for the tracepoint
                Where.ENTRY, "org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer", "getBlockLocations",  // entry point, class name, method
                "java.lang.String", "long", "long"); // arg types
        hdfs_namenode_getBlockLocations.addExport("src", "src"); // export literal 'src' as query var 'src'
        
        hdfs_namenode_getBlockLocations_return = new MethodTracepoint(
                "NN.GetBlockLocations", // name for the tracepoint
                Where.RETURN, "org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer", "getBlockLocations",  // entry point, class name, method
                "java.lang.String", "long", "long"); // arg types
        hdfs_namenode_getBlockLocations_return.addExport("src", "src"); // export literal 'src' as query var 'src'
        
        // Want to export the names of block locations.
        // $_ is a special javassist variable that lets you refer to the return value of the function
        // $_.getLastLocatedBlock().getLocations() is a list of DataNodeInfo objects
        // want to call getName on each of these instances
        hdfs_namenode_getBlockLocations_return.addModifiedMultiExport("replicas", "$_.getLastLocatedBlock().getLocations()", "org.apache.hadoop.hdfs.protocol.DatanodeInfo", "{}.getName()"); // export return block location arrays
        
        client_protocols = new MultiTracepoint(
                "ClientProtocols" // name for the tracepoint
        );

        stresstest_donextop = new MethodTracepoint("StressTest.DoNextOp", // name for the tracepoint
                Where.ENTRY, "edu.brown.cs.systems.hdfsworkloadgenerator.Client", "runRequest" // entry point, class name, method
        );
        
        // TODO: finish enumerating examples...
    }
}
