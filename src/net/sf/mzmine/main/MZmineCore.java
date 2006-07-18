/**
 * 
 */
package net.sf.mzmine.main;

import net.sf.mzmine.io.IOController;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;

/**
 * This interface represents MZmine core modules - I/O, task controller and GUI.
 */
public interface MZmineCore {

    /**
     * Returns a reference to local IO controller.
     * 
     * @return IO controller reference
     */
    public IOController getIOController();

    /**
     * Returns a reference to local task controller.
     * 
     * @return TaskController reference
     */
    public TaskController getTaskController();

    /**
     * Returns a reference to Desktop. May return null on MZmine nodes with no
     * GUI.
     * 
     * @return Desktop reference or null
     */
    public Desktop getDesktop();

}
