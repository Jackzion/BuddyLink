package com.ziio.buddylink.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户创作、点赞、收藏和浏览过的博客 VO
 */
@Data
public class UserBlogVO implements Serializable {

    // 用户信息
    private BlogUserVO blogUserVO;

    // 搜索出来的 blogs
    private List<BlogVO> blogVOList;

}
