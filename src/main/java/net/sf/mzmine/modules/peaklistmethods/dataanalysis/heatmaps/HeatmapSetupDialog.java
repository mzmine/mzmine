/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.heatmaps;

import java.awt.Window;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.parametertypes.ComboComponent;

public class HeatmapSetupDialog extends ParameterSetupDialog {

    private static final long serialVersionUID = 1L;
    private ComboComponent<?> selDataCombo, refGroupCombo;
    private UserParameter<?, ?> previousParameterSelection;

    public HeatmapSetupDialog(Window parent, boolean valueCheckRequired,
            HeatMapParameters parameters) {
        super(parent, valueCheckRequired, parameters);

        // Get a reference to the combo boxes
        selDataCombo = (ComboComponent<?>) this
                .getComponentForParameter(HeatMapParameters.selectionData);
        refGroupCombo = (ComboComponent<?>) this
                .getComponentForParameter(HeatMapParameters.referenceGroup);

        // Save a reference to current "Sample parameter" value
        previousParameterSelection = (UserParameter<?, ?>) selDataCombo
                .getSelectedItem();

        // Call parametersChanged() to rebuild the reference group combo
        parametersChanged();

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void parametersChanged() {

        // Get the current value of the "Sample parameter" combo
        UserParameter<?, ?> currentParameterSelection = (UserParameter<?, ?>) selDataCombo
                .getSelectedItem();
        if (currentParameterSelection == null)
            return;

        // If the value has changed, update the "Reference group" combo
        if (currentParameterSelection != previousParameterSelection) {
            ArrayList<Object> values = new ArrayList<Object>();

            // Obtain all possible values
            for (RawDataFile dataFile : MZmineCore.getProjectManager()
                    .getCurrentProject().getDataFiles()) {
                Object paramValue = MZmineCore.getProjectManager()
                        .getCurrentProject()
                        .getParameterValue(currentParameterSelection, dataFile);
                if (paramValue == null)
                    continue;
                if (!values.contains(paramValue))
                    values.add(paramValue);
            }

            // Update the parameter and combo model
            Object newValues[] = values.toArray();
            super.parameterSet.getParameter(HeatMapParameters.referenceGroup)
                    .setChoices(newValues);
            refGroupCombo.setModel(new DefaultComboBoxModel(newValues));

            previousParameterSelection = currentParameterSelection;
        }

        this.updateParameterSetFromComponents();

    }

}
