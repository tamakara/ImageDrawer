package com.imagedrawer.service;

import com.imagedrawer.dto.ImageQueryParams;
import com.imagedrawer.dto.ImageResponse;
import com.imagedrawer.entity.Image;
import com.imagedrawer.repository.ImageQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueryService {
    private final ImageQueryRepository imageQueryRepository;

    @Transactional(readOnly = true)
    public Page<ImageResponse> query(ImageQueryParams params) {
        return imageQueryRepository.query(
                params.getTags(),
                params.getSort(),
                params.getRating(),
                params.getPage(),
                params.getPageSize(),
                params.getSeed()
        );
    }
}
