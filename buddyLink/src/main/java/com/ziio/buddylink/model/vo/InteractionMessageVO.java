package com.ziio.buddylink.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class InteractionMessageVO implements Serializable {
    private long likeMessageNum;

    private long starMessageNum;

    private long followMessageNum;
}

