package com.ziio.buddylink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ziio.buddylink.esDao.BlogEsRepository;
import com.ziio.buddylink.esDao.UserEsRepository;
import com.ziio.buddylink.model.domain.Blog;
import com.ziio.buddylink.model.es.BlogEsDTO;
import com.ziio.buddylink.model.es.UserEsDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class BlogServiceTest {

    @Autowired
    BlogService blogService;


    @Test
    public void test() {
        String title = "";
        Long userId = 4L;
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.eq("userId", userId);
        List<Blog> blogList = blogService.page(new Page<>(1, 3), queryWrapper).getRecords();
        log.info("blogList:{}", blogList);
    }

    @Resource
    private BlogEsRepository blogEsRepository;
    @Resource
    private UserEsRepository userEsRepository;

    @Test
    void testSelect() {
        System.out.println(blogEsRepository.count());
        org.springframework.data.domain.Page<BlogEsDTO> blogPage = blogEsRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Order.desc("updateTime"))));//分页查询
        List<BlogEsDTO> postList = blogPage.getContent();
        System.out.println(postList);
    }

    @Test
    void testSelectUser() {
        System.out.println(userEsRepository.count());
        org.springframework.data.domain.Page<UserEsDTO> userEsDTOPage = userEsRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Order.desc("updateTime"))));//分页查询
        List<UserEsDTO> postList = userEsDTOPage.getContent();
        System.out.println(postList);
    }

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;


    @Test
    void testSelectByAny() {
        String searchText = "java";

        // 构造 query
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.filter(QueryBuilders.termQuery("isdelete", 0));
        boolQueryBuilder.should(QueryBuilders.matchQuery("profile", searchText));
        boolQueryBuilder.should(QueryBuilders.matchQuery("tags", searchText));
        boolQueryBuilder.should(QueryBuilders.matchQuery("userName", searchText));
        boolQueryBuilder.minimumShouldMatch(1);

        // 分页
        PageRequest pageRequest = PageRequest.of(0, 5);
        // 排序器
        SortBuilder<?> sortBuilder = SortBuilders.fieldSort("updateTime").order(SortOrder.DESC);
        // 构造查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(pageRequest)
                .withSorts(sortBuilder).build();
        // 查找并拆分
        SearchHits<UserEsDTO> searchHits = elasticsearchRestTemplate.search(searchQuery, UserEsDTO.class);
        List<SearchHit<UserEsDTO>> searchHits1 = searchHits.getSearchHits();
        List<Long> esIdList = searchHits1.stream().map(searchHit -> searchHit.getContent().getId()).collect(Collectors.toList());
        System.out.println(esIdList);
        // todo：查出结果后，根据id得到数据库全数据，数据同步
        // 数据同步 ： 将 postIdList（elastic） 上不存在于 postList（mysql） 的数据删除
//        List<Post> postList = baseMapper.selectBatchIds(postIdList);
//        if (postList != null) {
//            Map<Long, List<Post>> idPostMap = postList.stream().collect(Collectors.groupingBy(Post::getId));
//            postIdList.forEach(postId -> {
//                if (idPostMap.containsKey(postId)) {
//                    resourceList.add(idPostMap.get(postId).get(0));
//                } else {
//                    // 从 es 清空 db 已物理删除的数据
//                    String delete = elasticsearchRestTemplate.delete(String.valueOf(postId), PostEsDTO.class);
//                    log.info("delete post {}", delete);
//                }
//            });
//        }
//        page.setRecords(resourceList);
    }

}