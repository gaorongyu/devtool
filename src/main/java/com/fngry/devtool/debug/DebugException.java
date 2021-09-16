package com.fngry.devtool.debug;

/**
 * @author gaorongyu
 */
public class DebugException extends RuntimeException {

    public DebugException(Exception exception) {
        super(exception);
    }

    public DebugException(String message) {
        super(message);
    }

}
