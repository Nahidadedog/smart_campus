package com.huadi.campus.selection;

/** 名额预占层不可用，抛出后由 SelectionService 降级到数据库实现。 */
public class StockUnavailableException extends RuntimeException {

    public StockUnavailableException(Throwable cause) {
        super(cause);
    }
}
