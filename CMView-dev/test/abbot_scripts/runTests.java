package test.abbot_scripts;

import java.io.File;

import junit.extensions.abbot.*;
import junit.framework.Test;


public class runTests extends ScriptFixture {

    /** Name is the name of a script filename. */
    public runTests(String name) {
        super(name);
    }

    /** Return the set of scripts we want to run. */
    public static Test suite() {
        return new ScriptTestSuite(runTests.class, "test/abbot_scripts") {
            /** Determine whether the given script should be included. */
            public boolean accept(File file) {
                String name = file.getName();
                return name.startsWith("load");
            }
        };
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, runTests.class);
    }
}