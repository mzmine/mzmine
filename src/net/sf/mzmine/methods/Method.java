/**
 * 
 */
package net.sf.mzmine.methods;

import net.sf.mzmine.io.MZmineProject;

/**
 *
 */
public interface Method {

    
    public String getMethodDescription();
    
    public MethodParameters getParameters();
    
    public void runMethod(MZmineProject project);
    
}
