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

package net.sf.mzmine.parameters.parametertypes;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.sf.mzmine.parameters.UserParameter;

public class OptionalParameterComponent<EmbeddedComponent extends JComponent>
        extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;
    private JCheckBox checkBox;
    private EmbeddedComponent embeddedComponent;

    public OptionalParameterComponent(
            UserParameter<?, EmbeddedComponent> embeddedParameter) {

        super(new FlowLayout(FlowLayout.LEFT));

        checkBox = new JCheckBox();
        checkBox.addActionListener(this);
        add(checkBox);

        embeddedComponent = embeddedParameter.createEditingComponent();
        embeddedComponent.setEnabled(false);
        add(embeddedComponent);
    }

    EmbeddedComponent getEmbeddedComponent() {
        return embeddedComponent;
    }

    public boolean isSelected() {
        return checkBox.isSelected();
    }

    public void setSelected(boolean selected) {
        checkBox.setSelected(selected);
        embeddedComponent.setEnabled(selected);
    }

    public void actionPerformed(ActionEvent event) {
        Object src = event.getSource();

        if (src == checkBox) {
            boolean checkBoxSelected = checkBox.isSelected();
            embeddedComponent.setEnabled(checkBoxSelected);
        }
    }

    @Override
    public void setToolTipText(String toolTip) {
        checkBox.setToolTipText(toolTip);
    }
}
