package edu.brown.cs.systems.pivottracing.agent.advice;

import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.protobuf.Message;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AdviceSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Agg;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AggVar;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.EmitSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.FilterSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.GroupBySpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.LetSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.PackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.TupleSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.UnpackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.WhereSpec;
import edu.brown.cs.systems.pivottracing.agent.Advice;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Pack;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Unpack;
import edu.brown.cs.systems.pivottracing.agent.advice.output.EmitAPI;
import edu.brown.cs.systems.pivottracing.agent.advice.output.EmitAPI.Emit;


/** Implements advice */
public class AdviceImpl implements Advice {

    private static final Logger log = LoggerFactory.getLogger(AdviceImpl.class);
    
    public final AdviceSpec spec;  // Spec for this advice
    
    public final BaggageAPI baggageAPI;
    public final EmitAPI emitAPI;
    
    private final List<String> tupleNames = Lists.newArrayList(); // Names of each variable in the tuple
    
    private final List<Unpacker> unpackers = Lists.newArrayList(); // Responsible for retrieving tuples from baggage (UNPACK)
    private final List<LetImpl> lets = Lists.newArrayList(); // Responsible for constructing tuples from expressions (LET)
    private final List<WhereImpl> wheres = Lists.newArrayList(); // Responsible for evaluating predicates (WHERE)
    private final Outputter outputter; // Responsible for sending output tuples to the appropriate destionation (EMIT / PACK)
    
    public AdviceImpl(AdviceSpec spec, BaggageAPI baggageAPI, EmitAPI emitAPI) throws InvalidAdviceException {
        this.spec = spec;
        this.baggageAPI = baggageAPI;
        this.emitAPI = emitAPI;
        
        tupleNames.addAll(spec.getObserve().getVarList()); // OBSERVE
        
        for (UnpackSpec unpack : spec.getUnpackList()) {
            unpackers.add(new Unpacker(baggageAPI.create(unpack), AdviceUtils.varNames(unpack))); // UNPACK
        }
        
        for (LetSpec let : spec.getLetList()) {
            lets.add(new LetImpl(let)); // LET
        }
        
        for (WhereSpec where : spec.getWhereList()) {
            wheres.add(new WhereImpl(where)); // WHERE
        }        
        
        if (spec.hasPack()) {
            PackSpec pack = spec.getPack();
            if (pack.hasTupleSpec()) {
                outputter = new Packer(baggageAPI.create(pack), pack.getTupleSpec()); // PACK tuples
            } else if (pack.hasFilterSpec()) {
                outputter = new Packer(baggageAPI.create(pack), pack.getFilterSpec()); // PACK filtered tuples
            } else if (pack.hasGroupBySpec()) {
                outputter = new Packer(baggageAPI.create(pack), pack.getGroupBySpec()); // PACK grouped tuples
            } else {
                throw new InvalidAdviceException(pack, "PACK lacks specification");
            }
        } else if (spec.hasEmit()) {
            EmitSpec emit = spec.getEmit();
            if (emit.hasTupleSpec()) {
                outputter = new Emitter(emitAPI.create(emit), emit.getTupleSpec()); // EMIT tuples
            } else if (emit.hasGroupBySpec()) {
                outputter = new Emitter(emitAPI.create(emit), emit.getGroupBySpec()); // EMIT grouped tuples
            } else {
                throw new InvalidAdviceException(emit, "EMIT lacks specification");
            }
        } else {
            throw new InvalidAdviceException(spec, "No PACK or EMIT specified");
        }
    }
    
    /** Clean up anything that needs to be cleaned up when this advice is uninstalled */
    public void destroy() {
        outputter.destroy(); // For now just the outputter needs to be destroyed for EMIT
    }

    public void advise(Object... values) {
        try {
            // OBSERVE: create the tuple and copy observed values
            Object[] tuple = new Object[tupleNames.size()];
            System.arraycopy(values, 0, tuple, 0, values.length);
            
            // UNPACK: unpack everything from the baggage
            int inputcount = 1;
            Object[][][] unpacked = new Object[unpackers.size()][][];
            for (int i = 0; i < unpacked.length; i++) {
                unpacked[i] = unpackers.get(i).unpack();
                inputcount *= unpacked[i].length;
            }
            
            // Save up output tuples; no more than the number of input tuples
            List<Object[]> outputs = Lists.newArrayListWithExpectedSize(inputcount);
            
            // Now iterate over every combination of joined input tuples
            tupleloop: for (int i = 0; i < inputcount; i++) {
                // (Join): construct the tuple by copying specific unpacked tuples
                for (int j = 0, l = i; j < unpacked.length; j++) {
                    Object[] uTuple = unpacked[j][l % unpacked[j].length];
                    System.arraycopy(uTuple, 0, tuple, unpackers.get(j).offsetInInput, uTuple.length);
                    l /= unpacked[j].length;
                }
                
                // LET: construct let variables
                for (LetImpl c : lets) {
                    c.calculate(tuple);
                }
                
                // WHERE: apply predicates
                for (WhereImpl f : wheres) {
                    if (!f.satisfies(tuple)) {
                        continue tupleloop;
                    }
                }
                
                // Save output tuple
                outputs.add(outputter.makeOutputTuple(tuple));
            }

            // EMIT / PACK: send outputs
            outputter.output(outputs);
        } catch (Throwable t) {
            log.warn("Advice failed", t);
        }
    }
    
    public int indexOf(String var, Message sourceSpec) throws InvalidAdviceException {
        int index = tupleNames.indexOf(var);
        if (index == -1) {
            throw new InvalidAdviceException(sourceSpec, "Unknown var " + var);
        }
        return index;
    }
    
    
    /** Unpacks tuples from baggage */
    public class Unpacker {
        public final Unpack unpack;
        public final int offsetInInput; // Position in the tuple where the unpacked variables start
        
        public Unpacker(Unpack unpack, List<String> varNames) {
            this.unpack = unpack;
            this.offsetInInput = tupleNames.size();
            tupleNames.addAll(varNames);
        }
        
        /** Returns an array of unpacked tuples */
        public Object[][] unpack() {
            return unpack.unpack();
        }
    }
    
    // Yup, we're going there... this is seriously how we're going to do this... 
    private static ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
    
    /** Constructs LET variables */
    public class LetImpl {
        
        public final LetSpec let;
        private final int inputIndex;
        private final int[] replacementVariableIndices;
        
        public LetImpl(LetSpec let) throws InvalidAdviceException {
            this.let = let;
            inputIndex = tupleNames.size();
            tupleNames.add(let.getVar());
            replacementVariableIndices = new int[let.getReplacementVariablesCount()];
            for (int i = 0; i < replacementVariableIndices.length; i++) {
                replacementVariableIndices[i] = indexOf(let.getReplacementVariables(i), let);
            }
        }
        
        public void calculate(Object[] tuple) throws ScriptException {
            String toEval = let.getExpression();
            for (int i : replacementVariableIndices) {
                toEval = toEval.replaceFirst("\\{\\}", tuple[i].toString().replace("\"", "\\\\\""));
            }
            tuple[inputIndex] = engine.eval(toEval);
        }
        
    }
    
    /** Evaluates FILTER predicates */
    public class WhereImpl {
        
        public final WhereSpec where;
        private final String expression;
        private final int[] replacementVariableIndices;
        
        public WhereImpl(WhereSpec where) throws InvalidAdviceException {
            this.where = where;
            expression = where.getPredicate();
            replacementVariableIndices = new int[where.getReplacementVariablesCount()];
            for (int i = 0; i < replacementVariableIndices.length; i++) {
                replacementVariableIndices[i] = indexOf(where.getReplacementVariables(i), where);
            }
        }
        
        public boolean satisfies(Object[] tuple) throws ScriptException {
            String toEval = expression;
            for (int i : replacementVariableIndices) {
                toEval = toEval.replaceFirst("\\{\\}", tuple[i].toString().replace("\"", "\\\\\""));
            }
            return engine.eval(toEval) == (Boolean) true;
        }
        
        
    }
    
    /** Constructs output tuples from input tuple, superclass for PACK and EMIT */
    public abstract class Outputter {
        private final int[] outputTupleIndices;
        public Outputter(TupleSpec spec) throws InvalidAdviceException {
            outputTupleIndices = new int[spec.getVarCount()];
            for (int i = 0; i < spec.getVarCount(); i++) {
                outputTupleIndices[i] = indexOf(spec.getVar(i), spec);
            }
        }
        public Outputter(FilterSpec spec) throws InvalidAdviceException {
            outputTupleIndices = new int[spec.getVarCount()];
            for (int i = 0; i < spec.getVarCount(); i++) {
                outputTupleIndices[i] = indexOf(spec.getVar(i), spec);
            }
        }
        public Outputter(GroupBySpec spec) throws InvalidAdviceException {
            outputTupleIndices = new int[spec.getAggregateCount() + spec.getGroupByCount()];
            int i = 0;
            for (String groupBy : spec.getGroupByList()) {
                outputTupleIndices[i++] = indexOf(groupBy, spec);
            }
            for (AggVar aggregate : spec.getAggregateList()) {
                if (aggregate.getHow() == Agg.COUNT) {
                    outputTupleIndices[i++] = -1;
                } else {
                    outputTupleIndices[i++] = indexOf(aggregate.getName(), spec);
                }
            }
        }
        protected Object[] makeOutputTuple(Object[] tuple) {
            Object[] output = new Object[outputTupleIndices.length];
            for (int i = 0; i < outputTupleIndices.length; i++) {
                int j = outputTupleIndices[i];
                if (j == -1) {
                    output[i] = 1L;
                } else {
                    output[i] = tuple[j];
                }
            }
            return output;
        }
        protected abstract void output(List<Object[]> tuples);
        protected abstract void destroy();
    }
    
    /** PACK tuples into baggage */
    public class Packer extends Outputter {
        public final Pack pack;
        public Packer(Pack pack, TupleSpec spec) throws InvalidAdviceException {
            super(spec);
            this.pack = pack;
        }
        public Packer(Pack pack, FilterSpec spec) throws InvalidAdviceException {
            super(spec);
            this.pack = pack;
        }
        public Packer(Pack pack, GroupBySpec spec) throws InvalidAdviceException {
            super(spec);
            this.pack = pack;
        }
        protected void output(List<Object[]> tuples) {
            pack.pack(tuples);
        }
        protected void destroy() {
        }
    }
    
    /** EMIT tuples for global aggregation */
    public class Emitter extends Outputter {
        public final Emit emit;
        public Emitter(Emit emit, TupleSpec spec) throws InvalidAdviceException {
            super(spec);
            this.emit = emit;
        }
        public Emitter(Emit emit, GroupBySpec spec) throws InvalidAdviceException {
            super(spec);
            this.emit = emit;
        }
        public void output(List<Object[]> tuples) {
            emit.emit(tuples);
        }
        protected void destroy() {
            emitAPI.destroy(emit);
        }
    }

}
