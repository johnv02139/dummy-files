package org.jvfs.dummyfiles;

/**
 * A non-instantiable collection of data about groups of file extensions.
 */
public class Suffixes {

    private static final String[] VIDEO_SUFFIXES
        = { "mp4",
            "mkv",
            "mpg",
            "mpeg",
            "rm",
            "asf",
            "m4v",
            "mov",
            "qt",
            "wmv" };

    private static final String[] AUDIO_SUFFIXES
        = { "mp3",
            "m4a",
            "m4p",
            "wma",
            "wav",
            "mp4",
            "asf",
            "mp1" };

    private static final String[] IMAGE_SUFFIXES
        = { "jpg",
            "gif",
            "bmp",
            "tif",
            "png",
            "svg",
            "jpeg" };


    /**
     * Create a glob pattern that combines the given suffixes.
     *
     * @param suffixes
     *   an array of the suffixes to include in the globbing
     * @return
     *   a String that can be used as a glob pattern to return the
     *   specified suffixes
     */
    public static String makeGlobPattern(final String[] suffixes) {
        int nSuffixes = suffixes.length;
        StringBuilder globPattern = new StringBuilder();
        globPattern.append("*.{");
        for (int i=0; i<nSuffixes; i++) {
            if (i > 0) {
                globPattern.append(",");
            }
            globPattern.append(suffixes[i]);
        }
        globPattern.append("}");
        return globPattern.toString();
    }

    /**
     * The various types of sets of suffixes that we know about.
     *
     */
    public enum Type {
        VIDEO(VIDEO_SUFFIXES),
        AUDIO(AUDIO_SUFFIXES),
        IMAGE(IMAGE_SUFFIXES);

        private final String[] extensions;
        private final String globPattern;

        /**
         * Bind together the enum and the glob pattern.
         *
         * @param extensions
         *    the file extensions associated with the enum
         */
        Type(final String[] extensions) {
            this.extensions = extensions;
            globPattern = makeGlobPattern(extensions);
        }
    }

    /**
     * Prevents instantiation.
     *
     * <p>This class is not intended to be instantiated, and does not contain
     * any instance methods or variables.  Prevent any other class from
     * attempting to instantiate it by making its constructor private.
     */
    private Suffixes() { } // prevent instantiation
}
