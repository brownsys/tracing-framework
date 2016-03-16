/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2013, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.concurrent.impl

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import scala.util.{ Success, Failure }
import edu.brown.cs.systems.baggage.Baggage
import edu.brown.cs.systems.baggage.DetachedBaggage

object FutureWithBaggage {
  
  class PromiseCompletingRunnableWithBaggage[T](body: => T) extends Runnable {
    val promiseWithBaggage = new PromiseWithBaggage.DefaultPromiseWithBaggage[T]()
    var baggage: DetachedBaggage = Baggage.fork()

    override def run() = {
      promiseWithBaggage complete {
        try Success(body) catch { case NonFatal(e) => Failure(e) }
      }
    }
  }

  def apply[T](body: =>T)(implicit executor: ExecutionContext): scala.concurrent.Future[T] = {
    val runnable = new PromiseCompletingRunnableWithBaggage(body)
    executor.prepare.execute(runnable)
    runnable.promiseWithBaggage.future
  }
}