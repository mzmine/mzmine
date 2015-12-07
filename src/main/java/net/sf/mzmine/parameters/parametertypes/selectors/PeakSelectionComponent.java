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

package net.sf.mzmine.parameters.parametertypes.selectors;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ranges.IntRangeParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.GUIUtils;

public class PeakSelectionComponent extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final DefaultListModel<PeakSelection> selectionListModel;
    private final JList<PeakSelection> selectionList;
    private final JButton addButton, removeButton, allButton;

    public PeakSelectionComponent() {

        super(new BorderLayout());

        selectionListModel = new DefaultListModel<PeakSelection>();
        selectionList = new JList<>(selectionListModel);
        JScrollPane scrollPane = new JScrollPane(selectionList);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        addButton = GUIUtils.addButton(buttonPanel, "Add", null, this);
        removeButton = GUIUtils.addButton(buttonPanel, "Remove", null, this);
        allButton = GUIUtils.addButton(buttonPanel, "Set all", null, this);

        add(buttonPanel, BorderLayout.EAST);
    }

    void setValue(List<PeakSelection> newValue) {
        selectionListModel.clear();
        for (PeakSelection ps : newValue)
            selectionListModel.addElement(ps);
    }

    public List<PeakSelection> getValue() {
        List<PeakSelection> items = Lists.newArrayList();
        ListModel<PeakSelection> model = selectionList.getModel();
        for (int i = 0; i < model.getSize(); i++)
            items.add(model.getElementAt(i));
        return items;
    }

    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();

        if (src == addButton) {
            final IntRangeParameter idParameter = new IntRangeParameter("ID",
                    "Range of included peak IDs", false, null);
            final MZRangeParameter mzParameter = new MZRangeParameter(false);
            final RTRangeParameter rtParameter = new RTRangeParameter(false);
            SimpleParameterSet paramSet = new SimpleParameterSet(
                    new Parameter[] { idParameter, mzParameter, rtParameter });
            Window parent = (Window) SwingUtilities
                    .getAncestorOfClass(Window.class, this);
            ExitCode exitCode = paramSet.showSetupDialog(parent, true);
            if (exitCode == ExitCode.OK) {
                Range<Integer> idRange = paramSet.getParameter(idParameter)
                        .getValue();
                Range<Double> mzRange = paramSet.getParameter(mzParameter)
                        .getValue();
                Range<Double> rtRange = paramSet.getParameter(rtParameter)
                        .getValue();
                if ((idRange == null) && (mzRange == null) && (rtRange == null))
                    return;
                PeakSelection ps = new PeakSelection(idRange, mzRange, rtRange);
                selectionListModel.addElement(ps);
            }
        }

        if (src == allButton) {
            Range<Integer> idRange = Range.closed(1, Integer.MAX_VALUE);
            PeakSelection ps = new PeakSelection(idRange, null, null);
            selectionListModel.clear();
            selectionListModel.addElement(ps);
        }

        if (src == removeButton) {
            for (PeakSelection p : selectionList.getSelectedValuesList()) {
                selectionListModel.removeElement(p);
            }
        }

    }

    @Override
    public void setToolTipText(String toolTip) {
        selectionList.setToolTipText(toolTip);
    }

}
