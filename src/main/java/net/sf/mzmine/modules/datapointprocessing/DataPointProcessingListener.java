package net.sf.mzmine.modules.datapointprocessing;

import java.util.EventListener;

/**
 * Interface that must be supported for classes that listen to DataPointProcessingEvents.
 * 
 * @author SteffenHeu <steffen.heuckeroth@gmx.de>
 *
 */
public interface DataPointProcessingListener extends EventListener {

  public void handle(DataPointProcessingEvent event);
}
