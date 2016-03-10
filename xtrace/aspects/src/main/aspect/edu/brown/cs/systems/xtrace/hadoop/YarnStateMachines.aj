package edu.brown.cs.systems.xtrace.hadoop;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.logging.XTraceLogger;

/** Invoke X-Trace APIs when baggage is set and unset */
public aspect YarnStateMachines {

    static XTraceLogger xtrace = XTrace.getLogger(YarnStateMachines.class);
    
    /** Use reflection to get state machine current state to avoid weird classpath dependency issues */
    static java.lang.reflect.Method stateMachineGetCurrentState = null;
    static {
        try {
            Class<?> stateMachineClass = Class.forName("org.apache.hadoop.yarn.state.StateMachine");
            stateMachineGetCurrentState = stateMachineClass.getMethod("getCurrentState");
        } catch (Throwable t) {
        }
    }

    before(Object operand, Object event): if(xtrace.valid()) && args(operand, event) && call(void org.apache.hadoop.yarn.state.SingleArcTransition+.transition(Object+, Object+)) {
        xtrace.log(thisJoinPointStaticPart, "StateMachine transition {} with {}", operand, event.getClass().getSimpleName());
    }

    before(Object operand, Object event): if(xtrace.valid()) && args(operand, event) && call(Object+ org.apache.hadoop.yarn.state.MultipleArcTransition+.transition(Object+, Object+)) {
        xtrace.log(thisJoinPointStaticPart, "StateMachine transition {} with {}", operand, event.getClass().getSimpleName());
    }

    Object around(Object stateMachine, Object operand, Object event): if(xtrace.valid()) && target(stateMachine) && args(operand, event) 
                && call(Object+ org.apache.hadoop.yarn.state.StateMachine+.doTransition(Object+, Object+)) {
        Object newState = null, initialState = null;
        try {
            initialState = stateMachineGetCurrentState.invoke(stateMachine);
        } catch (Throwable t) {
            // ignore;
        }
        try {
            newState = proceed(stateMachine, operand, event);
            return newState;
        } finally {
            xtrace.log(thisJoinPointStaticPart, "StateMachine transitioned from {} to {} with {} on {}", initialState, newState, event.getClass().getSimpleName(), operand);            
        }
    }
    
    before(Object handler, Object event): if(xtrace.valid()) && this(handler) && args(event) && execution(void org.apache.hadoop.yarn.event.EventHandler+.handle(Object+)) {
        String handlerName = handler.getClass().getSimpleName();
        String eventName = event.getClass().getSimpleName();
        Object[] eventDetails = EventDetails(event);
        xtrace.log(thisJoinPointStaticPart, handlerName+" handling " + eventName, eventDetails);
    }
    
    private static Object[] EventDetails(Object event) {
        if (event == null) {
            return new Object[0];
        } else {
            List<Object> pairs = Lists.newArrayList();
            Class<?> current = event.getClass();
            while(current.getSuperclass()!=null) {
                for (Field f : current.getDeclaredFields()) {
                    String fieldName = f.getName();
                    try {
                        f.setAccessible(true);
                        String fieldValue = String.valueOf(f.get(event));
                        pairs.add(fieldName);
                        pairs.add(fieldValue);
                    } catch (Throwable t) {
                        pairs.add(fieldName);
                        pairs.add("(Inaccessible: "+t.getMessage()+")");
                    }
                }                
                current = current.getSuperclass();
            }
            return pairs.toArray();
        }
    }
    
    /** Use reflection to call shell methods */
    static java.lang.reflect.Method shellCommandExecutorGetExecString = null;
    static {
        try {
            Class<?> shellCommandExecutorClass = Class.forName("org.apache.hadoop.util.shell.ShellCommandExecutor");
            shellCommandExecutorGetExecString = shellCommandExecutorClass.getMethod("getExecString");
        } catch (Throwable t) {
        }
    }
    
    before(Object o): target(o) && call(void org.apache.hadoop.util.shell.ShellCommandExecutor+.execute()) && if(xtrace.valid()) {
        try {
            String[] execString = (String[]) shellCommandExecutorGetExecString.invoke(o);
            xtrace.log(thisJoinPointStaticPart, StringUtils.join(execString, " "));
        } catch (Throwable t) {
            // Do nothing
        }
    }

}
