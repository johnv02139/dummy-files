package org.jvfs.dummyfiles;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

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

    public static final Map<Character, String> SANITISE
        = Collections.unmodifiableMap(new HashMap<Character, String>()
        {
            // provide a replacement for anything that's not valid in Windows
            // this list is: \ / : * ? " < > |
            // see http://msdn.microsoft.com/en-us/library/aa365247%28VS.85%29.aspx for more information
            {
                put('\\', "-"); // replace backslash with hyphen
                put('/', "-");  // replace forward slash with hyphen
                put(':', "-");  // replace colon with a hyphen
                put('|', "-");  // replace vertical bar with hyphen
                put('*', "-");  // replace asterisk with hyphen; for example,
                                // the episode "C**tgate" of Veep should become "C--tgate", not "Ctgate"
                put('?', "");   // remove question marks
                put('<', "");   // remove less-than symbols
                put('>', "");   // remove greater-than symbols
                put('"', "'");  // replace double quote with apostrophe
                put('`', "'");  // replace backquote with apostrophe
            }
        });

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
     * See javadoc for public method {@link #replaceIllegalCharacters(String)}.
     *
     * <p>This helper method operates only on the specified portion of the
     * string, and ignores (strips away) anything that comes before the start
     * or after the end.
     *
     * @param title the original string, which may contain illegal characters
     * @param start the index of the first character to consider
     * @param end the index of the last character to consider
     * @return a version of the substring, from start to end, of the original
     *    string, which contains no illegal characters
     */
    private static String replaceIllegalCharacters(final String title, final int start, final int end) {
        StringBuilder sanitised = new StringBuilder(end + 1);
        for (int i = start; i <= end; i++) {
            char c = title.charAt(i);
            String replace = SANITISE.get(c);
            if (replace == null) {
                sanitised.append(c);
            } else {
                sanitised.append(replace);
            }
        }
        return sanitised.toString();
    }

    /**
     * Replaces characters which are not permitted in file paths.
     *
     * <p>Certain characters cannot be included in file or folder names.  We
     * create files and folders based on both information the user provides, and
     * data about the actual episode.  It's likely that sometimes illegal
     * characters will occur.  This method takes a String that may have illegal
     * characters, and returns one that is similar but has no illegal
     * characters.
     *
     * <p>How illegal characters are handled actually depends on the particular
     * character.  Some are simply stripped away, others are replaced with a
     * hyphen or apostrophe.
     *
     * @param title the original string, which may contain illegal characters
     * @return a version of the original string which contains no illegal
     *    characters
     */
    public static String replaceIllegalCharacters(final String title) {
        return replaceIllegalCharacters(title, 0, title.length() - 1);
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
     * Deletes the given file.  This method is intended to be used with "regular"
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
     * Creates an ASCII String from a byte array, without throwing an exception.
     *
     * <p>If an exception is thrown by the String constructor, catches it and
     * simply returns the empty string.
     *
     * @param buffer
     *    a byte array, where the bytes are to be interpreted as ASCII
     *    character codes
     * @return
     *    a String made from the character codes in the buffer, or the
     *    empty String if there was a problem (such as, not all the bytes
     *    are ASCII codes)
     */
    public static String makeString(byte[] buffer) {
        String rval;
        try {
            rval = new String(buffer, "ASCII");
        } catch (UnsupportedEncodingException uee) {
            rval = "";
        }
        return rval;
    }

    /**
     * Returns the name of the file or directory denoted by this path, as a
     * String.
     *
     * <p>The file name is the farthest element from the root in the directory
     * hierarchy.
     *
     * @param path
     *    the Path to get the filename of
     * @return
     *    a non-null String; if the Path is null or has zero elements, the
     *    String returned will be empty, but not null
     */
    public static String basename(final Path path) {
        Path name = (path == null) ? null : path.getFileName();
        String rval = (name == null) ? null : name.toString();
        if (rval == null) {
            return "";
        }
        return rval;
    }

    /**
     * Returns the <em>parent path</em> of the given path, or the empty string
     * if the path does not have a parent.
     *
     * <p>The parent of a path object consists of the path's root component, if
     * any, and each element in the path except for the <em>farthest</em> from
     * the root in the directory hierarchy.
     *
     * <p>This method does not access the file system; the path or its parent
     * may not exist.  Furthermore, this method does not eliminate special names
     * such as "."  and ".." that may be used in some implementations.  On UNIX
     * for example, the parent of "{@code /a/b/c}" is "{@code /a/b}", and the
     * parent of {@code "x/y/.}" is "{@code x/y}".  This method may be used with
     * the {@link java.nio.file.Path#normalize normalize} method, to eliminate
     * redundant names, for cases where <em>shell-like</em> navigation is
     * required.
     *
     * <p>If this path has one or more elements, and no root component, then
     * this method is equivalent to evaluating the expression:
     * <blockquote><pre>
     * subpath(0,&nbsp;getNameCount()-1).toString();
     * </pre></blockquote>
     *
     * @param path
     *    the Path to get the parent of
     * @return
     *    a non-null String representing the path's parent
     */
    public static String dirname(final Path path) {
        Path dir = (path == null) ? null : path.getParent();
        String rval = (dir == null) ? null : dir.toString();
        if (rval == null) {
            return "";
        }
        return rval;
    }

    /**
     * Determines if the Path is a root path, i.e., specifies the top
     * level directory of the path.
     *
     * <p>This differs from the Java NIO definition of "isAbsolute", for
     * file systems (like NTFS) that include a device as part of the path.
     *
     * <p>For example, "C:\Users\bill" is both absolute and a root path.
     * But "\Users\bill" is not considered absolute; we still want to
     * consider it a root path.
     *
     * @param path
     *   the path to examine
     * @return
     *    whether or not the Path is a "root path", as described above
     */
    public static boolean isRootPath(final Path path) {
        if (path == null) {
            return false;
        }
        if (path.isAbsolute()) {
            return true;
        }
        String pathName = path.toString();
        if (pathName.length() == 0) {
            return false;
        }
        return pathName.startsWith(path.getFileSystem().getSeparator());
    }

    /**
     * Combines the second path into the first path.
     *
     * <p>In the basic case, this just appends the second path with the first
     * (i.e., calls Path.resolve).  In the case where the second path is
     * absolute, it elides the common parts of the paths.
     *
     * @param parentPath
     *   the path to append into; the result will always start with this;
     *   may not be null
     * @param append
     *   the path to append; the result will always end with the last
     *   component of this path; may not be null or empty
     * @return
     *    a combining of the Paths, as described above
     */
    public static Path combinePaths(final Path parentPath, final Path append) {
        if (parentPath == null) {
            throw new IllegalArgumentException("cannot combine into a null path");
        }
        int nAppendParts = append.getNameCount();
        if (nAppendParts == 0) {
            throw new IllegalArgumentException("cannot combine an empty path");
        }
        Path appendPart = append;
        if (isRootPath(append)) {
            int nParentParts = parentPath.getNameCount();
            // We always want to append _something_.  If parentPath starts with
            // append (including the case where they're equal), we want to use
            // the last component of "append" (the filename) and add it onto
            // the parent path.  We should never return the parent path from
            // this method.  That's why we subtract one from nAppendParts.
            int maxCommon = Math.min(nParentParts, nAppendParts - 1);

            int k = 0;
            while (parentPath.getName(k).equals(append.getName(k))) {
                k++;
                if (k >= maxCommon) {
                    break;
                }
            }
            appendPart = append.subpath(k, nAppendParts);
        }
        return parentPath.resolve(appendPart).normalize();
    }

    /**
     * Combines the second path into the first path.
     *
     * <p>In the basic case, this just appends the second path with the first
     * (i.e., calls Path.resolve).  In the case where the second path is
     * absolute, it elides the common parts of the paths.
     *
     * @param parentPath
     *   the path to append into; the result will always start with this;
     *   may not be null
     * @param append
     *   a String of the path to append; the result will always end with the
     *   last component of this path; may not be null or empty
     * @return
     *    a combining of the Paths, as described above
     */
    public static Path combinePaths(final Path parentPath, final String append) {
        return combinePaths(parentPath, Paths.get(append));
    }

    /**
     * Moves a file to a new location.
     *
     * @param srcPath
     *   the Path to be moved
     * @param dstPath
     *   the destination to try to move the Path to
     * @return
     *   whether or not the given Path was successfully moved
     *   to the given destination
     */
    public static boolean moveFile(final Path srcPath, final Path dstPath) {
        Path returned = null;
        try {
            returned = Files.move(srcPath, dstPath);
            if (dstPath.equals(returned)) {
                return true;
            } else {
                logger.warning("unexpected error: " + srcPath
                               + " was not moved to " + dstPath
                               + " but " + returned);
                return false;
            }
        } catch (IOException ioe) {
            logger.warning("unable to move " + srcPath + " to " + dstPath);
            return false;
        }
    }

    /**
     * Moves all files beneath the given directory to a flat set of renamed
     * files, using the given basename.
     *
     * @param basename
     *   the basic part of the new name for the flattened files; an index
     *   will be added
     * @param dir
     *   the directory to flatten
     * @return
     *   true if we successfully flattened and renamed all the files; false
     *   if anything goes wrong
     */
    public static boolean flattenAndRename(final String basename, final Path dir) {
        int index = 100;
        if (Utilities.isWritableDirectory(dir)) {
            Deque<Path> subdirs = new ArrayDeque<>();
            Deque<Path> dirsToRemove = new ArrayDeque<>();
            subdirs.addFirst(dir);

            while (!subdirs.isEmpty()) {
                Path next = subdirs.removeFirst();
                if (Files.isDirectory(next)) {
                    try (DirectoryStream<Path> dirfiles = Files.newDirectoryStream(next)) {
                        for (Path entry : dirfiles) {
                            if (Files.isDirectory(entry)) {
                                subdirs.addFirst(entry);
                            } else {
                                Path newDest = entry;
                                while (Files.exists(newDest)) {
                                    index++;
                                    newDest = dir.resolve(basename + index);
                                }
                                if (!moveFile(entry, newDest)) {
                                    return false;
                                }
                            }
                        }
                    } catch (IOException ioe) {
                        // We log the exception and ultimately will return a
                        // failure, but we do not abort.  We still want to try
                        // to restore as many files as we can.
                        logger.log(Level.WARNING, "IO Exception descending " + dir, ioe);
                        return false;
                    }
                    dirsToRemove.addFirst(next);
                }
            }
            while (!dirsToRemove.isEmpty()) {
                rmdirIfEmpty(dirsToRemove.removeFirst());
            }

            return true;
        } else {
            return false;
        }
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
