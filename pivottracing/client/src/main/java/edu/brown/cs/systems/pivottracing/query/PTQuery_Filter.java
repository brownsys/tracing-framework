package edu.brown.cs.systems.pivottracing.query;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Filter;
import edu.brown.cs.systems.pivottracing.query.Components.LetVar;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;
import edu.brown.cs.systems.pivottracing.query.Components.Var;
import edu.brown.cs.systems.pivottracing.query.Components.WhereCondition;

public class PTQuery_Filter extends PTQuery {

    public final Filter filter;
    
    protected PTQuery_Filter(PTQuery other, Filter filter) {
        super(other);
        this.filter = filter;
    }

    @Override
    public PTQuery copy() {
        return new PTQuery_Filter(this, filter);
    }

    @Override
    protected Var requireOutput(String varName) throws PTQueryException {
        Var v = requireInput(varName);
        outputs.add(v);
        return v;
    }

    @Override
    protected boolean optimizable(WhereCondition condition) {
        // Filter queries can't optimize anything
        return false;
    }

    @Override
    protected boolean optimizable(LetVar let) {
        // Filter queries can't optimize anything
        return false;
    }
    
}
