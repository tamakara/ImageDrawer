package com.tamakara.imagedrawer.module.tagger.repository;

import com.tamakara.imagedrawer.module.tagger.entity.TaggerServerConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaggerServerConfigRepository extends JpaRepository<TaggerServerConfig, Long> {
}

