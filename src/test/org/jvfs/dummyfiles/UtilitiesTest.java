package org.jvfs.dummyfiles;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.jvfs.dummyfiles.Utilities.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests the functions defined in the Utilities class.
 *
 */
public class UtilitiesTest {

    /**
     * Temporary folder that gets created prior to each test and deleted
     * after the test is through.
     */
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

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
     * Tests that presumed typical usage of ensureWritableDirectory: creating
     * a fresh directory.
     *
     * <p>This confirms that the method returns true, and that the directory
     * was in fact created, is a directory, is empty, and can be removed.
     */
    @Test
    public void testEnsureWritableDirectory() {
        final String dirname = "folder";

        final Path sandbox = tempFolder.getRoot().toPath();

        final Path dirpath = sandbox.resolve(dirname);
        assertFalse("cannot test ensureWritableDirectory because target already exists",
                    Files.exists(dirpath));

        assertTrue("ensureWritableDirectory returned false",
                   ensureWritableDirectory(dirpath));
        assertTrue("dir from ensureWritableDirectory not found",
                   Files.exists(dirpath));
        assertTrue("dir from ensureWritableDirectory not a directory",
                   Files.isDirectory(dirpath));

        assertTrue("rmdirIfEmpty returned false", rmdirIfEmpty(dirpath));
        assertFalse("dir from rmdirIfEmpty not removed", Files.exists(dirpath));
    }

    /**
     * Tests that ensureWritableDirectory returns true if the directory already
     * exists and is writable before the method runs.
     *
     * <p>The expected likely usage of ensureWritableDirectory is that the
     * directory does <em>not</em> exist and that the method will create it,
     * but it's also just as good if it's already there.
     */
    @Test
    public void testEnsureWritableDirectoryAlreadyExists() {
        final Path dirpath = tempFolder.getRoot().toPath();

        assertTrue("cannot test ensureWritableDirectory because sandbox does not exist",
                   Files.exists(dirpath));

        assertTrue("ensureWritableDirectory returned false",
                   ensureWritableDirectory(dirpath));
        assertTrue("dir from ensureWritableDirectory not found",
                   Files.exists(dirpath));
        assertTrue("dir from ensureWritableDirectory not a directory",
                   Files.isDirectory(dirpath));
    }

    /**
     * Tests that we can detect a failure to create a writable directory due to
     * a file being in the way.
     */
    @Test
    public void testEnsureWritableDirectoryFileInTheWay() {
        final String dirname = "file";
        Path dirpath;

        try {
            dirpath = tempFolder.newFile(dirname).toPath();
        } catch (IOException ioe) {
            fail("cannot test ensureWritableDirectory because newFile failed");
            return;
        }

        assertTrue("cannot test ensureWritableDirectory because file does not exist",
                   Files.exists(dirpath));

        assertFalse("ensureWritableDirectory returned true when file was in the way",
                    ensureWritableDirectory(dirpath));
        assertTrue("file from ensureWritableDirectory not found",
                   Files.exists(dirpath));
        assertFalse("file from ensureWritableDirectory is a directory",
                    Files.isDirectory(dirpath));
    }

    /**
     * Tests that we can detect a non-writable directory.
     */
    @Test
    public void testEnsureWritableDirectoryCantWrite() {
        final String dirname = "folder";
        File myFolder;

        try {
            myFolder = tempFolder.newFolder(dirname);
        } catch (Exception e) {
            fail("cannot test ensureWritableDirectory because newFolder failed");
            return;
        }

        Path dirpath = myFolder.toPath();
        assertTrue("cannot test ensureWritableDirectory because folder does not exist",
                   Files.exists(dirpath));

        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(dirpath, perms);
        } catch (UnsupportedOperationException ue) {
            // If this file system can't support POSIX file permissions, then we just
            // punt.  We can't properly test it, so there is no failure.
            return;
        } catch (IOException ioe) {
            fail("cannot test ensureWritableDirectory because newFile failed");
            return;
        }

        assertFalse("failed to make temp dir not writable", Files.isWritable(dirpath));

        assertFalse("ensureWritableDirectory returned true when folder was not writable",
                    ensureWritableDirectory(dirpath));
        assertTrue("file from ensureWritableDirectory not found",
                   Files.exists(dirpath));
        assertTrue("file from ensureWritableDirectory is a directory",
                   Files.isDirectory(dirpath));
    }
}
