package edu.brown.cs.systems.tracing;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** Asynchronously writes lines to a file */
public class AsyncFileWriter {

  private final BlockingQueue<String> pending = new LinkedBlockingQueue<String>();
  private final FileWriter writer;

  private final Thread flushThread = new Thread() {
    public void run() {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          try {
            synchronized (writer) {
              writer.flush();
            }
            break;
          } catch (IOException e) {
            System.out.println("Failed to flush due to: " + e.getMessage());
          }
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          return;
        }
      }
    }
  };

  private final Thread writeThread = new Thread() {
    public void run() {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          String next = pending.take();
          int failures = 0;
          while (true) {
            try {
              synchronized (writer) {
                writer.write(next);
              }
              break;
            } catch (IOException e) {
              failures++;
              if (failures % 10 == 0) {
                System.out.println("Failed to write " + failures + " times: " + e.getMessage());
                System.out.println("Retrying...");
              }
              Thread.sleep(1000);
            }
          }
        } catch (InterruptedException e) {
          return;
        }
      }
    }
  };
  
  public AsyncFileWriter(String filename) throws IOException {
    this.writer = new FileWriter(filename);
    this.writeThread.setDaemon(true);
    this.flushThread.setDaemon(true);
    this.writeThread.start();
    this.flushThread.start();
  }

  public void write(Object o) {
    if (o != null)
      pending.add(String.valueOf(o));
  }

  public void writeln(Object o) {
    if (o != null)
      pending.add(String.valueOf(o) + "\n");
  }

  public void println(Object o) {
    if (o != null)
      writeln(o);
  }

  public void print(Object o) {
    if (o != null)
      write(o);
  }

}