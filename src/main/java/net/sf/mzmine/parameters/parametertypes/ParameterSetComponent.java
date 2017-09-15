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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

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
    private final JProgressBar progressBar;
    
    private ParameterSet parameters;
    
    public ParameterSetComponent(final ParameterSet parameters)
    {
        super(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        this.parameters = parameters;

        this.setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));
        
        lblParameters = new JLabel();
        lblParameters.setEnabled(false);
        gbc.gridx = 0;
        gbc.gridy = 0;
        this.add(lblParameters, gbc);

        btnChange = new JButton("Change");
        btnChange.addActionListener(this);
        btnChange.setEnabled(true);
        gbc.gridx = 1;
        gbc.gridy = 0;
        this.add(btnChange, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        progressBar = new JProgressBar();
        progressBar.setValue(0);
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);
        this.add(progressBar, gbc);

//        if (process != null) {
//            SwingUtilities.invokeLater(new Runnable() {
//                public void run() {
//                    int value = (int) Math.round(process.getFinishedPercentage());
//                    if (0 < value && value < 100) {
//                        progressBar.setValue(value);
//                        progressBar.setVisible(true);
//                    } else {
//                        progressBar.setValue(0);
//                        progressBar.setVisible(false);
//                    }
//
//                    try {
//                        Thread.sleep(5);
//                    }
//                    catch (InterruptedException e) {
//                        progressBar.setValue(0);
//                        progressBar.setVisible(false);
//                    }
//                }
//            });
//        }
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
        StringBuilder builder = new StringBuilder().append("<html>");
        Parameter[] params = parameters.getParameters();
        for (int i = 0; i < params.length; ++i) {
            builder.append(params[i].getName()).append(" = ").append(params[i].getValue());
            if (i < params.length - 1)
                builder.append("<br>");
        }
        builder.append("</html>");

        lblParameters.setText(builder.toString());
    }
}
