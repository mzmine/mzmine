package net.sf.mzmine.modules.datapointprocessing.isotopes.deisotoper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.IsotopePatternManipulator;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingController;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingTask;
import net.sf.mzmine.modules.datapointprocessing.datamodel.ProcessedDataPoint;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;
import net.sf.mzmine.util.FormulaUtils;

public class DPPIsotopeGrouperTask2 extends DataPointProcessingTask {

  private static Logger logger = Logger.getLogger(DPPIsotopeGrouperTask.class.getName());

  // peaks counter
  private int processedPeaks, totalPeaks;

  // parameter values
  private MZTolerance mzTolerance;
  private boolean monotonicShape;
  private int maximumCharge;
  private String elements;
  private boolean autoRemove;
  private double mergeWidth;
  private final double minAbundance = 0.01;

  public DPPIsotopeGrouperTask2(DataPoint[] dataPoints, SpectraPlot plot, ParameterSet parameterSet,
      DataPointProcessingController controller, TaskStatusListener listener) {
    super(dataPoints, plot, parameterSet, controller, listener);

    // Get parameter values for easier use
    mzTolerance = parameterSet.getParameter(DPPIsotopeGrouperParameters.mzTolerance).getValue();
    monotonicShape =
        parameterSet.getParameter(DPPIsotopeGrouperParameters.monotonicShape).getValue();
    maximumCharge = parameterSet.getParameter(DPPIsotopeGrouperParameters.maximumCharge).getValue();
    elements = parameterSet.getParameter(DPPIsotopeGrouperParameters.element).getValue();
    autoRemove = parameterSet.getParameter(DPPIsotopeGrouperParameters.autoRemove).getValue();
  }


  @Override
  public void run() {
    if (!(getDataPoints() instanceof ProcessedDataPoint[])) {
      logger.warning(
          "The data points passed to Isotope Grouper were not an instance of processed data points. Make sure to run mass detection first.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    // check formula
    if (!FormulaUtils.checkMolecularFormula(elements)) {
      setStatus(TaskStatus.ERROR);
      logger.warning("Invalid element parameter in " + this.getClass().getName());
    }



  }


  /*
   * private void checkOverlappingIsotopes(AllElements elements, double maxRange) {
   * 
   * for (AllIsotopes isotopes : elements) {
   * 
   * for (int x = 0; x < isotopes.size(); x++) {
   * 
   * for (AllIsotopes i : elements) { for (int y = 0; y < i.size(); y++) {
   * 
   * if (Math.abs(isotopes.getIsotope(x).getExactMass() - i.getIsotope(y).getExactMass()) <
   * mergeWidth) { logger.info("overlapping: " + isotopes.getIsotope(x).getSymbol()); }
   * 
   * } } } } }
   */



  public static IsotopePattern checkOverlappingIsotopes(IsotopePattern pattern, IIsotope[] isotopes,
      double mergeWidth, double minAbundance) {
    DataPoint[] dp = pattern.getDataPoints();
    double basemz = dp[0].getMZ();
    List<DataPoint> newPeaks = new ArrayList<DataPoint>();

    double isotopeBaseMass = 0d;
    for (IIsotope isotope : isotopes) {
      if (isotope.getNaturalAbundance() > minAbundance) {
        isotopeBaseMass = isotope.getExactMass();
        logger.info("isotopeBaseMass of " + isotope.getSymbol() + " = " + isotopeBaseMass);
        break;
      }
    }


    // loop all new isotopes
    for (IIsotope isotope : isotopes) {
      if (isotope.getNaturalAbundance() < minAbundance)
        continue;
      // the difference added by the heavier isotope peak
      double possiblemzdiff = isotope.getExactMass() - isotopeBaseMass;
      if (possiblemzdiff < 0.000001)
        continue;
      boolean add = true;
      for (DataPoint patternDataPoint : dp) {
        // here check for every peak in the pattern, if a new peak would overlap
        // if it overlaps good, we dont need to add a new peak
        
        int i = 1;
        do {
          if (Math.abs(patternDataPoint.getMZ() * i - possiblemzdiff) <= mergeWidth) {
            // TODO: maybe we should do a average of the masses? i can'T say if it makes sense,
            // since
            // we're just looking for isotope mass differences and dont look at the total
            // composition,
            // so we dont know the intensity ratios
            logger.info("possible overlap found: " + i + " * pattern dp = " + patternDataPoint.getMZ()
                + "\toverlaps with " + isotope.getMassNumber()
                + isotope.getSymbol() + " (" + (isotopeBaseMass - isotope.getExactMass())
                + ")\tdiff: " + Math.abs(patternDataPoint.getMZ() * i - possiblemzdiff));
            add = false;
          }
          i++;
//          logger.info("do");
        } while (patternDataPoint.getMZ() * i <= possiblemzdiff + mergeWidth && patternDataPoint.getMZ() != 0.0);
      }
      
      if (add)
        newPeaks.add(new SimpleDataPoint(possiblemzdiff, 1));
    }

    // now add all new mzs to the isotopePattern
    // DataPoint[] newDataPoints = new SimpleDataPoint[dp.length + newPeaks.size()];
    for (DataPoint p : dp) {
      newPeaks.add(p);
    }
    newPeaks.sort((o1, o2) -> {
      return Double.compare(o1.getMZ(), o2.getMZ());
    });

    return new SimpleIsotopePattern(newPeaks.toArray(new DataPoint[0]),
        IsotopePatternStatus.PREDICTED, "");
  }


  // too much i think
  public IsotopePattern checkOverlappingIsotopes1(IsotopePattern pattern, IIsotope[] isotopes,
      double mergeWidth) {
    DataPoint[] dp = pattern.getDataPoints();
    double basemz = dp[0].getMZ();
    List<DataPoint> newPeaks = new ArrayList<DataPoint>();

    // loop all new isotopes
    for (IIsotope isotope : isotopes) {

      // check for multiple isotopes (e.g. 2x 13C in a Cl pattern)
      int num = 1;
      do {

        // calc possible new peaks relative to the base peak added by the new isotopes.
        double possiblemz = basemz + num * (isotope.getExactMass() - isotopes[0].getExactMass());

        for (DataPoint patternDataPoint : dp) {
          // here check for every peak in the pattern, if a new peak would overlap
          // if it overlaps good, we dont need to add a new peak
          if (Math.abs(patternDataPoint.getMZ() - possiblemz) <= mergeWidth) {

            logger.info("possible overlap found: m/z(p)= " + patternDataPoint.getMZ()
                + "\toverlaps with base (" + basemz + ") + " + isotope.getMassNumber()
                + isotope.getSymbol() + " (" + isotope.getExactMass() + ")\tdiff: "
                + Math.abs(patternDataPoint.getMZ() - possiblemz));

          }
          // if it doesn't overlap, we need to add a new one
          else {
            // we don't care about intensities here, just want to look at mass differences
            newPeaks.add(new SimpleDataPoint(possiblemz, 1));
          }

        }
        num++;
      } while (basemz
          + num * (isotope.getExactMass() - isotopes[0].getExactMass()) < (dp[dp.length - 1].getMZ()
              + mergeWidth));
    }

    // now add all new mzs to the isotopePattern
    // DataPoint[] newDataPoints = new SimpleDataPoint[dp.length + newPeaks.size()];
    for (DataPoint p : dp) {
      newPeaks.add(p);
    }
    newPeaks.sort((o1, o2) -> {
      return Double.compare(o1.getMZ(), o2.getMZ());
    });

    return new SimpleIsotopePattern(newPeaks.toArray(new DataPoint[0]),
        IsotopePatternStatus.PREDICTED, "");
  }

  private class AllIsotopes extends ArrayList<IIsotope> {
    private static final long serialVersionUID = 1L;

    public IIsotope getIsotope(int i) {
      return get(i);
    }
  }

  private class AllElements extends ArrayList<AllIsotopes> {
    private static final long serialVersionUID = 1L;

    public AllIsotopes getElement(int i) {
      return get(i);
    }
  }

  public static IsotopePattern getMassDifferences(String elements, double mergeWidth,
      double minAbundance) {
    SilentChemObjectBuilder builder =
        (SilentChemObjectBuilder) SilentChemObjectBuilder.getInstance();
    IMolecularFormula form =
        MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(elements, builder);
    Isotopes ifac;

    try {
      ifac = Isotopes.getInstance();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
    IsotopePattern massDifferences = new SimpleIsotopePattern(
        new DataPoint[] {new SimpleDataPoint(0.0, 1.0)}, IsotopePatternStatus.PREDICTED, "");

    for (IIsotope element : form.isotopes()) {
      massDifferences = checkOverlappingIsotopes(massDifferences,
          ifac.getIsotopes(element.getSymbol()), mergeWidth, minAbundance);
    }
    logger.info(dataPointsToString(massDifferences.getDataPoints()));
    return massDifferences;
  }

  private static String dataPointsToString(DataPoint[] dp) {
    String str = "";
    for (DataPoint p : dp)
      str += "(" + p.getMZ() + ", " + p.getIntensity() + "), ";
    return str;
  }

  @Override
  public String getTaskDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    // TODO Auto-generated method stub
    return 0;
  }

}
