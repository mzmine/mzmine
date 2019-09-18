package net.sf.mzmine.util.exceptions;

public class EncryptionException extends Exception {
    public EncryptionException(Throwable cause) {
        super("Could not Encrypt String!", cause);
    }
}
