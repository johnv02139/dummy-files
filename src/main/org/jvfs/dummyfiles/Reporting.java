package org.jvfs.dummyfiles;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Utility class containing convenience methods for communicating results to
 * the user.
 *
 * <p>This class includes methods which help generate documents in plain text
 * and HTML.
 *
 * <p>There surely are many libraries out there that help with HTML generation,
 * but our needs are currently simple enough to not want to bother with adding
 * a dependency.  If we want to start producing more sophisticated reports,
 * this could be swapped out for a real library.
 *
 * <p>Not intended for instantiation or extension.  Simply a collection of
 * functions (static methods) that are useful for creating information for
 * the user.
 *
 */
final class Reporting {
    private static final Logger logger = LogManager.getFormatterLogger(Reporting.class);

    public static final Charset DF_CHARSET = Charset.forName("ISO-8859-1");

    public static final Path TMP_DIR = Paths.get(Environment.TMP_DIR_NAME);

    private static final String HTML_REPORT = "dummy-report.html";

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
     * Creates an HTML file, to be appended to.
     *
     * @param filename
     *   the name of the to write into
     * @return
     *   the Path that we wrote the first line into, or null
     *   if we weren't able to create the file
     *
     */
    public static Path startHtmlFile(final String filename) {
        try {
            Path htmlFile = TMP_DIR.resolve(filename);
            Files.write(htmlFile, "<!DOCTYPE html>\n".getBytes(DF_CHARSET));
            return htmlFile;
        } catch (IOException ioe) {
            logger.log(Level.WARN, "error creating file " + filename, ioe);
            return null;
        }
    }

    /**
     * Write out our hard-coded CSS, inline into the HTML.
     *
     * <p>This is obviously very hard-coded, not only in terms of the CSS
     * itself, but also in the assumption that the CSS is the only thing
     * we'll put into the "head" section.  Again, this is quick and dirty.
     *
     * @param htmlFile
     *   The file to write the embedded CSS out into.
     * @throws IOException if there's a problem accessing the file system
     */
    public static void writeTableCss(final Path htmlFile)
        throws IOException
    {
        appendToFile(htmlFile, "  <head>\n");
        appendToFile(htmlFile, "    <style>\n");
        appendToFile(htmlFile, "      tr:nth-child(even) {background: #DBFAD8;");
        appendToFile(htmlFile, "                          text-indent: 50px;\n");
        appendToFile(htmlFile, "                          font-style: italic;\n");
        appendToFile(htmlFile, "                          "
                     + "border-bottom: 1pt solid black;}\n");
        appendToFile(htmlFile, "      tr:nth-child(odd) {background: #FFF}\n");
        appendToFile(htmlFile, "    </style>\n");
        appendToFile(htmlFile, "  </head>\n");
    }

    /**
     * Writes the preamble for an HTML file that consists of a table.
     *
     * <p>It's awkward to write out the beginning and rely on the caller to know
     * that the end needs to be written as a separate call.  This can be
     * improved.
     *
     * @param htmlFile
     *   The file to write the HTML out into.
     * @throws IOException if there's a problem accessing the file system
     */
    public static void writeHtmlTableBeginning(final Path htmlFile)
        throws IOException
    {
        appendToFile(htmlFile, "<html>\n");
        writeTableCss(htmlFile);
        appendToFile(htmlFile, "  <body>\n");
        appendToFile(htmlFile, "    <table>\n");
    }

    /**
     * Writes the finish for an HTML file that consists of a table.
     *
     * @param htmlFile
     *   The file to write the HTML out into.
     * @throws IOException if there's a problem accessing the file system
     */
    public static void writeHtmlTableFinish(final Path htmlFile)
        throws IOException
    {
        appendToFile(htmlFile, "    </table>\n");
        appendToFile(htmlFile, "  </body>\n");
        appendToFile(htmlFile, "</html>\n");
    }

    /**
     * Write out a row of an HTML table.
     *
     * <p>This writes out a row with any number of elements.  Empty elements
     * can be included, but they must be supplied as empty strings.  The
     * method does not support null, and obviously cannot just infer when
     * an argument is "omitted".
     *
     * <p>We're attempting to produce HTML which is human-readable, so this
     * includes indentation and newlines.  It is assuming one top-level table;
     * if used in a more nested structure, the indentation will be wrong, but
     * obviously the HTML will work just the same.
     *
     * @param outfile
     *   The file to write the HTML out into.
     * @param data
     *   a sequence of Strings; each one represents a cell of data for the table
     *
     */
    public static void writeHtmlTableRow(final Path outfile, final String... data) {
        appendToFile(outfile, "      <tr>\n");
        for (String datum : data) {
            appendToFile(outfile, "        <td>");
            appendToFile(outfile, datum);
            appendToFile(outfile, "</td>\n");
        }
        appendToFile(outfile, "      </tr>\n");
    }

    /**
     * Produce an HTML report on the changed files.
     *
     * @param reportList
     *   a list of FileReports
     * @return
     *   the Path of the report
     */
    public static Path produceHtmlReport(final List<FileReport> reportList) {
        Path outfile = startHtmlFile(HTML_REPORT);
        if (outfile == null) {
            return null;
        }
        try {
            writeHtmlTableBeginning(outfile);
            for (FileReport report : reportList) {
                writeHtmlTableRow(outfile, report.currentLocation, report.currentName);
                writeHtmlTableRow(outfile,
                                  report.hasBeenMoved ? report.originalLocation : "",
                                  report.hasBeenRenamed ? report.originalName : "");
            }
            writeHtmlTableFinish(outfile);
            return outfile;
        } catch (IOException ioe) {
            logger.log(Level.WARN, "error writing to file " + outfile, ioe);
            return null;
        }
    }

    /**
     * Output a report on the changed files.
     *
     * @param reportList
     *   a list of FileReports
     */
    public static void produceTextReport(final List<FileReport> reportList) {
        for (FileReport report : reportList) {
            if (report.hasBeenRenamed) {
                logger.info("the file \"%s\" has been renamed to \"%s\"",
                            report.originalName, report.currentName);
            } else {
                logger.info("\"%s\" has not been renamed", report.originalName);
            }
            if (report.hasBeenMoved) {
                logger.info("  it was moved from \"%s\" to \"%s\"",
                            report.originalLocation, report.currentLocation);
            } else {
                logger.info("  it has not been moved from %s",
                            report.originalLocation);
            }
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
