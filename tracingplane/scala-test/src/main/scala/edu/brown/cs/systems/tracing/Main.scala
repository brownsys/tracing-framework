package edu.brown.cs.systems.tracing

import scala.concurrent._
import ExecutionContext.Implicits.global


object Main {
  def main(args: Array[String]): Unit = {
    println("Hello, world!ddd")
    
    val p1 = Promise[String]()

    val p2 = promise[String]()
    val p3 = promise()
    
    val s = "Hello"
    val f: Future[String] = Future {
      s + " future!"
    }
    val f2 = future {
      s + " future!"
    }
    println(f.getClass().getName())

    
  }
}