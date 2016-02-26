package edu.brown.cs.systems.pivottracing.query;

import java.util.List;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Filter;
import edu.brown.cs.systems.pivottracing.query.Components.LetVar;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;
import edu.brown.cs.systems.pivottracing.query.Components.QuerySource;
import edu.brown.cs.systems.pivottracing.query.Components.Var;
import edu.brown.cs.systems.pivottracing.query.Components.WhereCondition;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoint;

/** Query under construction that has not yet ended with a select or groupby */
public class PTQuery_Partial extends PTQuery {

    protected PTQuery_Partial(QuerySource source) {
        super(source);
    }

    protected PTQuery_Partial(PTQuery other) {
        super(other);
    }

    /** Select some keys to output for this query */
    public PTQuery_Select Select(String... ks) throws PTQueryException {
        return new PTQuery_Select(this, ks);
    }

    /** Group the output by the specified keys */
    public PTQuery_GroupBy GroupBy(String... groupByVars) throws PTQueryException {
        return new PTQuery_GroupBy(this, groupByVars);
    }

    /** Add a filter to this query */
    public PTQuery_Partial Where(String replacementExpression, String... replacementVariables) throws PTQueryException {
        PTQuery_Partial copy = new PTQuery_Partial(this);
        Var[] vars = copy.requireInputs(replacementVariables);
        copy.conditions.add(new WhereCondition(replacementExpression, vars));
        return copy;
    }

    /** Define a variable */
    public PTQuery_Partial Let(String newVarName, String replacementExpression, String... replacementVariables) throws PTQueryException {
        PTQuery_Partial copy = new PTQuery_Partial(this);
        copy.checkVarName(newVarName);
        List<Var> vars = Lists.newArrayList(copy.requireInputs(replacementVariables));
        LetVar let = new LetVar(newVarName, replacementExpression, vars);
        copy.constructed.put(newVarName, let);
        return copy;
    }
    
    /** Happened-before join with the other query.  This is:
     * 
     * ... (query so far) ...
     * Join q in Q ON q -> (this)
     */
    public PTQuery_Partial HappenedBeforeJoin(String q, PTQuery Q) throws PTQueryException {
        return AddHappenedBefore(this, q, Q);
    }
    
    /** Happened-before join with the other query, with a filter.  This is:
     * 
     * ... (query so far) ...
     * Join q In Filter(Q) ON q -> (this)
     * 
     * Filters cannot be applied to queries once Select or GROUPBY has been used
     */
    public PTQuery_Partial HappenedBeforeJoin(Filter filter, String q, PTQuery_Partial Q) throws PTQueryException {
        return AddHappenedBefore(this, filter, q, Q);
        
    }
    
    /** Happened-before join with the other tracepoint. This is:
     * 
     *  ... (query so far) ...
     *  Join x In tracepoint ON x -> (this)
     */
    public PTQuery_Partial HappenedBeforeJoin(String x, Tracepoint tracepoint) throws PTQueryException {
        return AddHappenedBefore(this, x, PTQuery.From(tracepoint));
    }
    
    /** Happened-before join with the other tracepoint. This is:
     * 
     *  ... (query so far) ...
     *  Join x In Filter(tracepoint) ON x -> (this)
     */
    public PTQuery_Partial HappenedBeforeJoin(Filter filter, String x, Tracepoint tracepoint) throws PTQueryException {
        return AddHappenedBefore(this, filter, x, PTQuery.From(tracepoint));
    }
    
    @Override
    protected Var requireOutput(String varName) throws PTQueryException {
        Var v = requireInput(varName);
        outputs.add(v);
        return v;
    }

    public PTQuery copy() {
        return new PTQuery_Partial(this);
    }

    @Override
    protected boolean optimizable(WhereCondition condition) {
        return outputs.containsAll(condition.replacementVariables);
    }

    @Override
    protected boolean optimizable(LetVar let) {
        return outputs.containsAll(let.replacementVariables);
    }

}
