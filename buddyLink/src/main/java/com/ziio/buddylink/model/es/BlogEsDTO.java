package com.ziio.buddylink.model.es;

import com.google.gson.Gson;
import com.ziio.buddylink.model.domain.Blog;
import lombok.Data;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;

@Document(indexName = "buddy_blog")
@Data
public class BlogEsDTO {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * id
     */
    @Id
    private Long id;

    private Integer commentNum;

    private String content;

    private String coverImage;

    private Integer likeNum;

    private Integer starNum;

    private String tags;

    private String title;

    /**
     * 创建时间
     */
    @Field(index = false, store = true, type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN)
    private Date createTime;

    /**
     * 更新时间
     */
    @Field(index = false, store = true, type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN)
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isdelete;

    private static final long serialVersionUID = 1L;

}
