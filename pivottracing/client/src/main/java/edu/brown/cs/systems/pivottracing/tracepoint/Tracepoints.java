package edu.brown.cs.systems.pivottracing.tracepoint;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.commons.lang3.reflect.TypeUtils;

import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.MethodTracepointSpec.Where;

/** Provides some static convenience methods for creating tracepoints */
public class Tracepoints {
    
    public static MethodTracepoint method(String className, String methodName, String... methodParamTypes) {
        String tracepointName = className.substring(className.lastIndexOf(".")) + "." + methodName;
        return new MethodTracepoint(tracepointName, Where.ENTRY, className, methodName, methodParamTypes);
    }
    
    /** Create a tracepoint for the entry of the named method of the class.
     * The name of the tracepoint will be the short class name and the method name,
     * eg java.lang.String.format will have short name String.format
     * Parameter types will be looked up, and if the method is overloaded, returns one of the methods.
     * If any of the method arguments are arrays or collections, they will be exported as multi-variables
     */
    public static MethodTracepoint create(Class<?> cls, String methodName, String... namesForMethodParameters) {
        Where where = Where.ENTRY;
        String tracepointName = String.format("%s.%s", cls.getSimpleName(), methodName);
        
        // Find the method
        Method m = getMethod(cls, methodName, namesForMethodParameters.length);
        
        // Create the tracepoint
        MethodTracepoint tracepoint = new MethodTracepoint(tracepointName, where, m);
        
        // Export the arguments as variables
        int paramCount = 0;
        for (Type paramType : m.getGenericParameterTypes()) {
            String exportAs = namesForMethodParameters[paramCount];
            String literal = String.format("$%d", ++paramCount);
            if (TypeUtils.isArrayType(paramType)) {
                Type arrayOfType = TypeUtils.getArrayComponentType(paramType);
                String arrayOf = TypeUtils.toString(arrayOfType);
                tracepoint.addMultiExport(exportAs, literal, arrayOf);
            } else if (TypeUtils.isAssignable(paramType, Collection.class)) {
                ParameterizedType pt = (ParameterizedType) paramType;
                Type collectionOfType = pt.getActualTypeArguments()[0];
                String collectionOf = TypeUtils.toString(collectionOfType);
                tracepoint.addMultiExport(exportAs, literal, collectionOf);
            } else {
                tracepoint.addExport(exportAs, literal);
            }
        }
        
        return tracepoint;
    }

    private static Method getMethod(Class<?> cls, String methodName, int paramCount) {
        for (Method m : cls.getDeclaredMethods()) {
            if (methodName.equals(m.getName()) && m.getParameterTypes().length == paramCount) {
                return m;
            }
        }
        return null;
    }

}
