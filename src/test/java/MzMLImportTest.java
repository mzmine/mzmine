/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import io.github.mzmine.datamodel.*;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.File;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Lifecycle#PER_CLASS} creates only one test instance of this class and executes everything
 * in sequence. As we are using data import, chromatogram building, ... Only with this option the
 * init (@BeforeAll) and tearDown method are not static.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
//@Disabled
public class MzMLImportTest {

    private static final Logger logger = Logger.getLogger(MzMLImportTest.class.getName());
    private static MZmineProject project;

    /**
     * Init MZmine core in headless mode with the options -r (keep running) and -m (keep in memory)
     */
    @BeforeAll
    public void init() {
        logger.info("Getting project");
        project = MZmineCore.getProjectManager().getCurrentProject();
    }

    @AfterAll
    public void tearDown() {
        //clean the project after this integration test
        MZmineTestUtil.cleanProject();
    }


    @Test
    @Order(1)
    @DisplayName("Test data import of mzML and mzXML without advanced parameters")
    void dataImportTest() throws InterruptedException, URISyntaxException {
        File[] files = new File[]{
                new File(MzMLImportTest.class.getClassLoader().getResource("rawdatafiles/DOM_a.mzML").getFile()), //ind 0
                new File(MzMLImportTest.class.getClassLoader().getResource("rawdatafiles/DOM_b.mzXML").getFile()), //ind 1
                new File(MzMLImportTest.class.getClassLoader().getResource("rawdatafiles/DOM_a_invalid_header.mzML").getFile()), //ind 2
                new File(MzMLImportTest.class.getClassLoader().getResource("rawdatafiles/DOM_b_invalid_header.mzXML").getFile()) //ind 3
        };

        AllSpectralDataImportParameters paramDataImport = new AllSpectralDataImportParameters();
        paramDataImport.setParameter(AllSpectralDataImportParameters.fileNames, files);
        paramDataImport.setParameter(SpectralLibraryImportParameters.dataBaseFiles, new File[0]);
        paramDataImport.setParameter(AllSpectralDataImportParameters.advancedImport, false);

        logger.info("Testing data import of mzML and mzXML without advanced parameters");
        TaskResult finished = MZmineTestUtil.callModuleWithTimeout(30,
                AllSpectralDataImportModule.class, paramDataImport);

        // should have finished by now
        assertEquals(TaskResult.FINISHED, finished, () -> switch (finished) {
            case TIMEOUT -> "Timeout during data import. Not finished in time.";
            case ERROR -> "Error during data import.";
            case FINISHED -> "";
        });

        assertEquals(4, project.getDataFiles().length);

        //todo check all scans and mass lists
        for (RawDataFile raw : project.getCurrentRawDataFiles()) {
            for (Scan scan : raw.getScans()) {
                assertNotNull(scan);
//                assertNotNull(scan.getMassList());
            }
        }

        //check if number of scans matches in files with/without 3-byte symbols
        RawDataFile rawA = project.getCurrentRawDataFiles().get(2);
        RawDataFile rawB = project.getCurrentRawDataFiles().get(1);
        RawDataFile rawAInvalid = project.getCurrentRawDataFiles().get(3);
        RawDataFile rawBInvalid = project.getCurrentRawDataFiles().get(0);

        assertEquals(521, rawA.getNumOfScans());
        assertEquals(521, rawB.getNumOfScans());

        assertEquals(rawA.getNumOfScans(),
                rawAInvalid.getNumOfScans());
        assertEquals(rawB.getNumOfScans(),
                rawBInvalid.getNumOfScans());

        //check number of data points
        //todo centroid??
        assertEquals(2400, rawA.getMaxRawDataPoints());
        assertEquals(rawA.getMaxRawDataPoints(), rawAInvalid.getMaxRawDataPoints());
        assertEquals(rawA.getMaxCentroidDataPoints(), rawAInvalid.getMaxCentroidDataPoints());

        assertEquals(2410, rawB.getMaxRawDataPoints());
        assertEquals(rawB.getMaxRawDataPoints(), rawBInvalid.getMaxRawDataPoints());
        assertEquals(rawB.getMaxCentroidDataPoints(), rawBInvalid.getMaxCentroidDataPoints());

        //check MS1, MS2 scans
        assertEquals(87, rawA.getScanNumbers(1).size());
        assertEquals(rawA.getScanNumbers(1).size(), rawAInvalid.getScanNumbers(1).size());
        assertEquals(rawA.getScanNumbers(2).size(), rawAInvalid.getScanNumbers(2).size());

        assertEquals(87, rawB.getScanNumbers(1).size());
        assertEquals(rawB.getScanNumbers(1).size(), rawBInvalid.getScanNumbers(1).size());
        assertEquals(rawB.getScanNumbers(2).size(), rawBInvalid.getScanNumbers(2).size());

        //todo do we need to add more checks here?
        //todo examples of files with errors in binary data
    }
}
