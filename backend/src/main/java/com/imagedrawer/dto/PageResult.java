package com.imagedrawer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResult<T> {
    private List<T> records;
    private Long totalElements;
    private Integer totalPages;
    private Integer page;
    private Integer pageSize;
}
