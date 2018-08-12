package org.jvfs.dummyfiles;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.jvfs.dummyfiles.SampleTrees.Number;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

/**
 * Tests the functions defined in the PathComparator class.
 *
 * <p>The PathComparator class is, itself, essentially a test class.  It
 * verifies that two trees, intended to be mirrors of each other, actually are.
 * This class tests that testing.
 *
 * <p>This class, therefore, is almost outside of this program.  But that
 * makes sense; we sort of want an "external" tool to verify that the
 * mirrors are made correctly.  If we wrote the tests in a way that relied
 * heavily on the same code that was doing the work, we'd be prone to the
 * code and the test having the same error.
 *
 * <p>So this class creates mirrors -- and, parallel directories which are
 * <em>almost</em> mirrors, but slightly different -- in a way unrelated to
 * how the {@link Mirror} class works.  And, in a simpler way.  So if we can
 * verify that we get the right answer on these file trees, we can feel
 * confident that we can verify the trees created by the Mirror class, and
 * that if the Mirror class has a bug, that the PathComparator can catch it.
 *
 */
public class PathComparatorTest {

    /**
     * Temporary folder that gets created prior to each test and deleted
     * after the test is through.
     */
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Tests verifying two identical trees.
     *
     * <p>This should successfully verify that the trees are mirrors of
     * each other.
     *
     * @throws IOException if there's a problem accessing the file system
     */
    @Test
    public void testCorrectMirror() throws IOException {
        SampleTrees dirs = new SampleTrees(tempFolder);
        String pathname1 = dirs.getPathname(Number.ONE);
        String pathname2 = dirs.getPathname(Number.TWO);

        PathComparator pc = new PathComparator();
        assertTrue("verifyMirror failed",
                   pc.verifyMirror(pathname1, pathname2));
    }

    /**
     * Tests for a missing file in one of the directories.
     *
     * <p>We start off with two identical trees, and then delete a file from
     * one of them.  This means that verifyMirror should return false.
     *
     * @param deleteDir
     *   the directory (ONE or TWO) from which to delete the file
     * @throws IOException if there's a problem accessing the file system
     */
    public void testMissingFile(final Number deleteDir) throws IOException {
        SampleTrees dirs = new SampleTrees(tempFolder);

        Number otherDir = SampleTrees.otherNumber(deleteDir);
        String pathname1 = dirs.getPathname(otherDir);
        String pathname2 = dirs.getPathname(deleteDir);

        boolean deleted = dirs.deleteDummyFile(deleteDir, 5);
        if (!deleted) {
            fail("unable to delete dummy file #5 of tree " + deleteDir);
        }

        PathComparator pc = new PathComparator();
        assertFalse("verifyMirror failed to detect missing file in dir"
                    + deleteDir, pc.verifyMirror(pathname1, pathname2));
    }

    /**
     * Tests for a missing (empty) directory in one of the directories.
     *
     * <p>It could be an easy mistake to write the comparator to only compare
     * regular files.  But if it's truly a mirror, the entire structure
     * needs to be the same.
     *
     * @param deleteDir
     *   the directory (ONE or TWO) from which to delete the file
     * @throws IOException if there's a problem accessing the file system
     */
    public void testMissingDirectory(final Number deleteDir) throws IOException {
        SampleTrees dirs = new SampleTrees(tempFolder);

        Number otherDir = SampleTrees.otherNumber(deleteDir);
        String pathname1 = dirs.getPathname(otherDir);
        String pathname2 = dirs.getPathname(deleteDir);

        boolean deleted = dirs.deleteEmptyDir(deleteDir, 1);
        if (!deleted) {
            fail("unable to delete empty dir #1 of tree " + deleteDir);
        }

        PathComparator pc = new PathComparator();
        assertFalse("verifyMirror failed to detect missing directory in dir"
                    + deleteDir, pc.verifyMirror(pathname1, pathname2));
    }

    /**
     * Tests for a missing file in dir2.
     *
     * <p>We start off with two identical trees, and then delete a file from
     * one of them.  This means that verifyMirror should return false.
     *
     * @throws IOException if there's a problem accessing the file system
     */
    @Test
    public void testMissingFileDir2() throws IOException {
        testMissingFile(Number.TWO);
    }

    /**
     * Tests for a missing file in dir1.
     *
     * <p>This deletes the same file as the previous test, but deletes it from
     * dir1, instead of dir2.  It is an easy bug to make, to verify that
     * "each of x's children is in y" and return false if it's not true,
     * but to be an actual mirror, it ALSO must be true that each of y's
     * children is in x.
     *
     * @throws IOException if there's a problem accessing the file system
     */
    @Test
    public void testMissingFileDir1() throws IOException {
        testMissingFile(Number.ONE);
    }

    /**
     * Tests for a missing (empty) directory in dir2.
     *
     * <p>It could be an easy mistake to write the comparator to only compare
     * regular files.  But if it's truly a mirror, the entire structure
     * needs to be the same.
     *
     * @throws IOException if there's a problem accessing the file system
     */
    @Test
    public void testMissingDirectoryDir2() throws IOException {
        testMissingDirectory(Number.TWO);
    }
}
