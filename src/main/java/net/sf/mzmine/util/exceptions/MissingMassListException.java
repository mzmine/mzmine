package net.sf.mzmine.util.exceptions;

public class MissingMassListException extends Exception {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public MissingMassListException(String massListName) {
    this("", massListName);
  }

  public MissingMassListException(String message, String massListName) {
    super("Missing mass list: "
        + (massListName == null || massListName.length() == 0 ? "no mass list available"
            : "no mass list named " + massListName)
        + ". " + message);
  }

}
