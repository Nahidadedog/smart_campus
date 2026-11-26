package com.huadi.campus.selection;

/**
 * 落库通道。实现类按 app.selection.persist 条件装配。
 * <p>
 * 契约：
 * <ul>
 *   <li>正常返回 = 指令已处理完毕（同步）或已交给下游（异步）</li>
 *   <li>抛出 PublishUnavailableException = 没能交给下游，调用方必须自己同步落库</li>
 *   <li>落库本身的业务失败（含回补）由 SelectionPersistHandler 处理，不再向这里抛出</li>
 * </ul>
 */
public interface SelectionPersistPublisher {

    boolean isAsync();

    void publish(SelectionCommand command);
}
