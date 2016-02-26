package edu.brown.cs.systems.pivottracing.tracepoint;

import java.util.Map;

import com.google.common.collect.Maps;

import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.ExportedVariable;

public abstract class TracepointBaseImpl implements Tracepoint {
    
    public interface Export {
        public ExportedVariable getSpec();
    };
    
    /** Export of a single literal value as a variable */
    public class SingleExport implements Export {
        public final String literal;
        public SingleExport(String literal) {
            this.literal = literal;
        }
        @Override
        public ExportedVariable getSpec() {
            ExportedVariable.Builder b = ExportedVariable.newBuilder();
            b.setLiteral(literal);
            return b.build();
        }
    }
    
    /** Export an array or collection as multiple tuples
     * Type should specify the type of the array or collection.
     * It's possible to handle array / collection types without this information,
     * but requires extra coding that I don't have time to do... */
    public class MultiExport implements Export {
        public final String literal, type;
        public MultiExport(String literal, String type) {
            this.literal = literal;
            this.type = type;
        }
        @Override
        public ExportedVariable getSpec() {
            ExportedVariable.Builder b = ExportedVariable.newBuilder();
            b.getMultiBuilder().setLiteral(literal).setType(type).setPostProcess("{}");
            return b.build();
        }
    }
    
    /** Export of an array or collection as multiple values and apply a function to each member
     * eg if the input array is [ obj1, obj2 ]
     * function f = "{}.toString()" will produce obj1.toString(), obj2.toString() */
    public class ModifiedMultiExport implements Export {
        public final String literal, type, f;
        public ModifiedMultiExport(String literal, String type, String f) {
            this.literal = literal;
            this.type = type;
            this.f = f;
        }
        @Override
        public ExportedVariable getSpec() {
            ExportedVariable.Builder b = ExportedVariable.newBuilder();
            b.getMultiBuilder().setLiteral(literal).setType(type).setPostProcess(f);
            return b.build();
        }
    }

    public final String name;
    protected final Map<String, Export> exports = Maps.newHashMap();
    
    protected TracepointBaseImpl(String name) {
        this.name = name;
        addDefaults();
    }

    public Tracepoint addExport(String as, String literal) {
        exports.put(as, new SingleExport(literal));
        return this;
    }
    
    public Tracepoint addMultiExport(String as, String arrayLiteral, String type) {
        exports.put(as, new MultiExport(arrayLiteral, type));
        return this;
    }
    
    public Tracepoint addModifiedMultiExport(String as, String arrayLiteral, String type, String f) {
        exports.put(as, new ModifiedMultiExport(arrayLiteral, type, f));
        return this;
    }
    
    protected void addDefaults() {
        exports.put("host", new SingleExport("edu.brown.cs.systems.tracing.Utils.getHost()"));
        exports.put("timestamp", new SingleExport("System.currentTimeMillis()"));
        exports.put("cpu", new SingleExport("edu.brown.cs.systems.clockcycles.CPUCycles.get()"));
        exports.put("threadId", new SingleExport("Thread.currentThread().getId()"));
        exports.put("procId", new SingleExport("edu.brown.cs.systems.tracing.Utils.getProcessID()"));
        exports.put("procName", new SingleExport("edu.brown.cs.systems.tracing.Utils.getProcessName()"));
    }

    @Override
    public boolean exports(String varName) {
        return exports.containsKey(varName);
    }

    @Override
    public String getName() {
        return name;
    }

}
