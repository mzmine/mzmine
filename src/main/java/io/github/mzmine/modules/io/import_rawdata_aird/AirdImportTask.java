/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.import_rawdata_aird;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import net.csibio.aird.bean.AirdInfo;
import net.csibio.aird.bean.CV;
import net.csibio.aird.bean.DDAMs;
import net.csibio.aird.constant.PSI;
import net.csibio.aird.enums.MsLevel;
import net.csibio.aird.parser.DDAParser;
import net.csibio.aird.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class AirdImportTask extends AbstractTask {

    private final ParameterSet parameters;
    private final Class<? extends MZmineModule> module;
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File file;
    private MZmineProject project;
    private RawDataFile newMZmineFile;
    private int totalScans = 0, parsedScans;

    public static final String Polarity_Type = "PolarityType"; // WINDOW_RANGE_HIGH
    public static final String Mass_Spectrum_Type = "MassSpectrumType"; // WINDOW_RANGE_HIGH
    public static final String FILTER_STRING = "FilterString"; // WINDOW_RANGE_HIGH
    public static final String LOWEST_MZ = "Lowest_Mz"; // LOWEST_MZ
    public static final String HIGHEST_MZ = "Highest_Mz"; // HIGHEST_MZ

    public AirdImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile,
                          @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
                          @NotNull Instant moduleCallDate) {
        super(null, moduleCallDate); // storage in raw data file
        this.parameters = parameters;
        this.module = module;
        this.project = project;
        this.file = fileToOpen;
        this.newMZmineFile = newMZmineFile;
    }

    /**
     * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    @Override
    public double getFinishedPercentage() {
        return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
    }

    /**
     * @see Runnable#run()
     */
    @Override
    public void run() {

        setStatus(TaskStatus.PROCESSING);
        logger.info("Started parsing file " + file);

        DDAParser parser = new DDAParser(file.getPath());
        AirdInfo airdInfo = parser.getAirdInfo();

        try {
            if (airdInfo == null) {
                throw new Exception("Parsing Cancelled");
            }
            if (airdInfo.getTotalScanCount() != null) {
                totalScans = airdInfo.getTotalScanCount().intValue();
            }

            List<DDAMs> cycleList = parser.readAllToMemory();

            for (int i = 0; i < cycleList.size(); i++) {
                DDAMs ms1 = cycleList.get(i);
                HashMap<String, Object> ms1CvList = parseCVList(ms1);
                SimpleScan ms1Scan = null;
                if (ms1CvList != null && ms1CvList.size() != 0) {
                    ms1Scan = new SimpleScan(newMZmineFile, ms1.getNum(), MsLevel.MS1.getCode(),
                            ms1.getRt(), null, ArrayUtil.fromFloatToDouble(ms1.getSpectrum().mzs()), ArrayUtil.fromFloatToDouble(ms1.getSpectrum().ints()),
                            (MassSpectrumType) ms1CvList.get(Mass_Spectrum_Type), (PolarityType) ms1CvList.get(Polarity_Type),
                            (String) ms1CvList.get(FILTER_STRING), Range.closed(Double.parseDouble((String) ms1CvList.get(LOWEST_MZ)), Double.parseDouble((String) ms1CvList.get(HIGHEST_MZ))));
                } else {
                    ms1Scan = new SimpleScan(newMZmineFile, ms1.getNum(), MsLevel.MS1.getCode(),
                            ms1.getRt(), null, ArrayUtil.fromFloatToDouble(ms1.getSpectrum().mzs()), ArrayUtil.fromFloatToDouble(ms1.getSpectrum().ints()),
                            MassSpectrumType.PROFILE, PolarityType.POSITIVE,
                            "", null);
                }

                parsedScans++;
                newMZmineFile.addScan(ms1Scan);
                if (ms1.getMs2List() != null && ms1.getMs2List().size() != 0) {
                    for (int j = 0; j < ms1.getMs2List().size(); j++) {
                        DDAMs ms2 = ms1.getMs2List().get(j);
                        HashMap<String, Object> ms2CvList = parseCVList(ms2);
                        SimpleScan ms2Scan = null;
                        if (ms2CvList != null && ms2CvList.size() != 0) {
                            ms2Scan = new SimpleScan(newMZmineFile, ms2.getNum(), MsLevel.MS2.getCode(),
                                    ms2.getRt(), null, ArrayUtil.fromFloatToDouble(ms2.getSpectrum().mzs()), ArrayUtil.fromFloatToDouble(ms2.getSpectrum().ints()),
                                    (MassSpectrumType) ms2CvList.get(Mass_Spectrum_Type), (PolarityType) ms2CvList.get(Polarity_Type),
                                    (String) ms2CvList.get(FILTER_STRING), Range.closed(Double.parseDouble((String) ms2CvList.get(LOWEST_MZ)), Double.parseDouble((String) ms2CvList.get(HIGHEST_MZ))));
                        } else {
                            ms2Scan = new SimpleScan(newMZmineFile, ms2.getNum(), MsLevel.MS2.getCode(),
                                    ms2.getRt(), null, ArrayUtil.fromFloatToDouble(ms2.getSpectrum().mzs()), ArrayUtil.fromFloatToDouble(ms2.getSpectrum().ints()),
                                    MassSpectrumType.PROFILE, PolarityType.POSITIVE,
                                    "", null);
                        }
                        parsedScans++;
                        newMZmineFile.addScan(ms2Scan);
                    }
                }
            }

            newMZmineFile.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));
            project.addFile(newMZmineFile);

        } catch (Throwable e) {
            e.printStackTrace();
            /* we may already have set the status to CANCELED */
            if (getStatus() == TaskStatus.PROCESSING) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage(ExceptionUtils.exceptionToString(e));
            }
            return;
        }

        if (isCanceled()) {
            return;
        }

        if (parsedScans == 0) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("No scans found");
            return;
        }

        logger.info("Finished parsing " + file + ", parsed " + parsedScans + " scans");
        setStatus(TaskStatus.FINISHED);

    }

    @Override
    public String getTaskDescription() {
        return "Opening file " + file;
    }

    public HashMap<String, Object> parseCVList(DDAMs ms) {
        HashMap<String, Object> cvMap = new HashMap<>();
        if (ms == null || ms.getCvList() == null || ms.getCvList().size() == 0) {
            return cvMap;
        }
        for (CV cv : ms.getCvList()) {
            if (cv.getCvid().contains(PSI.cvPolarityPositive)) {
                cvMap.put(Polarity_Type, PolarityType.POSITIVE);
                continue;
            }
            if (cv.getCvid().contains(PSI.cvPolarityNegative)) {
                cvMap.put(Polarity_Type, PolarityType.NEGATIVE);
                continue;
            }
            if (cv.getCvid().contains(PSI.cvProfileSpectrum)) {
                cvMap.put(Mass_Spectrum_Type, MassSpectrumType.PROFILE);
                continue;
            }
            if (cv.getCvid().contains(PSI.cvCentroidSpectrum)) {
                cvMap.put(Mass_Spectrum_Type, MassSpectrumType.CENTROIDED);
                continue;
            }
            if (cv.getCvid().contains(PSI.cvScanFilterString)) {
                cvMap.put(FILTER_STRING, cv.getValue());
                continue;
            }
            if (cv.getCvid().contains(PSI.cvLowestMz)) {
                cvMap.put(LOWEST_MZ, cv.getValue());
                continue;
            }
            if (cv.getCvid().contains(PSI.cvHighestMz)) {
                cvMap.put(HIGHEST_MZ, cv.getValue());
                continue;
            }
        }
        return cvMap;
    }
}
