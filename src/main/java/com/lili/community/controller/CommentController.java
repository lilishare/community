package com.lili.community.controller;

import com.lili.community.common.BaseResponse;
import com.lili.community.common.ErrorCode;
import com.lili.community.common.ResultUtils;
import com.lili.community.exception.BusinessException;
import com.lili.community.manager.SensitiveFilter;
import com.lili.community.model.dto.comment.CommentAddRequest;
import com.lili.community.model.entity.Comment;
import com.lili.community.model.entity.Post;
import com.lili.community.model.entity.User;
import com.lili.community.model.enums.CommentTypeEnum;
import com.lili.community.service.CommentService;
import com.lili.community.service.PostService;
import com.lili.community.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @projectName: community
 * @package: com.lili.community.controller
 * @className: CommentController
 * @author: lili
 * @description: 评论请求处理
 * @date: 2024/2/19 11:46
 * @version: 1.0
 */
@Controller
@Slf4j
@RequestMapping("/comment")
public class CommentController {
    @Resource
    private CommentService commentService;
    @Resource
    private SensitiveFilter sensitiveFilter;
    @Resource
    private UserService userService;
    @Resource
    private PostService postService;

    /**
     * 添加评论
     *
     * @param
     */
    @PostMapping("/add/{postId}")
    public String addComment(@PathVariable Long postId, CommentAddRequest commentAddRequest, HttpServletRequest request) {
        if (commentAddRequest == null || postId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String content = commentAddRequest.getContent();
        // 过滤敏感词
        content = sensitiveFilter.filter(content);
        content = HtmlUtils.htmlEscape(content);
        commentAddRequest.setContent(content);
        Comment comment = new Comment();
        BeanUtils.copyProperties(commentAddRequest,comment);
        User loginUser = userService.getLoginUser(request);
        comment.setUserId(loginUser.getId());
        int save = commentService.insertComment(comment);
        return "redirect:/post/get/vo?id=" + postId;

//        return ResultUtils.success(save,"添加评论成功");
    }

}
