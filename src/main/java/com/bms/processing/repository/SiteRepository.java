package com.bms.processing.repository;

import com.bms.processing.entity.SiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteRepository extends JpaRepository<SiteEntity, Long> {

    List<SiteEntity> findByFacilityNameContainingIgnoreCaseOrderByFacilityNameAsc(String facilityName);

    boolean existsByFacilityNameIgnoreCase(String facilityName);
}