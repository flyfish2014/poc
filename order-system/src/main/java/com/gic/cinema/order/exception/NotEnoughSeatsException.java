package com.gic.cinema.order.exception;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * NotEnoughSeatsException
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class NotEnoughSeatsException extends RuntimeException {
    /*
     * Error Code
     */
    private String code;
    public NotEnoughSeatsException(String msg) {
        super(msg);
    }
    public NotEnoughSeatsException(String code,String msg) {
        super(msg);
        this.code=code;
    }

    public NotEnoughSeatsException(String msg,Throwable throwable) {
        super(msg,throwable);
    }
    public NotEnoughSeatsException(String code,String msg,Throwable throwable) {
        super(msg,throwable);
        this.code=code;
    }
}
