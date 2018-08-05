package org.jvfs.dummyfiles;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;

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
final class Location {
    private static final Logger logger = LogManager.getFormatterLogger(Location.class);

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
                logger.warn("unexpected error: %s was not moved to %s but %s",
                            srcPath, dstPath, returned);
                return false;
            }
        } catch (IOException ioe) {
            logger.warn("unable to move %s to %s", srcPath, dstPath);
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
                        logger.log(Level.WARN, "IO Exception descending " + dir, ioe);
                        return false;
                    }
                    dirsToRemove.addFirst(next);
                }
            }
            while (!dirsToRemove.isEmpty()) {
                Utilities.rmdirIfEmpty(dirsToRemove.removeFirst());
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
    private Location() { }
}
