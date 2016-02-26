package edu.brown.cs.systems.tracing.aspects;

import edu.brown.cs.systems.tracing.Utils;

/**
 * Generally useful miscellaneous aspects go here
 */
public aspect UtilityAspects {

    /**
     * When a main method is run, immediately find out the name of the class
     */
    before(): execution(public static void main(String[])) {
        Utils.getMainClass();
    }

}
