package com.bms.processing.service;

import com.bms.processing.entity.SiteEntity;
import com.bms.processing.repository.SiteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SiteService {

    private final SiteRepository repository;

    public SiteService(SiteRepository repository) {
        this.repository = repository;
    }

    public List<SiteEntity> getAllSites() {
        return repository.findAll()
                .stream()
                .sorted((a, b) ->
                        a.getFacilityName().compareToIgnoreCase(b.getFacilityName()))
                .toList();
    }

    public SiteEntity save(SiteEntity site) {
        return repository.save(site);
    }

    public boolean exists(String facilityName) {
        return repository.existsByFacilityNameIgnoreCase(facilityName);
    }

    public boolean existsForOtherSite(String facilityName, Long currentSiteId) {
        return repository.findAll().stream()
                .anyMatch(site ->
                        site.getFacilityName() != null
                                && site.getFacilityName().equalsIgnoreCase(facilityName)
                                && (currentSiteId == null || !site.getId().equals(currentSiteId))
                );
    }

    public List<SiteEntity> search(String text) {
        return repository.findByFacilityNameContainingIgnoreCaseOrderByFacilityNameAsc(text);
    }
}