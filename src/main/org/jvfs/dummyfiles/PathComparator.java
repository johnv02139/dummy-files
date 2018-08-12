package org.jvfs.dummyfiles;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Verifies that two directory trees are, in fact, identical.
 */
final class PathComparator implements Comparator<Path>, Serializable {
    private static final Logger logger = LogManager.getFormatterLogger(PathComparator.class);

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 512L;

    /**
     * If the given Path exists, returns the name of the file or directory
     * denoted by it, as a String.  The file name is the farthest element
     * from the root in the  directory hierarchy.  Returns the empty string
     * for a null input, a nonexistent file, or any other Path where
     * the FileName cannot be found.
     *
     * @param path
     *    the Path object to get the name of; may be a file or a directory
     *    (and additionally may be non-existent or null)
     * @return
     *    the FileName -- the last component of the path -- if the file
     *    exists; an empty string if it doesn't exist, is null, or we
     *    cannot determine the FileName
     */
    private static String pathNameOnDisk(final Path path) {
        String name = null;
        if ((path != null) && Files.exists(path)) {
            Path fileName = path.getFileName();
            if (fileName != null) {
                name = fileName.toString();
            }
        }
        if (name == null) {
            return "";
        }
        return name;
    }

    /**
     * Compare Paths according to the criteria of {@link #pathNameOnDisk}.
     *
     * <p>The method name "compare" is not very informative, but this class
     * serves as a Comparator, so the method is an override and must have
     * this signature.
     *
     * @param p1 the first Path
     * @param p2 the second Path
     * @return the comparison
     */
    @Override
    public int compare(Path p1, Path p2) {
        return pathNameOnDisk(p1).compareTo(pathNameOnDisk(p2));
    }

    /**
     * Get a sorted list of the direct children of the given Path.
     *
     * @param dir
     *   the directory to get the children of
     * @return
     *   a list of direct children of the given directory, sorted according to
     *   the logic of {@link #pathNameOnDisk}.
     */
    private List<Path> listChildren(final Path dir) {
        if (!Files.isDirectory(dir)) {
            return null;
        }
        List<Path> children = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            files.forEach(children::add);
        } catch (IOException ioe) {
            logger.log(Level.WARN, "exception trying to list directory " + dir, ioe);
        }
        children.sort(this);
        return children;
    }

    /**
     * Checks the entire file structure beneath the two Paths, and returns true
     * if they are identical.
     *
     * <p>Note, the arguments themselves -- that is, the roots of the trees --
     * are not expected to have the same name.  You can create a mirror of
     * /x/y/z at /a/b/c.  It doesn't have to become /a/b/c/z.  There's no reason
     * for that extra level.  It is presumed that you want to work with all the
     * files *below* z, not z itself, and therefore, z itself does not get
     * mirrored.  So when verifying the mirror, we don't need to compare "z"
     * vs. "c", just what is below them.
     *
     * <p>For the recursive calls, the nodes of the trees obviously are expected
     * to have the same names, but that is confirmed BEFORE the recursion is
     * entered.
     *
     * @param p1
     *    one directory, as a Path
     * @param p2
     *    the other directory, as a Path
     * @return true if the files have identical structures beneath them
     *    (including the cases where they both have no structure beneath them,
     *    because they are regular non-directory files, or because they are
     *    both empty directories); false if any differences were found or
     *    any errors were encountered
     */
    private boolean verifyMirror(final Path p1, final Path p2) {
        // The one-arg version of "isRegularFile" follows any symbolic links.
        // So, this means, "is a regular file, or is a symlink to a regular file".
        if (Files.isRegularFile(p1)) {
            return Files.isRegularFile(p2);
        }
        List<Path> p1Children = listChildren(p1);
        if (p1Children == null) {
            return false;
        }
        List<Path> p2Children = listChildren(p2);
        if (p2Children == null) {
            return false;
        }
        int size = p1Children.size();
        if (size != p2Children.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            Path c1 = p1Children.get(i);
            String s1 = pathNameOnDisk(c1);
            Path c2 = p2Children.get(i);
            String s2 = pathNameOnDisk(c2);
            // Compare file names
            if (s1.compareTo(s2) != 0) {
                return false;
            }
            // recursive call
            if (!verifyMirror(c1, c2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks the entire file structure beneath the two nodes, and returns true
     * if they are identical.
     *
     * <p>Note, the arguments themselves are not expected to have the same name.
     * You can create a mirror of /x/y/z at /a/b/c.  It doesn't have to become
     * /a/b/c/z.  There's no reason for that extra level.  It is presumed that
     * you want to work with all the files *below* z, not z itself, and
     * therefore, z itself does not get mirrored.  So when verifying the mirror,
     * we don't need to compare "z" vs. "c", just what is below them.
     *
     * @param s1
     *    string giving the path to one directory
     * @param s2
     *    string giving the path to the other directory
     * @return true if the directories have identical structures beneath them;
     *    false if any differences were found or any errors were encountered
     */
    public boolean verifyMirror(final String s1, final String s2) {
        Path p1 = Paths.get(s1);
        Path p2 = Paths.get(s2);
        return verifyMirror(p1, p2);
    }
}
