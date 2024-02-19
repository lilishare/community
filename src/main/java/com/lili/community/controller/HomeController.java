package com.lili.community.controller;

/**
 * @projectName: community
 * @package: com.lili.community.controller
 * @className: HomeController
 * @author: lili
 * @description: TODO
 * @date: 2024/2/16 11:07
 * @version: 1.0
 */

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lili.community.common.ErrorCode;
import com.lili.community.common.ResultUtils;
import com.lili.community.exception.ThrowUtils;
import com.lili.community.model.dto.post.PostQueryRequest;
import com.lili.community.model.entity.Post;
import com.lili.community.model.vo.PostVO;
import com.lili.community.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Controller
@Slf4j
public class HomeController {
    @Resource
    private PostService postService;

    @RequestMapping("/home")
    public String getHomePage(Model model,PostQueryRequest postQueryRequest, HttpServletRequest request){
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        postQueryRequest.setSortField("updateTime");
        postQueryRequest.setSortOrder("desc");
        Page<Post> postPage = postService.page(new Page<>(current, size),
                postService.getQueryWrapper(postQueryRequest));
        Page<PostVO> postVOPage = postService.getPostVOPage(postPage, request);
        model.addAttribute("page",postVOPage);
        System.out.println(postPage.getRecords());
        return "index";
    }
}
