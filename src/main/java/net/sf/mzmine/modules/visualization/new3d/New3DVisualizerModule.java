package net.sf.mzmine.modules.visualization.new3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javafx.application.Application;

import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineRunnableModule;
import net.sf.mzmine.modules.visualization.threed.ThreeDVisualizerModule;
import net.sf.mzmine.modules.visualization.threed.ThreeDVisualizerParameters;
//import net.sf.mzmine.modules.visualization.threed.ThreeDVisualizerWindow;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.scans.ScanUtils;

public class New3DVisualizerModule implements MZmineRunnableModule  {

	  private static final Logger LOG = Logger.getLogger(ThreeDVisualizerModule.class.getName());

	  private static final String MODULE_NAME = "New 3D visualizer";
	  private static final String MODULE_DESCRIPTION = "New 3D visualizer."; // TODO

	  @Override
	  public @Nonnull String getName() {
	    return MODULE_NAME;
	  }

	  @Override
	  public @Nonnull String getDescription() {
	    return MODULE_DESCRIPTION;
	  }

	  @Override
	  @Nonnull
	  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
	      @Nonnull Collection<Task> tasks) {

	    final RawDataFile[] dataFiles = parameters.getParameter(ThreeDVisualizerParameters.dataFiles)
	        .getValue().getMatchingRawDataFiles();
	    final ScanSelection scanSel =
	        parameters.getParameter(ThreeDVisualizerParameters.scanSelection).getValue();
	    Scan scans[] = scanSel.getMatchingScans(dataFiles[0]);
	    Range<Double> rtRange = ScanUtils.findRtRange(scans);

	    final Desktop desktop = MZmineCore.getDesktop();

	    // Check scan numbers.
	    if (scans.length == 0) {
	      desktop.displayErrorMessage(MZmineCore.getDesktop().getMainWindow(), "No scans found");
	      return ExitCode.ERROR;

	    }

	    ParameterSet myParameters =
	        MZmineCore.getConfiguration().getModuleParameters(ThreeDVisualizerModule.class);
	    try {
			/*
			 * ThreeDVisualizerWindow window = new ThreeDVisualizerWindow(dataFiles[0],
			 * scans, rtRange,
			 * myParameters.getParameter(ThreeDVisualizerParameters.rtResolution).getValue()
			 * , myParameters.getParameter(ThreeDVisualizerParameters.mzRange).getValue(),
			 * myParameters.getParameter(ThreeDVisualizerParameters.mzResolution).getValue()
			 * ); window.setVisible(true);
			 */
	    	Application.launch(MoleculeSampleApp.class,null);
	   
	    } catch (Throwable e) {
	      e.printStackTrace();
	      // Missing Java3D may cause UnsatisfiedLinkError or
	      // NoClassDefFoundError.
	      final String msg =
	          "Error initializing Java3D. Please file an issue at https://github.com/mzmine/mzmine2/issues and include the complete output of your MZmine console.";
	      LOG.log(Level.SEVERE, msg, e);
	      desktop.displayErrorMessage(MZmineCore.getDesktop().getMainWindow(), msg);
	    }

	    return ExitCode.OK;

	  }

	  public static void setupNew3DVisualizer(final RawDataFile dataFile) {
	    setupNew3DVisualizer(dataFile, null, null);
	  }

	  public static void setupNew3DVisualizer(final RawDataFile dataFile, final Range<Double> mzRange,
	      final Range<Double> rtRange) {

	    final ParameterSet myParameters =
	        MZmineCore.getConfiguration().getModuleParameters(ThreeDVisualizerModule.class);
	    final ThreeDVisualizerModule myInstance =
	        MZmineCore.getModuleInstance(ThreeDVisualizerModule.class);
	    myParameters.getParameter(ThreeDVisualizerParameters.dataFiles)
	        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES, new RawDataFile[] {dataFile});
	    myParameters.getParameter(ThreeDVisualizerParameters.scanSelection)
	        .setValue(new ScanSelection(rtRange, 1));
	    myParameters.getParameter(ThreeDVisualizerParameters.mzRange).setValue(mzRange);
	    if (myParameters.showSetupDialog(MZmineCore.getDesktop().getMainWindow(),
	        true) == ExitCode.OK) {
	      myInstance.runModule(MZmineCore.getProjectManager().getCurrentProject(),
	          myParameters.cloneParameterSet(), new ArrayList<Task>());
	    }
	  }

	  @Override
	  public @Nonnull MZmineModuleCategory getModuleCategory() {
	    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
	  }

	  @Override
	  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	    return ThreeDVisualizerParameters.class;
	  }
	

}
