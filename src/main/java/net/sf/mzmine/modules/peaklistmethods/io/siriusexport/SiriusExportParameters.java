/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 * 
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package net.sf.mzmine.modules.peaklistmethods.io.siriusexport;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.*;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.util.ExitCode;

import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;


public class SiriusExportParameters extends SimpleParameterSet {


  public static final IntegerParameter COSINE_PARAMETER = new IntegerParameter("Cosine threshold (%)", "Threshold for the cosine similarity between two spectra for merging. Set to 0 if the spectra may have different collision energy!", 80, 0, 100);

  public static final IntegerParameter MASS_ACCURACY = new IntegerParameter("expected mass deviation (ppm)", "Expected mass deviation of your measurement in ppm (parts per million).", 10, 1, 100);

  public static final BooleanParameter AVERAGE_OVER_MASS = new BooleanParameter("Average over m/z", "When merging two peaks, the m/z of the merged peak is set to the weighted average of the source peaks. If unchecked, the m/z of the peak with largest intensity is used instead.", false);

  public static final ComboParameter<MERGE_MODE> MERGE =
      new ComboParameter<MERGE_MODE>("Merge mode", "How to merge MS/MS spectra",
          MERGE_MODE.values(), MERGE_MODE.MERGE_CONSECUTIVE_SCANS);

  public SiriusExportParameters() {
    super(new Parameter[] {PEAK_LISTS, FILENAME, MERGE, AVERAGE_OVER_MASS,  COSINE_PARAMETER, MASS_ACCURACY,  MASS_LIST, isolationWindowOffset, isolationWindowWidth});
  }

  public static final DoubleParameter isolationWindowOffset = new DoubleParameter("isolation window offset", "isolation window offset from the precursor m/z", NumberFormat.getNumberInstance(Locale.US), 0d);
  public static final DoubleParameter isolationWindowWidth = new DoubleParameter("isolation window width", "width (left and right from offset) of the isolation window", NumberFormat.getNumberInstance(Locale.US), 1d);


  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final FileNameParameter FILENAME = new FileNameParameter("Filename",
      "Name of the output MGF file. "
          + "Use pattern \"{}\" in the file name to substitute with peak list name. "
          + "(i.e. \"blah{}blah.mgf\" would become \"blahSourcePeakListNameblah.mgf\"). "
          + "If the file already exists, it will be overwritten.",
      "mgf");

  // public static final BooleanParameter FRACTIONAL_MZ = new BooleanParameter(
  // "Fractional m/z values", "If checked, write fractional m/z values",
  // true);

  /*
   * public static final BooleanParameter INCLUDE_MSSCAN = new BooleanParameter( "include MS1",
   * "For each MS/MS scan include also the corresponding MS scan (additionally to possibly detected isotope patterns). MS1 scans might contain valuable informations that can be processed by SIRIUS. But they increase file size significantly"
   * , true );
   */

  public static final MassListParameter MASS_LIST = new MassListParameter();

  public static enum MERGE_MODE {
    NO_MERGE("Do not merge"), MERGE_CONSECUTIVE_SCANS(
        "Merge consecutive scans"), MERGE_OVER_SAMPLES(
            "Merge all MS/MS belonging to the same feature");


    private final String name;

    private MERGE_MODE(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    String message = "<html>SIRIUS Module Disclaimer:"
        + "<br>    - If you use the SIRIUS export module, cite <a href=\"https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-11-395\">MZmine2 paper</a> and the following articles: "
        + "<br>     <a href=\"http://www.pnas.org/content/112/41/12580.abstract\">Dührkop et al., Proc Natl Acad Sci USA 112(41):12580-12585</a> "
        + "and <a href=\"https://jcheminf.springeropen.com/articles/10.1186/s13321-016-0116-8\">Boecker et al., Journal of Cheminformatics (2016) 8:5</a>"
        + "<br>    - Sirius can be downloaded at the following address: <a href=\"https://bio.informatik.uni-jena.de/software/sirius/\">https://bio.informatik.uni-jena.de/software/sirius/</a>"
        + "<br>    - Sirius results can be mapped into <a href=\"http://gnps.ucsd.edu/\">GNPS</a> molecular networks. <a href=\"https://bix-lab.ucsd.edu/display/Public/Mass+spectrometry+data+pre-processing+for+GNPS\">See the documentation</a>.";
    ParameterSetupDialog dialog =
        new ParameterSetupDialog(parent, valueCheckRequired, this, message);
    dialog.setVisible(true);
    return dialog.getExitCode();
  }
}
