package oracle.jdbc.xa;

import javax.transaction.xa.XAException;

public class OracleXAException extends XAException {
    
    private int oracleError;

    public OracleXAException(String msg, int oracleError) {
        super(msg);
        this.oracleError = oracleError;
    }

    public OracleXAException(int oracleError) {
        this.oracleError = oracleError;
    }

    public int getOracleError() {
        return oracleError;
    }

}
