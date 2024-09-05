package com.ziio.buddylink.model.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Document(indexName = "buddy_user")  // The index name is "user", update if needed.
@Data
public class UserEsDTO {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * User ID
     */
    @Id
    @Field(type = FieldType.Keyword)
    private Long id;

    /**
     * Avatar URL
     */
    @Field(type = FieldType.Keyword, index = false)
    private String avatarUrl;

    /**
     * Is Delete
     */
    @Field(type = FieldType.Long)
    private Long isdelete;

    /**
     * Profile
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String profile;

    /**
     * Tags
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String tags;

    /**
     * Update Time
     */
    @Field(type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN)
    private Date updateTime;

    /**
     * User Name
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String userName;

    private static final long serialVersionUID = 1L;
}
