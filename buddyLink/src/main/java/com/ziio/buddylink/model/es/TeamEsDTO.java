package com.ziio.buddylink.model.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Document(indexName = "buddy_team")  // The index name is "team", update if needed.
@Data
public class TeamEsDTO {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * Team ID
     */
    @Id
    @Field(type = FieldType.Keyword)
    private Long id;

    /**
     * Description
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String description;

    /**
     * Expire Time
     */
    @Field(type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN)
    private Date expireTime;

    /**
     * Is Delete
     */
    @Field(type = FieldType.Long)
    private Long isdelete;

    /**
     * Max Number
     */
    @Field(type = FieldType.Integer, index = false)
    private Integer maxNum;

    /**
     * Team Name
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String teamName;

    /**
     * Update Time
     */
    @Field(type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN)
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
