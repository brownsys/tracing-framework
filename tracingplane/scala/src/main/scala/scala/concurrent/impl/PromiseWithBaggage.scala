/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2013, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.concurrent.impl

import scala.concurrent.{ ExecutionContext, CanAwait, OnCompleteRunnable, TimeoutException, ExecutionException, blocking }
import scala.concurrent.Future.InternalCallbackExecutor
import scala.concurrent.duration.{ Duration, Deadline, FiniteDuration, NANOSECONDS }
import scala.annotation.tailrec
import scala.util.control.NonFatal
import scala.util.{ Try, Success, Failure }
import java.io.ObjectInputStream
import java.util.concurrent.locks.AbstractQueuedSynchronizer
import edu.brown.cs.systems.baggage.{ DetachedBaggage, Baggage }

private[concurrent] trait PromiseWithBaggage[T] extends scala.concurrent.Promise[T] with scala.concurrent.Future[T] {
  def future: this.type = this
}


/* Precondition: `executor` is prepared, i.e., `executor` has been returned from invocation of `prepare` on some other `ExecutionContext`.
 */
private class CallbackRunnableWithBaggage[T](executor: ExecutionContext, onComplete: Try[T] => Any) extends CallbackRunnable[T](executor, onComplete) {
  var baggage: DetachedBaggage = null

  override def run() = {
    require(value ne null) // must set value to non-null before running!
    val old: DetachedBaggage = Baggage.swap(baggage)
    try onComplete(value) catch { case NonFatal(e) => executor reportFailure e }
    Baggage.swap(old)
  }

  def executeWithValueAndBaggage(v: Try[T], b: DetachedBaggage): Unit = {
    require(value eq null) // can't complete it twice
    value = v
    baggage = b
    // Note that we cannot prepare the ExecutionContext at this point, since we might
    // already be running on a different thread!
    try executor.execute(this) catch { case NonFatal(t) => executor reportFailure t }
  }
}

private[concurrent] object PromiseWithBaggage {

  private def resolveTry[T](source: Try[T]): Try[T] = source match {
    case Failure(t) => resolver(t)
    case _          => source
  }

  private def resolver[T](throwable: Throwable): Try[T] = throwable match {
    case t: scala.runtime.NonLocalReturnControl[_] => Success(t.value.asInstanceOf[T])
    case t: scala.util.control.ControlThrowable    => Failure(new ExecutionException("Boxed ControlThrowable", t))
    case t: InterruptedException                   => Failure(new ExecutionException("Boxed InterruptedException", t))
    case e: Error                                  => Failure(new ExecutionException("Boxed Error", e))
    case t                                         => Failure(t)
  }

   /**
    * Latch used to implement waiting on a DefaultPromise's result.
    *
    * Inspired by: http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/locks/AbstractQueuedSynchronizer.java
    * Written by Doug Lea with assistance from members of JCP JSR-166
    * Expert Group and released to the public domain, as explained at
    * http://creativecommons.org/publicdomain/zero/1.0/
    */
    final class CompletionLatch[T] extends AbstractQueuedSynchronizer with (Try[T] => Unit) {
      override protected def tryAcquireShared(ignored: Int): Int = if (getState != 0) 1 else -1
      override protected def tryReleaseShared(ignore: Int): Boolean = {
        setState(1)
        true
      }
      override def apply(ignored: Try[T]): Unit = releaseShared(1)
    }


  /** Default promise implementation.
   *
   *  A DefaultPromise has three possible states. It can be:
   *
   *  1. Incomplete, with an associated list of callbacks waiting on completion.
   *  2. Complete, with a result.
   *  3. Linked to another DefaultPromise.
   *
   *  If a DefaultPromise is linked to another DefaultPromise, it will
   *  delegate all its operations to that other promise. This means that two
   *  DefaultPromises that are linked will appear, to external callers, to have
   *  exactly the same state and behaviour. For instance, both will appear as
   *  incomplete, or as complete with the same result value.
   *
   *  A DefaultPromise stores its state entirely in the AnyRef cell exposed by
   *  AbstractPromise. The type of object stored in the cell fully describes the
   *  current state of the promise.
   *
   *  1. List[CallbackRunnable] - The promise is incomplete and has zero or more callbacks
   *     to call when it is eventually completed.
   *  2. Try[T] - The promise is complete and now contains its value.
   *  3. DefaultPromise[T] - The promise is linked to another promise.
   *
   * The ability to link DefaultPromises is needed to prevent memory leaks when
   * using Future.flatMap. The previous implementation of Future.flatMap used
   * onComplete handlers to propagate the ultimate value of a flatMap operation
   * to its promise. Recursive calls to flatMap built a chain of onComplete
   * handlers and promises. Unfortunately none of the handlers or promises in
   * the chain could be collected until the handlers had been called and
   * detached, which only happened when the final flatMap future was completed.
   * (In some situations, such as infinite streams, this would never actually
   * happen.) Because of the fact that the promise implementation internally
   * created references between promises, and these references were invisible to
   * user code, it was easy for user code to accidentally build large chains of
   * promises and thereby leak memory.
   *
   * The problem of leaks is solved by automatically breaking these chains of
   * promises, so that promises don't refer to each other in a long chain. This
   * allows each promise to be individually collected. The idea is to "flatten"
   * the chain of promises, so that instead of each promise pointing to its
   * neighbour, they instead point directly the promise at the root of the
   * chain. This means that only the root promise is referenced, and all the
   * other promises are available for garbage collection as soon as they're no
   * longer referenced by user code.
   *
   * To make the chains flattenable, the concept of linking promises together
   * needed to become an explicit feature of the DefaultPromise implementation,
   * so that the implementation to navigate and rewire links as needed. The idea
   * of linking promises is based on the [[Twitter promise implementation
   * https://github.com/twitter/util/blob/master/util-core/src/main/scala/com/twitter/util/Promise.scala]].
   *
   * In practice, flattening the chain cannot always be done perfectly. When a
   * promise is added to the end of the chain, it scans the chain and links
   * directly to the root promise. This prevents the chain from growing forwards
   * But the root promise for a chain can change, causing the chain to grow
   * backwards, and leaving all previously-linked promise pointing at a promise
   * which is no longer the root promise.
   *
   * To mitigate the problem of the root promise changing, whenever a promise's
   * methods are called, and it needs a reference to its root promise it calls
   * the `compressedRoot()` method. This method re-scans the promise chain to
   * get the root promise, and also compresses its links so that it links
   * directly to whatever the current root promise is. This ensures that the
   * chain is flattened whenever `compressedRoot()` is called. And since
   * `compressedRoot()` is called at every possible opportunity (when getting a
   * promise's value, when adding an onComplete handler, etc), this will happen
   * frequently. Unfortunately, even this eager relinking doesn't absolutely
   * guarantee that the chain will be flattened and that leaks cannot occur.
   * However eager relinking does greatly reduce the chance that leaks will
   * occur.
   *
   * Future.flatMap links DefaultPromises together by calling the `linkRootOf`
   * method. This is the only externally visible interface to linked
   * DefaultPromises, and `linkedRootOf` is currently only designed to be called
   * by Future.flatMap.
   */
  class DefaultPromiseWithBaggage[T] extends AbstractPromise with PromiseWithBaggage[T] { self =>
    updateState(null, Nil) // The promise is incomplete and has no callbacks

    /** Get the root promise for this promise, compressing the link chain to that
     *  promise if necessary.
     *
     *  For promises that are not linked, the result of calling
     *  `compressedRoot()` will the promise itself. However for linked promises,
     *  this method will traverse each link until it locates the root promise at
     *  the base of the link chain.
     *
     *  As a side effect of calling this method, the link from this promise back
     *  to the root promise will be updated ("compressed") to point directly to
     *  the root promise. This allows intermediate promises in the link chain to
     *  be garbage collected. Also, subsequent calls to this method should be
     *  faster as the link chain will be shorter.
     */
    @tailrec
    private def compressedRoot(): DefaultPromiseWithBaggage[T] = {
      getState match {
        case linked: DefaultPromiseWithBaggage[_] =>
          val target = linked.asInstanceOf[DefaultPromiseWithBaggage[T]].root
          if (linked eq target) target else if (updateState(linked, target)) target else compressedRoot()
        case _ => this
      }
    }

    /** Get the promise at the root of the chain of linked promises. Used by `compressedRoot()`.
     *  The `compressedRoot()` method should be called instead of this method, as it is important
     *  to compress the link chain whenever possible.
     */
    @tailrec
    private def root: DefaultPromiseWithBaggage[T] = {
      getState match {
        case linked: DefaultPromiseWithBaggage[_] => linked.asInstanceOf[DefaultPromiseWithBaggage[T]].root
        case _ => this
      }
    }

    /** Try waiting for this promise to be completed.
     */
    protected final def tryAwait(atMost: Duration): Boolean = {
      if (!isCompleted) {
        import Duration.Undefined
        import scala.concurrent.Future.InternalCallbackExecutor
        atMost match {
          case e if e eq Undefined => throw new IllegalArgumentException("cannot wait for Undefined period")
          case Duration.Inf        =>
            val l = new CompletionLatch[T]()
            onComplete(l)(InternalCallbackExecutor)
            l.acquireSharedInterruptibly(1)
          case Duration.MinusInf   => // Drop out
          case f: FiniteDuration   =>
            if (f > Duration.Zero) {
              val l = new CompletionLatch[T]()
              onComplete(l)(InternalCallbackExecutor)
              l.tryAcquireSharedNanos(1, f.toNanos)
            }
        }
      }
      if (isCompleted) {
        Baggage.join(baggage)
        return true
      } else {
        return false
      }
    }

    @throws(classOf[TimeoutException])
    @throws(classOf[InterruptedException])
    def ready(atMost: Duration)(implicit permit: CanAwait): this.type =
      if (tryAwait(atMost)) this
      else throw new TimeoutException("Futures timed out after [" + atMost + "]")

    @throws(classOf[Exception])
    def result(atMost: Duration)(implicit permit: CanAwait): T =
      ready(atMost).value.get.get // ready throws TimeoutException if timeout so value.get is safe here

    def value: Option[Try[T]] = value0

    @tailrec
    private def value0: Option[Try[T]] = getState match {
      case (c: Try[_], b: DetachedBaggage) => Some(c.asInstanceOf[Try[T]])
      case _: DefaultPromiseWithBaggage[_] => compressedRoot().value0
      case _ => None
    }
    
    @tailrec
    private def baggage: DetachedBaggage = getState match {
      case (c: Try[_], b: DetachedBaggage) => b
      case _: DefaultPromiseWithBaggage[_] => compressedRoot().baggage
      case _ => null
    }

    override def isCompleted: Boolean = isCompleted0

    @tailrec
    private def isCompleted0: Boolean = getState match {
      case (_: Try[_], _: DetachedBaggage) => true
      case _: DefaultPromiseWithBaggage[_] => compressedRoot().isCompleted0
      case _ => false
    }

    def tryComplete(value: Try[T]): Boolean = {
      tryComplete(value, Baggage.fork())
    }

    def tryComplete(value: Try[T], baggage: DetachedBaggage): Boolean = {
      val resolved = resolveTry(value)
      tryCompleteAndGetListeners(resolved, baggage) match {
        case null             => false
        case rs if rs.isEmpty => true
        case rs               => rs.foreach(r => r match {
          case rb: CallbackRunnableWithBaggage[T] => rb.executeWithValueAndBaggage(resolved, Baggage.fork())
          case _ => r.executeWithValue(resolved)
        }); true
      }
    }

    /** Called by `tryComplete` to store the resolved value and get the list of
     *  listeners, or `null` if it is already completed.
     */
    @tailrec
    private def tryCompleteAndGetListeners(v: Try[T], b: DetachedBaggage): List[CallbackRunnable[T]] = {
      getState match {
        case raw: List[_] =>
          val cur = raw.asInstanceOf[List[CallbackRunnable[T]]]
          if (updateState(cur, (v, b))) cur else tryCompleteAndGetListeners(v, b)
        case _: DefaultPromiseWithBaggage[_] =>
          compressedRoot().tryCompleteAndGetListeners(v, b)
        case _ => null
      }
    }

    def onComplete[U](func: Try[T] => U)(implicit executor: ExecutionContext): Unit = {
      val preparedEC = executor.prepare()
      val runnable = func match {
        case _: CompletionLatch[T] => new CallbackRunnable[T](preparedEC, func)
        case _ => new CallbackRunnableWithBaggage[T](preparedEC, func)
      }
      dispatchOrAddCallback(runnable)
    }

    /** Tries to add the callback, if already completed, it dispatches the callback to be executed.
     *  Used by `onComplete()` to add callbacks to a promise and by `link()` to transfer callbacks
     *  to the root promise when linking two promises togehter.
     */
    @tailrec
    private def dispatchOrAddCallback(runnable: CallbackRunnable[T]): Unit = {
      getState match {
        case (r: Try[_], b: DetachedBaggage) => {
          runnable match {
            case rb: CallbackRunnableWithBaggage[T] => rb.executeWithValueAndBaggage(r.asInstanceOf[Try[T]], b)
            case _ => runnable.executeWithValue(r.asInstanceOf[Try[T]])
          }
        }
        case _: DefaultPromiseWithBaggage[_] => compressedRoot().dispatchOrAddCallback(runnable)
        case listeners: List[_] => if (updateState(listeners, runnable :: listeners)) () else dispatchOrAddCallback(runnable)
      }
    }

    /** Link this promise to the root of another promise using `link()`. Should only be
     *  be called by Future.flatMap.
     */
    protected[concurrent] final def linkRootOf(target: DefaultPromiseWithBaggage[T]): Unit = link(target.compressedRoot())

    /** Link this promise to another promise so that both promises share the same
     *  externally-visible state. Depending on the current state of this promise, this
     *  may involve different things. For example, any onComplete listeners will need
     *  to be transferred.
     *
     *  If this promise is already completed, then the same effect as linking -
     *  sharing the same completed value - is achieved by simply sending this
     *  promise's result to the target promise.
     */
    @tailrec
    private def link(target: DefaultPromiseWithBaggage[T]): Unit = if (this ne target) {
      getState match {
        case (r: Try[_], b: DetachedBaggage) =>
          if (!target.tryComplete(r.asInstanceOf[Try[T]], b)) {
            // Currently linking is done from Future.flatMap, which should ensure only
            // one promise can be completed. Therefore this situation is unexpected.
            throw new IllegalStateException("Cannot link completed promises together")
          }
        case _: DefaultPromiseWithBaggage[_] =>
          compressedRoot().link(target)
        case listeners: List[_] => if (updateState(listeners, target)) {
          if (!listeners.isEmpty) listeners.asInstanceOf[List[CallbackRunnable[T]]].foreach(target.dispatchOrAddCallback(_))
        } else link(target)
      }
    }
  }

  /** An already completed Future is given its result at creation.
   *
   *  Useful in Future-composition when a value to contribute is already available.
   */
  final class KeptPromise[T](suppliedValue: Try[T]) extends PromiseWithBaggage[T] {

    val value = Some(resolveTry(suppliedValue))

    override def isCompleted: Boolean = true

    def tryComplete(value: Try[T]): Boolean = false

    def onComplete[U](func: Try[T] => U)(implicit executor: ExecutionContext): Unit = {
      val completedAs = value.get
      val preparedEC = executor.prepare()
      (new CallbackRunnable(preparedEC, func)).executeWithValue(completedAs)
    }

    def ready(atMost: Duration)(implicit permit: CanAwait): this.type = this

    def result(atMost: Duration)(implicit permit: CanAwait): T = value.get.get
  }

}