package org.jvfs.dummyfiles;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class containing convenience methods for operating on Strings and
 * Paths (files).
 *
 * <p>Not intended for instantiation or extension.  Simply a collection of
 * functions (static methods) that are useful for dealing with Strings and
 * files.
 *
 */
@SuppressWarnings("SameParameterValue")
final class Utilities {
    private static final Logger logger = LogManager.getFormatterLogger(Utilities.class);

    /**
     * Return true if the given arguments refer to the same actual, existing
     * file on the file system.
     *
     * <p>Note that on file systems that support symbolic links, two Paths could
     * be the same file even if their locations appear completely different.
     *
     * @param path1
     *    first Path to compare; it is expected that this Path exists
     * @param path2
     *    second Path to compare; this may or may not exist
     * @return
     *    true if the paths refer to the same file, false if they don't;
     *    logs an exception if one occurs while trying to check, including
     *    if path1 does not exist; but does not log one if path2 doesn't
     */
    @SuppressWarnings("SimplifiableIfStatement")
    public static boolean isSameFile(final Path path1, final Path path2) {
        try {
            if (Files.notExists(path2)) {
                return false;
            }
            return Files.isSameFile(path1, path2);
        } catch (IOException ioe) {
            logger.log(Level.WARN, "exception checking files "
                       + path1 + " and " + path2, ioe);
            return false;
        }
    }

    /**
     * Creates a directory by creating all nonexistent parent directories first.
     * No exception is thrown if the directory could not be created because it
     * already exists.
     *
     * <p>If this method fails, then it may do so after creating some, but not
     * all, of the parent directories.
     *
     * @param dir - the directory to create
     * @return
     *    true if the the directory exists at the conclusion of this method:
     *    that is, true if the directory already existed, or if we created it;
     *    false if it we could not create the directory
     */
    public static boolean mkdirs(final Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException ioe) {
            logger.log(Level.WARN, "exception trying to create directory " + dir, ioe);
            return false;
        }
        return Files.exists(dir);
    }

    /**
     * If the given String names a readable directory, returns it as a Path.
     *
     * @param dirname
     *    a String giving a path to what is presumed to be a readable directory
     * @return the path as a Path object if, in fact, it names a readable
     *     directory; null -- in addition to logging the specific error --
     *     if not.
     */
    public static Path getReadableDirectory(final String dirname) {
        if (dirname == null) {
            logger.warn("cannot use null for directory name");
            return null;
        }
        Path path = Paths.get(dirname);
        if (path == null) {
            logger.warn("could not create folder from dirname %s", dirname);
            return null;
        }
        if (Files.notExists(path)) {
            logger.warn("specified directory \"%s\" does not exist", dirname);
            return null;
        }
        if (Files.isRegularFile(path)) {
            logger.warn("specified value \"%s\" is a regular file, not a directory",
                        dirname);
            return null;
        }
        if (!Files.isReadable(path)) {
            logger.warn("specified file \"%s\" is not readable", dirname);
            return null;
        }
        if (Files.exists(path)
            && Files.isDirectory(path))
        {
            return path;
        }
        logger.warn("something went wrong trying to access \"%s\"",
                    dirname);
        return null;
    }

    /**
     * Returns whether or not a Path is a writable directory.  The argument may
     * be null.
     *
     * @param path
     *    the path to check; may be null
     * @return true if path names a directory that is writable by the user
     *    running this process; false otherwise.
     */
    public static boolean isWritableDirectory(final Path path) {
        return ((path != null)
                && Files.isDirectory(path)
                && Files.isWritable(path));
    }

    /**
     * Ensures the given path is a writable directory.
     *
     * <p>Takes a Path which is a directory that the user wants to write into.
     * Makes sure that the directory exists (or creates it if it doesn't) and is
     * writable.  If the directory cannot be created, or is not a directory, or
     * is not writable, this method fails.
     *
     * @param destDir
     *    the Path that the caller will want to write into
     * @return true if, upon completion of this method, the desired Path exists,
     *         is a directory, and is writable.  False otherwise.
     */
    public static boolean ensureWritableDirectory(final Path destDir) {
        if (Files.notExists(destDir)) {
            try {
                Files.createDirectories(destDir);
            } catch (IOException ioe) {
                logger.log(Level.ERROR, "Unable to create directory " + destDir, ioe);
                return false;
            }
        }
        if (!Files.exists(destDir)) {
            logger.warn("could not create destination directory %s", destDir);
            return false;
        }
        if (!Files.isDirectory(destDir)) {
            logger.warn("cannot use specified destination %s because it is not a directory",
                        destDir);
            return false;
        }
        if (!Files.isWritable(destDir)) {
            logger.warn("cannot write file to %s", destDir);
            return false;
        }

        return true;
    }

    /**
     * Returns a Path's nearest existing ancestor.
     *
     * <p>Given a Path, if the Path exists, returns it.  If not, but its parent
     * exists, returns that, etc.  That is, returns the closest ancestor
     * (including itself) that exists.
     *
     * @param checkPath the path to check for the closest existing ancestor
     * @return the longest path, from the root dir towards the given path,
     *   that exists
     */
    private static Path existingAncestor(final Path checkPath) {
        if (checkPath == null) {
            logger.info("check path is null");
            return null;
        }
        Path fullPath = checkPath.toAbsolutePath();
        logger.info("full path is %s", fullPath);
        Path root = fullPath.getRoot();
        if (root == null) {
            logger.info("root is null");
            return null;
        }
        Path existent = fullPath;
        while (Files.notExists(existent)) {
            if (root.equals(existent)) {
                logger.info("root (%s) does not exist", root);
                // Presumably, this can't happen, because it suggests
                // the root dir doesn't exist, which doesn't make sense.
                // But just to be sure to avoid an infinite iteration...
                return null;
            }
            existent = existent.getParent();
            if (existent == null) {
                logger.info("came up with null from %s", fullPath);
                return null;
            }
        }

        return existent;
    }

    /**
     * Returns a creatable path if the argument names one.
     *
     * <p>Takes the name of a Path which is a directory that the user may want
     * to write into.  If the given String names a creatable directory, returns
     * it as a Path.
     *
     * <p>Does not actually create the directory.
     *
     * @param destDirName
     *    the name (a String) of the Path that the caller will want to write into
     * @return the path as a Path object if, in fact, it names a creatable
     *    directory; null -- in addition to logging the specific error -- if not.
     *
     *    As an example, if the value is /Users/me/Files/PDFs/Work, and no such
     *    file exists, we just keep going up the tree until something exists.
     *    If we find /Users/me/Files exists, and it's a writable directory, then
     *    presumably we could create a "PDFs" directory in it, and "Work" in
     *    that, thereby creating the directory.  But if /Users/me/Files is not a
     *    directory, or is not writable, then we know we cannot create the
     *    target, and so we return false.
     *
     */
    public static Path creatableDirectory(final String destDirName) {
        if (destDirName == null) {
            logger.warn("received null directory name");
            return null;
        }
        Path destDir = Paths.get(destDirName);
        Path ancestor = existingAncestor(destDir);
        if (isWritableDirectory(ancestor)) {
            return destDir;
        } else {
            logger.warn("%s could not be created", destDirName);
            return null;
        }
    }

    /**
     * Returns true if the given argument is an empty directory.
     *
     * @param dir
     *    the directory to check for emptiness
     * @return
     *    true if the path existed and was an empty directory; false otherwise
     */
    private static boolean isDirEmpty(final Path dir) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            return !dirStream.iterator().hasNext();
        } catch (IOException ioe) {
            logger.log(Level.WARN, "exception checking directory " + dir, ioe);
            return false;
        }
    }

    /**
     * Creates and returns an empty directory by creating all nonexistent parent
     * directories first.
     *
     * <p>If the desired path already exists as an empty directory, returns it.
     * If it exists as anything other than an empty directory, this method
     * fails and returns null.  If it doesn't exist, tries to create it, and
     * returns it if successful.
     *
     * <p>If this method fails, then it may do so after creating some, but not
     * all, of the parent directories.
     *
     * @param dir - the directory to create
     * @return
     *    the Path if the the directory exists and is empty at the conclusion
     *    of this method: that is, if the directory already existed and was
     *    empty, or if we created it; null if it existed as something other
     *    than an empty directory, or if we could not create the directory
     */
    public static Path cleanDirectory(final Path dir) {
        try {
            if (Files.exists(dir)) {
                if (Files.isDirectory(dir)) {
                    if (isDirEmpty(dir)) {
                        return dir;
                    } else {
                        logger.warn("directory %s has contents", dir);
                        return null;
                    }
                } else {
                    logger.warn("file %s already exists, as a non-directory", dir);
                    return null;
                }
            }
            Files.createDirectories(dir);
            if (Files.exists(dir)) {
                return dir;
            } else {
                logger.warn("after calling createDirectories(%s), it still doesn't exist",
                            dir);
                return null;
            }
        } catch (IOException ioe) {
            logger.log(Level.WARN, "exception trying to create directory " + dir, ioe);
            return null;
        }
    }

    /**
     * Creates and returns an empty directory by creating all nonexistent parent
     * directories first.
     *
     * <p>If the file denoted by the given string  already exists as an empty
     * directory, returns it as a Path.  If it exists as anything other than
     * an empty directory, this method fails and returns null.  If it doesn't
     * exist, tries to create it, and returns the Path if successful.
     *
     * <p>If this method fails, then it may do so after creating some, but not
     * all, of the parent directories.
     *
     * @param dirname - the name of the directory to create
     * @return
     *    the Path if the the directory exists and is empty at the conclusion
     *    of this method: that is, if the directory already existed and was
     *    empty, or if we created it; null if it existed as something other
     *    than an empty directory, or if we could not create the directory
     */
    public static Path cleanDirectory(final String dirname) {
        return cleanDirectory(Paths.get(dirname));
    }

    /**
     * If the given argument is an empty directory, removes it.
     *
     * @param dir
     *    the directory to delete if empty
     * @return
     *    true if the path existed and was deleted; false if not
     */
    public static boolean rmdirIfEmpty(final Path dir) {
        if (dir == null) {
            return false;
        }
        if (Files.notExists(dir)) {
            return false;
        }
        if (!Files.isDirectory(dir)) {
            return false;
        }
        if (!isDirEmpty(dir)) {
            return false;
        }

        try {
            Files.delete(dir);
        } catch (IOException ioe) {
            logger.log(Level.WARN, "exception trying to remove directory " + dir, ioe);
            return false;
        }
        return Files.notExists(dir);
    }

    /**
     * Delete the given file.  This method is intended to be used with "regular"
     * files, not directories.
     *
     * <p>(Implementation detail: this method actually should work fine to
     * remove an empty directory; the preference for it to be used only for
     * regular files is purely a convention, to mirror what shells do.)
     *
     * @param file
     *    the file to be deleted
     * @return
     *    true if the file existed and was deleted; false if not
     */
    public static boolean deleteFile(Path file) {
        if (Files.notExists(file)) {
            logger.warn("cannot delete file, does not exist: %s", file);
            return false;
        }
        try {
            Files.delete(file);
        } catch (IOException ioe) {
            logger.log(Level.WARN, "Error deleting file " + file, ioe);
            return false;
        }
        return Files.notExists(file);
    }

    /**
     * Prevents instantiation.
     *
     * <p>This class is not intended to be instantiated, and does not contain
     * any instance methods or variables.  Prevent any other class from
     * attempting to instantiate it by making its constructor private.
     */
    private Utilities() { }
}
