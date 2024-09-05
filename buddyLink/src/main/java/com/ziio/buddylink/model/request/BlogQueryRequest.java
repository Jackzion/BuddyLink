package com.ziio.buddylink.model.request;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class BlogQueryRequest extends PageRequest implements Serializable {

    // searchText for title , tags or content
    private String searchText;

    private Long userId;

    Integer type; // 查询类型（ -1 查自己创建的, 0 查自己收藏的，1 查自己点赞的，2 查自己的阅读过的文章 3 查别人收藏的，4 查别人点赞的 5 查别人的阅读过的文章）
}
