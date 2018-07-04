/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.ELEMENTS;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.MZ_TOLERANCE;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.MAX_RESULTS;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.NEUTRAL_MASS;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.datamodel.IonType;
import io.github.msdk.datamodel.MsSpectrum;
import io.github.msdk.datamodel.SimpleMsSpectrum;
import io.github.msdk.id.sirius.ConstraintsGenerator;
import io.github.msdk.id.sirius.FingerIdWebMethod;
import io.github.msdk.id.sirius.SiriusIdentificationMethod;
import io.github.msdk.id.sirius.SiriusIonAnnotation;
import io.github.msdk.util.IonTypeUtil;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import org.openscience.cdk.formula.MolecularFormulaRange;

public class SingleRowIdentificationTask extends AbstractTask {
  public static final NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

  private int finishedItems = 0, numItems;

  private double searchedMass;
  private MZTolerance mzTolerance;
  private int charge;
  private int numOfResults;
  private PeakListRow peakListRow;
  private IonizationType ionType;
  private MolecularFormulaRange formulaRange;
  private Double parentMass;

  /**
   * Create the task.
   * 
   * @param parameters task parameters.
   * @param peakListRow peak-list row to identify.
   */
  public SingleRowIdentificationTask(ParameterSet parameters, PeakListRow peakListRow) {

    this.peakListRow = peakListRow;

    searchedMass = parameters.getParameter(NEUTRAL_MASS).getValue();
    mzTolerance = parameters.getParameter(MZ_TOLERANCE).getValue();
    numOfResults = parameters.getParameter(MAX_RESULTS).getValue();

    ionType = parameters.getParameter(NEUTRAL_MASS).getIonType();
    charge = parameters.getParameter(NEUTRAL_MASS).getCharge();
    parentMass = parameters.getParameter(NEUTRAL_MASS).getValue();

    formulaRange = parameters.getParameter(ELEMENTS).getValue();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (numItems == 0)
      return 0;
    return ((double) finishedItems) / numItems;
  }

  public String getTaskDescription() {
    return "Peak identification of " + massFormater.format(searchedMass) + " using Sirius module";
  }

  /**
   * @see Runnable#run()
   */
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

    ResultWindow window = new ResultWindow(peakListRow, searchedMass, this);
    window.setTitle("Sirius makes fun " + massFormater.format(searchedMass) + " amu");
    window.setVisible(true);

    ConstraintsGenerator generator = new ConstraintsGenerator();
    FormulaConstraints constraints = generator.generateConstraint(formulaRange);


    double ppm = mzTolerance.getPpmTolerance();

    Feature bestPeak = peakListRow.getBestPeak();
    int ms1index = bestPeak.getRepresentativeScanNumber();
    int ms2index = bestPeak.getMostIntenseFragmentScanNumber();


    RawDataFile rawfile = bestPeak.getDataFile();

    List<MsSpectrum> ms1list = processRawScan(rawfile, ms1index);
    List<MsSpectrum> ms2list = processRawScan(rawfile, ms2index);

    IonType siriusIon = IonTypeUtil.createIonType(ionType.toString());

    SiriusIdentificationMethod siriusMethod = new SiriusIdentificationMethod(
        ms1list,
        ms2list,
        parentMass,
        siriusIon,
        numOfResults,
        constraints,
        ppm
    );

    FingerIdWebMethod fingerMethod = null;
    List<IonAnnotation> siriusResults = null;
    try {
      siriusMethod.execute();
      siriusResults = siriusMethod.getResult();

      if (ms2index != -1) {
        SiriusIonAnnotation siriusAnnotation = (SiriusIonAnnotation) siriusResults.get(0);
        Ms2Experiment experiment = siriusMethod.getExperiment();
        fingerMethod = new FingerIdWebMethod(experiment, siriusAnnotation, 10);

        List<IonAnnotation> fingerResults = fingerMethod.execute();

        if (fingerResults != null && fingerResults.size() > 0) {
          for (IonAnnotation a: fingerResults) {
            SiriusIonAnnotation temp = (SiriusIonAnnotation) a;
            SiriusCompound compound = new SiriusCompound(temp, temp.getFingerIdScore());
            window.addNewListItem(compound);
          }
        } else {
          for (IonAnnotation a: siriusResults) {
            SiriusIonAnnotation temp = (SiriusIonAnnotation) a;
            SiriusCompound compound = new SiriusCompound(temp, temp.getSiriusScore());
            window.addNewListItem(compound);
          }
        }
      } else {
        for (IonAnnotation a: siriusResults) {
          SiriusIonAnnotation temp = (SiriusIonAnnotation) a;
          SiriusCompound compound = new SiriusCompound(temp, temp.getSiriusScore());
          window.addNewListItem(compound);
        }
      }
    } catch (RuntimeException t) {
      System.out.println("No edges stuf happened");
      for (IonAnnotation a: siriusResults) {
        SiriusIonAnnotation temp = (SiriusIonAnnotation) a;
        SiriusCompound compound = new SiriusCompound(temp, temp.getSiriusScore());
        window.addNewListItem(compound);
      }
      t.printStackTrace();
    } catch (MSDKException e) {
      e.printStackTrace();
      System.out.println("Hell is here");
    }


    setStatus(TaskStatus.FINISHED);

  }

  private List<MsSpectrum> processRawScan(RawDataFile rawfile, int index) {
    LinkedList<MsSpectrum> spectra = null;
    if (index != -1) {
      spectra = new LinkedList<>();
      Scan scan = rawfile.getScan(index);
      DataPoint[] points = scan.getDataPoints();
      MsSpectrum ms = buildSpectrum(points);
      spectra.add(ms);
    }

    return spectra;
  }

  private MsSpectrum buildSpectrum(DataPoint[] ms1points) {
    SimpleMsSpectrum spectrum = new SimpleMsSpectrum();
    double mz[] = new double[ms1points.length];
    float intensity[] = new float[ms1points.length];
    IonType siriusIonType = IonTypeUtil.createIonType(ionType.toString());

    for (int i = 0; i < ms1points.length; i++) {
      mz[i] = ms1points[i].getMZ();
      intensity[i] = (float) ms1points[i].getIntensity();
    }

    spectrum.setDataPoints(mz, intensity, ms1points.length);
    return spectrum;
  }

}
