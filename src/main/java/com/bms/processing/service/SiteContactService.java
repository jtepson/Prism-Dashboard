package com.bms.processing.service;

import com.bms.processing.entity.SiteContactEntity;
import com.bms.processing.entity.SiteEntity;
import com.bms.processing.repository.SiteContactRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SiteContactService {

    private final SiteContactRepository repository;

    public SiteContactService(SiteContactRepository repository) {
        this.repository = repository;
    }

    public List<SiteContactEntity> getContactsForSite(SiteEntity site) {
        return repository.findBySiteOrderByContactNameAsc(site);
    }

    public SiteContactEntity save(SiteContactEntity contact) {
        return repository.save(contact);
    }

    public void delete(SiteContactEntity contact) {
        repository.delete(contact);
    }
}