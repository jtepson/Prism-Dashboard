package com.bms.processing.service;

import com.bms.processing.entity.DicomConfigEntity;
import org.springframework.stereotype.Service;

@Service
public class DicomService {

    public boolean testEcho(DicomConfigEntity config) {

        if (config == null) {
            return false;
        }

        // Real C-FIND implementation coming next.

        return true;
    }

    public String testQuery(DicomConfigEntity config) {

        if (config == null) {
            return "No configuration selected.";
        }

        // Real C-FIND implementation coming next.


        return "Query test placeholder successful.";
    }

    public String testRetrieve(DicomConfigEntity config) {

        if (config == null) {
            return "No configuration selected.";
        }

        // Real C-MOVE implementation coming next.

        return "Retrieve test placeholder successful.";
    }
}