package org.jvfs.dummyfiles;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Utility class containing convenience methods for communicating results to
 * the user.
 *
 * <p>Not intended for instantiation or extension.  Simply a collection of
 * functions (static methods) that are useful for creating information for
 * the user.
 *
 */
final class Reporting {
    private static final Logger logger = LogManager.getFormatterLogger(Reporting.class);

    public static final Charset DF_CHARSET = Charset.forName("ISO-8859-1");

    /**
     * Turns logging off, aside from severe errors.
     *
     */
    public static void loggingOff() {
        Configurator.setRootLevel(Level.ERROR);
    }

    /**
     * Turns logging on, for anything up to the "INFO" level.
     *
     */
    public static void loggingOn() {
        Configurator.setRootLevel(Level.INFO);
    }

    /**
     * Create a file, with the given content.
     *
     * @param filePath
     *   the path of the file to create
     * @param contents
     *   a String to write into the file
     * @return
     *   true if the file was created and the content written;
     *   false otherwise
     */
    public static boolean createTextFile(final Path filePath, final String contents) {
        Path destDir = filePath.getParent();
        if (destDir == null) {
            logger.warn("unable to find parent of %s", filePath);
            return false;
        }

        try {
            Files.createDirectories(destDir);
            Files.write(filePath, contents.getBytes(DF_CHARSET));
        } catch (IOException ioe) {
            logger.log(Level.WARN, "error creating file " + filePath, ioe);
            return false;
        }
        return true;
    }

    /**
     * Append the given text to the given path.
     *
     * @param filePath
     *   the Path to write to
     * @param contents
     *   the String to write; it will be interpreted using {@link #DF_CHARSET}
     */
    public static void appendToFile(final Path filePath, final String contents) {
        try {
            Files.write(filePath, contents.getBytes(DF_CHARSET), StandardOpenOption.APPEND);
        } catch (IOException ioe) {
            logger.log(Level.WARN, "error appending to file " + filePath, ioe);
            return;
        }
    }

    /**
     * Prevents instantiation.
     *
     * <p>This class is not intended to be instantiated, and does not contain
     * any instance methods or variables.  Prevent any other class from
     * attempting to instantiate it by making its constructor private.
     */
    private Reporting() { }
}
