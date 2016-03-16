package edu.brown.cs.systems.tracing

import scala.concurrent._
import ExecutionContext.Implicits.global


object Main {
  def main(args: Array[String]): Unit = {
    println("Hello, world!ddd")
    
    val promise = Promise[String]()
    
    val s = "Hello"
    val f: Future[String] = Future {
      s + " future!"
    }
    Util.process(f);
    
  }
}