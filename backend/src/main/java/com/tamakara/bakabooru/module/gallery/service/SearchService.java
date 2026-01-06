package com.tamakara.bakabooru.module.gallery.service;

import com.tamakara.bakabooru.module.gallery.dto.ImageDto;
import com.tamakara.bakabooru.module.gallery.entity.Image;
import com.tamakara.bakabooru.module.gallery.mapper.ImageMapper;
import com.tamakara.bakabooru.module.gallery.repository.ImageRepository;
import com.tamakara.bakabooru.module.gallery.dto.SearchRequestDto;
import com.tamakara.bakabooru.module.tags.entity.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;

    @Transactional(readOnly = true)
    public Page<ImageDto> search(SearchRequestDto request, Pageable pageable) {
        Specification<Image> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Set<String> includedTags = new HashSet<>();
            Set<String> excludedTags = new HashSet<>();

            // 解析标签搜索字符串
            if (request.getTagSearch() != null && !request.getTagSearch().trim().isEmpty()) {
                String[] tokens = request.getTagSearch().trim().split("\\s+");
                for (String token : tokens) {
                    if (token.startsWith("-") && token.length() > 1) {
                        excludedTags.add(token.substring(1));
                    } else if (!token.isEmpty()) {
                        includedTags.add(token);
                    }
                }
            }

            // 包含的标签 (AND)
            if (!includedTags.isEmpty()) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Image> subRoot = subquery.from(Image.class);
                Join<Image, Tag> subTags = subRoot.join("tags");

                subquery.select(subRoot.get("id"));
                subquery.where(
                        cb.equal(subRoot.get("id"), root.get("id")),
                        subTags.get("name").in(includedTags)
                );
                subquery.groupBy(subRoot.get("id"));
                subquery.having(cb.equal(cb.countDistinct(subTags.get("id")), (long) includedTags.size()));

                predicates.add(cb.exists(subquery));
            }

            // 排除的标签 (NOT)
            if (!excludedTags.isEmpty()) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Image> subRoot = subquery.from(Image.class);
                Join<Image, Tag> subTags = subRoot.join("tags");

                subquery.select(subRoot.get("id"));
                subquery.where(
                        cb.equal(subRoot.get("id"), root.get("id")),
                        subTags.get("name").in(excludedTags)
                );

                predicates.add(cb.not(cb.exists(subquery)));
            }


            // 关键字搜索 (Title or FileName)
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String likePattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), likePattern),
                        cb.like(cb.lower(root.get("fileName")), likePattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return imageRepository.findAll(spec, pageable).map(imageMapper::toDto);
    }
}
