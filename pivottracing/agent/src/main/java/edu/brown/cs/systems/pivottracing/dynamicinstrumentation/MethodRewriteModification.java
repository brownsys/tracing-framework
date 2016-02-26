package edu.brown.cs.systems.pivottracing.dynamicinstrumentation;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.dynamicinstrumentation.DynamicModification;
import edu.brown.cs.systems.pivottracing.agent.PrivilegedProxy;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.ExportedVariable;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.MethodTracepointSpec;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.MultiExportedVariable;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/** Rewrite of a method */
public class MethodRewriteModification implements DynamicModification {
    
    private static final Logger log = LoggerFactory.getLogger(MethodRewriteModification.class);

    public final MethodTracepointSpec methodSpec;
    public final int adviceLookupId;
    
    public MethodRewriteModification(MethodTracepointSpec methodSpec, int adviceLookupId) {
        this.methodSpec = methodSpec;
        this.adviceLookupId = adviceLookupId;
    }

    @Override
    public Collection<String> affects() {
        return Lists.newArrayList(methodSpec.getClassName());
    }

    @Override
    public void apply(ClassPool pool) throws NotFoundException, CannotCompileException {
        CtMethod cm = getMethod(pool, methodSpec);
        String code = invocation();
        log.info("Invocation:\n{}", code);
        switch (methodSpec.getWhere()) {
        case ENTRY: cm.insertBefore(code); break;
        case RETURN: cm.insertAfter(code); break;
        case FINALLY: cm.insertAfter(code, true); break;
        case LINENUM: cm.insertAt(methodSpec.getLineNumber(), code); break;
        }
    }
    
    /** Get a modifiable method object based on a MethodTracepointSpec  */
    public static CtMethod getMethod(ClassPool pool, MethodTracepointSpec methodSpec) throws NotFoundException {
        return getMethod(pool, methodSpec.getClassName(), methodSpec.getMethodName(), methodSpec.getParamClassList());
    }

    /** Get a modifiable method object by name */
    public static CtMethod getMethod(ClassPool pool, String className, String methodName, List<String> argClassNames) throws NotFoundException {
        CtClass[] ctParameters = pool.get(argClassNames.toArray(new String[argClassNames.size()]));
        return pool.get(className).getDeclaredMethod(methodName, ctParameters);
    }
    
    /** Get the invocation string that invokes advice */
    public String invocation() {
        // Determine any advice args preprocessing
        List<String> adviceArgs = Lists.newArrayList();
        List<MultiExportedVariable> multiLiterals = Lists.newArrayList();
        for (ExportedVariable x : methodSpec.getAdviceArgList()) {
            if (x.hasLiteral()) {
                adviceArgs.add(x.getLiteral());
            } else if (x.hasMulti()) {
                String multi_literal = String.format("pivottracing_arg_%d", multiLiterals.size());
                multiLiterals.add(x.getMulti());
                adviceArgs.add(multi_literal);
            }
        }
        
        // Args passed to advice
        String adviceArgString;
        if (methodSpec.getAdviceArgCount() == 0) {
            adviceArgString = "new Object[0]";
        } else {
            // Must box primitives
            List<String> boxedArgs = Lists.newArrayList();
            for (String adviceArg : adviceArgs) {
                boxedArgs.add(String.format("%s.box(%s)", PrivilegedProxy.class.getName(), adviceArg));
            }
            adviceArgString = String.format("new Object[] { %s }", StringUtils.join(boxedArgs, ", "));
        }
        
        // Invocation of advice
        String proxyClass = PrivilegedProxy.class.getName();
        String invocation = String.format("%s.Advise(%d, %s);", proxyClass, adviceLookupId, adviceArgString);
        
        // Wrap in loops for multi variables
        for (int i = 0; i < multiLiterals.size(); i++) {
            MultiExportedVariable x = multiLiterals.get(i);

            String var = String.format("pivottracing_arg_%s", i);
            String var_array = String.format("pivottracing_arg_%s_array", i);
            String var_i = var + "_i";
            String var_indexed = String.format("((%s)%s[%s])", x.getType(), var_array, var_i);
            String var_processed = x.getPostProcess().replace("{}", var_indexed);

            // The weird Object casts here are to prevent compiler errors when the code is obviously (to the compiler) wrong
            String code = StringUtils.join(new String[] {
                    "Object[] %s;",
                    "if (((Object)%s) instanceof java.util.Collection) {",
                    "   %s = ((java.util.Collection)(Object)%s).toArray();",
                    "} else {",
                    "   %s = (Object[])(Object)%s;",
                    "}",
                    "for (int %s = 0; %s < %s.length; %s++) {",
                    "   Object %s = %s;",
                        invocation,
                    "}"
            }, "\n");
            invocation = String.format(code, 
                    var_array,
                    x.getLiteral(),
                    var_array, x.getLiteral(),
                    var_array, x.getLiteral(),
                    var_i, var_i, var_array, var_i,
                    var, var_processed
            );
        }
        
        // Wrapped in exception
        invocation = StringUtils.join(new String[] {
                "try {",
                    invocation,
                "} catch (java.lang.Throwable t) {",
                "}"
        }, "\n");
        return invocation;
    }

    
}
