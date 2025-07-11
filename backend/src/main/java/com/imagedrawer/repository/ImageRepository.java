package com.imagedrawer.repository;

import com.imagedrawer.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {
    Optional<Image> findByHash(String hash);

    void deleteByHash(String hash);

    boolean existsByHash(String hash);
}
