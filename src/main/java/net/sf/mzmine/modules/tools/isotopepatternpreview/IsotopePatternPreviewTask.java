package net.sf.mzmine.modules.tools.isotopepatternpreview;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.impl.ExtendedIsotopePattern;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class IsotopePatternPreviewTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  
  private String message;
  
  private boolean parametersChanged;
  private double minIntensity, mergeWidth;
  private int charge;
  private PolarityType polarity;
  private String formula;
  
  ExtendedIsotopePattern pattern;
  
  public IsotopePatternPreviewTask() {
    message = "Wating for parameters";
    parametersChanged = false;
    formula = "";
    minIntensity = 0.d;
    mergeWidth = 0.d;
    charge = 0;
    pattern = null;
  }
  
  public void initialise(String formula, double minIntensity, double mergeWidth, int charge, PolarityType polarity) {
    message = "Wating for parameters";
    parametersChanged = false;
    this.minIntensity = minIntensity;
    this.mergeWidth = mergeWidth;
    this.charge = charge;
    this.formula = formula;
    this.polarity = polarity;
    parametersChanged = true;
    pattern = null;
  }
  
  public IsotopePatternPreviewTask(String formula, double minIntensity, double mergeWidth, int charge, PolarityType polarity) {
    message = "Wating for parameters";
    parametersChanged = false;
    this.minIntensity = minIntensity;
    this.mergeWidth = mergeWidth;
    this.charge = charge;
    this.formula = formula;
    this.polarity = polarity;
    parametersChanged = true;
    pattern = null;
  }
  
  public void run() {
    ExtendedIsotopePattern pattern;
    while(getStatus() != TaskStatus.FINISHED) {
      try {
        TimeUnit.MILLISECONDS.sleep(50);
      } catch (InterruptedException e) {
        e.printStackTrace();
        return;
      }
      
      if(parametersChanged) {
        pattern = (ExtendedIsotopePattern) IsotopePatternCalculator.calculateIsotopePattern(formula, minIntensity, mergeWidth, charge, polarity, true);
        parametersChanged = false;
        logger.info("Pattern " + pattern.getDescription() + " calculated.");
      }
    }
    
    updateWindow();
    
    setStatus(TaskStatus.FINISHED);
  }
  
  public void updateWindow() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        
      }
    });
  }
  
  public ExtendedIsotopePattern getPattern() {
    return pattern;
  }

  @Override
  public String getTaskDescription() {
    return message;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }
}
