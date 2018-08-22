package org.jvfs.dummyfiles;

import static org.jvfs.dummyfiles.Environment.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Launches an application.
 *
 * <p>This package contains various functionality around the creation and
 * management of dummy files.  While it can be used as a library, it is also
 * exposed via a Java main method so that the functionality can be accessed
 * directly from the command line.  This class is the entry to that access.  It
 * processes the command line to determine what functionality to invoke and with
 * what arguments, and then does so.
 */
class Launcher {
    private static final Logger logger = LogManager.getFormatterLogger(Launcher.class);

    /**
     * Encapsulates the information necessary to launch an application.
     *
     */
    static class ApplicationParameters {
        final String appName;
        final List<String> appOptions;

        /**
         * Creates an instance of ApplicationParameters.
         *
         * @param args
         *    whatever the user passes on the command line
         */
        ApplicationParameters(final String[] args) {
            List<String> allOptions = Arrays.asList(args);
            if (allOptions.size() == 0) {
                logger.warn("must specify an application");
                appName = null;
                appOptions = null;
            } else {
                String name;
                List<String> options;
                try {
                    name = allOptions.remove(0);
                    options = allOptions;
                } catch (UnsupportedOperationException uoe) {
                    name = allOptions.get(0);
                    options = allOptions.subList(1, allOptions.size());
                }
                appName = name;
                appOptions = options;
            }
        }

        /**
         * Runs the application associated with the appName.
         *
         * @return
         *    the exit status of the application
         */
        int run() {
            if (appName == null) {
                return NO_APP_SPECIFIED;
            }
            if (appName.equals(MIRROR)) {
                return Mirror.createMirror(appOptions);
            }
            if (appName.equals(RESTORE)) {
                return DummyFiles.restoreToOriginalNames(appOptions);
            }
            if (appName.equals(REPORT)) {
                return DummyFiles.reportOnFiles(appOptions);
            }
            logger.warn("unknown application specified: %s", appName);
            return UNKNOWN_APP_SPECIFIED;
        }
    }

    /**
     * Runs a program and returns its exit status.
     *
     * <p>This is essentially the main method of the application, but since
     * Java's main method is void, have this one that returns int.
     *
     * @param args
     *    whatever the user provided on the command line
     * @return
     *    an exit status for the VM
     */
    private static int run(final String[] args) {
        ApplicationParameters app = new ApplicationParameters(args);
        if (app.appName == null) {
            return NO_APP_SPECIFIED;
        }
        return app.run();
    }

    /**
     * Runs a program.
     *
     * <p>Exits the VM with the exit status returned by the program.
     *
     * @param args
     *    whatever the user provided on the command line
     */
    public static void main(String[] args) {
        System.exit(run(args));
    }
}
