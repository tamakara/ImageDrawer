package com.imagedrawer.repository;

import com.imagedrawer.dto.ImageResponse;
import com.imagedrawer.entity.Image;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ImageQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Page<ImageResponse> query(
            List<String> tagNames,
            String sort,
            String rating,
            Integer page,
            Integer pageSize,
            String seed
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT i.*
                FROM image i
                """);

        StringBuilder countSql = new StringBuilder("""
                SELECT COUNT(*) FROM (
                SELECT i.id
                FROM image i
                """);

        if (tagNames != null && !tagNames.isEmpty()) {
            String joinClause = """
                    JOIN image_tag it ON i.id = it.image_id
                    JOIN tag t ON t.id = it.tag_id
                    """;
            sql.append(joinClause);
            countSql.append(joinClause);
        }

        sql.append(" WHERE 1=1 ");
        countSql.append(" WHERE 1=1 ");

        if (tagNames != null && !tagNames.isEmpty()) {
            sql.append(" AND t.name IN :tagNames ");
            countSql.append(" AND t.name IN :tagNames ");
        }

        if (rating != null && !rating.equalsIgnoreCase("all")) {
            sql.append(" AND i.rating = :rating ");
            countSql.append(" AND i.rating = :rating ");
        }

        if (tagNames != null && !tagNames.isEmpty()) {
            sql.append("""
                    GROUP BY i.id
                    HAVING COUNT(DISTINCT t.name) = :tagCount
                    """);

            countSql.append("""
                    GROUP BY i.id
                    HAVING COUNT(DISTINCT t.name) = :tagCount
                    """);
        }

        countSql.append(") AS subquery");

        // 排序
        String orderBy;
        switch (sort.toLowerCase()) {
            case "random" -> {
                if (seed == null || seed.isBlank()) {
                    seed = String.valueOf(System.currentTimeMillis());
                }
                orderBy = " ORDER BY ((i.id * :seed) % 100000) ";
            }
            case "size" -> orderBy = " ORDER BY i.size DESC ";
            case "time" -> orderBy = " ORDER BY i.created_at DESC ";
            default -> orderBy = "";
        }
        sql.append(orderBy);

        sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append((page - 1) * pageSize);

        Query dataQuery = entityManager.createNativeQuery(sql.toString(), Image.class);

        if (tagNames != null && !tagNames.isEmpty()) {
            dataQuery.setParameter("tagNames", tagNames);
            dataQuery.setParameter("tagCount", tagNames.size());
        }
        if (rating != null && !rating.equalsIgnoreCase("all")) {
            dataQuery.setParameter("rating", rating);
        }
        if ("random".equalsIgnoreCase(sort)) {
            dataQuery.setParameter("seed", Long.parseLong(seed));
        }

        @SuppressWarnings("unchecked")
        List<Image> images = dataQuery.getResultList();

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        if (tagNames != null && !tagNames.isEmpty()) {
            countQuery.setParameter("tagNames", tagNames);
            countQuery.setParameter("tagCount", tagNames.size());
        }
        if (rating != null && !rating.equalsIgnoreCase("all")) {
            countQuery.setParameter("rating", rating);
        }

        long total = ((Number) countQuery.getSingleResult()).longValue();

        List<ImageResponse> responses = images.stream()
                .map(Image::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(
                responses,
                PageRequest.of(page - 1, pageSize),
                total
        );
    }
}
