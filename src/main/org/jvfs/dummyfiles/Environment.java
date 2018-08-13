package org.jvfs.dummyfiles;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Provides information about the environment.
 *
 * <p>The information provided by this class -- which is not intended to
 * be instantiated or extended -- includes the locale, which operating system
 * is running, where to locate resources for the functionality, and generic
 * values that may be shared between other classes.
 */
public final class Environment {
    private static final Logger logger = Logger.getLogger(Environment.class.getName());

    public static final int NUM_DIRECTORIES_GUESS_DEFAULT =  128;
    public static final int NUM_FILES_GUESS_DEFAULT       = 1024;

    public static final int BAD_ARGUMENTS         = -65;

    public static final int NO_PARSE_CMD_LINE     = -99;
    public static final int NO_APP_SPECIFIED      = -98;
    public static final int UNKNOWN_APP_SPECIFIED = -97;

    public static final int REWRITE_PATH_INVALID  = -130;
    public static final int EXCEPTION_DESCENDING  = -131;
    public static final int EXCEPTION_RESTORING   = -132;

    // This value is used in a context where a small positive number
    // could indicate an actual error count; so it should be large
    // enough that it could not be confused with an actual count.
    public static final int COULD_NOT_CREATE_HTML = 1500;

    public static final String TMP_DIR_NAME = System.getProperty("java.io.tmpdir");
    public static final String USER_HOME = System.getProperty("user.home");
    private static final String OS_NAME = System.getProperty("os.name");

    public static final String MIRROR = "mirror";
    public static final String RESTORE = "restore";
    public static final String REPORT = "report";

    /**
     * A convenient way to know which operating system is being used.
     *
     * <p>Technically, we don't care which operating system is running.  We may,
     * however, care about which file system(s) are being used.  That's not
     * quite as easy to access, though, so this serves as a good heuristic.
     *
     * <p>The main reason we care about this at all is simply because we "know"
     * that Windows does not support symbolic links.
     *
     * <p>The value "LINUX" actually represents any platform which is not
     * clearly identifiable as either Windows or Mac OS.
     */
    private enum OSType {
        WINDOWS,
        LINUX,
        MAC
    }

    /**
     * Returns an {@link OSType} given the os.name property.
     *
     * <p>Turns the <code>os.name</code> System property into a more
     * definitive value that is easier for us to use.
     *
     * @return an OSType representing the OS where this is running
     */
    private static OSType chooseOSType() {
        if (OS_NAME.contains("Mac")) {
            return OSType.MAC;
        }
        if (OS_NAME.contains("Windows")) {
            return OSType.WINDOWS;
        }
        return OSType.LINUX;
    }

    private static final OSType JVM_OS_TYPE = chooseOSType();
    public static final boolean IS_WINDOWS = (JVM_OS_TYPE == OSType.WINDOWS);

    // If InputStream.read() fails, it returns -1.  So, anything less than zero is
    // clearly a failure.  But we assume a version must at least be "x.y", so let's
    // call anything less than three bytes a fail.
    private static final int MIN_BYTES_FOR_VERSION = 3;

    /**
     * Reads the version number from the version file.
     *
     * @return
     *   the version number read from the version file
     */
    static String readVersionNumber() {
        byte[] buffer = new byte[10];
        // Release env (jar)
        InputStream versionStream = Environment.class.getResourceAsStream("/dummyfiles.version");
        // Dev env
        if (versionStream == null) {
            versionStream = Environment.class.getResourceAsStream("/src/main/dummyfiles.version");
        }

        int bytesRead = -1;
        try {
            bytesRead = versionStream.read(buffer);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception when reading version file", e);
            // Has to be unchecked exception as in static block, otherwise
            // exception isn't actually handled (mainly for junit in ant)
            throw new RuntimeException("Exception when reading version file", e);
        } finally {
            try {
                versionStream.close();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Exception trying to close version file", ioe);
            }
        }
        if (bytesRead < MIN_BYTES_FOR_VERSION) {
            throw new RuntimeException("Unable to extract version from version file");
        }
        return Utilities.makeString(buffer).trim();
    }

    /**
     * Prevents instantiation.
     *
     * <p>This class is not intended to be instantiated, and does not contain
     * any instance methods or variables.  Prevent any other class from
     * attempting to instantiate it by making its constructor private.
     */
    private Environment() { }
}
