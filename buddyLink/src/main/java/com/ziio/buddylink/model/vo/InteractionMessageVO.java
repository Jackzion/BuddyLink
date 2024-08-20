package com.ziio.buddylink.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InteractionMessageVO {
    private long likeMessageNum;

    private long starMessageNum;

    private long followMessageNum;
}

