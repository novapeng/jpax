package org.novapeng.jpax;


/**
 *
 * Created with IntelliJ IDEA.
 * User: pengchangguo
 * Date: 15-10-23
 * Time: 上午12:25
 */
public class JPAException extends RuntimeException {

    public JPAException(String message) {
        super(message);
    }

    public JPAException(String message, Throwable e) {
        super(message, e);
    }
}
