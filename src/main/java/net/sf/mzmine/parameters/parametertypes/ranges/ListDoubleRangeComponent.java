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

package net.sf.mzmine.parameters.parametertypes.ranges;

import com.google.common.collect.Range;
import java.awt.Color;


import java.util.List;
import javax.swing.JLabel;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sf.mzmine.util.components.GridBagPanel;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */


public class ListDoubleRangeComponent extends GridBagPanel
{
    private JTextField inputField;
    private JLabel textField;
    
    public ListDoubleRangeComponent()
    {
        inputField = new JTextField();
        inputField.setColumns(16);
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {update();}
            
            @Override
            public void removeUpdate(DocumentEvent e) {update();}
            
            @Override
            public void insertUpdate(DocumentEvent e) {update();}
        });
        
        textField = new JLabel();
        //textField.setColumns(8);
        
        add(inputField, 0, 0);
        add(textField, 0, 1);
    }
    
    public List <Range <Double>> getValue() 
    {
        try {
            return dulab.adap.common.algorithms.String.toRanges(
                    textField.getText());
        } catch (Exception e) {
            return null;
        }
    }
    
    public void setValue(List <Range <Double>> ranges) {
        String text = dulab.adap.common.algorithms.String.fromRanges(ranges);
        
        textField.setForeground(Color.black);
        textField.setText(text);
        inputField.setText(text);
    }
    
    @Override
    public void setToolTipText (String toolTip)
    {
        textField.setToolTipText(toolTip);
        inputField.setToolTipText(toolTip);
    }
    
    @Override
    public void setEnabled (boolean enabled)
    {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
        inputField.setEnabled(enabled);
    }
    
    private void update() 
    {
        try {
            List <Range <Double>> ranges = 
                    dulab.adap.common.algorithms.String.toRanges(
                            inputField.getText());

            textField.setForeground(Color.black);
            textField.setText(
                    dulab.adap.common.algorithms.String.fromRanges(ranges));
        } 
        catch (IllegalArgumentException e) {
            textField.setForeground(Color.red);
            textField.setText(e.getMessage());
        }
    }
}
