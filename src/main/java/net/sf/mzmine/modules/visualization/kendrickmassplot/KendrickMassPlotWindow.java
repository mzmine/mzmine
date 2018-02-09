package net.sf.mzmine.modules.visualization.kendrickmassplot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import net.sf.mzmine.desktop.impl.WindowsMenu;

/**
 * Window for kendrick mass plots
 */
public class KendrickMassPlotWindow extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;
    private KendrickMassPlotToolBar toolBar;
    private JFreeChart chart;

    public KendrickMassPlotWindow(JFreeChart chart, int paintScaleNumber) {

        this.chart = chart;
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        // Add toolbar
        toolBar = new KendrickMassPlotToolBar(this);
        add(toolBar, BorderLayout.EAST);

        // Add the Windows menu
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new WindowsMenu());
        setJMenuBar(menuBar);

        pack();
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("TOGGLE_BLOCK_SIZE")) {

            XYPlot plot = chart.getXYPlot();
            KendrickMassPlotRenderer renderer = (KendrickMassPlotRenderer) plot
                    .getRenderer();
            int height = (int) renderer.getBlockHeightPixel();

            if (height == 1) {
                height++;
            }
            else if (height == 5) {
                height = 1;
            }
            else if (height < 5 && height != 1) {
                height++;
            }
            renderer.setBlockHeightPixel(height);
            renderer.setBlockWidthPixel(height);

        }

        if (command.equals("TOGGLE_BACK_COLOR")) {

            XYPlot plot = chart.getXYPlot();
            if (plot.getBackgroundPaint() == Color.WHITE) {
                plot.setBackgroundPaint(Color.BLACK);
            } else {
                plot.setBackgroundPaint(Color.WHITE);
            }

        }

        if (command.equals("TOGGLE_GRID")) {

            XYPlot plot = chart.getXYPlot();
            if (plot.getDomainGridlinePaint() == Color.BLACK) {
                plot.setDomainGridlinePaint(Color.WHITE);
                plot.setRangeGridlinePaint(Color.WHITE);
            } else {
                plot.setDomainGridlinePaint(Color.BLACK);
                plot.setRangeGridlinePaint(Color.BLACK);
            }

        }
        
    }

}
