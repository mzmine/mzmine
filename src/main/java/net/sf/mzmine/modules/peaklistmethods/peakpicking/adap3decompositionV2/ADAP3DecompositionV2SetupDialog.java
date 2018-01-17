/* 
 * Copyright (C) 2017 Du-Lab Team <dulab.binf@gmail.com>
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
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV2;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.border.Border;

import com.google.common.collect.Range;
import dulab.adap.datamodel.Chromatogram;
import dulab.adap.workflow.decomposition.PeakDetector;
import dulab.adap.workflow.decomposition.RetTimeClusterer;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.BaselineCorrectorSetupDialog;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */


public class ADAP3DecompositionV2SetupDialog extends ParameterSetupDialog
{
    /** Minimum dimensions of plots */
    private static final Dimension MIN_DIMENSIONS = new Dimension(400, 300);

    /** One of three states:
     *  > no changes made,
     *  > change in the first phase parameters,
     *  > change in the second phase parameters
     */
    private enum CHANGE_STATE {NONE, FIRST_PHASE, SECOND_PHASE}

    private final DataProvider dataProvider;

    /**
     * Elements of the interface
     */
    private JPanel pnlUIElements;
    private JPanel pnlPlots;
    private JCheckBox chkPreview;

    /** Current values of the parameters */
    private final Object[] currentParameters;

    /** Creates an instance of the class and saves the current values of all parameters */
    ADAP3DecompositionV2SetupDialog(Window parent, boolean valueCheckRequired,
            @Nonnull final ParameterSet parameters)
    {    
        super(parent, valueCheckRequired, parameters);

        currentParameters = Arrays.stream(parameters.getParameters())
                .map(Parameter::getValue).toArray(Object[]::new);

        PeakList[] peakLists = parameters.getParameter(ADAP3DecompositionV2Parameters.PEAK_LISTS)
                .getValue().getMatchingPeakLists();

        dataProvider = new DataProvider(parameters);
    }

    /** Creates the interface elements */
    @Override
    protected void addDialogComponents()
    {
        super.addDialogComponents();
        
        PeakList[] peakLists = MZmineCore.getDesktop().getSelectedPeakLists();
        
        // -----------------------------
        // Panel with preview UI elements
        // -----------------------------

        // Preview CheckBox
        chkPreview = new JCheckBox("Show preview");
        chkPreview.addActionListener(this);
        chkPreview.setHorizontalAlignment(SwingConstants.CENTER);
        chkPreview.setEnabled(peakLists != null && peakLists.length > 0);

        // Preview panel that will contain ComboBoxes
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JSeparator(), BorderLayout.NORTH);
        panel.add(chkPreview, BorderLayout.CENTER);
        panel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);
        pnlUIElements = new JPanel(new BorderLayout());
        pnlUIElements.add(panel, BorderLayout.NORTH);
        
        // --------------------------------------------------------------------
        // ----- Panel with plots --------------------------------------
        // --------------------------------------------------------------------

        pnlPlots = new JPanel();
        pnlPlots.setLayout(new BoxLayout(pnlPlots, BoxLayout.Y_AXIS));

        JTabbedPane tabbedPane = new JTabbedPane();
        for (AlgorithmSupplier s : ADAP3DecompositionV2Parameters.SUPPLIERS) {
            tabbedPane.addTab(s.getName(), s.getPanel());
        }

        pnlPlots.add(tabbedPane);
        
        super.mainPanel.add(pnlUIElements, 0, super.getNumberOfParameters() + 3,
                2, 1, 0, 0, GridBagConstraints.HORIZONTAL);
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        super.actionPerformed(e);
        
        final Object source = e.getSource();

        if (source.equals(chkPreview))
        {
            if (chkPreview.isSelected()) {
                // Set the height of the chkPreview to 200 cells, so it will span
                // the whole vertical length of the dialog (buttons are at row
                // no 100). Also, we set the weight to 10, so the chkPreview
                // component will consume most of the extra available space.
                mainPanel.add(pnlPlots, 3, 0, 1, 200, 10, 10,
                        GridBagConstraints.BOTH);

                for (AlgorithmSupplier s : ADAP3DecompositionV2Parameters.SUPPLIERS)
                    s.updateData(dataProvider);
            }
            else {
                mainPanel.remove(pnlPlots);
            }

            updateMinimumSize();
            pack();
            setLocationRelativeTo(MZmineCore.getDesktop().getMainWindow());
        }
    }
    
    
    @Override
    public void parametersChanged()
    {
        super.updateParameterSetFromComponents();

        if (!chkPreview.isSelected()) return;

        Parameter parameter = getChangedParameter();

        AlgorithmSupplier[] suppliers = ADAP3DecompositionV2Parameters.SUPPLIERS;
        for (int i = 0; i < suppliers.length; ++i)
            for (Parameter p : suppliers[i].getParameters())
                if (p == parameter)
                    while (i < suppliers.length)
                        suppliers[i++].updateData(dataProvider);
    }

    private Parameter getChangedParameter()
    {
        Parameter[] parameters = parameterSet.getParameters();
        for (int i = 0; i < currentParameters.length; ++i)
            if (currentParameters[i] == null || !currentParameters[i].equals(parameters[i].getValue())) {
                currentParameters[i] = parameters[i].getValue();
                return parameters[i];
            }
        return null;
    }
}
