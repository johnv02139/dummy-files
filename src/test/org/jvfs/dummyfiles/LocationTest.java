package org.jvfs.dummyfiles;

import static org.junit.Assert.assertEquals;
import static org.jvfs.dummyfiles.Location.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests the functions defined in the Location class.
 *
 */
public class LocationTest {

    /**
     * Turns logging off, aside from severe errors.
     *
     * <p>We do this at the very beginning, because we don't want to see log
     * messages.
     */
    @BeforeClass
    public static void setLogging() {
        Reporting.loggingOff();
    }

    /**
     * Turns logging on, for anything up to the "INFO" level.
     *
     * <p>We do this at the very end, because we can't say whether or not
     * whatever comes after us, wants to see log messages.
     */
    @AfterClass
    public static void restoreLogging() {
        Reporting.loggingOn();
    }

    /**
     * Tests how we combine relative paths.
     *
     * <p>It is NOT expected that any of these paths will exist (though it
     * would not be harmful if they did).  The functionality in combinePaths
     * does not access the file system, it just manipulates pathnames.
     */
    @Test
    public void testCombineRelativePath() {
        Path a1 = Paths.get("/Users/steve/Documents/merges");
        Path r1 = Paths.get("spreadsheets/Revenue");
        Path r2 = Paths.get("Documents/spreadsheets/Profits");
        Path r3 = Paths.get("Documents/artwork");

        assertEquals("did not combine relative path as expected;",
                     Paths.get("/Users/steve/Documents/merges/spreadsheets/Revenue"),
                     combinePaths(a1, r1));

        assertEquals("did not combine relative path as expected;",
                     Paths.get("/Users/steve/Documents/merges"
                               + "/Documents/spreadsheets/Profits"),
                     combinePaths(a1, r2));

        assertEquals("did not combine relative path as expected;",
                     Paths.get("/Users/steve/Documents/merges/Documents/artwork"),
                     combinePaths(a1, r3));
    }

    /**
     * Tests how we combine absolute paths on non-Windows systems.
     *
     * <p>It is NOT expected that any of these paths will exist (though it
     * would not be harmful if they did).  The functionality in combinePaths
     * does not access the file system, it just manipulates pathnames.
     */
    public void testCombineAbsolutePathsUnix() {
        Path a1 = Paths.get("/Users/steve/Documents/merges");
        Path a2 = Paths.get("/Users/steve/Documents/spreadsheets/Expenses1");
        Path a3 = Paths.get("/home/steve/Docs/spreadsheets/Expenses2");
        Path a4 = Paths.get("/Users/steve/Documents/merges/Expenses3");
        Path a5 = Paths.get("/Users/steve/Expenses4");
        Path a6 = Paths.get("/Users/steve/Documents");
        Path a7 = Paths.get("/Users/steve/Documents/merges");
        Path a8 = Paths.get("/kernel");

        assertEquals("(a2) did not combine absolute paths as expected;",
                     Paths.get("/Users/steve/Documents/merges/spreadsheets/Expenses1"),
                     combinePaths(a1, a2));

        assertEquals("(a3) did not combine absolute paths as expected;",
                     Paths.get("/Users/steve/Documents/merges"
                               + "/home/steve/Docs/spreadsheets/Expenses2"),
                     combinePaths(a1, a3));

        assertEquals("(a4) did not combine absolute paths as expected;",
                     Paths.get("/Users/steve/Documents/merges/Expenses3"),
                     combinePaths(a1, a4));

        assertEquals("(a5) did not combine absolute paths as expected;",
                     Paths.get("/Users/steve/Documents/merges/Expenses4"),
                     combinePaths(a1, a5));

        assertEquals("(a6) did not combine absolute paths as expected;",
                     Paths.get("/Users/steve/Documents/merges/Documents"),
                     combinePaths(a1, a6));

        assertEquals("(a7) did not combine absolute paths as expected;",
                     Paths.get("/Users/steve/Documents/merges/merges"),
                     combinePaths(a1, a7));

        assertEquals("(a8) did not combine absolute paths as expected;",
                     Paths.get("/Users/steve/Documents/merges/kernel"),
                     combinePaths(a1, a8));
    }

    /**
     * Tests how we combine absolute paths on Windows.
     *
     * <p>It is NOT expected that any of these paths will exist (though it
     * would not be harmful if they did).  The functionality in combinePaths
     * does not access the file system, it just manipulates pathnames.
     */
    public void testCombineAbsolutePathsWindows() {
        Path a1 = Paths.get("C:\\Users\\steve\\Documents\\merges");
        Path a2 = Paths.get("C:\\Users\\steve\\Documents\\spreadsheets\\Expenses1");
        Path a3 = Paths.get("C:\\home\\steve\\Docs\\spreadsheets\\Expenses2");
        Path a4 = Paths.get("C:\\Users\\steve\\Documents\\merges\\Expenses3");
        Path a5 = Paths.get("C:\\Users\\steve\\Expenses4");
        Path a6 = Paths.get("C:\\Users\\steve\\Documents");
        Path a7 = Paths.get("C:\\Users\\steve\\Documents\\merges");
        Path a8 = Paths.get("C:\\kernel");

        assertEquals("(a2) did not combine absolute paths as expected;",
                     Paths.get("C:\\Users\\steve\\Documents\\merges\\"
                               + "spreadsheets\\Expenses1"),
                     combinePaths(a1, a2));

        assertEquals("(a3) did not combine absolute paths as expected;",
                     Paths.get("C:\\Users\\steve\\Documents\\merges"
                               + "\\home\\steve\\Docs\\spreadsheets\\Expenses2"),
                     combinePaths(a1, a3));

        assertEquals("(a4) did not combine absolute paths as expected;",
                     Paths.get("C:\\Users\\steve\\Documents\\merges\\Expenses3"),
                     combinePaths(a1, a4));

        assertEquals("(a5) did not combine absolute paths as expected;",
                     Paths.get("C:\\Users\\steve\\Documents\\merges\\Expenses4"),
                     combinePaths(a1, a5));

        assertEquals("(a6) did not combine absolute paths as expected;",
                     Paths.get("C:\\Users\\steve\\Documents\\merges\\Documents"),
                     combinePaths(a1, a6));

        assertEquals("(a7) did not combine absolute paths as expected;",
                     Paths.get("C:\\Users\\steve\\Documents\\merges\\merges"),
                     combinePaths(a1, a7));

        assertEquals("(a8) did not combine absolute paths as expected;",
                     Paths.get("C:\\Users\\steve\\Documents\\merges\\kernel"),
                     combinePaths(a1, a8));
    }

    /**
     * Tests how we combine paths on Windows that start at the root
     * of a particular device.
     *
     * <p>It is NOT expected that any of these paths will exist (though it
     * would not be harmful if they did).  The functionality in combinePaths
     * does not access the file system, it just manipulates pathnames.
     */
    public void testCombineRootPathsWindows() {
        Path a1 = Paths.get("\\Users\\steve\\Documents\\merges");
        Path a2 = Paths.get("\\Users\\steve\\Documents\\spreadsheets\\Expenses1");
        Path a3 = Paths.get("\\home\\steve\\Docs\\spreadsheets\\Expenses2");
        Path a4 = Paths.get("\\Users\\steve\\Documents\\merges\\Expenses3");
        Path a5 = Paths.get("\\Users\\steve\\Expenses4");
        Path a6 = Paths.get("\\Users\\steve\\Documents");
        Path a7 = Paths.get("\\Users\\steve\\Documents\\merges");
        Path a8 = Paths.get("\\kernel");

        assertEquals("(a2) did not combine absolute paths as expected;",
                     Paths.get("\\Users\\steve\\Documents\\merges\\spreadsheets\\Expenses1"),
                     combinePaths(a1, a2));

        assertEquals("(a3) did not combine absolute paths as expected;",
                     Paths.get("\\Users\\steve\\Documents\\merges"
                               + "\\home\\steve\\Docs\\spreadsheets\\Expenses2"),
                     combinePaths(a1, a3));

        assertEquals("(a4) did not combine absolute paths as expected;",
                     Paths.get("\\Users\\steve\\Documents\\merges\\Expenses3"),
                     combinePaths(a1, a4));

        assertEquals("(a5) did not combine absolute paths as expected;",
                     Paths.get("\\Users\\steve\\Documents\\merges\\Expenses4"),
                     combinePaths(a1, a5));

        assertEquals("(a6) did not combine absolute paths as expected;",
                     Paths.get("\\Users\\steve\\Documents\\merges\\Documents"),
                     combinePaths(a1, a6));

        assertEquals("(a7) did not combine absolute paths as expected;",
                     Paths.get("\\Users\\steve\\Documents\\merges\\merges"),
                     combinePaths(a1, a7));

        assertEquals("(a8) did not combine absolute paths as expected;",
                     Paths.get("\\Users\\steve\\Documents\\merges\\kernel"),
                     combinePaths(a1, a8));
    }

    /**
     * Tests how we combine absolute paths.
     *
     * <p>It is NOT expected that any of these paths will exist (though it
     * would not be harmful if they did).  The functionality in combinePaths
     * does not access the file system, it just manipulates pathnames.
     */
    @Test
    public void testCombineAbsolutePaths() {
        if (Environment.IS_WINDOWS) {
            testCombineAbsolutePathsWindows();
            testCombineRootPathsWindows();
        } else {
            testCombineAbsolutePathsUnix();
        }
    }
}
