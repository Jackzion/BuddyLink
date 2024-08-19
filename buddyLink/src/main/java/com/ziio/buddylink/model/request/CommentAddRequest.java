package com.ziio.buddylink.model.request;

import lombok.Data;

@Data
public class CommentAddRequest {
    private String text;

    private Long blogId;

}
