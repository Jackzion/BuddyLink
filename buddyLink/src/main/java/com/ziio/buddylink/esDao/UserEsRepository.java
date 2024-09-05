package com.ziio.buddylink.esDao;

import com.ziio.buddylink.model.es.BlogEsDTO;
import com.ziio.buddylink.model.es.UserEsDTO;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface UserEsRepository extends ElasticsearchRepository<UserEsDTO, Long> {
}
