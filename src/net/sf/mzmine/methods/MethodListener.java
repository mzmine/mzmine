package net.sf.mzmine.methods;

public interface MethodListener {

	public enum MethodReturnStatus { ERROR, CANCELED, FINISHED };
	
	public void methodFinished(MethodReturnStatus status);
}
