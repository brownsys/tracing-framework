package edu.brown.cs.systems.tracing

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


object Main {
  def main(args: Array[String]): Unit = {
    println("Hello, world!ddd")
    
    val p1:Promise[String] = Promise[String]()

    val p2 = promise[String]()
    val p3 = promise()
    
    val s = "Hello"
    val f: Future[String] = Future {
      s + " future!"
    }
    val f2 = future {
      s + " future!"
    }
    println(p1.getClass().getName())
    println(p2.getClass().getName())
    println(p3.getClass().getName())
    println(f.getClass().getName())
    println(f2.getClass().getName())

    p1.completeWith(f)

    Await.ready(p1.future, Duration.Inf)
    
  }
}