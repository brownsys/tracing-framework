/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2013, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.concurrent.impl

import edu.brown.cs.systems.xtrace.reporting.{XTraceReport, XTraceReporter}
import org.aspectj.lang.JoinPoint

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import scala.util.{ Success, Failure }
import edu.brown.cs.systems.baggage.Baggage
import edu.brown.cs.systems.baggage.DetachedBaggage
import edu.brown.cs.systems.tracing.aspects.Annotations.BaggageInheritanceDisabled;

object FutureWithBaggage {

  @BaggageInheritanceDisabled
  class PromiseCompletingRunnableWithBaggage[T](body: => T, baggage: DetachedBaggage, jp: JoinPoint.StaticPart) extends Runnable {
    val promiseWithBaggage = new PromiseWithBaggage.DefaultPromiseWithBaggage[T]()

    override def run() = {

      XTraceReport.entering(jp)
      val old: DetachedBaggage = Baggage.swap(baggage);
      XTraceReport.left(jp)

      val result = {
        try Success(body) catch { case NonFatal(e) => Failure(e) }
      }

      XTraceReport.entering(jp)
      promiseWithBaggage.complete(result)
      XTraceReport.left(jp)

      XTraceReport.entering(jp)
      Baggage.swap(old)
      XTraceReport.left(jp)
    }
  }

  def apply[T](body: =>T, jp: JoinPoint.StaticPart)(implicit executor: ExecutionContext): scala.concurrent.Future[T] = {
    XTraceReport.entering(jp)
    val baggage = Baggage.fork()
    XTraceReport.left(jp)

    val runnable = new PromiseCompletingRunnableWithBaggage(body, baggage, jp)
    executor.prepare.execute(runnable)
    runnable.promiseWithBaggage.future
  }
}