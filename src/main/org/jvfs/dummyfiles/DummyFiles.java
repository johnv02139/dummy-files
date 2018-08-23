package org.jvfs.dummyfiles;

import static org.jvfs.dummyfiles.Environment.*;
import static org.jvfs.dummyfiles.Location.combinePaths;
import static org.jvfs.dummyfiles.Reporting.DF_CHARSET;
import static org.jvfs.dummyfiles.Utilities.creatableDirectory;
import static org.jvfs.dummyfiles.Utilities.getReadableDirectory;
import static org.jvfs.dummyfiles.Utilities.isSameFile;
import static org.jvfs.dummyfiles.Utilities.mkdirs;
import static org.jvfs.dummyfiles.Utilities.rmdirIfEmpty;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * DummyFiles -- work with "fake" files.
 *
 */
class DummyFiles {
    private static final Logger logger = LogManager.getFormatterLogger(DummyFiles.class);

    private static int numDirectoriesGuess = NUM_DIRECTORIES_GUESS_DEFAULT;
    private static int numFilesGuess       = NUM_FILES_GUESS_DEFAULT;

    /**
     * Prints a usage message and returns an error code.
     *
     */
    private static void usage() {
        System.out.println("  usage: java " + DummyFiles.class.getName()
                           + " <basedir> <outdir>");
    }

    /**
     * Prints a usage message due to bad arguments.
     *
     * @param appName
     *    the name of the application that was specified
     * @param args
     *    the arguments that were problematic
     */
    private static void usage(final String appName, final List<String> args) {
        logger.warn("bad arguments for %s: %s", appName, args);
        usage();
    }

    /**
     * Sets the approximate number of files that will be created.
     *
     * <p>It is completely unnecessary to call or use this; it simply can be a
     * tiny, tiny optimization if a huge number of files are going to be
     * created.  Rather than having the ArrayDeque resized several times, you
     * can get it to be big enough from the beginning.
     *
     * @param nFiles
     *    approximate number of files that will be created; better to be
     *    a little high rather than a little low
     * @return
     *    true if the provided value was accepted, false if not
     */
    public static boolean setNumFiles(final int nFiles) {
        if (nFiles <= 0) {
            logger.warn("number of files must be greater than zero");
            return false;
        }
        numFilesGuess = nFiles;
        return true;
    }

    /**
     * Sets the approximate number of directories that will be created.
     *
     * <p>It is completely unnecessary to call or use this; it simply can be a
     * tiny, tiny optimization if a huge number of files are going to be
     * created.  Rather than having the ArrayDeque resized several times, you
     * can get it to be big enough from the beginning.
     *
     * @param nDirs
     *    approximate number of directories that will be created; better to be
     *    a little high rather than a little low
     * @return
     *    true if the provided value was accepted, false if not
     */
    public static boolean setNumDirectories(final int nDirs) {
        if (nDirs <= 0) {
            logger.warn("number of directories must be greater than zero");
            return false;
        }
        numDirectoriesGuess = nDirs;
        return true;
    }

    /**
     * Identifies a file that we should ignore.
     *
     * <p>Basically identifies a file that was not created as a dummy file by
     * this program.  If the content does not match what we expect to find,
     * we ignore the file.
     *
     * @param content
     *   the contents of the file, as a String
     * @return
     *   false if the contents look like a "dummy file" created by this program;
     *   true if they do not
     */
    public static boolean isIgnoreFile(final String content) {
        // These are the test files that are created by
        //   a previous, less fully-featured script.
        return content.startsWith("content");
    }

    private final Path rewritePath;
    private final Path basepath;
    private final Deque<Path> subFiles = new ArrayDeque<>(numFilesGuess);
    private final Deque<Path> subDirs = new ArrayDeque<>(numDirectoriesGuess);
    private int errorStatus = 0;

    /**
     * Build the instance variables necessary to do other functionality.
     *
     * <p>Clears and recreates the instance variables {@link #subFiles} and
     * {@link #subDirs}.  Those variables are how other functions
     * (here or external) can analyze and manipulate the dummy files.
     *
     */
    private void processBasepath() {
        subFiles.clear();
        subDirs.clear();
        Deque<Path> unsubDirs = new ArrayDeque<>(numDirectoriesGuess);
        unsubDirs.addFirst(basepath);

        while (!unsubDirs.isEmpty()) {
            Path next = unsubDirs.removeFirst();
            if (Files.isDirectory(next)) {
                try (DirectoryStream<Path> dirfiles = Files.newDirectoryStream(next)) {
                    for (Path entry : dirfiles) {
                        if (Files.isDirectory(entry)) {
                            unsubDirs.addFirst(entry);
                        } else {
                            subFiles.addLast(entry);
                        }
                    }
                } catch (IOException ioe) {
                    // We log the exception and ultimately will return a
                    // failure, but we do not abort.  We still want to try
                    // to restore as many files as we can.
                    logger.log(Level.WARN, "IO Exception descending " + basepath, ioe);
                    errorStatus = EXCEPTION_DESCENDING;
                }
                subDirs.addFirst(next);
            } else {
                subFiles.addLast(next);
            }
        }
    }

    /**
     * Restores dummy files to the paths where they were originally created.
     *
     * <p>Takes no arguments.  Operates on the instance variables which should
     * have been built prior to the invocation of this method.
     *
     * @return
     *    the number of errors encountered during the process of restoring
     *    the files
     */
    private int restoreFiles() {
        int nErrors = 0;
        for (Path file : subFiles) {
            try {
                String content = new String(Files.readAllBytes(file), DF_CHARSET).trim();
                if (isIgnoreFile(content)) {
                    logger.info("ignoring %s", file);
                } else {
                    // logger.info("processing %s", file);
                    Path newPath = combinePaths(rewritePath, content);
                    if (isSameFile(file, newPath)) {
                        logger.debug("nothing to be done to %s", file);
                    } else if (Files.exists(newPath)) {
                        logger.warn("already exists: %s", newPath);
                    } else {
                        Path dstParent = newPath.getParent();
                        if (dstParent == null) {
                            logger.warn("unable to find parent of %s", newPath);
                        } else {
                            Files.createDirectories(dstParent);
                            Files.move(file, newPath);
                        }
                    }
                }
            } catch (IOException ioe) {
                // As above, we do not abort.
                logger.log(Level.WARN, "I/O Exception restoring " + file, ioe);
                ++nErrors;
            }
        }
        return nErrors;
    }

    /**
     * Restore the fake files to their original locations.
     *
     * @return
     *   the number of errors encountered in processing the files.
     */
    private int restoreFilesToDirectory() {
        boolean created = mkdirs(rewritePath);
        if (!created) {
            logger.warn("could not create directory %s", rewritePath);
            return REWRITE_PATH_INVALID;
        }
        processBasepath();
        int nRestoreProblems = restoreFiles();
        if (nRestoreProblems != 0) {
            errorStatus = EXCEPTION_RESTORING;
        }
        while (!subDirs.isEmpty()) {
            Path next = subDirs.removeFirst();
            rmdirIfEmpty(next);
        }
        return errorStatus;
    }

    /**
     * Instantiates an object that collects status of previously created dummy
     * files and can be used to report on their status or to restore them to
     * their original names and relative locations.
     *
     * @param basedir
     *    the current location of the files
     * @param outdir
     *    the root directory where to move the renamed files
     */
    private DummyFiles(final String basedir, final String outdir) {
        rewritePath = creatableDirectory(outdir);
        basepath = getReadableDirectory(basedir);
        if (basepath == null) {
            logger.warn("%s does not name a readable directory", basedir);
            usage();
        } else if (rewritePath == null) {
            logger.warn("could not create directory %s", outdir);
            usage();
        }
    }

    /**
     * Instantiates an object that collects status of previously created dummy
     * files and can be used to report on their status or to restore them to
     * their original names and locations.
     *
     * @param dummyDir
     *    the current location of the files; if they will be moved by this
     *    object, it will be within this same directory
     */
    private DummyFiles(final String dummyDir) {
        this(dummyDir, dummyDir);
    }

    /**
     * Restore the dummy files to their original names and locations.
     *
     * @param args
     *    arguments to specify files
     * @return
     *    0 on success, non-zero for failure
     */
    public static int restoreToOriginalNames(final List<String> args) {
        int nArgs = args.size();
        // TODO: process flags, use third-party package
        if ((nArgs < 1) || (nArgs > 2)) {
            usage(RESTORE, args);
            return BAD_ARGUMENTS;
        }
        DummyFiles restorer = (nArgs == 2)
            ? new DummyFiles(args.get(0), args.get(1))
        // If only one argument is supplied, then it is to be used for both
        // the input and the output directories.
            : new DummyFiles(args.get(0));
        return restorer.restoreFilesToDirectory();
    }
}
