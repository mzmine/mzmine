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
import io.github.mzmine.datamodel.*;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
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
import net.csibio.aird.util.AirdScanUtil;
import net.csibio.aird.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AirdImportTask extends AbstractTask {

    private final ParameterSet parameters;
    private final Class<? extends MZmineModule> module;
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File file;
    private MZmineProject project;
    private RawDataFile newMZmineFile;
    private AirdInfo airdInfo;
    private String description;
    private int totalScans = 0, parsedScans;

    public AirdImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile,
                          @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
                          @NotNull Instant moduleCallDate) {
        super(newMZmineFile.getMemoryMapStorage(), moduleCallDate); // storage in raw data file
        this.project = project;
        if (fileToOpen.getName().toLowerCase().endsWith(".aird")){
            this.file = new File(AirdScanUtil.getIndexPathByAirdPath(fileToOpen.getPath()));
        }else{
            this.file = fileToOpen;
        }
        this.newMZmineFile = newMZmineFile;
        description = "Importing aird data file:" + fileToOpen.getName();
        this.parameters = parameters;
        this.module = module;
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

        DDAParser parser = null;
        try {

            parser = new DDAParser(file.getPath());
            airdInfo = parser.getAirdInfo();
            if (airdInfo == null) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Parsing Cancelled, The aird index file(.json, metadata) not exists or the json file is broken.");
                return;
            }


            totalScans = airdInfo.getTotalScanCount().intValue();
            List<DDAMs> msList = parser.readAllToMemory();
            if (msList.size() == 0) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Parsing Cancelled, No MS1 Scan found.");
                return;
            }

            for (int i = 0; i < msList.size(); i++) {
                DDAMs ms1 = msList.get(i);
                if (ms1.getCvList() == null || ms1.getCvList().size() == 0) {
                    setStatus(TaskStatus.ERROR);
                    setErrorMessage("Please check the 'PSI CV' option when using AirdPro for conversion");
                    return;
                }
                SimpleScan ms1Scan = buildSimpleScan(ms1, MsLevel.MS1.getCode(), null);
                parsedScans++;
                newMZmineFile.addScan(ms1Scan);
                if (ms1.getMs2List() != null && ms1.getMs2List().size() != 0) {
                    for (int j = 0; j < ms1.getMs2List().size(); j++) {
                        DDAMs ms2 = ms1.getMs2List().get(j);
                        if (ms2.getCvList() == null || ms2.getCvList().size() == 0) {
                            setStatus(TaskStatus.ERROR);
                            setErrorMessage("Please check the 'PSI CV' option when using AirdPro for conversion");
                            return;
                        }
                        SimpleScan ms2Scan = buildSimpleScan(ms2, MsLevel.MS2.getCode(), ms1Scan);
                        parsedScans++;
                        newMZmineFile.addScan(ms2Scan);
                    }
                }
            }
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Error during aird import of file" + file.getName());
            logger.log(Level.WARNING, e.getMessage(), e);
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Error Parsing Aird File:" + ExceptionUtils.exceptionToString(e));
            return;
        } finally {
            if (parser != null) {
                parser.close();
            }
        }

        if (parsedScans == 0) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("No scans found!");
            return;
        }

        logger.info("Finished parsing " + file + ", parsed " + parsedScans + " scans");

        newMZmineFile.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));
        project.addFile(newMZmineFile);
        setStatus(TaskStatus.FINISHED);

    }

    @Override
    public String getTaskDescription() {
        return description;
    }

    private SimpleScan buildSimpleScan(DDAMs ms, int msLevel, Scan parentScan) {
        MassSpectrumType massSpectrumType = null;
        PolarityType polarityType = null;
        String filterString = null;
        Double lowestMz = null;
        Double highestMz = null;

        for (CV cv : ms.getCvList()) {
            if (cv.getCvid().contains(PSI.cvPolarityPositive)) {
                polarityType = PolarityType.POSITIVE;
                continue;
            }
            if (cv.getCvid().contains(PSI.cvPolarityNegative)) {
                polarityType = PolarityType.NEGATIVE;
                continue;
            }
            if (cv.getCvid().contains(PSI.cvProfileSpectrum)) {
                massSpectrumType = MassSpectrumType.PROFILE;
                continue;
            }
            if (cv.getCvid().contains(PSI.cvCentroidSpectrum)) {
                massSpectrumType = MassSpectrumType.CENTROIDED;
                continue;
            }
            if (cv.getCvid().contains(PSI.cvScanFilterString)) {
                filterString = cv.getValue();
                continue;
            }
            if (cv.getCvid().contains(PSI.cvLowestMz)) {
                lowestMz = Double.parseDouble(cv.getValue());
                continue;
            }
            if (cv.getCvid().contains(PSI.cvHighestMz)) {
                highestMz = Double.parseDouble(cv.getValue());
                continue;
            }
        }

        Range mzRange = null;
        if (lowestMz != null && highestMz != null) {
            mzRange = Range.closed(lowestMz, highestMz);
        }

        DDAMsMsInfoImpl msMsInfo = null;
        if (msLevel == MsLevel.MS2.getCode()) {
            msMsInfo = buildMsMsInfo(airdInfo, ms, parentScan);
        }
        SimpleScan msScan = new SimpleScan(newMZmineFile, ms.getNum(), msLevel,
                ms.getRt(), msMsInfo, ArrayUtil.fromFloatToDouble(ms.getSpectrum().mzs()), ArrayUtil.fromFloatToDouble(ms.getSpectrum().ints()),
                massSpectrumType, polarityType,
                filterString, mzRange);
        return msScan;
    }

    private DDAMsMsInfoImpl buildMsMsInfo(AirdInfo airdInfo, DDAMs ddaMs, Scan parentScan) {
        Double precursorMz = ddaMs.getRange().getMz();
        Integer charge = ddaMs.getRange().getCharge() == 0 ? null : ddaMs.getRange().getCharge();
        Float energy = airdInfo.getEnergy() == -1 ? null : airdInfo.getEnergy();
        ActivationMethod method = ActivationMethod.valueOf(airdInfo.getActivator());
        if (precursorMz == null) {
            return null;
        }
        DDAMsMsInfoImpl msMsInfo = new DDAMsMsInfoImpl(precursorMz, charge, energy, null, parentScan, MsLevel.MS2.getCode(), method, null);
        return msMsInfo;
    }
}
