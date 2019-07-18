package net.sf.mzmine.parameters.parametertypes.selectors;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;

import net.sf.mzmine.datamodel.Feature;

public class FeaturesComponent extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;
    public List<Feature> currentValue;
    private final JList<Feature> jlist;
    private final JButton addButton;
    private final JButton removeButton;

    public FeaturesComponent() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        jlist = new JList<Feature>((Feature[]) currentValue.toArray());
        add(jlist, BorderLayout.CENTER);

        addButton = new JButton("Add");
        addButton.setEnabled(false);
        addButton.addActionListener(this);
        add(addButton, BorderLayout.EAST);

        removeButton = new JButton("Remove");
        removeButton.setEnabled(false);
        removeButton.addActionListener(this);
        add(removeButton, BorderLayout.EAST);

    }

    void setValue(List<Feature> newValue) {
        currentValue = newValue;
    }

    List<Feature> getValue() {
        return currentValue;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

}
