/* 
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
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
package net.sf.mzmine.parameters.parametertypes;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.ExitCode;

/**
 *
 * @author aleksandrsmirnov
 */
public class ParameterSetComponent extends JPanel implements ActionListener {
    
    private final JLabel lblParameters;
    private final JButton btnChange;
    
    private ParameterSet parameters;
    
    public ParameterSetComponent(final ParameterSet parameters) {
        
        this.parameters = parameters;
        
        this.setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));
        
        lblParameters = new JLabel();
        //lblParameters.getFont().getSize()
        lblParameters.setEnabled(false);
        this.add(lblParameters, BorderLayout.WEST);
        
        btnChange = new JButton("Change");
        btnChange.addActionListener(this);
        btnChange.setEnabled(true);
        
        //this.add(btnChange, 1, 0, 1, 1, 1, 0, GridBagConstraints.NONE);
        this.add(btnChange, BorderLayout.EAST);
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {
        Object src = event.getSource();
        
        if (src == btnChange) {
            
            if (parameters == null) return;
            
            ExitCode exitCode = parameters.showSetupDialog(null, true);
            if (exitCode != ExitCode.OK) return;
            
        }
        
        updateLabel();
    }
    
    public ParameterSet getValue() {
        return parameters;
    }
    
    public void setValue(final ParameterSet parameters) {
        this.parameters = parameters;
        
        updateLabel();
    }
    
    private void updateLabel() {
        // Update text for lblParameters
        String text = "<html>";
        for (final Parameter p : parameters.getParameters())
            text += p.getName() + " = " + p.getValue() + "<br>";
        
        text += "</html>";
        
        lblParameters.setText(text);
    }
}
