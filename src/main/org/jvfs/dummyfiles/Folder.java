package org.jvfs.dummyfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

// Folder - represents a directory on disk.  This is not much more than a wrapper around
// a Path object.
//
// It's also worth noting that Java's "Path" class (like "File" before it) does not make
// any class-level distinction between a directory and an ordinary file.  This class does.
// It is only used for directories.  So in the code, when you see a "Folder", you know
// right away it's a directory, whereas if you see a "Path", it's probably a movie file.
public class Folder {
    private static final Logger logger = Logger.getLogger(Folder.class.getName());

    private static final Map<Path, Folder> ALL_FOLDERS = new ConcurrentHashMap<>();

    // To understand what this means, let's say the user tells us to work on a folder,
    //   C:/Users/me/Documents/Incoming/Videos/TV/to-rename
    // Then within that folder, there might be elaborate paths like:
    //   .../comedies/live-action/BigBang/1080p/s08/Big.Bang.Theory.S08E02.dvdrip.x264/
    // with contents like "Big.Bang.Theory.S08E02.dvdrip.x264.avi".
    // Or, of course, it might be much more simple, like:
    //  .../Veep/S04E01.mp4
    // However long the path is, it might help us figure out the series information, and it might
    // be something that the user wants to preserve.  But, we assume that's only true of the
    // "subpath" -- the part of the path below the folder that the user originally specified.

    // A Folder has a "parent", also a Folder.  But it tops out at the directory that the user
    // has given us to search -- NOT at the actual root of the file system.  So, using the
    // example above, "live-action" would have a parent of "comedies", but "comedies" would
    // have a parent of null.

    private Folder parent;
    private Path element;
    private Path realpath;

    private Folder(Folder parent, Path element, Path realpath) {
        if (realpath == null) {
            throw new IllegalArgumentException("realpath cannot be null");
        }
        logger.fine("creating folder!!\n  parent = " + parent + "\n  element = " + element);
        this.parent = parent;
        this.element = element;
        this.realpath = realpath;
    }

    /**
     * Provides a Folder for the given Path.
     *
     * @param realpath
     *   the Path to look up
     * @return
     *   a Folder that represents the Path
     */
    public static Folder getFolder(final Path realpath) {
        if (realpath == null) {
            logger.warning("cannot have null folder");
            return null;
        }
        Folder rval = ALL_FOLDERS.get(realpath);
        if (rval == null) {
            if (Files.exists(realpath)) {
                Path parentPath = realpath.getParent();
                Folder parent = ALL_FOLDERS.get(parentPath);
                Path fileName = realpath.getFileName();
                rval = new Folder(parent, fileName, realpath);
                ALL_FOLDERS.put(realpath, rval);
            } else {
                logger.warning("did not create folder for " + realpath);
            }
        }
        return rval;
    }

    /**
     * Provides a Folder for the given string.
     *
     * @param pathname
     *   a String of the Path to look up
     * @return
     *   a Folder that represents the Path
     */
    public static Folder getFolder(final String pathname) {
        if (pathname == null) {
            logger.warning("cannot have null folder");
            return null;
        }
        Path path = Paths.get(pathname);
        Path realpath;
        try {
            Files.createDirectories(path);
            realpath = path.toRealPath();
        } catch (IOException ioe) {
            if (Files.notExists(path)) {
                logger.warning("cannot find tree of nonexistent file: " + path);
                return null;
            }
            logger.log(Level.WARNING, "exception trying to create directory " + path, ioe);
            return null;
        }
        return getFolder(realpath);
    }

    public Path asPath() {
        return realpath;
    }

    public Path resolve(Path child) {
        return realpath.resolve(child);
    }

    public Path resolve(String child) {
        return realpath.resolve(child);
    }

    public Folder getParent() {
        return parent;
    }

    public Folder descend(Path file) {
        return getFolder(realpath.resolve(file));
    }

    public Path relativize(Path other) {
        return realpath.relativize(other);
    }

    public Path toPath() {
        return realpath;
    }

    public String getName() {
        return element.toString();
    }

    /**
     * Returns whether or not this Folder is a readable directory.
     *
     * @return true if path names a directory that is readable by the user running this
     *    process; false otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isReadableDirectory() {
        return (Files.isDirectory(realpath)
                && Files.isReadable(realpath));
    }

    /**
     * Creates a directory by creating all nonexistent parent directories first.
     * No exception is thrown if the directory could not be created because it
     * already exists.
     *
     * <p>If this method fails, then it may do so after creating some, but not
     * all, of the parent directories.
     *
     * @return
     *    true if the the directory exists at the conclusion of this method:
     *    that is, true if the directory already existed, or if we created it;
     *    false if it we could not create the directory
     */
    public boolean mkdirs() {
        try {
            Files.createDirectories(realpath);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "exception trying to create directory " + realpath, ioe);
            return false;
        }
        return Files.exists(realpath);
    }

    /**
     * This is certainly _not_ the root of the file system; it's just the root of the tree
     * that the user specified.
     *
     * @return the "root" folder that caused this Folder to be added
     */
    public Path getRoot() {
        if (parent == null) {
            return realpath;
        }
        // recursive call
        return parent.getRoot();
    }

    /**
     * Get the relative path from this Folder's root (see {@link #getRoot}) to the Folder.
     *
     * @return the the relative path from the root
     */
    public Path getRelativePath() {
        return getRoot().relativize(realpath);
    }

    /**
     * See {@link #getRoot}.
     *
     * @param location the file that we want the root of
     * @return the "root" folder that caused this Folder to be added
     */
    public static Folder folderTree(Path location) {
        if (location == null) {
            return null;
        }

        Path realpath;
        try {
            realpath = location.toRealPath();
        } catch (IOException ioe) {
            if (Files.notExists(location)) {
                logger.warning("cannot find tree of nonexistent file: " + location);
                return null;
            }
            logger.warning("can't get tree of " + location + " due to I/O exception");
            return null;
        }

        Folder leafFolder;
        if (Files.isDirectory(realpath)) {
            leafFolder = getFolder(realpath);
        } else {
            final Path parent = realpath.getParent();
            leafFolder = ALL_FOLDERS.get(parent);
        }
        if (leafFolder == null) {
            logger.warning("not found in Folders: " + location);
            return null;
        }

        while (leafFolder.parent != null) {
            leafFolder = leafFolder.parent;
        }

        return leafFolder;
    }

    /**
     * Standard object method to represent this Folder as a string.
     *
     * @return string version of this; just says how many episodes are in the map.
     */
    @Override
    public String toString() {
        return "Folder { " + realpath + " }";
    }
}
