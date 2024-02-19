package com.lili.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lili.community.common.ErrorCode;
import com.lili.community.constant.CommonConstant;
import com.lili.community.exception.BusinessException;
import com.lili.community.mapper.CommentMapper;
import com.lili.community.mapper.PostMapper;
import com.lili.community.model.dto.comment.CommentQueryRequest;
import com.lili.community.model.entity.Comment;
import com.lili.community.model.entity.Post;
import com.lili.community.model.entity.User;
import com.lili.community.model.enums.CommentTypeEnum;
import com.lili.community.model.vo.CommentVo;
import com.lili.community.model.vo.UserVO;
import com.lili.community.service.CommentService;
import com.lili.community.service.PostService;
import com.lili.community.service.UserService;
import com.lili.community.utils.SqlUtils;
import javafx.geometry.Pos;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author asus
 * @description 针对表【comment(帖子)】的数据库操作Service实现
 * @createDate 2024-02-18 20:59:39
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
        implements CommentService {
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private UserService userService;
    @Resource
    private PostMapper postMapper;

    @Override
    public Page<CommentVo> getCommentVOPage(CommentQueryRequest commentQueryRequest, HttpServletRequest request) {
        if (commentQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = commentQueryRequest.getCurrent();
        long size = commentQueryRequest.getPageSize();
        long postId = commentQueryRequest.getId();
        commentQueryRequest.setEntityType(CommentTypeEnum.POST_COMMENT.getValue());
        commentQueryRequest.setEntityId(commentQueryRequest.getId());
        Page<Comment> commentPage = this.page(new Page<>(current, size),
                this.getQueryWrapper(commentQueryRequest));

        List<Comment> comments = commentPage.getRecords();
        List<CommentVo> commentVoList = new ArrayList<>();
        for (Comment comment : comments) {
            CommentVo commentVo = getCommentVo(comment);
            // 获取当前评论的回复列表
            QueryWrapper<Comment> recoverQuerywrapper = new QueryWrapper<>();
            recoverQuerywrapper.eq("entityType", CommentTypeEnum.RECOVER.getValue());
            recoverQuerywrapper.eq(comment.getId() != null, "entityId", comment.getId());
            recoverQuerywrapper.eq(comment.getTargetId() != null, "targetId", comment.getUserId());
            List<Comment> recoverList = commentMapper.selectList(recoverQuerywrapper);
            List<CommentVo> recoverVoList = new ArrayList<>();
            for (Comment recover : recoverList) {
                CommentVo recoverVo = getCommentVo(recover);
                recoverVoList.add(recoverVo);
            }
            commentVo.setRecoverList(recoverVoList);
            commentVoList.add(commentVo);
        }
        Page<CommentVo> commentVoPage = new Page<>();
        BeanUtils.copyProperties(commentPage, commentVoPage);
        commentVoPage.setRecords(commentVoList);
        return commentVoPage;

    }

    public CommentVo getCommentVo(Comment comment) {
        // 封装评论信息
        CommentVo commentVo = new CommentVo();
        BeanUtils.copyProperties(comment, commentVo);
        // 获取当前评论的用户
        Long commentUserId = comment.getUserId();
        User commentUser = userService.getById(commentUserId);
        UserVO commentUserVo = new UserVO();
        BeanUtils.copyProperties(commentUser, commentUserVo);
        commentVo.setCommentator(commentUserVo);
        // 获取被评论对象的信息
        if (comment.getTargetId() != null) {
            User targetUser = userService.getById(comment.getTargetId());
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(targetUser, userVO);
            commentVo.setTargetUser(userVO);
        }
        return commentVo;

    }

    /**
     * 获取查询包装类
     *
     * @param commentQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Comment> getQueryWrapper(CommentQueryRequest commentQueryRequest) {
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        if (commentQueryRequest == null) {
            return queryWrapper;
        }
        Long postId = commentQueryRequest.getId();
        Integer entityType = commentQueryRequest.getEntityType();
        Long entityId = commentQueryRequest.getEntityId();
        Integer targetId = commentQueryRequest.getTargetId();
        String sortField = commentQueryRequest.getSortField();
        String sortOrder = commentQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(entityType != null, "entityType", entityType);
        queryWrapper.eq(entityId != null, "entityId", postId);
        queryWrapper.eq(targetId != null, "targetId", targetId);


        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * @param comment
     * @return result 插入数量
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public int insertComment(Comment comment) {
        if (comment == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = commentMapper.insert(comment);
        if (comment.getEntityType() == CommentTypeEnum.POST_COMMENT.getValue()) {
            Post post = postMapper.selectById(comment.getEntityId());
            post.setCommentCount(post.getCommentCount() + result);
            QueryWrapper<Post> postQueryWrapper = new QueryWrapper<>();
            postQueryWrapper.eq("id", comment.getEntityId());
            int b = postMapper.update(post, postQueryWrapper);
            if (b < 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新帖子评论数量失败");
            }
        }
        return result;
    }
}




