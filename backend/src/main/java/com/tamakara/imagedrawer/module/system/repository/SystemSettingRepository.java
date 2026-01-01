package com.tamakara.imagedrawer.module.system.repository;

import com.tamakara.imagedrawer.module.system.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}

