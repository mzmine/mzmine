/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

package net.sf.mzmine.parameters.parametertypes;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.tools.mzrangecalculator.MzRangeCalculatorModule;
import net.sf.mzmine.util.Range;

public class MZRangeComponent extends RangeComponent implements ActionListener {

    private final JButton setAutoButton, fromFormulaButton;

    public MZRangeComponent() {

        super(MZmineCore.getConfiguration().getMZFormat());

        setAutoButton = new JButton("Auto range");
        setAutoButton.addActionListener(this);
        RawDataFile currentFiles[] = MZmineCore.getCurrentProject()
                .getDataFiles();
        setAutoButton.setEnabled(currentFiles.length > 0);
        add(setAutoButton, 3, 0, 1, 1, 1, 0, GridBagConstraints.NONE);

        fromFormulaButton = new JButton("From formula");
        fromFormulaButton.addActionListener(this);
        add(fromFormulaButton, 4, 0, 1, 1, 1, 0, GridBagConstraints.NONE);

    }

    @Override
    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();

        if (src == setAutoButton) {
            Range mzRange = null;
            RawDataFile currentFiles[] = MZmineCore.getCurrentProject()
                    .getDataFiles();
            for (RawDataFile file : currentFiles) {
                Range fileRange = file.getDataMZRange(1);
                if (fileRange == null)
                    continue;
                if (mzRange == null)
                    mzRange = fileRange;
                else
                    mzRange.extendRange(fileRange);
            }
            if (mzRange != null)
                setValue(mzRange);
        }

        if (src == fromFormulaButton) {
            Range mzRange = MzRangeCalculatorModule
                    .showRangeCalculationDialog();
            if (mzRange != null)
                setValue(mzRange);
        }

    }

}
