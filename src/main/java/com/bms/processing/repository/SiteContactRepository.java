package com.bms.processing.repository;

import com.bms.processing.entity.SiteContactEntity;
import com.bms.processing.entity.SiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteContactRepository extends JpaRepository<SiteContactEntity, Long> {

    List<SiteContactEntity> findBySiteOrderByContactNameAsc(SiteEntity site);
}