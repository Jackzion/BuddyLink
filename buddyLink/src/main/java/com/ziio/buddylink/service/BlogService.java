package com.ziio.buddylink.service;

import com.ziio.buddylink.model.domain.Blog;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ziio.buddylink.model.request.*;
import com.ziio.buddylink.model.vo.BlogVO;
import com.ziio.buddylink.model.vo.UserBlogVO;

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
     * 分页获取 all blogs
     * @param blogQueryRequest
     * @param request
     * @return
     */
    List<BlogVO> listBlogs(BlogQueryRequest blogQueryRequest, HttpServletRequest request);

    List<BlogVO> listBlogsFromEs(BlogQueryRequest blogQueryRequest, HttpServletRequest request);

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

    /**
     * 删除 blog
     * @param deleteRequest
     * @param request
     * @return
     */
    boolean deleteBlog(DeleteRequest deleteRequest, HttpServletRequest request);

    /**
     * 收藏 blog
     * @param starRequest
     * @param request
     * @return
     */
    boolean starBlog(StarRequest starRequest, HttpServletRequest request);

    /**
     * 点赞 blog
     * @param likeRequest
     * @param request
     * @return
     */
    boolean likeBlog(LikeRequest likeRequest, HttpServletRequest request);

    // 取消收藏
    boolean cancelStarBlog(StarRequest starRequest, HttpServletRequest request);

    /**
     * 取消点赞
     * @param likeRequest
     * @param request
     * @return
     */
    boolean cancelLikeBlog(LikeRequest likeRequest, HttpServletRequest request);

    /**
     * 查看 userId 创建 的 blogs , 可模糊查询 ： title
     * @param id
     * @param blogQueryRequest
     * @param request
     * @return
     */
    List<BlogVO> listUserBlogs(Long id, BlogQueryRequest blogQueryRequest, HttpServletRequest request);

    /**
     * 查询所有自己或其他用户的点赞、收藏、浏览过的博客
     * @param blogQueryRequest
     * @param request
     * @return
     */
    List<BlogVO> listInteractionBlogs(BlogQueryRequest blogQueryRequest, HttpServletRequest request);

    /**
     * 查询用户的创作、点赞、收藏、浏览过的博客 ， 并附带用户详细信息
     * @param blogQueryRequest
     * @param request
     * @return
     */
    UserBlogVO listUserInteractionBlogs(BlogQueryRequest blogQueryRequest, HttpServletRequest request);

    /**
     * 编写 blog
     * @param blogEditRequest
     * @param request
     * @return
     */
    long editBlog(BlogEditRequest blogEditRequest, HttpServletRequest request);
}
