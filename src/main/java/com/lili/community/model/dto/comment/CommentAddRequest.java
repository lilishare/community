package com.lili.community.model.dto.comment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 创建请求
 *
 */
@Data
public class CommentAddRequest implements Serializable {

    /**
     * 评论目标的类型 0-评论 1-回复
     */
    private Integer entityType;

    /**
     * 评论目标的id
     */
    private Long entityId;

    /**
     * 对于有指向性的评论，如，针对某个人的评论进行评论
     */
    private Long targetId;

    /**
     * 内容
     */
    private String content;




    private static final long serialVersionUID = 1L;
}