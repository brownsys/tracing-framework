package edu.brown.cs.systems.pivottracing.tracepoint;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AdviceSpec;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.MethodTracepointSpec;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.MethodTracepointSpec.Where;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.TracepointSpec;

public class MethodTracepoint extends TracepointBaseImpl {
    
    public enum Interception {
        ENTRY, EXIT, EXCEPTION
    }
    
    public final Where interceptAt;
    public final int interceptAtLineNumber;
    public final String className, methodName;
    public final String[] args;
    
    public MethodTracepoint(String name, Where interceptAt, Method method) {
        this(name, interceptAt, method.getDeclaringClass().getName(), method.getName(), parametersToString(method.getParameterTypes()));
    }
    
    private static String[] parametersToString(Class<?>[] parameterClasses) {
        String[] parameterClassNames = new String[parameterClasses.length];
        for (int i = 0; i < parameterClasses.length; i++) {
            parameterClassNames[i] = parameterClasses[i].getCanonicalName();
        }
        return parameterClassNames;
    }
    
    public MethodTracepoint(String name, Where interceptAt, String fullyQualifiedClassname, String methodName, String... fullyQualifiedArgs) {
        super(name);
        this.interceptAt = interceptAt;
        this.interceptAtLineNumber = 0;
        this.className = fullyQualifiedClassname;
        this.methodName = methodName;
        this.args = fullyQualifiedArgs;
    }
    
    public MethodTracepoint(String name, int interceptAtLineNumber, String fullyQualifiedClassname, String methodName, String... fullyQualifiedArgs) {
        super(name);
        this.interceptAt = Where.LINENUM;
        this.interceptAtLineNumber = interceptAtLineNumber;
        this.className = fullyQualifiedClassname;
        this.methodName = methodName;
        this.args = fullyQualifiedArgs;
    }
    
    @Override
    public String toString() {
        return String.format("%s = %s [%s].[%s]([%s])", getName(), interceptAt, className, methodName, StringUtils.join(args, "],["));
    }

    @Override
    public List<TracepointSpec> getTracepointSpecsForAdvice(AdviceSpec advice) {
        TracepointSpec.Builder tsb = TracepointSpec.newBuilder();
        MethodTracepointSpec.Builder b = tsb.getMethodTracepointBuilder();
        b.setClassName(className);
        b.setMethodName(methodName);
        b.addAllParamClass(Lists.newArrayList(args));
        b.setWhere(interceptAt);
        if (interceptAt == Where.LINENUM) {
            b.setLineNumber(interceptAtLineNumber);
        }
        for (String observedVar : advice.getObserve().getVarList()) {
            b.addAdviceArg(exports.get(observedVar.split("\\.")[1]).getSpec());
        }
        return Lists.<TracepointSpec>newArrayList(tsb.build());
    }

}
