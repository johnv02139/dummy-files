package org.jvfs.dummyfiles;

import static org.jvfs.dummyfiles.Reporting.createTextFile;
import static org.jvfs.dummyfiles.Utilities.cleanDirectory;
import static org.jvfs.dummyfiles.Utilities.deleteFile;
import static org.jvfs.dummyfiles.Utilities.rmdirIfEmpty;

import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;

/**
 * Used to create two identical directory trees.
 *
 * <p>This does not use the {@link Mirror} class.  It's not creating a
 * mirror, it's creating a copy.  That is, not only is the structure the
 * same, but the content is, as well.  We also set file modification on
 * each file, so that those match up between the two directories, too.
 *
 * <p>We could accomplish this by creating one directory and then copying
 * it, but instead we have a single method which creates a hard-coded file
 * tree, and we call that method twice, with two newly created directories.
 * That should guarantee that they are created identical.
 */
@SuppressWarnings("SameParameterValue")
public class SampleTrees {

    /**
     * A list of sample files (relative paths) to create.
     *
     * <p>This list is used to create two identical file trees.  It should
     * contain examples of any identified edge or corner cases, or anything
     * else that has the potential to trigger inconsistent behavior.
     *
     * <p>The list, as created in the first version of this testing file, may
     * well be found to be insufficient.  It will be fine, going forward
     * indefinitely, to add more items to the end of the list.  In order to
     * avoid regressions, nothing should ever be removed or re-ordered, though.
     */
    private static final String[] STANDARD_FILES = new String[] {
        "dir1/file1.txt",
        "dir1/file2.txt",
        "dir3/srcfile1.c",
        "dir3/srcfile2.c",
        "backup/dir1/file1.txt",
        "backup/dir1/file1~2.txt",
        "backup/dir1/out_2018_08_12.txt",
        "My Files/file1~2.txt",
        "My Files/Music/The Title of the Song.mp3",
        "My Files/Music/The Oneders - That Other Thing.mp3",
        "My Files/Videos/An Interesting Movie.mkv"
    };

    /**
     * A list of sample empty directories (relative paths) to create.
     *
     * <p>When the standard files (above) are created, any required directories
     * are created as needed.  So, this list obviously does NOT represent all
     * directories to be created as part of the tree.
     *
     * <p>It could be an easy mistake when comparing two file trees, to only
     * compare regular files.  But if it's truly a mirror, the entire structure
     * needs to be the same.  So, we make sure to include empty directories,
     * as well.
     */
    private static final String[] EMPTY_DIRS = new String[] {
        "dir2/",
        "My Files/Videos/TV/"
    };

    /**
     * Get a sorted list of the files that are created.
     *
     * @return
     *    an array of the names of the files that get created.
     */
    public static String[] getStandardFiles() {
        return Arrays.copyOf(STANDARD_FILES, STANDARD_FILES.length);
    }

    /**
     * Creates a dummy file.
     *
     * @param rootPath
     *    the root of the path where we want to create the file
     * @param filePath
     *    the relative path to create from the root
     * @param timestamp
     *    the time to set "last modified" to
     * @return
     *    true if the dummy file was created, false if not
     */
    private static boolean createDummyFile(final Path rootPath,
                                           final String filePath,
                                           final FileTime timestamp)
    {
        Path fileToCreate = rootPath.resolve(filePath);
        boolean success = createTextFile(fileToCreate, filePath);
        if (success) {
            try {
                Files.setLastModifiedTime(fileToCreate, timestamp);
                return true;
            } catch (IOException ioe) {
                return false;
            }
        }
        return false;
    }

    /**
     * Creates an empty directory.
     *
     * @param rootPath
     *    the root of the path where we want to create the file
     * @param dirPath
     *    the relative path to create from the root
     * @param timestamp
     *    the time to set "last modified" to
     * @return
     *    true if the empty directory was created, false if not
     */
    private static boolean createEmptyDir(final Path rootPath,
                                          final String dirPath,
                                          final FileTime timestamp)
    {
        Path dirToCreate = rootPath.resolve(dirPath);
        Path newDir = cleanDirectory(dirToCreate);
        if (newDir != null) {
            try {
                Files.setLastModifiedTime(dirToCreate, timestamp);
                return true;
            } catch (IOException ioe) {
                return false;
            }
        }
        return false;
    }

    /**
     * Creates a tree of files beneath the given root.
     *
     * <p>The files are not provided as an argument; this creates a specific,
     * hard-coded tree of files.
     *
     * <p>If this method fails at any point, it immediately aborts, returns
     * false, and does <em>not</em> try to clean up after itself.  It's
     * assumed this will be used only with a temporary directory.  We don't
     * explicitly try to clean up in the success case, either.
     *
     * @param root
     *    the directory under which we want to create the files
     * @param modified
     *    the time to set "last modified" to for each file
     * @param includeEmpty
     *    whether or not to include the empty directories
     * @return
     *    true if the entire tree was created successfully;
     */
    private static boolean createSampleTree(final Path root,
                                            final FileTime modified,
                                            final boolean includeEmpty)
    {
        for (String filePath : STANDARD_FILES) {
            boolean createdFile = createDummyFile(root, filePath, modified);
            if (!createdFile) {
                return false;
            }
        }
        if (includeEmpty) {
            for (String filePath : EMPTY_DIRS) {
                boolean createdDir = createEmptyDir(root, filePath, modified);
                if (!createdDir) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Trivial enum to allow us to specify one of two options.
     */
    public enum Number {
        ONE, TWO
    }

    /**
     * Given one Number, get the other one.
     *
     * @param thisNumber
     *    the Number not to return
     * @return
     *    the other Number
     */
    public static Number otherNumber(final Number thisNumber) {
        if (thisNumber == Number.ONE) {
            return Number.TWO;
        }
        return Number.ONE;
    }

    private final Path dir1;
    private final Path dir2;

    private final String pathname1;
    private final String pathname2;

    /**
     * Get either of the trees, as a Path.
     *
     * @param dirNum
     *    indicates whether we want from directory "one" or directory "two".
     * @return
     *    either directory "one" or directory "two", as a Path
     *
     */
    public Path getPath(final Number dirNum) {
        if (dirNum == Number.ONE) {
            return dir1;
        } else {
            return dir2;
        }
    }

    /**
     * Get the pathname of either of the trees.
     *
     * @param dirNum
     *    indicates whether we want from directory "one" or directory "two".
     * @return
     *    either directory "one" or directory "two", as a String
     *
     */
    public String getPathname(final Number dirNum) {
        if (dirNum == Number.ONE) {
            return pathname1;
        } else {
            return pathname2;
        }
    }

    /**
     * Deletes a file, thereby leaving the tree no longer equal to the
     * other one.
     *
     * <p>The point of this method is to test that verifyMirror can
     * recognize when one tree is NOT a mirror of the other.  We start
     * off with two identical trees, and then delete one file from one
     * of them, and make sure that we recognize them as different.
     *
     * @param dirNum
     *    indicates whether we want to delete the file from directory "one"
     *    or directory "two".
     * @param index
     *    indicates which file to delete; it is an index into the array
     *    {@link #STANDARD_FILES}.
     * @return
     *    true if we successfully deleted the file, false if not
     */
    public boolean deleteDummyFile(final Number dirNum, final int index) {
        if ((index < 0) || (index >= STANDARD_FILES.length)) {
            return false;
        }
        String toRemove = STANDARD_FILES[index];
        Path root = (dirNum == Number.ONE) ? dir1 : dir2;
        Path file = root.resolve(toRemove);
        return deleteFile(file);
    }

    /**
     * Deletes an empty directory, thereby leaving the tree no longer
     * equal to the other one.
     *
     * <p>The point of this method is to test that verifyMirror can
     * recognize when one tree is NOT a mirror of the other.  We start
     * off with two identical trees, and then delete one empty directory
     * from one of them, and make sure that we recognize them as
     * different.
     *
     * @param dirNum
     *    indicates whether we want to delete the file from directory "one"
     *    or directory "two".
     * @param index
     *    indicates which directory to delete; it is an index into the array
     *    {@link #EMPTY_DIRS}.
     * @return
     *    true if we successfully deleted the directory, false if not
     */
    public boolean deleteEmptyDir(final Number dirNum, final int index) {
        if ((index < 0) || (index >= EMPTY_DIRS.length)) {
            return false;
        }
        String toRemove = EMPTY_DIRS[index];
        Path root = (dirNum == Number.ONE) ? dir1 : dir2;
        Path dir = root.resolve(toRemove);
        return rmdirIfEmpty(dir);
    }

    /**
     * Deletes all the empty directories of the sample tree.
     *
     * @param dirNum
     *    indicates whether we want to delete the directories from
     *    directory "one" or directory "two".
     * @return
     *    true if we successfully deleted the directories, false if not
     */
    public boolean deleteAllEmptyDirs(final Number dirNum) {
        for (int index = 0; index < EMPTY_DIRS.length; index++) {
            boolean deleted = deleteEmptyDir(dirNum, index);
            if (!deleted) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates an instance of "SampleTrees", which means creating two identical
     * directories on disk.
     *
     * @param tempRoot
     *    the directory into which the two identical directories should be
     *    created
     * @param includeEmpty
     *    whether or not to include the empty directories
     * @throws IOException if there's a problem accessing the file system
     */
    public SampleTrees(final TemporaryFolder tempRoot,
                       final boolean includeEmpty)
        throws IOException
    {
        File filedir1 = tempRoot.newFolder();
        File filedir2 = tempRoot.newFolder();

        pathname1 = filedir1.getAbsolutePath();
        pathname2 = filedir2.getAbsolutePath();

        dir1 = filedir1.toPath();
        dir2 = filedir2.toPath();

        FileTime timestamp = FileTime.from(Instant.now());

        boolean success = createSampleTree(dir1, timestamp, includeEmpty);
        if (success) {
            success = createSampleTree(dir2, timestamp, includeEmpty);
        }
        if (!success) {
            throw new IOException("could not create test file trees");
        }
    }

    /**
     * Creates an instance of "SampleTrees", which means creating two identical
     * directories on disk.
     *
     * @param tempRoot
     *    the directory into which the two identical directories should be
     *    created
     * @throws IOException if there's a problem accessing the file system
     */
    public SampleTrees(final TemporaryFolder tempRoot) throws IOException {
        this(tempRoot, true);
    }
}
