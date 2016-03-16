package edu.brown.cs.systems.tracing

import edu.brown.cs.systems.xtrace.XTrace

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Try, Success}


object Main {
  def main(args: Array[String]): Unit = {
    XTrace.startTask(true)
    XTrace.getDefaultLogger().tag("scala test", "Scala test");
    println("Hello, world!ddd")
    
    val p1:Promise[String] = Promise[String]()

    val p2 = promise[String]()
    val p3: Promise[String] = promise[String]()
    
    val s = "Hello"
    val f: Future[String] = Future {
      s + " future!"
    }
    val f2 = future {
      s + " future!"
    }
    val f3 = future {
      p3.complete(Success("boo"))
    }
    println(p1.getClass().getName())
    println(p2.getClass().getName())
    println(p3.getClass().getName())
    println(f.getClass().getName())
    println(f2.getClass().getName())

    p1.completeWith(f)

    Await.ready(p1.future, Duration.Inf)


    XTrace.getDefaultLogger().log("Nearly Done");

    Await.result(f2, Duration.Inf)

    XTrace.getDefaultLogger().log("Done");

    val result: String = Await.result[String](p3.future, Duration.Inf)

    XTrace.getDefaultLogger().log("Really Done");

    Thread.sleep(1000)
  }
}