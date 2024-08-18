package com.ziio.buddylink.controller;

import com.ziio.buddylink.common.BaseResponse;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.common.ResultUtils;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.model.request.*;
import com.ziio.buddylink.model.vo.BlogVO;
import com.ziio.buddylink.service.BlogService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/blog")
public class BlogController {
    @Resource
    private BlogService blogService;

    @PostMapping("/add")
    public BaseResponse<Long> addBlog(@RequestBody BlogAddRequest blogAddRequest, HttpServletRequest request) {
        if (blogAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long b = blogService.addBlog(blogAddRequest, request);
        return ResultUtils.success(b);
    }

    @PostMapping("/recommend")
    public BaseResponse<List<BlogVO>> listBlogs(@RequestBody BlogQueryRequest blogQueryRequest, HttpServletRequest request) {
        if (blogQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (blogQueryRequest.getPageNum() <= 0 || blogQueryRequest.getPageSize() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<BlogVO> blogVOList = blogService.listBlogs(blogQueryRequest, request);
        return ResultUtils.success(blogVOList);
    }

    @GetMapping("/get/{id}")
    public BaseResponse<BlogVO> getBlogById(@PathVariable("id") Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        BlogVO blogVO = blogService.getBlogDetailById(id, request);
        return ResultUtils.success(blogVO);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteBlog(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = blogService.deleteBlog(deleteRequest, request);
        return ResultUtils.success(b);
    }

    @PostMapping("/star")
    public BaseResponse<Boolean> starBlog(@RequestBody StarRequest starRequest, HttpServletRequest request) {
        if (starRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = blogService.starBlog(starRequest, request);
        return ResultUtils.success(b);
    }

    @PostMapping("/like")
    public BaseResponse<Boolean> likeBlog(@RequestBody LikeRequest likeRequest, HttpServletRequest request) {
        if (likeRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = blogService.likeBlog(likeRequest, request);
        return ResultUtils.success(b);
    }

    @PostMapping("/star/cancel")
    public BaseResponse<Boolean> cancelStarBlog(@RequestBody StarRequest starRequest, HttpServletRequest request) {
        if (starRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = blogService.cancelStarBlog(starRequest, request);
        return ResultUtils.success(b);
    }

    @PostMapping("/like/cancel")
    public BaseResponse<Boolean> cancelLikeBlog(@RequestBody LikeRequest likeRequest, HttpServletRequest request) {
        if (likeRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = blogService.cancelLikeBlog(likeRequest, request);
        return ResultUtils.success(b);
    }
}
