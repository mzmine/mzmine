package net.sf.mzmine.modules.datapointprocessing;

import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingController.ControllerStatus;

/**
 * Listens to changes in the status of a DataPointProcessingController.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
@FunctionalInterface
public interface DPPControllerStatusListener {
  public void statusChanged(DataPointProcessingController controller, ControllerStatus newStatus, ControllerStatus oldStatus);
}
