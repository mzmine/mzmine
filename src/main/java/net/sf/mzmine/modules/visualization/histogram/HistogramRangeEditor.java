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

package net.sf.mzmine.modules.visualization.histogram;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeComponent;

import com.google.common.collect.Range;

public class HistogramRangeEditor extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;
    private JComboBox<HistogramDataType> dataTypeCombo;
    private DoubleRangeComponent dataRangeComponent;

    public HistogramRangeEditor() {

	super(new BorderLayout());

	dataTypeCombo = new JComboBox<HistogramDataType>(
		HistogramDataType.values());
	add(dataTypeCombo, BorderLayout.WEST);

	dataRangeComponent = new DoubleRangeComponent(
		NumberFormat.getNumberInstance());
	add(dataRangeComponent, BorderLayout.CENTER);

    }

    public void setValue(Range<Double> value) {
	dataRangeComponent.setValue(value);
    }

    public HistogramDataType getSelectedType() {
	return (HistogramDataType) dataTypeCombo.getSelectedItem();
    }

    public Range<Double> getValue() {
	return dataRangeComponent.getValue();
    }

    @Override
    public void actionPerformed(ActionEvent event) {

	Object src = event.getSource();

	if (src == dataTypeCombo) {
	    HistogramDataType selectedType = (HistogramDataType) dataTypeCombo
		    .getSelectedItem();
	    if (selectedType == null)
		return;

	    switch (selectedType) {
	    case MASS:
		dataRangeComponent.setNumberFormat(MZmineCore
			.getConfiguration().getMZFormat());
		return;
	    case HEIGHT:
	    case AREA:
		dataRangeComponent.setNumberFormat(MZmineCore
			.getConfiguration().getIntensityFormat());
		return;
	    case RT:
		dataRangeComponent.setNumberFormat(MZmineCore
			.getConfiguration().getRTFormat());
		return;
	    }
	}

    }

}
