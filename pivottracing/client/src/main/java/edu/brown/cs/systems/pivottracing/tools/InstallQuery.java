package edu.brown.cs.systems.pivottracing.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import edu.brown.cs.systems.pivottracing.PivotTracingClient;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;
import edu.brown.cs.systems.pivottracing.query.PTQuery;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoint;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoints;

public class InstallQuery {
    
    public static void main(String[] args) throws PTQueryException, FileNotFoundException, IOException {
        Properties props = new Properties();
        props.load(new FileInputStream("src/main/resources/log4j.properties"));
        PropertyConfigurator.configure(props);
        
        // Get a client for submitting queries
        PivotTracingClient client = PivotTracingClient.newInstance();
        
        // Create a tracepoint
        String path = "org.apache.hadoop.fs.Path";
        String filesystem = "org.apache.hadoop.fs.FileSystem";
        Tracepoint FileSystem_Open = Tracepoints.method(filesystem, "open", path).addExport("path", "f");
        
        client.addTracepoint(FileSystem_Open);
        
        PTQuery query = PTQuery.From(FileSystem_Open).GroupBy("path").Count();
        client.install(query);
    }

}
