package edu.brown.cs.systems.tracing.aspects.scala;

/**
 * Generally useful miscellaneous aspects go here
 */
public aspect UtilityAspects {

    /**
     * When a main method is run, immediately find out the name of the class
     */
    before(): execution(public static void main(String[])) {
        System.out.println("Hello from scala aspect");
    }

}
