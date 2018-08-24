package org.jvfs.dummyfiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.jvfs.dummyfiles.SampleTrees.Number;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Tests the functions defined in the DummyFiles class.
 *
 */
public class ReportTest {

    /**
     * This rule creates a new temporary folder before each test, and
     * (tries to) delete it at the end of each test.
     */
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Tests making FileReports.
     *
     * <p>Executes the following:<ul>
     * <li>creates sample trees</li>
     * <li>creates an instance of DummyFiles</li>
     * <li>creates a List to hold FileReports</li>
     * <li>uses the instance of DummyFiles to populate the List with FileReports
     *     for each of the files in one of the sample trees</li>
     * <li>asserts that there were no errors in populating the list</li>
     * <li>gets an array of the original list of files</li>
     * <li>sorts the array</li>
     * <li>sorts the list by the content of the dummy files</li>
     * </ul>
     *
     * <p>At that point, the list of FileReports and the array of Strings should
     * be ordered in the same way, and we can go one by one over the list and
     * confirm that there is a 1:1 match between the list and the array.  That
     * validates that we got exactly one FileReport for each file we created.
     */
    @Test
    public void testMakeFileReport() {
        try {
            SampleTrees dirs = new SampleTrees(tempFolder, false);
            String pathname1 = dirs.getPathname(Number.ONE);

            DummyFiles reporter = new DummyFiles(pathname1);
            List<FileReport> reportList = new ArrayList<>();
            int nErrors = reporter.makeFileReport(reportList);
            assertFalse("makeFileReport had errors", (nErrors > 0));

            String[] expectedFilenames = SampleTrees.getStandardFiles();
            int nFiles = expectedFilenames.length;
            Arrays.sort(expectedFilenames);
            reportList.sort(Comparator.comparing(FileReport::getContent));

            assertEquals("got different number of standard files and FileReports",
                         nFiles, reportList.size());
            for (int i = 0; i < nFiles; i++) {
                assertEquals("standard file and FileReport do not match",
                             expectedFilenames[i], reportList.get(i).getContent());
            }
        } catch (IOException ioe) {
            fail("IO Exception making sample trees or producing report");
        }
    }

    /**
     * Tests that a deleted file fails to get a FileReport.
     *
     * <p>See {@link #testMakeFileReport}.  This method does a similar thing.
     *
     * <p>The difference is, here we delete one of the files in the sample
     * trees.  Then we confirm that when we produce file reports for that
     * tree, the deleted file is correctly not included.
     */
    @Test
    public void testFileReportDeletedFile() {
        Number treeToWorkWith = Number.ONE;
        int indexToDelete = 6;

        try {
            SampleTrees dirs = new SampleTrees(tempFolder, false);
            String pathname1 = dirs.getPathname(treeToWorkWith);
            dirs.deleteDummyFile(treeToWorkWith, indexToDelete);

            DummyFiles reporter = new DummyFiles(pathname1);
            List<FileReport> reportList = new ArrayList<>();
            int nErrors = reporter.makeFileReport(reportList);
            assertFalse("makeFileReport had errors", (nErrors > 0));
            int nFiles = reportList.size();

            String[] expectedFilenames = SampleTrees.getStandardFiles();
            String deletedFilename = expectedFilenames[indexToDelete];
            Arrays.sort(expectedFilenames);
            reportList.sort(Comparator.comparing(FileReport::getContent));

            assertEquals("got unexpected number of standard files or FileReports",
                         nFiles, expectedFilenames.length - 1);
            int j = 0;
            for (int i = 0; i < nFiles; i++) {
                String expected = expectedFilenames[j];
                if (expected.equals(deletedFilename)) {
                    expected = expectedFilenames[++j];
                }
                assertEquals("standard file and FileReport do not match",
                             expected, reportList.get(i).getContent());
                ++j;
            }
        } catch (IOException ioe) {
            fail("IO Exception making sample trees or producing report");
        }
    }
}
