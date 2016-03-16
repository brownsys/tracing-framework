import edu.brown.cs.systems.tracing.aspects.scala.FutureUtils;
import scala.Function0;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.Future$;
import scala.concurrent.impl.FutureWithBaggage;

public class sdfads {
    
    public static void main(String[] args) {
        FutureUtils.test();
    }

    public static Future proxy(Function0 f, ExecutionContext exec){
        return FutureWithBaggage.apply(f, exec);
    }

}
