package com.ziio.buddylink.service;

import com.ziio.buddylink.model.domain.Blog;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ziio.buddylink.model.request.*;
import com.ziio.buddylink.model.vo.BlogVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Ziio
* @description 针对表【blog(博客表)】的数据库操作Service
* @createDate 2024-08-18 17:11:33
*/
public interface BlogService extends IService<Blog> {

    /**
     * 添加博客文章
     * @param blogAddRequest
     * @param request
     * @return
     */
    Long addBlog(BlogAddRequest blogAddRequest, HttpServletRequest request);

    /**
     * 分页获取 blogs
     * @param blogQueryRequest
     * @param request
     * @return
     */
    List<BlogVO> listBlogs(BlogQueryRequest blogQueryRequest, HttpServletRequest request);

    /**
     * 按 id 搜索 blog
     * @param id
     * @param request
     * @return
     */
    BlogVO getBlogDetailById(Long id, HttpServletRequest request);

    // 判断是否收藏
    boolean isStarred(long blogId, long userId);

    // 判断是否点赞
    boolean isLiked(long blogId, long userId);

    boolean deleteBlog(DeleteRequest deleteRequest, HttpServletRequest request);

    boolean starBlog(StarRequest starRequest, HttpServletRequest request);

    boolean likeBlog(LikeRequest likeRequest, HttpServletRequest request);

    boolean cancelStarBlog(StarRequest starRequest, HttpServletRequest request);

    boolean cancelLikeBlog(LikeRequest likeRequest, HttpServletRequest request);
}
