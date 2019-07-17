package net.sf.mzmine.parameters.parametertypes.selectors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JList;

import net.sf.mzmine.datamodel.Feature;

public class FeaturesComponent extends JList<Object> implements ActionListener {

    private static final long serialVersionUID = 1L;
    public List<Feature> currentValue;

    public FeaturesComponent() {

    }

    void setValue(List<Feature> newValue) {
        currentValue = newValue;
    }

    List<Feature> getValue() {
        return currentValue;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub

    }

}
