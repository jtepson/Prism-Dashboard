package com.bms.processing.service;

import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.model.PatientStatus;
import com.bms.processing.model.ThirdPartyStatus;
import com.bms.processing.repository.CaseRecordRepository;
import com.bms.processing.service.InvalidWorkflowTransitionException;
import com.bms.processing.service.AuditEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.bms.processing.service.NotificationService;

class CaseRecordServiceTest {

    private CaseRecordRepository repository;
    private AuditEventService auditEventService;
    private CaseRecordService caseRecordService;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        repository = mock(CaseRecordRepository.class);
        auditEventService = mock(AuditEventService.class);
        notificationService = mock(NotificationService.class);

        caseRecordService = new CaseRecordService(
                repository,
                auditEventService,
                notificationService
        );

        when(repository.save(any(CaseRecordEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldThrowWhenFinalizeNotReady() {
        CaseRecordEntity record = new CaseRecordEntity();

        // Simulate an unfinished ready case
        record.setPatientStatus(PatientStatus.PROCESSING);
        record.setImekaStatus(ThirdPartyStatus.NOT_SENT);
        record.setNeuroreaderStatus(ThirdPartyStatus.NOT_SENT);
        record.setDuramapStatus(ThirdPartyStatus.NOT_SENT);

        assertThrows(
                InvalidWorkflowTransitionException.class,
                () -> caseRecordService.finalizeCase(record, true)
        );
    }

    @Test
        void shouldFinalizeWhenValid() {
            CaseRecordEntity record = new CaseRecordEntity();

            // Simulate a valid adult case
            record.setPatientStatus(PatientStatus.PROCESSING);

            // IMEKA complete (uses uploaded in system)
            record.setImekaStatus(ThirdPartyStatus.UPLOADED);

            // Neuroreader sent
            record.setNeuroreaderStatus(ThirdPartyStatus.SENT);

            // DuraMap not needed (since IMEKA not error)
            record.setDuramapStatus(ThirdPartyStatus.NOT_SENT);

            CaseRecordEntity result = caseRecordService.finalizeCase(record, true);

            assertNotNull(result.getProcessedDate());
            assertTrue(
                    result.getPatientStatus() == PatientStatus.PROCESSED
                            || result.getPatientStatus() == PatientStatus.PROCESSED_WITH_ERRORS
                            || result.getPatientStatus() == PatientStatus.PROCESSED_WITH_THIRD_PARTY_ERRORS
            );
        }

        @Test
        void shouldClearImekaAndNeuroreaderForMinor() {
            CaseRecordEntity record = new CaseRecordEntity();

            // Simulate a minor case
            record.setPatientStatus(PatientStatus.PROCESSING);
            record.setDateOfBirth(LocalDate.now().minusYears(10)); // minor

            // Set invalid values for a minor
            record.setImekaStatus(ThirdPartyStatus.SENT);
            record.setNeuroreaderStatus(ThirdPartyStatus.SENT);

            // Save through edit path (this is where cleanup happens)
            CaseRecordEntity result = caseRecordService.saveEditedCase(record);

            assertEquals(ThirdPartyStatus.NOT_SENT, result.getImekaStatus());
            assertNull(result.getImekaSentDate());

            assertEquals(ThirdPartyStatus.NOT_SENT, result.getNeuroreaderStatus());
            assertNull(result.getNeuroreaderSentDate());
        }

        @Test
        void shouldThrowWhenAdultUsesDuramapWithoutImekaError() {
            CaseRecordEntity record = new CaseRecordEntity();

            // Simulate adult
            record.setPatientStatus(PatientStatus.PROCESSING);
            record.setDateOfBirth(LocalDate.now().minusYears(30));

            // IMEKA is not errored
            record.setImekaStatus(ThirdPartyStatus.SENT);

            // DuraMap should not be allowed here
            record.setDuramapStatus(ThirdPartyStatus.SENT);
            record.setDuramapSentDate(LocalDate.now());

            assertThrows(
                    InvalidWorkflowTransitionException.class,
                    () -> caseRecordService.saveEditedCase(record)
            );
        }

        @Test
        void shouldThrowWhenProcessedWithErrorsWithoutNotes() {
            CaseRecordEntity record = new CaseRecordEntity();

            record.setPatientStatus(PatientStatus.PROCESSED_WITH_ERRORS);

            assertThrows(
                    InvalidWorkflowTransitionException.class,
                    () -> caseRecordService.saveEditedCase(record)
            );
        }

        @Test
        void shouldThrowWhenProcessedWithThirdPartyErrorsWithoutNotes() {
            CaseRecordEntity record = new CaseRecordEntity();

            record.setPatientStatus(PatientStatus.PROCESSED_WITH_THIRD_PARTY_ERRORS);

            assertThrows(
                    InvalidWorkflowTransitionException.class,
                    () -> caseRecordService.saveEditedCase(record)
            );
        }

        @Test
        void shouldThrowWhenErrorStatusWithoutNotes() {
            CaseRecordEntity record = new CaseRecordEntity();

            record.setPatientStatus(PatientStatus.ERROR);

            assertThrows(
                    InvalidWorkflowTransitionException.class,
                    () -> caseRecordService.saveEditedCase(record)
            );
        }

        @Test
        void shouldThrowWhenImekaSentWithoutSentDate() {
            CaseRecordEntity record = new CaseRecordEntity();

            record.setPatientStatus(PatientStatus.PROCESSING);
            record.setDateOfBirth(LocalDate.now().minusYears(30)); // adult
            record.setImekaStatus(ThirdPartyStatus.SENT);
            record.setNeuroreaderStatus(ThirdPartyStatus.NOT_SENT);
            record.setDuramapStatus(ThirdPartyStatus.NOT_SENT);

            assertThrows(
                    InvalidWorkflowTransitionException.class,
                    () -> caseRecordService.saveEditedCase(record)
            );
        }

        @Test
        void shouldThrowWhenDuramapSentWithoutSentDateForMinor() {
            CaseRecordEntity record = new CaseRecordEntity();

            record.setPatientStatus(PatientStatus.PROCESSING);
            record.setDateOfBirth(LocalDate.now().minusYears(10)); // minor
            record.setDuramapStatus(ThirdPartyStatus.SENT);

            assertThrows(
                    InvalidWorkflowTransitionException.class,
                    () -> caseRecordService.saveEditedCase(record)
            );
        }

        @Test
        void shouldThrowWhenNeuroreaderSentWithoutSentDate() {
            CaseRecordEntity record = new CaseRecordEntity();

            record.setPatientStatus(PatientStatus.PROCESSING);
            record.setDateOfBirth(LocalDate.now().minusYears(30)); // adult
            record.setImekaStatus(ThirdPartyStatus.NOT_SENT);
            record.setDuramapStatus(ThirdPartyStatus.NOT_SENT);
            record.setNeuroreaderStatus(ThirdPartyStatus.SENT);

            assertThrows(
                    InvalidWorkflowTransitionException.class,
                    () -> caseRecordService.saveEditedCase(record)
            );
        }

        @Test
        void shouldSetProcessedDateWhenSavingProcessedCase() {
            CaseRecordEntity record = new CaseRecordEntity();

            record.setPatientStatus(PatientStatus.PROCESSED);
            record.setDateOfBirth(LocalDate.now().minusYears(30)); // adult
            record.setImekaStatus(ThirdPartyStatus.UPLOADED);
            record.setImekaSentDate(LocalDate.now());
            record.setNeuroreaderStatus(ThirdPartyStatus.SENT);
            record.setNeuroreaderSentDate(LocalDate.now());
            record.setDuramapStatus(ThirdPartyStatus.NOT_SENT);

            CaseRecordEntity result = caseRecordService.saveEditedCase(record);

            assertNotNull(result.getProcessedDate());
            assertEquals(PatientStatus.PROCESSED, result.getPatientStatus());
        }
}
