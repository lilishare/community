package com.lili.community.model.vo;

import com.lili.community.model.entity.Comment;
import com.lili.community.model.entity.User;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @projectName: community
 * @package: com.lili.community.model.vo
 * @className: CommentVo
 * @author: lili
 * @description: TODO
 * @date: 2024/2/18 21:11
 * @version: 1.0
 */
@Data
public class CommentVo {
    /**
     * id
     */
    private Long id;
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

    /**
     * 评论人的信息
     */
    private UserVO  commentator;
    /**
     * 回复人的信息
     */
    private UserVO targetUser;
    /**
     * 回复的信息列表
     */
    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
    private List<CommentVo> recoverList;


}
