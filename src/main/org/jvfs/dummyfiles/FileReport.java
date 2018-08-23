package org.jvfs.dummyfiles;

import static org.jvfs.dummyfiles.Environment.*;
import static org.jvfs.dummyfiles.Location.basename;
import static org.jvfs.dummyfiles.Location.dirname;
import static org.jvfs.dummyfiles.Reporting.DF_CHARSET;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Contains a summary of the status of a dummy file.
 *
 */
class FileReport {
    private static final Logger logger = LogManager.getFormatterLogger(FileReport.class);

    private String content;
    final String currentName;
    String currentLocation;
    String originalName;
    String originalLocation;
    boolean isIgnore = false;
    boolean hasError = false;
    boolean hasBeenMoved = false;
    boolean hasBeenRenamed = false;

    /**
     * Creates a FileReport.
     *
     * @param basePath
     *   the directory presumed to be the root of the mirror
     * @param file
     *   the file to create a report structure for
     */
    FileReport(final Path basePath, final Path file) {
        currentName = basename(file);

        try {
            content = new String(Files.readAllBytes(file), DF_CHARSET).trim();
            if (DummyFiles.isIgnoreFile(content)) {
                isIgnore = true;
            } else {
                Path reconstruct = Paths.get(content);

                originalLocation = dirname(reconstruct);
                originalName = basename(reconstruct);

                if (file.endsWith(reconstruct)) {
                    currentLocation = originalLocation;
                } else {
                    hasBeenRenamed = !currentName.equals(originalName);

                    Path baseParent = basePath.getParent();
                    if (baseParent == null) {
                        currentLocation = dirname(file);
                    } else {
                        Path relative = baseParent.relativize(file);
                        currentLocation = dirname(relative);
                    }
                    hasBeenMoved = !currentLocation.endsWith(originalLocation);
                }
            }
        } catch (IOException ioe) {
            logger.log(Level.WARN, "I/O Exception reporting on " + file, ioe);
            hasError = true;
        }
    }

    /**
     * Gets the content of the file.
     *
     * <p>This represents the relative path where the original file was.
     *
     * @return
     *   the content of the file
     */
    public String getContent() {
        return content;
    }

    /**
     * Represents the object as a String.
     *
     * @return
     *   the representation of this object as a String
     */
    @Override
    public String toString() {
        return "<FileReport: " + content + ">";
    }
}
