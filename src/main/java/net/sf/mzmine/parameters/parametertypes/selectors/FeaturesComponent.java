package net.sf.mzmine.parameters.parametertypes.selectors;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import net.sf.mzmine.datamodel.Feature;

public class FeaturesComponent extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;
    public List<Feature> currentValue;
    private final JList<Feature> jlist = null;
    private final JButton addButton;
    private final JButton removeButton;

    public FeaturesComponent() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        FeaturesParameter features = new FeaturesParameter();
        currentValue = features.getValue();
        // jlist.setSize(5, 10);
        JScrollPane scrollPane = new JScrollPane(jlist);
        scrollPane.setSize(30, 10);
        add(scrollPane, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();

        add(toolBar, BorderLayout.EAST);
        addButton = new JButton("Add");
        addButton.setEnabled(true);
        addButton.addActionListener(this);

        removeButton = new JButton("Remove");
        removeButton.setEnabled(true);
        removeButton.addActionListener(this);
        toolBar.add(addButton);
        toolBar.add(removeButton);
    }

    public void setValue(List<Feature> newValue) {
        currentValue = newValue;
    }

    public List<Feature> getValue() {
        return currentValue;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        Object src = event.getSource();
        if (src == addButton) {
            FeaturesParameter features = new FeaturesParameter();
            currentValue = features.getValue();
            jlist.setListData(currentValue.toArray(new Feature[0]));
        }

        if (src == removeButton) {

        }

    }

}
