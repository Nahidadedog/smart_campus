package com.huadi.campus.vo;

import lombok.Data;

/** GROUP BY 聚合的一行：分组字段值 + 数量 */
@Data
public class GroupCount {
    private String name;
    private Long total;
}
