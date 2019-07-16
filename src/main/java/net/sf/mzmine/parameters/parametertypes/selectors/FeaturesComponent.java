package net.sf.mzmine.parameters.parametertypes.selectors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JList;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.main.MZmineCore;

public class FeaturesComponent extends JList<Object> implements ActionListener {

    private static final long serialVersionUID = 1L;
    public Feature[] peaks;

    public FeaturesComponent() {
        peaks = (Feature[]) MZmineCore.getProjectManager().getCurrentProject()
                .getPeakLists();
        setListData(peaks);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub

    }

}
