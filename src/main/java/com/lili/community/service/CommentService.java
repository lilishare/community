package com.lili.community.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lili.community.model.dto.comment.CommentQueryRequest;
import com.lili.community.model.entity.Comment;
import com.lili.community.model.vo.CommentVo;

import javax.servlet.http.HttpServletRequest;

/**
* @author asus
* @description 针对表【comment(帖子)】的数据库操作Service
* @createDate 2024-02-18 20:59:39
*/
public interface CommentService extends IService<Comment> {
    public Page<CommentVo> getCommentVOPage(CommentQueryRequest commentQueryRequest, HttpServletRequest request) ;
    QueryWrapper<Comment> getQueryWrapper(CommentQueryRequest commentQueryRequest);

    int insertComment(Comment comment);
}
