package edu.brown.cs.systems.pivottracing.agent.advice;

import com.google.protobuf.Message;

public class InvalidAdviceException extends Exception {
    
    public final Message message;
    
    public InvalidAdviceException(String string) {
        this(null, string);
    }
    
    public InvalidAdviceException(Message message) {
        this(message, String.format("Invalid: %s", message));
    }
    
    public InvalidAdviceException(Message message, String string) {
        super(string);
        this.message = message;
    }

}
