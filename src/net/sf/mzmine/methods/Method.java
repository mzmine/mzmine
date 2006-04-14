/**
 * 
 */
package net.sf.mzmine.methods;

import net.sf.mzmine.io.MZmineFile;

/**
 *
 */
public interface Method {

    
    public String getMethodDescription();
    
    public MethodParameters getParameters();
    
    public void runMethod(MZmineFile[] files);
    
}
