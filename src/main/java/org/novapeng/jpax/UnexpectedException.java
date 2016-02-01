package org.novapeng.jpax;

/**
 * Created with IntelliJ IDEA.
 * User: pengchangguo
 * Date: 15-10-23
 * Time: P.M 12:22
 */
public class UnexpectedException extends RuntimeException {

    public UnexpectedException(String message) {
        super(message);
    }

    public UnexpectedException(Exception ex) {
        super(ex);
    }

    public UnexpectedException(String message, Exception e) {
        super(message, e);
    }
}
