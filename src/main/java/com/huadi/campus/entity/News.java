package com.huadi.campus.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("news")
public class News {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;
    private String coverImage;
    private String category;

    /** 作者 sys_user.id */
    private Long authorId;

    /** 浏览量。修改通过 NewsMapper.incrementViewCount 原子更新 */
    private Integer viewCount = 0;

    private LocalDateTime publishTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
