package com.lili.community.model.dto.comment;

import com.lili.community.common.PageRequest;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建请求
 *
 */
@Data
public class CommentQueryRequest extends PageRequest implements Serializable {
    /**
     * 要查询帖子的id
     */
   private long id;
    /**
     * 评论目标的类型 0-评论 1-回复
     */
    private Integer entityType;

    /**
     * 评论目标的id(帖子的id)
     */
    private Long entityId;

    /**
     * 对于有指向性的评论，如，针对某个人的评论进行评论
     */
    private Integer targetId;

    private static final long serialVersionUID = 1L;
}