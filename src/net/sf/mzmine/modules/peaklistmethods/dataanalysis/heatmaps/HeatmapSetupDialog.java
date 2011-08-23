/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import javax.swing.JComboBox;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.project.MZmineProject;

public class HeatmapSetupDialog extends ParameterSetupDialog implements ActionListener {

        private JComboBox comp, comp2;

        public HeatmapSetupDialog(HeatMapParameters parameters) {

                super(parameters, null, null);
                comp = (JComboBox) this.getComponentForParameter(parameters.getParameter(HeatMapParameters.referenceGroup));
                comp2 = (JComboBox) this.getComponentForParameter(parameters.getParameter(HeatMapParameters.selectionData));
                comp2.addActionListener(this);
                if (parameterSet.getParameter(HeatMapParameters.selectionData) != null) {
                        setValues((ParameterType) this.comp2.getSelectedItem());
                }
        }

        @Override
        public void actionPerformed(ActionEvent event) {
                super.actionPerformed(event);
                Object src = event.getSource();
                if (src == comp2) {
                        // setValues((ParameterType) this.comp2.getSelectedItem());
                }
        }

        @Override
        public void parametersChanged() {
                setValues((ParameterType) this.comp2.getSelectedItem());
        }

        public final void setValues(ParameterType parameterName) {
                comp.removeAllItems();
                ArrayList<ParameterType> choicesList = new ArrayList<ParameterType>();
                PeakList selectedPeakList = MZmineCore.getDesktop().getSelectedPeakLists()[0];
                // Collect all data files
                Vector<RawDataFile> allDataFiles = new Vector<RawDataFile>();
                allDataFiles.addAll(Arrays.asList(selectedPeakList.getRawDataFiles()));

                MZmineProject project = MZmineCore.getCurrentProject();
                if (parameterName != null) {
                        UserParameter selectedParameter = parameterName.getParameter();
                        for (RawDataFile rawDataFile : allDataFiles) {

                                Object paramValue = project.getParameterValue(
                                        selectedParameter, rawDataFile);

                                if (!contains((String) paramValue, choicesList)) {
                                        choicesList.add(new ParameterType((String) paramValue));
                                }
                        }
                }

                for (Object choice : choicesList) {
                        comp.addItem(choice);
                }
                this.updateParameterSetFromComponents();
                this.repaint();
        }

        private boolean contains(String paramValue, ArrayList<ParameterType> choicesList) {
                for (ParameterType choice : choicesList) {
                        if (choice.toString().equals(paramValue)) {
                                return true;
                        }
                }
                return false;
        }
}
