package edu.brown.cs.systems.pivottracing.agent.advice.utils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

import edu.brown.cs.systems.pivottracing.agent.WeaveProtos;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.MethodTracepointSpec;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.MethodTracepointSpec.Builder;

public class TracepointsTestUtils {

    public static Method getMethod(String className, String methodName) throws ClassNotFoundException {
        return getMethod(ClassUtils.getClass(className), methodName);
    }

    public static Method getMethod(Class<?> cls, String methodName) throws ClassNotFoundException {
        for (Method m : cls.getDeclaredMethods()) {
            if (methodName.equals(m.getName())) {
                return m;
            }
        }
        return null;
    }

    public static String[] inferParamNames(Method m) {
        int numParams = m.getGenericParameterTypes().length;
        String[] paramNames = new String[numParams];
        for (int i = 0; i < numParams; i++) {
            paramNames[i] = String.format("$%d", i + 1); // $1, $2, ...
        }
        return paramNames;
    }

    public static MethodTracepointSpec getMethodSpec(Class<?> cls, String methodName)
            throws ClassNotFoundException {
        MethodTracepointSpec.Builder b = MethodTracepointSpec.newBuilder();
        b.setClassName(cls.getName());
        b.setMethodName(methodName);

        Method m = getMethod(cls, methodName);
        for (Class<?> paramClass : m.getParameterTypes()) {
            b.addParamClass(paramClass.getCanonicalName());
        }

        int paramCount = 0;
        for (Type paramType : m.getGenericParameterTypes()) {
            String paramName = String.format("$%d", ++paramCount);
            if (TypeUtils.isArrayType(paramType)) {
                Type arrayOfType = TypeUtils.getArrayComponentType(paramType);
                String arrayOf = TypeUtils.toString(arrayOfType);
                b.addAdviceArgBuilder().getMultiBuilder().setLiteral(paramName).setPostProcess("{}")
                        .setType(arrayOf);
            } else if (TypeUtils.isAssignable(paramType, Collection.class)) {
                ParameterizedType pt = (ParameterizedType) paramType;
                Type collectionOfType = pt.getActualTypeArguments()[0]; // doesn't work if multiple type params, but
                                                                        // good enough
                String collectionOf = TypeUtils.toString(collectionOfType);
                b.addAdviceArgBuilder().getMultiBuilder().setLiteral(paramName).setPostProcess("{}")
                        .setType(collectionOf);
            } else {
                b.addAdviceArgBuilder().setLiteral(paramName);
            }
        }

        return b.build();
    }

    public static MethodTracepointSpec getSimple(Class<?> cls, String methodName, String... literalsToExport) throws ClassNotFoundException {
        Method m = getMethod(cls, methodName);

        MethodTracepointSpec.Builder b = MethodTracepointSpec.newBuilder();
        b.setClassName(cls.getName());
        b.setMethodName(methodName);

        for (Class<?> paramClass : m.getParameterTypes()) {
            b.addParamClass(paramClass.getCanonicalName());
        }
        
        for (String literal : literalsToExport) {
            b.addAdviceArgBuilder().setLiteral(literal);
        }

        return b.build();
    }

}
