package net.sf.mzmine.util.exceptions;

public class DecryptionException extends Exception {
    public DecryptionException(Throwable cause) {
        super("Could not Decrypt String!", cause);
    }
}
