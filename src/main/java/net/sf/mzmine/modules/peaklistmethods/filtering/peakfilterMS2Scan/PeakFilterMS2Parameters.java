/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang 
 * at the Dorrestein Lab (University of California, San Diego). 
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 */

package net.sf.mzmine.modules.peaklistmethods.filtering.peakfilterMS2Scan;

import java.awt.Window;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.IntRangeParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.util.ExitCode;

import com.google.common.collect.Range;

public class PeakFilterMS2Parameters extends SimpleParameterSet {

    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final StringParameter SUFFIX = new StringParameter(
            "Name suffix", "Suffix to be added to peak list name", "filtered");



    public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
            "Remove source peak list after filtering",
            "If checked, the original peak list will be removed leaving only the filtered version");
    
    
    public PeakFilterMS2Parameters() {
        super(new Parameter[] { PEAK_LISTS, SUFFIX,AUTO_REMOVE });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

        // Update the parameter choices
        UserParameter<?, ?> newChoices[] = MZmineCore.getProjectManager()
                .getCurrentProject().getParameters();
        String[] choices;
        if (newChoices == null || newChoices.length == 0) {
            choices = new String[1];
            choices[0] = "No parameters defined";
        } else {
            choices = new String[newChoices.length + 1];
            choices[0] = "Ignore groups";
            for (int i = 0; i < newChoices.length; i++) {
                choices[i + 1] = "Filtering by " + newChoices[i].getName();
            }
        }

        ParameterSetupDialog dialog = new ParameterSetupDialog(parent,
                valueCheckRequired, this);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }
}
