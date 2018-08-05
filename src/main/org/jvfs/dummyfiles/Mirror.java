package org.jvfs.dummyfiles;

import static org.jvfs.dummyfiles.Environment.*;
import static org.jvfs.dummyfiles.Reporting.createTextFile;
import static org.jvfs.dummyfiles.Utilities.cleanDirectory;
import static org.jvfs.dummyfiles.Utilities.ensureWritableDirectory;
import static org.jvfs.dummyfiles.Utilities.getReadableDirectory;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Mirror -- creates a file tree with "fake" files.
 *
 */
class Mirror {
    private static final Logger logger = LogManager.getFormatterLogger(Mirror.class);

    /**
     * Prints a usage message and returns an error code.
     *
     * @return an error code
     */
    @SuppressWarnings("SameReturnValue")
    private static int usage() {
        logger.error("  usage: java %s <basedir> <outdir>", Mirror.class.getName());
        return BAD_ARGUMENTS;
    }

    /**
     * Prints a usage message due to bad arguments.
     *
     * @param args
     *    the arguments that were problematic
     * @return an error code
     */
    private static int usage(List<String> args) {
        logger.error("bad arguments for %s: %s", MIRROR, args);
        return usage();
    }

    /**
     * Create the dummy files, with the directories already verified.
     *
     * @param dstRoot
     *   the root directory into which to create all the dummy files
     * @param srcRoot
     *   the root directory to mirror
     * @param current
     *   the file (regular or directory) currently under consideration
     * @return
     *   the number of errors encountered during the process
     *
     */
    private static int createMirrorFromDirectory(Path dstRoot, Path srcRoot, Path current) {
        int nErrors = 0;
        Path rel = srcRoot.relativize(current);
        Path dest = dstRoot.resolve(rel);
        if (Files.isDirectory(current)) {
            ensureWritableDirectory(dest);
            try (DirectoryStream<Path> dirfiles = Files.newDirectoryStream(current)) {
                if (dirfiles != null) {
                    // recursive call
                    dirfiles.forEach(pth -> createMirrorFromDirectory(dstRoot, srcRoot, pth));
                }
            } catch (IOException ioe) {
                nErrors++;
                logger.log(Level.WARN, "I/O Exception descending " + current, ioe);
            }
        } else {
            boolean wroteFile = false;
            Path destDir = dest.getParent();
            if (destDir != null) {
                wroteFile = createTextFile(dest, rel.toString());
            }
            if (!wroteFile) {
                nErrors++;
                logger.warn("unable to create file %s", dest);
            }
        }
        return nErrors;
    }

    /**
     * Creates the dummy files, if the arguments are valid.
     *
     * <p>Checks the arguments.  The basedir needs to be an existing, readable
     * directory.  The outdir should NOT exist, but must be creatable.
     *
     * <p>If these conditions are met, continues on to actually create the
     * dummy files.
     *
     * @param basedir
     *   the directory to "mirror"
     * @param outdir
     *   the location to create the mirror; it should not exist prior to
     *   entering the method
     * @return
     *    0 on success, non-zero for failure
     */
    public static int createMirror(String basedir, String outdir) {
        Path outpath = cleanDirectory(outdir);
        if (outpath == null) {
            logger.warn("could not create output directory %s", outdir);
            return usage();
        }
        Path basepath = getReadableDirectory(basedir);
        if (basepath == null) {
            logger.warn("%s does not name a readable directory", basedir);
            return usage();
        }
        return createMirrorFromDirectory(outpath, basepath, basepath);
    }

    /**
     * Creates the dummy files.
     *
     * @param args
     *    arguments to specify files
     * @return
     *    0 on success, non-zero for failure
     */
    public static int createMirror(List<String> args) {
        int nArgs = args.size();
        // TODO: process flags, use third-party package
        if (nArgs != 2) {
            return usage(args);
        }
        return createMirror(args.get(0), args.get(1));
    }
}
