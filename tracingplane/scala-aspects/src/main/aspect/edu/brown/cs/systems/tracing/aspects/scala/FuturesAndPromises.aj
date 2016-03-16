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
        System.out.println("Advising " + thisJoinPointStaticPart);
        return FutureWithBaggage.apply(f, e);
    }

    Promise around(): call(Promise+ Promise$.apply()) ||
                      call(Promise+ ops$+.promise()) ||
                      call(Promise+ ops+.promise()) ||
                      call(Promise+ package$+.promise()) {
        System.out.println("Advising promise " + thisJoinPointStaticPart);
        return new PromiseWithBaggage.DefaultPromiseWithBaggage();
    }

}
