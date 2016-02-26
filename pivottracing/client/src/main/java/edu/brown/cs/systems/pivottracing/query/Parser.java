package edu.brown.cs.systems.pivottracing.query;

import org.apache.commons.lang3.tuple.Pair;

import edu.brown.cs.systems.pivottracing.PivotTracingClient;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Agg;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Filter;
import edu.brown.cs.systems.pivottracing.query.Components.NamedQuerySource;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoint;

public class Parser {
    
    public final PivotTracingClient pt;
    
    public static final String from = "from";
    public static final String in = "in";
    public static final String join = "join";
    public static final String on = "on";
    public static final String hb = "->";
    public static final String where = "where";
    public static final String let = "let";
    public static final String groupby = "groupby";
    public static final String select = "select";
    public static final String count = Agg.COUNT.name();
    
    public Parser(PivotTracingClient pt) {
        this.pt = pt;
    }
    
    public PTQuery parse(String queryName, String queryString) throws PTQueryParserException, PTQueryException {
        PTQuery query = parse(queryString);
        pt.queries.put(queryName, query);
        return query;
    }
    
    private PTQuery parse(String queryString) throws PTQueryParserException, PTQueryException {
        String[] lines = queryString.split("\n");
        PTQuery_Partial q = parseFrom(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            String type = tokens(lines[i])[0].toLowerCase();
            switch (type) {
            case from: throw new PTQueryParserException("Found multiple From statements in query");
            case join: q = parseHappenedBefore(q, lines[i]); break;
            case where: q = parseWhere(q, lines[i]); break;
            case let: q = parseLet(q, lines[i]); break;
            case select: return parseSelect(q, lines[i]);
            case groupby: return parseGroupBy(q, lines[i], lines[i+1]);
            }
        }
        return q;
    }
    
    private PTQuery_Partial parseFrom(String fromLine) throws PTQueryParserException, PTQueryException {
        String[] splits = tokens(fromLine);
        checkToken(fromLine, splits[0], from);
        if (splits.length == 2) {
            // From tracepoint
            String tracepointName = splits[1];
            if (!pt.tracepoints.containsKey(tracepointName)) {
                throw new PTQueryException("Unknown tracepoint %s", tracepointName);
            }
            return PTQuery.From(pt.tracepoints.get(tracepointName));
        } else if (splits.length == 4) {
            // From x In tracepoint
            checkToken(fromLine, splits[2], in);
            String varName = splits[1];
            String tracepointName = splits[3];
            if (!pt.tracepoints.containsKey(tracepointName)) {
                throw new PTQueryException("Unknown tracepoint %s", tracepointName);
            }
            return PTQuery.From(varName, pt.tracepoints.get(tracepointName));
        } else {
            throw new PTQueryParserException("Unable to parse query line %s", fromLine);
        }
    }

    private PTQuery_Partial parseHappenedBefore(PTQuery_Partial Q, String hbLine) throws PTQueryParserException, PTQueryException {
        String[] splits = tokens(hbLine);
        checkToken(hbLine, splits[0], join);
        checkToken(hbLine, splits[2], in);
        checkToken(hbLine, splits[4], on);
        checkToken(hbLine, splits[6], hb);
        // Can't HBJoin if you don't have a name for what you're joining to ...
        if (!(Q.source instanceof NamedQuerySource)) {
            throw new PTQueryParserException("Invalid line %s", hbLine);
        }
        
        // Make sure it's on the correct side of the join
        String x = splits[1];
        if (!splits[5].equals(x)) {
            throw new PTQueryParserException("Cannot do HBJoin starting from %s, expected %s", splits[5], x);
        }
        
        // Pull out the tracepoint, query, filter
        String X = splits[3];
        Pair<Filter, String> filtered = QueryUtils.splitFilter(X);
        if (filtered != null) {
            X = filtered.getRight();
        }
        Tracepoint tracepoint = pt.tracepoints.get(X);
        PTQuery query = pt.queries.get(X);
        
        // Hopefully it's just joining to us
        String jointo = splits[7];
        String ourname = ((NamedQuerySource) Q.source).name;
        if (jointo.equals(ourname)) {
            if (tracepoint != null && filtered != null) {
                return Q.HappenedBeforeJoin(filtered.getLeft(), x, tracepoint);
            } else if (tracepoint != null) {
                return Q.HappenedBeforeJoin(x, tracepoint);
            } else if (query != null && filtered != null && !(query instanceof PTQuery_Partial)) {
                throw new PTQueryParserException("Cannot apply filter to query %s", X);
            } else if (query != null && filtered != null) {
                return Q.HappenedBeforeJoin(filtered.getLeft(), x, (PTQuery_Partial) query);
            } else if (query != null) {
                return Q.HappenedBeforeJoin(x, query);
            } else {
                throw new PTQueryException("Unknown query or tracepoint %s", X);
            }
        } else if (Q.happenedBefore.containsKey(jointo)) {
            Q = (PTQuery_Partial) Q.copy();
            PTQuery upstream = Q.happenedBefore.get(jointo).copy();
            upstream.checkVarName(jointo);          // this is not required, but here for now. TODO
            if (tracepoint != null && filtered != null) {
                upstream = PTQuery.AddHappenedBefore(upstream, filtered.getLeft(), x, PTQuery.From(tracepoint));
            } else if (tracepoint != null) {
                upstream = PTQuery.AddHappenedBefore(upstream, x, PTQuery.From(tracepoint));
            } else if (query != null && filtered != null && !(query instanceof PTQuery_Partial)) {
                throw new PTQueryParserException("Cannot apply filter to query %s", X);
            } else if (query != null && filtered != null) {
                upstream = PTQuery.AddHappenedBefore(upstream, filtered.getLeft(), x, (PTQuery_Partial) query);
            } else if (query != null) {
                upstream = PTQuery.AddHappenedBefore(upstream, x, (PTQuery_Partial) query);
            }
            String fullyQualifiedX = String.format("%s.%s", jointo, x);
            Q.happenedBefore.put(jointo, upstream);
            Q.shortVars.put(x, fullyQualifiedX);
            return Q;
        }
        
        throw new PTQueryParserException("Unknown query or tracepoint %s", jointo);
    }
    
    private PTQuery_Partial parseWhere(PTQuery_Partial q, String whereLine) throws PTQueryParserException, PTQueryException {
        String[] splits = whereLine.split("\\s+");
        checkToken(whereLine, splits[0], where);
        String replacementExpression = splits[1];
        String[] replacementVariables = subtokens(splits, 2);        
        return q.Where(replacementExpression, replacementVariables);
    }
    
    private PTQuery_Partial parseLet(PTQuery_Partial q, String letLine) throws PTQueryParserException, PTQueryException {
        String[] splits = letLine.split("\\s+");
        checkToken(letLine, splits[0], let);
        String newVarName = splits[1];
        String replacementExpression = splits[2];
        String[] replacementVariables = subtokens(splits, 3);
        return q.Let(newVarName, replacementExpression, replacementVariables);
    }
    
    private PTQuery parseSelect(PTQuery_Partial q, String selectLine) throws PTQueryParserException, PTQueryException {
        String[] splits = selectLine.split("\\s+");
        checkToken(selectLine, splits[0], select);
        String[] keysToSelect = subtokens(splits, 1);
        return q.Select(keysToSelect);
    }
    
    private PTQuery parseGroupBy(PTQuery_Partial q, String groupByLine, String selectLine) throws PTQueryParserException, PTQueryException {
        String[] splits = groupByLine.split("\\s+");
        checkToken(groupByLine, splits[0], groupby);
        String[] groupByVars = subtokens(splits, 1);
        PTQuery_GroupBy g = q.GroupBy(groupByVars);
        
        // Now do select line
        splits = selectLine.split("\\s+");
        checkToken(selectLine, splits[0], select);
        
        // Select vars must match group vars (for now)
        String[] selectVars = subtokens(splits, 1);
        for (int i = 0; i < groupByVars.length; i++) {
            if (!groupByVars[i].equals(selectVars[i])) {
                throw new PTQueryParserException(String.format("Mismatch in GroupBy and Select keys: %s and %s", groupByVars[i], selectVars[i]));
            }
        }
        
        // Must have at least one aggregation
        if (selectVars.length == groupByVars.length) {
            throw new PTQueryParserException("No GroupBy aggregation specifieid: " + selectLine);
        }
        
        // Parse the aggregations
        for (int i = groupByVars.length; i < selectVars.length; i++) {
            if (selectVars[i].toUpperCase().equals(count)) {
                g = g.Count();
            } else {
                Pair<Agg, String> agg = QueryUtils.splitAgg(selectVars[i]);
                if (agg == null) {
                    throw new PTQueryParserException("Unable to parse aggregation type " + selectVars[i]);
                }
                g = g.Aggregate(agg.getRight(), agg.getLeft());
            }
        }
        return g;
    }
    
    private static String[] subtokens(String[] tokens, int startAt) {
        String[] subtokens = new String[tokens.length - startAt];
        for (int i = startAt; i < tokens.length; i++) {
            subtokens[i-startAt] = tokens[i].replace(",", "");
        }
        return subtokens;
    }
    
    private static String[] tokens(String line) {
        return line.trim().split("\\s+");
    }
    
    private static void checkToken(String line, String token, String expected) throws PTQueryParserException {
        if (!expected.equals(token.toLowerCase())) {
            throw new PTQueryParserException(String.format("Exception parsing query line %s: found token %s, expected %s", line, token, expected));
        }
    }
    
    /** General exception if we fail to parse a query */
    public static class PTQueryParserException extends Exception {
        public PTQueryParserException(String message) {
            super(message);
        }
        public PTQueryParserException(String format, Object... args) {
            this(String.format(format, args));
        }
    }

}
