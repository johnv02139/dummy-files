package org.jvfs.dummyfiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.jvfs.dummyfiles.DummyFiles.restoreToOriginalNames;
import static org.jvfs.dummyfiles.Location.flattenAndRename;
import static org.jvfs.dummyfiles.SampleTrees.Number;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the functions defined in the DummyFiles class.
 *
 */
public class DummyFilesTest {

    /**
     * Single instance of PathComparator we can use for all tests.
     */
    private static final PathComparator pc = new PathComparator();

    private static final String DUMMY_BASENAME = "dummy";

    /**
     * This rule creates a new temporary folder before each test, and
     * (tries to) delete it at the end of each test.
     */
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Exercises restoreToOriginalNames using the given directory.
     *
     * @param flattenDir
     *   the directory (ONE or TWO) to flatten
     * @throws IOException if there's a problem accessing the file system
     */
    public void testRestoreToOriginalNames(final Number flattenDir) throws IOException {
        SampleTrees dirs = new SampleTrees(tempFolder, false);

        String flattenPath = dirs.getPathname(flattenDir);
        Number otherDir = SampleTrees.otherNumber(flattenDir);
        String otherPath = dirs.getPathname(otherDir);

        // The SampleTrees object creates two identical trees of files;
        // let's first verify this is true.
        assertTrue("SampleTrees did not create directories equivalent",
                   pc.verifyMirror(flattenPath, otherPath));

        // Now we're going to mess one of them up.
        Path flatDir = dirs.getPath(flattenDir);
        boolean flattened = flattenAndRename(DUMMY_BASENAME, flatDir);
        if (!flattened) {
            fail("failed to flatten test directory");
        }
        // Confirm that they are NOT any longer mirrors.
        assertFalse("trees appear equivalent even after flattening",
                   pc.verifyMirror(flattenPath, otherPath));

        // Now "restore" it, in place, and verify they are (again) identical.
        List<String> args = new ArrayList<>();
        args.add(flattenPath);
        int status = restoreToOriginalNames(args);
        assertEquals("restoreToOriginalNames restored nonzero status",
                     0, status);

        assertTrue("restoreToOriginalNames did not make directories equivalent",
                   pc.verifyMirror(flattenPath, otherPath));
    }

    /**
     * Tests restoreToOriginalNames.
     *
     * @throws IOException if there's a problem accessing the file system
     */
    @Test
    public void testRestoreDir1ToOriginalNames() throws IOException {
        testRestoreToOriginalNames(Number.ONE);
    }

    /**
     * Tests restoreToOriginalNames with a different location.
     *
     * @throws IOException if there's a problem accessing the file system
     */
    @Test
    public void testRestoreToNewLocation() throws IOException {
        SampleTrees dirs = new SampleTrees(tempFolder, false);
        String pathname1 = dirs.getPathname(Number.ONE);
        String pathname2 = dirs.getPathname(Number.TWO);

        // The SampleTrees object creates two identical trees of files;
        // let's first verify this is true.
        assertTrue("SampleTrees did not create directories equivalent",
                   pc.verifyMirror(pathname1, pathname2));

        // Now we're going to mess one of them up.
        Path flatDir = dirs.getPath(Number.ONE);
        boolean flattened = flattenAndRename(DUMMY_BASENAME, flatDir);
        if (!flattened) {
            fail("failed to flatten test directory");
        }
        // Confirm that they are NOT any longer mirrors.
        assertFalse("trees appear equivalent even after flattening",
                   pc.verifyMirror(pathname1, pathname2));

        // Now "restore" it, to a different location, and verify the new
        // location is identical to the untouched tree.
        String newLocation = tempFolder.newFolder().getAbsolutePath();

        List<String> args = new ArrayList<>();
        args.add(pathname1);
        args.add(newLocation);
        int status = restoreToOriginalNames(args);
        assertEquals("restoreToOriginalNames restored nonzero status",
                     0, status);

        assertTrue("restoreToOriginalNames did not make directories equivalent",
                   pc.verifyMirror(newLocation, pathname2));
    }

    /**
     * Tests the API when called with bad arguments.
     */
    @Test
    public void testBadArgs() {
        List<String> badArgs = new ArrayList<>();
        assertFalse("got success status despite zero args",
                    0 == restoreToOriginalNames(badArgs));
        badArgs.add("src");
        badArgs.add("foo");
        badArgs.add("bar");
        assertFalse("got success status despite three args",
                    0 == restoreToOriginalNames(badArgs));
    }

}
