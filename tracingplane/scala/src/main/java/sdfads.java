import edu.brown.cs.systems.tracing.aspects.scala.FutureUtils;
import scala.Function0;
import scala.concurrent.*;
import scala.concurrent.impl.FutureWithBaggage;
import scala.concurrent.package$;

public class sdfads {
    
    public static void main(String[] args) {
        System.out.println(ops$.MODULE$.getClass().getName());
        FutureUtils.test();
    }

    public static Future proxy(Function0 f, ExecutionContext exec){
        return FutureWithBaggage.apply(f, exec);
    }

}
