package net.sf.mzmine.modules.datapointprocessing;

import java.util.EventObject;

public class DataPointProcessingEvent extends EventObject {
  
//  public enum DPTaskEvent { FINISHED, ERROR, STARTED };
  
  private DataPointProcessingTask task;
//  private DPTaskEvent type;
  
  public DataPointProcessingEvent(Object source/*, DPTaskEvent type*/) {
    super(source);
    if(!(this.source instanceof DataPointProcessingTask))
      throw new IllegalArgumentException("Event source is not an instance of DataPointProcessingTask");
      
    setTask((DataPointProcessingTask) source);
//    setType(type);
  }
  
  /*public DPTaskEvent getType() {
    return type;
  }

  public void setType(DPTaskEvent type) {
    this.type = type;
  }*/

  public DataPointProcessingTask getTask() {
    return task;
  }

  public void setTask(DataPointProcessingTask task) {
    this.task = task;
  }
}
