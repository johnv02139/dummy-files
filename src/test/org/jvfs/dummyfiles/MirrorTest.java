package org.jvfs.dummyfiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.jvfs.dummyfiles.Environment.BAD_ARGUMENTS;
import static org.jvfs.dummyfiles.Mirror.createMirror;
import static org.jvfs.dummyfiles.SampleTrees.Number;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the functions defined in the Mirror class.
 *
 */
public class MirrorTest {

    /**
     * Single instance of PathComparator we can use for all tests.
     */
    private static final PathComparator pc = new PathComparator();

    /**
     * This rule creates a new temporary folder before each test, and
     * (tries to) delete it at the end of each test.
     */
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Sets up the Mirror tests.
     */
    @BeforeClass
    public static void setupMirrorTest() {
        Reporting.loggingOff();
    }

    /**
     * Tears down the Mirror tests.
     */
    @AfterClass
    public static void restoreLogging() {
        Reporting.loggingOn();
    }

    /**
     * Tests the programmatic API.
     *
     * <p>When the functionality is accessed by Java code, it will use the more
     * straightforward API, which is to simply pass the source and target as
     * individual arguments.
     *
     * @throws IOException if there's a problem accessing the file system
     */
    @Test
    public void testCreateMirror() throws IOException {
        SampleTrees dirs = new SampleTrees(tempFolder);
        String pathname1 = dirs.getPathname(Number.ONE);
        String pathname2 = dirs.getPathname(Number.TWO);

        File dstFolder = tempFolder.newFolder();
        String dst = dstFolder.getAbsolutePath();

        // The SampleTrees object gives us a tree of files, whose location can
        // be accessed at "pathname1".  Create a mirror of that tree, and verify
        // that the return status is zero.
        assertEquals("createMirror returned failure status", 0,
                     createMirror(pathname1, dst));

        // Now use the PathComparator to confirm that the trees really do have
        // identical structure.
        assertTrue("directories created by createMirror were not found equivalent",
                   pc.verifyMirror(pathname1, dst));

        // The SampleTrees object actually creates TWO identical trees of files;
        // the "other" one's location can be accessed at "pathname2".  The mirror
        // of the first tree should be identical to the second tree, as well.
        assertTrue("directory created by createMirror was not found equivalent to copy",
                   pc.verifyMirror(pathname2, dst));
    }

    /**
     * Tests that we refuse to use an existing, populated directory for output.
     *
     * <p>For the postcondition to be that the output directory is an exact
     * mirror of the input directory, there can't be any superfluous files
     * in the output directory.  Therefore, we require that the output directory
     * be empty to start with.
     *
     * @throws IOException if there's a problem accessing the file system
     */
    @Test
    public void testNoOverwrite() throws IOException {
        SampleTrees dirs = new SampleTrees(tempFolder);
        String pathname1 = dirs.getPathname(Number.ONE);

        File dstFolder = tempFolder.newFolder();
        File junkFile = File.createTempFile("junk", null, dstFolder);

        if (junkFile.exists()) {
            String dst = dstFolder.getAbsolutePath();

            int status = createMirror(pathname1, dst);
            assertEquals("createMirror returned incorrect status (" + status
                         + ") for existing output dir",
                         BAD_ARGUMENTS, status);
        } else {
            fail("could not create junk file to test no overwrite");
        }
    }

    /**
     * Tests that we refuse to use an nonexisting directory for input.
     *
     * @throws IOException if there's a problem accessing the file system
     */
    @Test
    public void testInputMustExist() throws IOException {
        File srcFolder = tempFolder.newFolder();
        String src = srcFolder.getAbsolutePath();
        boolean deleted = srcFolder.delete();

        if (!deleted || srcFolder.exists()) {
            fail("could not create delete folder to test must exist");
        } else {
            File dstFolder = tempFolder.newFolder();
            String dst = dstFolder.getAbsolutePath();

            int status = createMirror(src, dst);
            assertEquals("createMirror returned incorrect status (" + status
                         + ") for existing output dir",
                         BAD_ARGUMENTS, status);
        }
    }

    /**
     * Tests the command-line API.
     *
     * <p>When invoked from the command line, the arguments come in as a List of
     * Strings, rather than individual arguments.  Obviously this ends up
     * calling the individual arguments API, but test that the harness method
     * is working.
     *
     * @throws IOException if there's a problem accessing the file system
     */
    @Test
    public void testCreateMirrorFromArglist() throws IOException {
        SampleTrees dirs = new SampleTrees(tempFolder);
        String pathname1 = dirs.getPathname(Number.ONE);
        String pathname2 = dirs.getPathname(Number.TWO);

        File dstFolder = tempFolder.newFolder();
        String dst = dstFolder.getAbsolutePath();

        // See comments in testCreateMirror(); this is doing the exact same
        // thing, just with a List.
        List<String> args = new ArrayList<>();
        args.add(pathname1);
        args.add(dst);
        assertEquals("createMirror returned failure status", 0,
                     createMirror(args));

        assertTrue("directories created by createMirror were not found equivalent",
                   pc.verifyMirror(pathname1, dst));

        assertTrue("directory created by createMirror was not found equivalent to copy",
                   pc.verifyMirror(pathname2, dst));
    }

    /**
     * Tests the API when called with bad arguments.
     */
    @Test
    public void testBadArgs() {
        List<String> badArgs = new ArrayList<>();
        assertFalse("got success status despite zero args",
                    0 == createMirror(badArgs));
        badArgs.add("src");
        assertFalse("got success status despite one arg",
                    0 == createMirror(badArgs));
        badArgs.add("foo");
        badArgs.add("bar");
        assertFalse("got success status despite three args",
                    0 == createMirror(badArgs));
    }

}
