package javastory.server.login;

/**
 *
 * @author Tosho
 */
public enum AuthReplyCode {
    SUCCESS(0),
    TEMPORARY_BAN(2),
    DELETED_OR_BLOCKED(3),
    WRONG_PASSWORD(4),
    NOT_REGISTERED(5),
    ALREADY_LOGGED_IN(7),
    TOO_MANY_CONNECTIONS(10),
    FIRST_RUN(23);
    
    private int value;
    
    private AuthReplyCode(int value) {
        this.value = value;
    }
    
    public int asNumber() {
        return this.value;
    }
}
