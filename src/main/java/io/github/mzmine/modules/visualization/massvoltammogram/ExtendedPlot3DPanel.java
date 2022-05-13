package io.github.mzmine.modules.visualization.massvoltammogram;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.math.plot.Plot3DPanel;

public class ExtendedPlot3DPanel extends Plot3DPanel {

  private final ExtendedPlotToolBar extendedPlotToolBar;

  //Data for later export.
  private List<double[][]> rawScans;
  private List<double[][]> rawScnasInMzRange;

  public ExtendedPlot3DPanel() {
    removePlotToolBar();
    extendedPlotToolBar = new ExtendedPlotToolBar(this);
  }

  //Extending the png export function to work with the extended plot toolbar.
  @Override
  public void toGraphicFile(File file) throws IOException {
    super.toGraphicFile(file);

    //Extracting the buffered frame as an image.
     Image image = createImage(getWidth(), getHeight());
    paint(image.getGraphics());
    image = new ImageIcon(image).getImage();

    BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
        BufferedImage.TYPE_INT_RGB);
    Graphics g = bufferedImage.createGraphics();
    g.drawImage(image, 0, 0, Color.WHITE, null);
    g.dispose();

    //saving the buffered image to a png file.
    try {
      ImageIO.write(bufferedImage, "PNG", file);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Method to get the plots ExtendedToolBar to integrate it into the MassvoltammogramTasks toolbar.
   * @return The plots toolbar.
   */
  public ExtendedPlotToolBar getExtendedPlotToolBar() {
    return extendedPlotToolBar;
  }

  //Methods to add the data to the plot for later export.
  public void addRawScans(List<double[][]> rawScans){
    this.rawScans = rawScans;
  }
  public void addRawScansInMzRange(List<double[][]> rawScansInMzRange){
    this.rawScnasInMzRange = rawScansInMzRange;
  }

  //Methods to get the data to export.
  public List<double[][]> getRawScans(){
    return  rawScans;
  }
  public List<double[][]> getRawScansInMzRange() {
    return rawScnasInMzRange;
  }
}
