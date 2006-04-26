/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.basepeak;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

/**
 * 
 */
class BasePeakToolBar extends JToolBar {

    private JButton zoomOutButton, showSpectraButton, annotationsButton;

    static final Icon zoomOutIcon = new ImageIcon("zoomouticon.png");
    static final Icon showSpectrumIcon = new ImageIcon("spectrumicon.png");
    static final Icon annotationsIcon = new ImageIcon("annotationsicon.png");
    
    BasePeakToolBar(BasePeakVisualizer masterFrame) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setFocusable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        zoomOutButton = new JButton(zoomOutIcon);
        zoomOutButton.setEnabled(false);
        zoomOutButton.setActionCommand("ZOOM_OUT");
        zoomOutButton.setToolTipText("Zoom out");
        zoomOutButton.addActionListener(masterFrame);

        annotationsButton = new JButton(annotationsIcon);
        annotationsButton.setActionCommand("SHOW_ANNOTATIONS");
        annotationsButton.setToolTipText("Toggle displaying of peak values");
        annotationsButton.addActionListener(masterFrame);

        showSpectraButton = new JButton(showSpectrumIcon);
        showSpectraButton.setEnabled(false);
        showSpectraButton.setActionCommand("SHOW_SPECTRUM");
        showSpectraButton.setToolTipText("Show spectrum of selected scan");
        showSpectraButton.addActionListener(masterFrame);
        
        add(zoomOutButton);
        addSeparator();
        add(annotationsButton);
        addSeparator();
        add(showSpectraButton);

    }

    void setZoomOutButton(boolean enabled) {
        zoomOutButton.setEnabled(enabled);
    }

    void setSpectraButton(boolean enabled) {
        showSpectraButton.setEnabled(enabled);
    }

}
