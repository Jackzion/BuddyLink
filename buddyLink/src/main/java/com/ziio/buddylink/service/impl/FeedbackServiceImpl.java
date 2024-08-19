package com.ziio.buddylink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziio.buddylink.model.domain.Feedback;
import com.ziio.buddylink.service.FeedbackService;
import com.ziio.buddylink.mapper.FeedbackMapper;
import org.springframework.stereotype.Service;

/**
* @author Ziio
* @description 针对表【feedback(反馈表)】的数据库操作Service实现
* @createDate 2024-08-19 23:47:55
*/
@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback>
    implements FeedbackService{

}




