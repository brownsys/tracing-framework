package edu.brown.cs.systems.pivottracing.query;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.pivottracing.query.Components.LetVar;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;
import edu.brown.cs.systems.pivottracing.query.Components.Var;
import edu.brown.cs.systems.pivottracing.query.Components.WhereCondition;

public class PTQuery_Select extends PTQuery {
    
    public final List<Var> selected = Lists.newArrayList();
    
    private PTQuery_Select(PTQuery_Select other) {
        super(other);
        this.selected.addAll(other.selected);
    }
    
    protected PTQuery_Select(PTQuery other, String... keysToSelect) throws PTQueryException {
        super(other);
        for (String key : keysToSelect) {
            selected.add(requireInput(key));
        }
    }

    public PTQuery copy() {
        return new PTQuery_Select(this);
    }

    @Override
    protected Var requireOutput(String varName) throws PTQueryException {
        Var v = requireInput(varName);
        if (selected.contains(v)) {
            outputs.add(v);
            return v;            
        }
        throw new PTQueryException("Output %s not part of Select query", varName);
    }

    @Override
    protected boolean optimizable(WhereCondition condition) {
        return outputs.containsAll(condition.replacementVariables);
    }

    @Override
    protected boolean optimizable(LetVar let) {
        return outputs.containsAll(let.replacementVariables);
    }
    
    /** Variables that will be output by this query */
    Collection<Var> outputs(boolean pack) {
        if (pack) {
            return outputs;
        } else {
            return selected;
        }
    }

}
