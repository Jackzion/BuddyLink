package com.ziio.buddylink.esDao;

import com.ziio.buddylink.model.es.BlogEsDTO;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BlogEsRepository extends ElasticsearchRepository<BlogEsDTO, Long> {
}
