/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.threed;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import visad.DisplayImpl;
import visad.ScalarMap;
import visad.util.ColorMapWidget;
import visad.util.GMCWidget;


/**
 *
 */
class ThreeDPropertiesDialog extends JDialog implements ActionListener {

    private static final String TITLE = "3D visualizer properties"; 
    
    private GMCWidget gmcWidget;
    private ColorMapWidget colorWidget;

    public ThreeDPropertiesDialog(DisplayImpl display) {
        
        super(MainWindow.getInstance(), TITLE, true);

        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        
        try {
            
            ScalarMap colorMap = (ScalarMap) display.getMapVector().get(4);
            colorWidget = new ColorMapWidget(colorMap);
            JLabel colorLabel = new JLabel("Color mapping", SwingConstants.CENTER);
            add(colorLabel);
            add(colorWidget);
         
            JLabel gmcLabel = new JLabel("Graphics mode control", SwingConstants.CENTER);

            gmcWidget = new GMCWidget(display.getGraphicsModeControl());
            add(gmcLabel);
            add(gmcWidget);
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        add(new JSeparator());
        
        JButton okButton = new JButton("OK");
        okButton.addActionListener(this);
        add(okButton);
        
        pack();
        setLocationRelativeTo(MainWindow.getInstance());

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
        dispose();
    }

}
