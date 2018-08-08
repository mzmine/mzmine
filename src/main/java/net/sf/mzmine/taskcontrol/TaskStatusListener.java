package net.sf.mzmine.taskcontrol;

@FunctionalInterface
public interface TaskStatusListener {
  public void taskStatusChanged(Task task, TaskStatus newStatus, TaskStatus oldStatus);
}
