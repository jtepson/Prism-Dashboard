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
import com.bms.processing.service.PatientFileService;
import com.bms.processing.service.DicomConfigService;
import com.bms.processing.service.DicomService;
import com.bms.processing.service.DicomRetrieveService;
import com.bms.processing.components.CaseIssueDialog;
import com.bms.processing.service.CaseIssueService;
import com.bms.processing.service.CurrentUserService;
import com.bms.processing.model.CaseIssueSource;
import com.bms.processing.model.CaseIssueStatus;
import com.bms.processing.model.CaseIssueType;
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
import org.springframework.beans.factory.annotation.Value;

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
	private final PatientFileService patientFileService;
	private final String baseStoragePath;
    private final DicomConfigService dicomConfigService;
    private final DicomService dicomService;
	private final DicomRetrieveService dicomRetrieveService;
	private final CurrentUserService currentUserService;
	private final CaseIssueService caseIssueService;

    public ProcessingView(
			CaseRecordService caseRecordService,
            SiteService siteService,
            AuditEventService auditEventService,
            PatientFileService patientFileService,
			@Value("${prism.files.storage-path}") String baseStoragePath,
			DicomConfigService dicomConfigService,
			DicomService dicomService,
			DicomRetrieveService dicomRetrieveService,
			CurrentUserService currentUserService,
			CaseIssueService caseIssueService
	) {
        this.caseRecordService = caseRecordService;
        this.siteService = siteService;
        this.auditEventService = auditEventService;
        this.patientFileService = patientFileService;
		this.baseStoragePath = baseStoragePath;
		this.dicomConfigService = dicomConfigService;
        this.dicomService = dicomService;
		this.dicomRetrieveService = dicomRetrieveService;
		this.currentUserService = currentUserService;
		this.caseIssueService = caseIssueService;

		setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(MainLayout.pageTitle("Processing"));

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
						auditEventService,
                        patientFileService,
						baseStoragePath,
						dicomConfigService,
                        dicomService,
						dicomRetrieveService,
						currentUserService
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
						promptVendorIssue(
								record,
								"DuraMap",
								CaseIssueSource.DURAMAP,
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
						promptVendorIssue(
								record,
								"Neuroreader",
								CaseIssueSource.NEUROREADER,
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
						promptVendorIssue(
								record,
								"IMEKA",
								CaseIssueSource.IMEKA,
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
							promptVendorIssue(
									record,
									"DuraMap",
									CaseIssueSource.DURAMAP,
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
						promptVendorIssue(
								record,
								"Neuroreader",
								CaseIssueSource.NEUROREADER,
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

	//added new column for issues - updated 08182026
	grid.addComponentColumn(record -> {
		var activeIssues = caseIssueService.findActiveByCaseRecord(record);

		if (activeIssues.isEmpty()) {
			return new Span("");
		}

		long blockingCount = activeIssues.stream()
				.filter(issue -> Boolean.TRUE.equals(issue.getBlocking()))
				.count();

		Span badge = new Span(
				activeIssues.size() == 1
						? "Issue"
						: "Issues (" + activeIssues.size() + ")"
		);

		badge.getStyle()
				.set("display", "inline-block")
				.set("padding", "0.2rem 0.55rem")
				.set("border-radius", "999px")
				.set("font-size", "0.75rem")
				.set("font-weight", "700");

		if (blockingCount > 0) {
			badge.getStyle()
					.set("background", "var(--lumo-error-color-10pct)")
					.set("color", "var(--lumo-error-text-color)");
		} else {
			badge.getStyle()
					.set("background", "var(--lumo-warning-color-10pct)")
					.set("color", "var(--lumo-warning-text-color)");
		}

		badge.getStyle().set("cursor", "pointer");

		badge.getElement().addEventListener(
				"click",
				event -> openActiveIssuesDialog(record)
		).addEventData("event.stopPropagation()");

		return badge;
	})
	.setHeader("Issues")
	.setAutoWidth(true);
	
	//im not fixing this indentation
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

			//new issue button, replacing error - updated 08182026
			Button addIssueButton = new Button("Add Issue");
			addIssueButton.addThemeVariants(
					ButtonVariant.LUMO_SMALL,
					ButtonVariant.LUMO_ERROR
			);

			addIssueButton.addClickListener(event ->
					new CaseIssueDialog(
							record,
							caseIssueService,
							currentUserService,
							this::refreshProcessingGrid
					).open()
			);

			actions.add(finalizeButton, addIssueButton);
			return actions;
		}).setHeader("").setAutoWidth(true);

		}

	private void handleThirdPartyStatusSelection(
			CaseRecordEntity record,
			String label,
			ThirdPartyStatus selectedStatus
	) {
		CaseIssueSource issueSource = switch (label) {
			case "IMEKA" -> CaseIssueSource.IMEKA;
			case "DuraMap" -> CaseIssueSource.DURAMAP;
			case "Neuroreader" -> CaseIssueSource.NEUROREADER;
			default -> null;
		};

		if (selectedStatus == ThirdPartyStatus.SENT) {
			promptSentDateChoice(label, record, selectedStatus);
		} else {
			try {
				applyThirdPartyStatus(
						record,
						label,
						selectedStatus,
						null,
						null
				);

				if (issueSource != null) {
					caseIssueService.resolveActiveIssueBySourceAndType(
							record,
							issueSource,
							CaseIssueType.VENDOR_FAILURE,
							currentUserService.getUsername(),
							label + " status changed from Error to "
									+ formatEnum(selectedStatus) + "."
					);
				}

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
				resolveVendorIssueAfterStatusChange(
						record,
						label,
						selectedStatus
				);
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
				resolveVendorIssueAfterStatusChange(
						record,
						label,
						selectedStatus
				);
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

	//updating vendor specific error method - updated 08192026
	private void promptVendorIssue(
			CaseRecordEntity record,
			String vendorLabel,
			CaseIssueSource source,
			String existingNote,
			Consumer<String> onSave
	) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle(vendorLabel + " Error");
		dialog.setWidth("600px");

		ComboBox<CaseIssueType> issueType =
				new ComboBox<>("Issue Category");

		issueType.setItems(
				CaseIssueType.VENDOR_FAILURE,
				CaseIssueType.MISSING_DATA,
				CaseIssueType.IMAGE_QUALITY,
				CaseIssueType.QA_ARTIFACT,
				CaseIssueType.OTHER
		);

		issueType.setValue(CaseIssueType.VENDOR_FAILURE);
		issueType.setItemLabelGenerator(this::formatEnum);
		issueType.setWidthFull();

		TextArea note = new TextArea("Error Note");
		note.setWidthFull();
		note.setMinHeight("160px");
		note.setValue(existingNote != null ? existingNote : "");

		Button cancelButton =
				new Button("Cancel", e -> dialog.close());

		Button saveButton = new Button("Save");
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		saveButton.addClickListener(event -> {
			if (issueType.getValue() == null) {
				showError("Issue category is required.");
				return;
			}

			if (note.getValue() == null
					|| note.getValue().trim().isEmpty()) {
				showError("Error note is required.");
				return;
			}

			onSave.accept(note.getValue().trim());

			createOrUpdateVendorIssue(
					record,
					source,
					issueType.getValue(),
					note.getValue().trim()
			);

			dialog.close();
		});

		VerticalLayout content = new VerticalLayout(
				issueType,
				note
		);
		content.setPadding(false);
		content.setSpacing(true);

		dialog.add(content);
		dialog.getFooter().add(cancelButton, saveButton);
		dialog.open();
	}

	//added in for third party to auto update caseissue instead of only writing the legacy vendor note - updated again 08192026 to add in issuetypes
	private void createOrUpdateVendorIssue(
			CaseRecordEntity record,
			CaseIssueSource source,
			CaseIssueType issueType,
			String note
	) {
		var existingIssue = caseIssueService.findActiveByCaseRecord(record).stream()
				.filter(issue -> issue.getIssueSource() == source)
				.findFirst();

		if (existingIssue.isPresent()) {
			caseIssueService.updateIssue(
					existingIssue.get(),
					source,
					issueType,
					false,
					formatEnum(source) + " Vendor Failure",
					note,
					currentUserService.getUsername()
			);
		} else {
			caseIssueService.createIssue(
					record,
					source,
					issueType,
					false,
					formatEnum(source) + " Vendor Failure",
					note,
					currentUserService.getUsername()
			);
		}
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

	private void resolveVendorIssueAfterStatusChange(
			CaseRecordEntity record,
			String label,
			ThirdPartyStatus selectedStatus
	) {
		CaseIssueSource source = switch (label) {
			case "IMEKA" -> CaseIssueSource.IMEKA;
			case "DuraMap" -> CaseIssueSource.DURAMAP;
			case "Neuroreader" -> CaseIssueSource.NEUROREADER;
			default -> null;
		};

		if (source == null) {
			return;
		}

		caseIssueService.resolveActiveIssueBySourceAndType(
				record,
				source,
				CaseIssueType.VENDOR_FAILURE,
				currentUserService.getUsername(),
				label + " status changed from Error to "
						+ formatEnum(selectedStatus) + "."
		);
	}

	private void openActiveIssuesDialog(CaseRecordEntity record) {
		var activeIssues = caseIssueService.findActiveByCaseRecord(record);

		if (activeIssues.isEmpty()) {
			return;
		}

		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("Active Issues");
		dialog.setWidth("600px");

		VerticalLayout issues = new VerticalLayout();
		issues.setPadding(false);
		issues.setSpacing(true);

		activeIssues.forEach(issue -> {
			Button issueButton = new Button(
					issue.getTitle()
							+ (Boolean.TRUE.equals(issue.getBlocking())
							? " - Blocking"
							: "")
			);

			issueButton.setWidthFull();

			issueButton.addClickListener(event -> {
				dialog.close();

				new CaseIssueDialog(
						record,
						issue,
						caseIssueService,
						currentUserService,
						this::refreshProcessingGrid
				).open();
			});

			issues.add(issueButton);
		});

		Button close = new Button("Close", event -> dialog.close());

		dialog.add(issues);
		dialog.getFooter().add(close);
		dialog.open();
	}
}
