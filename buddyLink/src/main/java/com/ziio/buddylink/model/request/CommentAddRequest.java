package com.ziio.buddylink.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommentAddRequest implements Serializable {

    private String text;

    private Long blogId;

}
