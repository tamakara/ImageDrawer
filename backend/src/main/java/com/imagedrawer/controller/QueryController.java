package com.imagedrawer.controller;

import com.imagedrawer.dto.ImageQueryParams;
import com.imagedrawer.dto.ImageResponse;
import com.imagedrawer.dto.PageResult;
import com.imagedrawer.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/query")
@RequiredArgsConstructor
public class QueryController {
    private final QueryService queryService;

    @GetMapping
    public ResponseEntity<PageResult<ImageResponse>> query(ImageQueryParams params) {
        Page<ImageResponse> responses = queryService.query(params);
        return ResponseEntity.ok().body(new PageResult<>(
                responses.getContent(),
                responses.getTotalElements(),
                responses.getTotalPages(),
                responses.getNumber() + 1,
                responses.getSize()
        ));
    }

}
