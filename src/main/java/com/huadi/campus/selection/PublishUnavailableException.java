package com.huadi.campus.selection;

/** 指令没有成功交给下游（Broker 不可达、线程池队列满等），调用方应改为同步落库。 */
public class PublishUnavailableException extends RuntimeException {

    public PublishUnavailableException(Throwable cause) {
        super(cause);
    }
}
