package com.tamakara.imagedrawer.module.search.service;

import com.tamakara.imagedrawer.module.gallery.dto.ImageDto;
import com.tamakara.imagedrawer.module.gallery.entity.Image;
import com.tamakara.imagedrawer.module.gallery.mapper.ImageMapper;
import com.tamakara.imagedrawer.module.gallery.repository.ImageRepository;
import com.tamakara.imagedrawer.module.search.dto.SearchRequestDto;
import com.tamakara.imagedrawer.module.search.entity.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.util.ArrayList;
import java.util.List;

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

            // 包含的标签 (AND)
            if (request.getIncludedTags() != null && !request.getIncludedTags().isEmpty()) {
                List<String> tags = request.getIncludedTags();

                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Image> subRoot = subquery.from(Image.class);
                Join<Image, Tag> subTags = subRoot.join("tags");

                subquery.select(subRoot.get("id"));
                subquery.where(
                        cb.equal(subRoot.get("id"), root.get("id")),
                        subTags.get("name").in(tags)
                );
                subquery.groupBy(subRoot.get("id"));
                subquery.having(cb.equal(cb.countDistinct(subTags.get("id")), (long) tags.size()));

                predicates.add(cb.exists(subquery));
            }

            // 排除的标签 (NOT)
            if (request.getExcludedTags() != null && !request.getExcludedTags().isEmpty()) {
                List<String> tags = request.getExcludedTags();

                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Image> subRoot = subquery.from(Image.class);
                Join<Image, Tag> subTags = subRoot.join("tags");

                subquery.select(subRoot.get("id"));
                subquery.where(
                        cb.equal(subRoot.get("id"), root.get("id")),
                        subTags.get("name").in(tags)
                );

                predicates.add(cb.not(cb.exists(subquery)));
            }

            // 任意标签 (OR)
            if (request.getAnyTags() != null && !request.getAnyTags().isEmpty()) {
                Join<Image, Tag> tagsJoin = root.join("tags", JoinType.LEFT);
                predicates.add(tagsJoin.get("name").in(request.getAnyTags()));
                query.distinct(true);
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
