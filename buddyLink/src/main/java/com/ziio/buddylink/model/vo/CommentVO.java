package com.ziio.buddylink.model.vo;

import lombok.Data;

/**
 * blog评论视图数据
 */
@Data
public class CommentVO {
    private Long id;

    private Long userId;

    private Long blogId;

    private String text;

    private String username;

    private String userAvatarUrl;

    private boolean isMyComment;
}
