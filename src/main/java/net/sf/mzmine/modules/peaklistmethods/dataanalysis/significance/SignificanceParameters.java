/*
 * Copyright (C) 2018 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.significance;

import java.awt.Window;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.util.ExitCode;

public class SignificanceParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter(1, 1);

    public static final ComboParameter<UserParameter<?, ?>> selectionData =
        new ComboParameter<UserParameter<?, ?>>("Sample parameter",
            "One sample parameter has to be selected to be used in the test calculation. They can be defined in \"Project -> Set sample parameters\"",
            new UserParameter[0]);

    public SignificanceParameters() {
        super(new Parameter[] {peakLists, selectionData});
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

        // Update the parameter choices
        MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
        UserParameter[] newChoices = project.getParameters();
        getParameter(SignificanceParameters.selectionData).setChoices(newChoices);

        // Add a message
        String message = "<html>To view the results of ANOVA test, export the peak list to CSV file "
            + "and look for column ANOVA_P_VALUE. Click Help for details.</html>";

        ParameterSetupDialog dialog = new ParameterSetupDialog(
            parent, valueCheckRequired, this, message);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }
}
