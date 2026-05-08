package com.bcommerce.marketing;

import com.bcommerce.mapper.SeckillActivityMapper;
import com.bcommerce.web.dto.SeckillActivityResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeckillCatalogService {

    private final SeckillActivityMapper activityMapper;

    public List<SeckillActivityResponse> listOnline() {
        return activityMapper.listOnline().stream().map(SeckillActivityResponse::from).toList();
    }
}
