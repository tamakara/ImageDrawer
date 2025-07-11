package com.imagedrawer.repository;

import com.imagedrawer.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByNameIn(List<String> names);

    @Query(value = """
            SELECT t.name FROM tag t
            WHERE LOWER(t.name) LIKE '%' || LOWER(:keyword) || '%'
            ORDER BY  -- 完全匹配优先
                ABS(LENGTH(t.name) - LENGTH(:keyword)),                              
                t.name COLLATE NOCASE                                             
            """, nativeQuery = true)
    List<String> findTagsOrderByMatch(@Param("keyword") String keyword);
}
