package edu.brown.cs.systems.pivottracing.tools;

import java.io.FileNotFoundException;
import java.io.IOException;

import edu.brown.cs.systems.pivottracing.PTAgentProtos.AgentStatus;
import edu.brown.cs.systems.pivottracing.PTAgentProtos.PivotTracingCommand;
import edu.brown.cs.systems.pivottracing.PivotTracingConfig;
import edu.brown.cs.systems.pubsub.PubSub;
import edu.brown.cs.systems.pubsub.PubSubClient.Subscriber;

/** Just subscribes and prints all query results to stdout */
public class GetAgentStatus extends Subscriber<AgentStatus> {
    
    @Override
    protected void OnMessage(AgentStatus message) {
        System.out.println(message);
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException {
        PubSub.subscribe(PivotTracingConfig.STATUS_TOPIC, new GetAgentStatus());
        while (!Thread.currentThread().isInterrupted()) {
            PubSub.publish(PivotTracingConfig.COMMANDS_TOPIC, PivotTracingCommand.newBuilder().setSendStatus(true).build());
            Thread.sleep(10000);
        }
    }

}
