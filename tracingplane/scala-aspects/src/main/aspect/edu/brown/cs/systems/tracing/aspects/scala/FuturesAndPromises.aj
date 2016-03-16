package edu.brown.cs.systems.tracing.aspects.scala;

import scala.Function0;
import scala.concurrent.Future;
import scala.concurrent.Future$;
import scala.concurrent.Promise;
import scala.concurrent.Promise$;
import scala.concurrent.ops;
import scala.concurrent.ops$;
import scala.concurrent.package$;
import scala.concurrent.ExecutionContext;
import scala.concurrent.impl.FutureWithBaggage;
import scala.concurrent.impl.PromiseWithBaggage;
import edu.brown.cs.systems.xtrace.reporting.XTraceReport;

/**
 * Generally useful miscellaneous aspects go here
 */
public aspect FuturesAndPromises {
    
    Future around(Function0 f, ExecutionContext e): args(f, e) &&
     (call(Future+ Future$+.apply(Function0+, ExecutionContext+)) ||
      call(Future+ ops$+.future(Function0+, ExecutionContext+)) ||
      call(Future+ ops+.future(Function0+, ExecutionContext+)) ||
      call(Future+ package$+.future(Function0+, ExecutionContext+))
      ) {
        return FutureWithBaggage.apply(f, thisJoinPointStaticPart, e);
    }

    Promise around(): call(Promise+ Promise$.apply()) ||
                      call(Promise+ ops$+.promise()) ||
                      call(Promise+ ops+.promise()) ||
                      call(Promise+ package$+.promise()) {
        return new PromiseWithBaggage.DefaultPromiseWithBaggage();
    }

    before(): call(* Promise+.complete(..)) {
      XTraceReport.entering(thisJoinPointStaticPart);
    }

    after(): call(* Promise+.complete(..)) {
      XTraceReport.left(thisJoinPointStaticPart);
    }

    before(): call(* scala.concurrent.Awaitable+.ready(..)) ||
              call(* scala.concurrent.Awaitable+.result(..)) ||
              call(* scala.concurrent.Await+.ready(..)) ||
              call(* scala.concurrent.Await+.result(..)) ||
              call(* scala.concurrent.Await$+.ready(..)) ||
              call(* scala.concurrent.Await$+.result(..)) {
        XTraceReport.entering(thisJoinPointStaticPart);
    }

    after(): call(* scala.concurrent.Awaitable+.ready(..)) ||
              call(* scala.concurrent.Awaitable+.result(..)) ||
              call(* scala.concurrent.Await+.ready(..)) ||
              call(* scala.concurrent.Await+.result(..)) ||
              call(* scala.concurrent.Await$+.ready(..)) ||
              call(* scala.concurrent.Await$+.result(..)) {
        XTraceReport.left(thisJoinPointStaticPart);
    }

}
