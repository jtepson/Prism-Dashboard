package com.bms.processing.views;

import com.bms.processing.layouts.MainLayout;
import com.bms.processing.model.FinalOutcome;
import com.bms.processing.model.PatientStatus;
import com.bms.processing.model.ThirdPartyStatus;
import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.service.CaseRecordService;
import com.bms.processing.service.InvalidWorkflowTransitionException;
import com.bms.processing.service.SiteService;
import com.bms.processing.components.CaseRecordDialog;
import com.bms.processing.service.AuditEventService;
import com.bms.processing.entity.AuditEventEntity;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.util.function.Consumer;
import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Div;

import java.time.LocalDate;
import java.time.LocalDateTime;

@PageTitle("Processing")
@PermitAll
@Route(value = "processing", layout = MainLayout.class)
public class ProcessingView extends VerticalLayout {

    private final CaseRecordService caseRecordService;
	private final AuditEventService auditEventService;
    private final Grid<CaseRecordEntity> grid = new Grid<>(CaseRecordEntity.class, false);
	private final TextField searchField = new TextField();
	private final SiteService siteService;

    public ProcessingView(
			CaseRecordService caseRecordService,
			SiteService siteService,
			AuditEventService auditEventService
	) {
        this.caseRecordService = caseRecordService;
		this.siteService = siteService;
		this.auditEventService = auditEventService;
		setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Processing"));
        add(new Span("Prism team workflow page for IMEKA, DuraMap, Neuroreader, notes, and completion handling."));

		searchField.setPlaceholder("Search last name, ID, or site");
		searchField.setClearButtonVisible(true);
		searchField.setWidth("420px");
		searchField.addValueChangeListener(event -> refreshProcessingGrid());

        configureGrid();
		refreshProcessingGrid();

		grid.addItemClickListener(event ->
				new CaseRecordDialog(
						event.getItem(),
						caseRecordService,
						CaseRecordDialog.Mode.PROCESSING,
						this::refreshProcessingGrid,
						siteService,
						auditEventService
				).open()
		);

        add(searchField, grid);
        expand(grid);
    }

	
    private void configureGrid() {
		grid.setSizeFull();
		grid.setWidthFull();
		grid.addClassName("workflow-grid");

		grid.addThemeVariants(
				GridVariant.LUMO_ROW_STRIPES
	);

        grid.addColumn(CaseRecordEntity::getPatientLastName)
                .setHeader("Last Name")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(CaseRecordEntity::getPatientFirstName)
                .setHeader("First Name")
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getPatientId)
                .setHeader("Patient ID")
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getSiteName)
                .setHeader("Site Name")
                .setAutoWidth(true);

	grid.addComponentColumn(record -> {
		VerticalLayout stack = new VerticalLayout();
		stack.setPadding(false);
		stack.setSpacing(false);
		stack.setMargin(false);
		stack.setAlignItems(FlexComponent.Alignment.START);
		stack.getStyle()
				.set("font-size", "0.8rem")
				.set("line-height", "1.1")
				.set("justify-content", "center")
				.set("height", "100%");

		//changing this around to allow for duramap to be the default, and if patient is not a minor, then hey they have IMEKA as an option. 
			if (record.isMinorAtScan()) {
				stack.add(thirdPartyLabel("DuraMap"));
				stack.add(thirdPartyLabel("Neuroreader"));
			} else {
				stack.add(thirdPartyLabel("IMEKA"));
				
				if (record.getImekaStatus() == ThirdPartyStatus.ERROR) {
					stack.add(thirdPartyLabel("DuraMap"));
				}
				
				stack.add(thirdPartyLabel("Neuroreader"));
		}

		return stack;

	}).setHeader("Third Party Processing").setAutoWidth(true);


	//rewriting this so that pt with an age under 18 has a default of Duramap, if adult then they would get imeka, then errored results in dura
	grid.addComponentColumn(record -> {
		VerticalLayout stack = new VerticalLayout();
		stack.setPadding(false);
		stack.setSpacing(false);
		stack.setMargin(false);
	
		if (record.isMinorAtScan()) {
			ComboBox<ThirdPartyStatus> dura = new ComboBox<>();
			dura.setItems(
					ThirdPartyStatus.NOT_SENT,
					ThirdPartyStatus.SENT,
					ThirdPartyStatus.ERROR
			);
			dura.setValue(record.getDuramapStatus());
			dura.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
			dura.setWidth("110px");
	
			dura.addValueChangeListener(e -> {
				if (e.getValue() != null) {
					if (e.getValue() == ThirdPartyStatus.ERROR) {
						promptRequiredErrorNote(
								"DuraMap Error Note Required",
								record.getDuramapErrorNote(),
								noteValue -> {
									try {
										applyThirdPartyStatus(
												record,
												"DuraMap",
												ThirdPartyStatus.ERROR,
												noteValue,
												record.getDuramapSentDate()
										);
										refreshProcessingGrid();
									} catch (InvalidWorkflowTransitionException ex) {
										showError(ex.getMessage());
									}
								}
						);
					} else {
						handleThirdPartyStatusSelection(
							record,
							"DuraMap",
							e.getValue()
						);
					}
				}
			});
	
			stack.add(dura);

			ComboBox<ThirdPartyStatus> nr = new ComboBox<>();
			nr.setItems(
					ThirdPartyStatus.NOT_SENT,
					ThirdPartyStatus.SENT,
					ThirdPartyStatus.ERROR
			);
			nr.setValue(record.getNeuroreaderStatus());
			nr.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
			nr.setWidth("110px");

			nr.addValueChangeListener(e -> {
				if (e.getValue() != null) {
					if (e.getValue() == ThirdPartyStatus.ERROR) {
						promptRequiredErrorNote(
								"Neuroreader Error Note Required",
								record.getNeuroreaderErrorNote(),
								noteValue -> {
									try {
										applyThirdPartyStatus(
												record,
												"Neuroreader",
												ThirdPartyStatus.ERROR,
												noteValue,
												record.getNeuroreaderSentDate()
										);
										refreshProcessingGrid();
									} catch (InvalidWorkflowTransitionException ex) {
										showError(ex.getMessage());
									}
								}
						);
					} else {
						handleThirdPartyStatusSelection(
								record,
								"Neuroreader",
								e.getValue()
						);
					}
				}
			});

			stack.add(nr);
	
		} else {
			ComboBox<ThirdPartyStatus> imeka = new ComboBox<>();
			imeka.setItems(
					ThirdPartyStatus.NOT_SENT,
					ThirdPartyStatus.SENT,
					ThirdPartyStatus.UPLOADED,
					ThirdPartyStatus.ERROR
			);
			imeka.setValue(record.getImekaStatus());
			imeka.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
			imeka.setWidth("110px");
	
			imeka.addValueChangeListener(e -> {
				if (e.getValue() != null) {
					if (e.getValue() == ThirdPartyStatus.ERROR) {
						promptRequiredErrorNote(
								"IMEKA Error Note Required",
								record.getImekaErrorNote(),
								noteValue -> {
									try {
										applyThirdPartyStatus(
												record,
												"IMEKA",
												ThirdPartyStatus.ERROR,
												noteValue,
												record.getImekaSentDate()
										);
										refreshProcessingGrid();
									} catch (InvalidWorkflowTransitionException ex) {
										showError(ex.getMessage());
									}
								}
						);
					} else {
						handleThirdPartyStatusSelection(
							record,
							"IMEKA",
							e.getValue()
						);
					}
				}
			});
	
			stack.add(imeka);
	
			if (record.getImekaStatus() == ThirdPartyStatus.ERROR) {
				ComboBox<ThirdPartyStatus> dura = new ComboBox<>();
				dura.setItems(
						ThirdPartyStatus.NOT_SENT,
						ThirdPartyStatus.SENT,
						ThirdPartyStatus.ERROR
				);
				dura.setValue(record.getDuramapStatus());
				dura.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
				dura.setWidth("110px");
	
				dura.addValueChangeListener(e -> {
					if (e.getValue() != null) {
						if (e.getValue() == ThirdPartyStatus.ERROR) {
							promptRequiredErrorNote(
        "DuraMap Error Note Required",
        record.getDuramapErrorNote(),
        noteValue -> {
            try {
                applyThirdPartyStatus(
                        record,
                        "DuraMap",
                        ThirdPartyStatus.ERROR,
                        noteValue,
                        record.getDuramapSentDate()
                );
                refreshProcessingGrid();
            } catch (InvalidWorkflowTransitionException ex) {
                showError(ex.getMessage());
            }
        }
);
						} else {
							handleThirdPartyStatusSelection(
								record,
								"DuraMap",
								e.getValue()
							);
						}
					}
				});
	
				stack.add(dura);
			}
	
			ComboBox<ThirdPartyStatus> nr = new ComboBox<>();
			nr.setItems(
					ThirdPartyStatus.NOT_SENT,
					ThirdPartyStatus.SENT,
					ThirdPartyStatus.ERROR
			);
			nr.setValue(record.getNeuroreaderStatus());
			nr.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
			nr.setWidth("110px");
	
			nr.addValueChangeListener(e -> {
				if (e.getValue() != null) {
					if (e.getValue() == ThirdPartyStatus.ERROR) {
						promptRequiredErrorNote(
								"Neuroreader Error Note Required",
								record.getNeuroreaderErrorNote(),
								noteValue -> {
									try {
										applyThirdPartyStatus(
												record,
												"Neuroreader",
												ThirdPartyStatus.ERROR,
												noteValue,
												record.getNeuroreaderSentDate()
										);
										refreshProcessingGrid();
									} catch (InvalidWorkflowTransitionException ex) {
										showError(ex.getMessage());
									}
								}
						);
					} else {
						handleThirdPartyStatusSelection(
							record,
							"Neuroreader",
							e.getValue()
						);
					}
				}
			});
	
			stack.add(nr);
		}
	
		return stack;
	}).setHeader("Third Party Status").setAutoWidth(true);

	grid.addComponentColumn(record -> {
		VerticalLayout stack = new VerticalLayout();
		stack.setPadding(false);
		stack.setSpacing(false);
		stack.setMargin(false);
	
		if (record.isMinorAtScan()) {
			DatePicker duraDate = new DatePicker();
			duraDate.setValue(record.getDuramapSentDate());
			duraDate.addThemeVariants(DatePickerVariant.LUMO_SMALL);
			duraDate.setWidth("135px");

			duraDate.addValueChangeListener(e -> {
				try {
					caseRecordService.updateDuramapSentDate(record, e.getValue());
					refreshProcessingGrid();
				} catch (InvalidWorkflowTransitionException ex) {
					showError(ex.getMessage());
				}
			});

			stack.add(duraDate);

			DatePicker nrDate = new DatePicker();
			nrDate.setValue(record.getNeuroreaderSentDate());
			nrDate.addThemeVariants(DatePickerVariant.LUMO_SMALL);
			nrDate.setWidth("135px");

			nrDate.addValueChangeListener(e -> {
				try {
					caseRecordService.updateNeuroreaderSentDate(record, e.getValue());
					refreshProcessingGrid();
				} catch (InvalidWorkflowTransitionException ex) {
					showError(ex.getMessage());
				}
			});

			stack.add(nrDate);

		} else {
			DatePicker imekaDate = new DatePicker();
			imekaDate.setValue(record.getImekaSentDate());
			imekaDate.setReadOnly(record.getImekaStatus() == ThirdPartyStatus.UPLOADED);
			imekaDate.addThemeVariants(DatePickerVariant.LUMO_SMALL);
			imekaDate.setWidth("135px");
	
			imekaDate.addValueChangeListener(e -> {
				try {
					caseRecordService.updateImekaSentDate(record, e.getValue());
					refreshProcessingGrid();
				} catch (InvalidWorkflowTransitionException ex) {
					showError(ex.getMessage());
				}
			});
	
			stack.add(imekaDate);
	
			if (record.getImekaStatus() == ThirdPartyStatus.ERROR) {
				DatePicker duraDate = new DatePicker();
				duraDate.setValue(record.getDuramapSentDate());
				duraDate.addThemeVariants(DatePickerVariant.LUMO_SMALL);
				duraDate.setWidth("135px");
	
				duraDate.addValueChangeListener(e -> {
					try {
						caseRecordService.updateDuramapSentDate(record, e.getValue());
						refreshProcessingGrid();
					} catch (InvalidWorkflowTransitionException ex) {
						showError(ex.getMessage());
					}
				});
	
				stack.add(duraDate);
			}
	
			DatePicker nrDate = new DatePicker();
			nrDate.setValue(record.getNeuroreaderSentDate());
			nrDate.addThemeVariants(DatePickerVariant.LUMO_SMALL);
			nrDate.setWidth("135px");
	
			nrDate.addValueChangeListener(e -> {
				try {
					caseRecordService.updateNeuroreaderSentDate(record, e.getValue());
					refreshProcessingGrid();
				} catch (InvalidWorkflowTransitionException ex) {
					showError(ex.getMessage());
				}
			});
	
			stack.add(nrDate);
		}
	
		return stack;
	}).setHeader("Third Party Sent Date").setAutoWidth(true);

	
		grid.addComponentColumn(record -> {
				Span noteFlag = new Span("!");
				noteFlag.getStyle()
					.set("font-weight", "700")
					.set("cursor", "pointer")
					.set("color", "var(--lumo-error-text-color)");

		boolean hasNotes =
				(record.getNotes() != null && !record.getNotes().trim().isEmpty()) ||
				(record.getImekaErrorNote() != null && !record.getImekaErrorNote().trim().isEmpty()) ||
				(record.getDuramapErrorNote() != null && !record.getDuramapErrorNote().trim().isEmpty()) ||
				(record.getNeuroreaderErrorNote() != null && !record.getNeuroreaderErrorNote().trim().isEmpty());
				noteFlag.setVisible(hasNotes);

			noteFlag.getElement().setProperty("title", hasNotes ? "View notes" : "");

			noteFlag.getElement().addEventListener("click", e -> openNotesDialog(record))
			.addEventData("event.stopPropagation()");

				return noteFlag;
		}).setHeader("Notes").setAutoWidth(true);

		grid.addComponentColumn(record -> {
			if (record.getPatientStatus() == PatientStatus.ACQUIRED) {
				Button startButton = new Button("Start Processing");
				startButton.addThemeVariants(
						ButtonVariant.LUMO_SMALL,
						ButtonVariant.LUMO_PRIMARY
				);

				startButton.addClickListener(event -> {
					try {
						caseRecordService.startProcessing(record);
						refreshProcessingGrid();
						showSuccess("Case moved to Processing.");
					} catch (InvalidWorkflowTransitionException ex) {
						showError(ex.getMessage());
					}
				});

				return startButton;
			}

			return buildStatusChip(formatEnum(record.getPatientStatus()));
		}).setHeader("Patient Status").setAutoWidth(true);

		grid.addComponentColumn(record -> {
			VerticalLayout actions = new VerticalLayout();
			actions.setPadding(false);
			actions.setSpacing(false);
			actions.setMargin(false);

			Button finalizeButton = new Button("Finalize");
			finalizeButton.addThemeVariants(
					ButtonVariant.LUMO_SMALL,
					ButtonVariant.LUMO_SUCCESS
			);

			boolean readyToFinalize = caseRecordService.isReadyToFinalize(record);
			finalizeButton.setEnabled(readyToFinalize);

			if (!readyToFinalize) {
				finalizeButton.getElement().setProperty(
						"title",
						"Case not ready for finalization"
				);
			}

			finalizeButton.addClickListener(event -> openFinalizeDialog(record));

			Button errorButton = new Button("Error");
			errorButton.addThemeVariants(
					ButtonVariant.LUMO_SMALL,
					ButtonVariant.LUMO_ERROR
			);

			errorButton.addClickListener(event -> openSubmitErrorDialog(record));

			actions.add(finalizeButton, errorButton);
			return actions;
		}).setHeader("").setAutoWidth(true);

		}

	private void openEditDialog(CaseRecordEntity record) {
	    Dialog dialog = new Dialog();
	    dialog.setHeaderTitle("Patient Information");
	    dialog.setWidth("1000px");

	    TextField lastName = new TextField("Last Name");
	    lastName.setValue(nullSafe(record.getPatientLastName()));

	    TextField firstName = new TextField("First Name");
	    firstName.setValue(nullSafe(record.getPatientFirstName()));

	    TextField patientId = new TextField("Patient ID");
	    patientId.setValue(nullSafe(record.getPatientId()));

	    TextField siteName = new TextField("Site Name");
	    siteName.setValue(nullSafe(record.getSiteName()));

		DatePicker dateOfBirth = new DatePicker("Date of Birth");
		dateOfBirth.setValue(record.getDateOfBirth());

		DatePicker dateScanned = new DatePicker("Date Scanned");
		dateScanned.setValue(record.getDateScanned());

		DatePicker imagesReceivedDate = new DatePicker("Images Received");
		imagesReceivedDate.setValue(record.getImagesReceivedDate());

	    ComboBox<PatientStatus> patientStatus = new ComboBox<>("Patient Status");
	    patientStatus.setItems(PatientStatus.values());
	    patientStatus.setValue(record.getPatientStatus());
	    patientStatus.setItemLabelGenerator(this::formatEnum);

	    TextArea notes = new TextArea("Notes");
	    notes.setWidthFull();
	    notes.setMinHeight("120px");
	    notes.setValue(nullSafe(record.getNotes()));

	    ComboBox<ThirdPartyStatus> imekaStatus = new ComboBox<>("IMEKA Status");
	    imekaStatus.setItems(ThirdPartyStatus.values());
	    imekaStatus.setValue(record.getImekaStatus());
	    imekaStatus.setItemLabelGenerator(this::formatEnum);

	    DatePicker imekaSentDate = new DatePicker("IMEKA Sent Date");
	    imekaSentDate.setValue(record.getImekaSentDate());

	    ComboBox<ThirdPartyStatus> duramapStatus = new ComboBox<>("DuraMap Status");
	    duramapStatus.setItems(ThirdPartyStatus.values());
	    duramapStatus.setValue(record.getDuramapStatus());
	    duramapStatus.setItemLabelGenerator(this::formatEnum);

	    DatePicker duramapSentDate = new DatePicker("DuraMap Sent Date");
	    duramapSentDate.setValue(record.getDuramapSentDate());

	    ComboBox<ThirdPartyStatus> nrStatus = new ComboBox<>("Neuroreader Status");
	    nrStatus.setItems(ThirdPartyStatus.values());
	    nrStatus.setValue(record.getNeuroreaderStatus());
	    nrStatus.setItemLabelGenerator(this::formatEnum);

	    DatePicker nrSentDate = new DatePicker("Neuroreader Sent Date");
	    nrSentDate.setValue(record.getNeuroreaderSentDate());

	    ComboBox<FinalOutcome> finalOutcome = new ComboBox<>("Final Outcome");
	    finalOutcome.setItems(FinalOutcome.values());
	    finalOutcome.setValue(record.getFinalOutcome());
	    finalOutcome.setItemLabelGenerator(this::formatEnum);

	    TextArea imekaErrorNote = new TextArea("IMEKA Error Note");
	    imekaErrorNote.setWidthFull();
	    imekaErrorNote.setMinHeight("100px");
	    imekaErrorNote.setValue(nullSafe(record.getImekaErrorNote()));
	    imekaErrorNote.setVisible(record.getImekaStatus() == ThirdPartyStatus.ERROR);

	    TextArea duramapErrorNote = new TextArea("DuraMap Error Note");
	    duramapErrorNote.setWidthFull();
	    duramapErrorNote.setMinHeight("100px");
	    duramapErrorNote.setValue(nullSafe(record.getDuramapErrorNote()));
	    duramapErrorNote.setVisible(record.getDuramapStatus() == ThirdPartyStatus.ERROR);

	    TextArea neuroreaderErrorNote = new TextArea("Neuroreader Error Note");
	    neuroreaderErrorNote.setWidthFull();
	    neuroreaderErrorNote.setMinHeight("100px");
	    neuroreaderErrorNote.setValue(nullSafe(record.getNeuroreaderErrorNote()));
	    neuroreaderErrorNote.setVisible(record.getNeuroreaderStatus() == ThirdPartyStatus.ERROR);

		boolean minorAtScan = record.isMinorAtScan();

	    H4 patientHeader = new H4("Patient Information");
	    FormLayout patientForm = new FormLayout();
	    patientForm.setWidthFull();
	    patientForm.setResponsiveSteps(
	            new FormLayout.ResponsiveStep("0", 1),
	            new FormLayout.ResponsiveStep("700px", 2)
	    );
	    patientForm.add(lastName, firstName, patientId, siteName, dateOfBirth, dateScanned);

	    H4 processingHeader = new H4("Processing Status");
	    FormLayout processingForm = new FormLayout();
	    processingForm.setWidthFull();
	    processingForm.setResponsiveSteps(
	            new FormLayout.ResponsiveStep("0", 1),
	            new FormLayout.ResponsiveStep("700px", 2)
	    );
	    processingForm.add(imagesReceivedDate, patientStatus, notes);
	    processingForm.setColspan(notes,2);

	    H4 thirdPartyHeader = new H4("Third Party Processing");
	    FormLayout thirdPartyForm = new FormLayout();
	    thirdPartyForm.setWidthFull();
	    thirdPartyForm.setResponsiveSteps(
	            new FormLayout.ResponsiveStep("0", 1),
	            new FormLayout.ResponsiveStep("700px", 2)
	    );

		imekaStatus.setVisible(!minorAtScan);
		imekaSentDate.setVisible(!minorAtScan);
		imekaErrorNote.setVisible(!minorAtScan && record.getImekaStatus() == ThirdPartyStatus.ERROR);

		nrStatus.setVisible(!minorAtScan);
		nrSentDate.setVisible(!minorAtScan);
		neuroreaderErrorNote.setVisible(!minorAtScan && record.getNeuroreaderStatus() == ThirdPartyStatus.ERROR);

		duramapStatus.setVisible(minorAtScan || record.getImekaStatus() == ThirdPartyStatus.ERROR);
		duramapSentDate.setVisible(minorAtScan || record.getImekaStatus() == ThirdPartyStatus.ERROR);
		duramapErrorNote.setVisible(
				(minorAtScan || record.getImekaStatus() == ThirdPartyStatus.ERROR)
						&& record.getDuramapStatus() == ThirdPartyStatus.ERROR
		);

		VerticalLayout duramapBlock = new VerticalLayout(duramapStatus, duramapSentDate, duramapErrorNote);
		duramapBlock.setPadding(false);
		duramapBlock.setSpacing(true);
		duramapBlock.setVisible(minorAtScan || record.getImekaStatus() == ThirdPartyStatus.ERROR);

		thirdPartyForm.add(imekaStatus, imekaSentDate);
		thirdPartyForm.add(imekaErrorNote);
		thirdPartyForm.setColspan(imekaErrorNote, 2);

		thirdPartyForm.add(duramapBlock);
		thirdPartyForm.setColspan(duramapBlock, 2);

		thirdPartyForm.add(nrStatus, nrSentDate);
		thirdPartyForm.add(neuroreaderErrorNote);
		thirdPartyForm.setColspan(neuroreaderErrorNote, 2);

		imekaStatus.addValueChangeListener(event -> {
			if (minorAtScan) {
				return;
			}
		
			boolean imekaErrored = event.getValue() == ThirdPartyStatus.ERROR;
			imekaErrorNote.setVisible(imekaErrored);
			duramapBlock.setVisible(imekaErrored);
		
			if (!imekaErrored) {
				duramapStatus.setValue(ThirdPartyStatus.NOT_SENT);
				duramapSentDate.clear();
				duramapErrorNote.clear();
				duramapErrorNote.setVisible(false);
			} else if (duramapStatus.getValue() == null) {
				duramapStatus.setValue(ThirdPartyStatus.NOT_SENT);
			}
		});

	    duramapStatus.addValueChangeListener(event ->
	            duramapErrorNote.setVisible(event.getValue() == ThirdPartyStatus.ERROR)
	    );

	    nrStatus.addValueChangeListener(event -> {
			if (minorAtScan) {
				neuroreaderErrorNote.setVisible(false);
				return;
			}
		
			neuroreaderErrorNote.setVisible(event.getValue() == ThirdPartyStatus.ERROR);
		});

	    Button saveButton = new Button("Save");
	    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

	    Button cancelButton = new Button("Cancel", e -> dialog.close());

	    saveButton.addClickListener(event -> {
	        if (imekaStatus.getValue() == ThirdPartyStatus.ERROR && imekaErrorNote.getValue().trim().isEmpty()) {
	            showError("IMEKA error note is required.");
	            return;
	        }

	        if (duramapBlock.isVisible()
	                && duramapStatus.getValue() == ThirdPartyStatus.ERROR
	                && duramapErrorNote.getValue().trim().isEmpty()) {
	            showError("DuraMap error note is required.");
	            return;
	        }

	        if (nrStatus.getValue() == ThirdPartyStatus.ERROR
	                && neuroreaderErrorNote.getValue().trim().isEmpty()) {
	            showError("Neuroreader error note is required.");
	            return;
	        }

	        if ((finalOutcome.getValue() == FinalOutcome.PROCESSED_WITH_ERRORS
	                || finalOutcome.getValue() == FinalOutcome.PROCESSED_WITH_THIRD_PARTY_ERRORS)
	                && notes.getValue().trim().isEmpty()) {
	            showError("Notes are required for processed cases with errors.");
	            return;
	        }

	        record.setPatientLastName(lastName.getValue().trim());
			record.setPatientFirstName(firstName.getValue().trim());
			record.setPatientId(patientId.getValue().trim());
			record.setSiteName(siteName.getValue().trim());
			record.setDateOfBirth(dateOfBirth.getValue());
			record.setDateScanned(dateScanned.getValue());
			record.setImagesReceivedDate(imagesReceivedDate.getValue());
			record.setPatientStatus(patientStatus.getValue());

	        record.setImekaStatus(imekaStatus.getValue());
	        record.setImekaSentDate(imekaSentDate.getValue());

	        record.setDuramapStatus(duramapBlock.isVisible() ? duramapStatus.getValue() : ThirdPartyStatus.NOT_SENT);
	        record.setDuramapSentDate(duramapBlock.isVisible() ? duramapSentDate.getValue() : null);

	        record.setNeuroreaderStatus(nrStatus.getValue());
	        record.setNeuroreaderSentDate(nrSentDate.getValue());

	        record.setNotes(notes.getValue().trim());
	        record.setImekaErrorNote(imekaErrorNote.isVisible() ? imekaErrorNote.getValue().trim() : null);
	        record.setDuramapErrorNote(duramapErrorNote.isVisible() ? duramapErrorNote.getValue().trim() : null);
	        record.setNeuroreaderErrorNote(neuroreaderErrorNote.isVisible() ? neuroreaderErrorNote.getValue().trim() : null);
	        record.setFinalOutcome(finalOutcome.getValue());

	        if (finalOutcome.getValue() == FinalOutcome.PROCESSED
	                || finalOutcome.getValue() == FinalOutcome.PROCESSED_WITH_ERRORS
	                || finalOutcome.getValue() == FinalOutcome.PROCESSED_WITH_THIRD_PARTY_ERRORS) {
	            record.setProcessedDate(LocalDateTime.now());

	            if (finalOutcome.getValue() == FinalOutcome.PROCESSED) {
	                record.setPatientStatus(PatientStatus.PROCESSED);
	            } else if (finalOutcome.getValue() == FinalOutcome.PROCESSED_WITH_ERRORS) {
	                record.setPatientStatus(PatientStatus.PROCESSED_WITH_ERRORS);
	            } else {
	                record.setPatientStatus(PatientStatus.PROCESSED_WITH_THIRD_PARTY_ERRORS);
	            }
	        }

	        try {
				caseRecordService.saveEditedCase(record);
				refreshProcessingGrid();
				dialog.close();
				showSuccess("Case updated.");
			} catch (InvalidWorkflowTransitionException ex) {
				showError(ex.getMessage());
			}
	    });

	    VerticalLayout content = new VerticalLayout(
	            patientHeader, patientForm,
	            processingHeader, processingForm,
	            thirdPartyHeader, thirdPartyForm
	    );
	    content.setPadding(false);
	    content.setSpacing(true);
	    content.setWidthFull();

	    dialog.add(content);
	    dialog.getFooter().add(cancelButton, saveButton);
	    dialog.open();
	}

    private void openNotesDialog(CaseRecordEntity record) {
    	Dialog dialog = new Dialog();
    	dialog.setHeaderTitle("Case Notes");
    	dialog.setWidth("700px");

    	VerticalLayout layout = new VerticalLayout();
    	layout.setPadding(false);
    	layout.setSpacing(true);
    	layout.setWidthFull();

    	addNoteSection(layout, "Processing Notes", record.getNotes());
    	addNoteSection(layout, "IMEKA Error Notes", record.getImekaErrorNote());
    	addNoteSection(layout, "DuraMap Error Notes", record.getDuramapErrorNote());
    	addNoteSection(layout, "Neuroreader Error Notes", record.getNeuroreaderErrorNote());

    	Button closeButton = new Button("Close", e -> dialog.close());

    	dialog.add(layout);
    	dialog.getFooter().add(closeButton);
    	dialog.open();
    }

    private void addNoteSection(VerticalLayout parent, String title, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
    	}

    	H4 header = new H4(title);

    	Div body = new Div();
    	body.setText(value);
    	body.getStyle()
            .set("white-space", "pre-wrap")
            .set("padding", "0.75rem")
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-radius", "8px")
            .set("background", "var(--lumo-contrast-5pct)")
            .set("width", "100%");

    	parent.add(header, body);
    }

	private void handleThirdPartyStatusSelection(
			CaseRecordEntity record,
			String label,
			ThirdPartyStatus selectedStatus
	) {
		if (selectedStatus == ThirdPartyStatus.SENT) {
			promptSentDateChoice(label, record, selectedStatus);
		} else {
			try {
				applyThirdPartyStatus(record, label, selectedStatus, null, null);
				refreshProcessingGrid();
			} catch (InvalidWorkflowTransitionException ex) {
				showError(ex.getMessage());
			}
		}
	}

	private void promptSentDateChoice(
			String label,
			CaseRecordEntity record,
			ThirdPartyStatus selectedStatus
	) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle(label + " Sent Date");
		dialog.setWidth("420px");

		Span question = new Span("Sent today?");

		Button yesButton = new Button("Yes");
		yesButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		Button noButton = new Button("No");

		yesButton.addClickListener(event -> {
			try {
				applyThirdPartyStatus(record, label, selectedStatus, null, LocalDate.now());
				refreshProcessingGrid();
				dialog.close();
			} catch (InvalidWorkflowTransitionException ex) {
				showError(ex.getMessage());
			}
		});

		noButton.addClickListener(event -> {
			dialog.close();
			promptSpecificSentDate(label, record, selectedStatus);
		});

		VerticalLayout content = new VerticalLayout(question);
		content.setPadding(false);
		content.setSpacing(true);

		dialog.add(content);
		dialog.getFooter().add(noButton, yesButton);
		dialog.open();
	}

	private void promptSpecificSentDate(
			String label,
			CaseRecordEntity record,
			ThirdPartyStatus selectedStatus
	) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle(label + " Sent Date");
		dialog.setWidth("420px");

		DatePicker customDate = new DatePicker("Select Date Sent");
		customDate.setWidthFull();

		Button cancelButton = new Button("Cancel", e -> dialog.close());

		Button saveDateButton = new Button("Save Date");
		saveDateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		saveDateButton.addClickListener(event -> {
			if (customDate.getValue() == null) {
				showError("Please select a date.");
				return;
			}

			try {
				applyThirdPartyStatus(record, label, selectedStatus, null, customDate.getValue());
				refreshProcessingGrid();
				dialog.close();
			} catch (InvalidWorkflowTransitionException ex) {
				showError(ex.getMessage());
			}
		});

		VerticalLayout content = new VerticalLayout(customDate);
		content.setPadding(false);
		content.setSpacing(true);

		dialog.add(content);
		dialog.getFooter().add(cancelButton, saveDateButton);
		dialog.open();
	}
	
	private Span buildStatusChip(String text) {
		Span chip = new Span(text == null ? "Unknown" : text);
		chip.getStyle()
				.set("display", "inline-block")
				.set("padding", "0.2rem 0.55rem")
				.set("border-radius", "999px")
				.set("font-size", "0.75rem")
				.set("font-weight", "600")
				.set("line-height", "1")
				.set("white-space", "nowrap");

		String value = text == null ? "" : text.toLowerCase();

		if (value.contains("processing")) {
			chip.getStyle().set("background", "#e3f2fd").set("color", "#0d47a1");
		} else if (value.contains("acquired")) {
			chip.getStyle().set("background", "#e0f7fa").set("color", "#006064");
		} else {
			chip.getStyle().set("background", "#eceff1").set("color", "#37474f");
		}

		return chip;
	}

	private String formatEnum(Enum<?> value) {
        return value == null ? "" : value.name().replace("_", " ");
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.toString().replace("T", " ");
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

	private Span thirdPartyLabel(String text) {
		Span label = new Span(text);

		label.getStyle()
			.set("font-size", "0.8rem")
			.set("display", "flex")
			.set("align-items", "center")
			.set("height", "36px")   // match ComboBox height
			.set("line-height", "36px");

		return label;
	}

	private void promptRequiredErrorNote(
        String title,
        String existingValue,
        Consumer<String> onSave
	) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle(title);
		dialog.setWidth("600px");

		TextArea note = new TextArea("Error Explanation");
		note.setWidthFull();
		note.setMinHeight("160px");
		note.setValue(existingValue != null ? existingValue : "");

		Button saveButton = new Button("Save");
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		Button cancelButton = new Button("Cancel", e -> dialog.close());

		saveButton.addClickListener(event -> {
			if (note.getValue().trim().isEmpty()) {
				showError("Error explanation is required.");
				return;
			}

			onSave.accept(note.getValue().trim());
			dialog.close();
		});

		dialog.add(note);
		dialog.getFooter().add(cancelButton, saveButton);
		dialog.open();
	}


    private void showError(String message) {
        Notification notification = Notification.show(message, 3500, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 2500, Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void refreshProcessingGrid() {
		String filter = searchField.getValue() == null ? "" : searchField.getValue().trim().toLowerCase();
	
		grid.setItems(
				caseRecordService.findAll().stream()
						.filter(record ->
								record.getPatientStatus() == PatientStatus.ACQUIRED
										|| record.getPatientStatus() == PatientStatus.PROCESSING)
						.filter(record -> filter.isEmpty()
								|| containsIgnoreCase(record.getPatientLastName(), filter)
								|| containsIgnoreCase(record.getPatientFirstName(), filter)
								|| containsIgnoreCase(record.getPatientId(), filter)
								|| containsIgnoreCase(record.getSiteName(), filter)
								|| containsIgnoreCase(record.getFunder(), filter))
						.toList()
		);
	}

	private void openFinalizeDialog(CaseRecordEntity record) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("Finalize Case");

		Checkbox bmsCheck = new Checkbox("Is study available in BMS View?");
		bmsCheck.setRequiredIndicatorVisible(true);

		Span message = new Span("This will move the case to Processed.");

		Button cancel = new Button("Cancel", e -> dialog.close());

		Button finalize = new Button("Finalize");
		finalize.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		finalize.addClickListener(e -> {
			try {
				caseRecordService.finalizeCase(record, bmsCheck.getValue());
				refreshProcessingGrid();
				dialog.close();
				showSuccess("Case finalized.");
			} catch (InvalidWorkflowTransitionException ex) {
				showError(ex.getMessage());
			}
		});

		VerticalLayout layout = new VerticalLayout(message, bmsCheck);

		dialog.add(layout);
		dialog.getFooter().add(cancel, finalize);

		dialog.open();
	}

	private boolean containsIgnoreCase(String value, String filter) {
		return value != null && value.toLowerCase().contains(filter);
	}

	private void openSubmitErrorDialog(CaseRecordEntity record) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("Submit Study to Errors");
		dialog.setWidth("600px");

		TextArea errorNote = new TextArea("Explain the issue");
		errorNote.setWidthFull();
		errorNote.setMinHeight("180px");
		errorNote.setValue(record.getNotes() != null ? record.getNotes() : "");

		Button cancelButton = new Button("Cancel", e -> dialog.close());

		Button submitButton = new Button("Submit Error");
		submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

		submitButton.addClickListener(event -> {
			try {
				caseRecordService.markCaseAsError(record, errorNote.getValue());
				refreshProcessingGrid();
				dialog.close();
				showSuccess("Study moved to Errors.");
			} catch (InvalidWorkflowTransitionException ex) {
				showError(ex.getMessage());
			}
		});

		VerticalLayout content = new VerticalLayout(errorNote);
		content.setPadding(false);
		content.setSpacing(true);

		dialog.add(content);
		dialog.getFooter().add(cancelButton, submitButton);
		dialog.open();
	}

	private void applyThirdPartyStatus(
			CaseRecordEntity record,
			String label,
			ThirdPartyStatus status,
			String errorNote,
			LocalDate sentDate
	) {
		switch (label) {
			case "IMEKA" -> caseRecordService.updateImekaStatus(record, status, errorNote, sentDate);
			case "DuraMap" -> caseRecordService.updateDuramapStatus(record, status, errorNote, sentDate);
			case "Neuroreader" -> caseRecordService.updateNeuroreaderStatus(record, status, errorNote, sentDate);
			default -> throw new InvalidWorkflowTransitionException("Unsupported third-party workflow: " + label);
		}
	}

}
