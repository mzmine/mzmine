package net.sf.mzmine.parameters;

/**
 * Classes who handle (read/write) parameters
 * like a ParameterSet should be able to handle sensitive parameters
 * */
public interface ParameterContainer {
    /**
     * Specify whether sensitive parameters should be skipped during saveValuesToXML().
     */
    public void setSkipSensitiveParameters(boolean skipSensitiveParameters);
}
