package com.tamakara.bakabooru.module.tags.repository;

import com.tamakara.bakabooru.module.tags.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    List<Tag> findByNameContainingIgnoreCase(String name);

}
