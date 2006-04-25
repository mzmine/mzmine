/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.spectra;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

/**
 * 
 */
class SpectrumToolBar extends JToolBar {

    private JButton zoomOutButton;

    SpectrumToolBar(SpectrumVisualizer masterFrame) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setFocusable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        zoomOutButton = new JButton(new ImageIcon("zoomouticon.png"));
        zoomOutButton.setEnabled(false);
        zoomOutButton.setActionCommand("ZOOM_OUT");
        zoomOutButton.addActionListener(masterFrame);

        add(zoomOutButton);

    }

    void setZoomOutButtonEnabled(boolean enabled) {
        zoomOutButton.setEnabled(enabled);
    }

}
