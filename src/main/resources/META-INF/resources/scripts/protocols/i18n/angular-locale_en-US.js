/*
 * XNAT http://www.xnat.org
 * Copyright (c) 2016, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Author: Justin Cleveland <clevelandj@wustl.edu> (jcleve01)
 */

function initializeContent(s, $sce){
	s.display = {
		protocolOptions: true,
		armSupport: false,
		nonVisitDataCollectionSupport: false
	};
	
  s.labels = {};
	
	s.labels.title = {
		manageProtocols: "Manage Protocols",
		editProtocols: "Edit Protocol",
		existingProtocols: "Existing Protocols",
		createNewProtocol: "Create A New Protocol"
	};
  s.labels.protocolsTable = {
		noExistingProtocols: "No saved protocols.",
		header: {
			name: "Name",
			lastUpdated: "Last Updated",
			version: "Version"
		}
	};
	s.labels.newProtocol = {
		directions: $sce.trustAsHtml("<p>If your study protocol has not yet been transferred to XNAT, you can create it using this wizard. You will be prompted to define a series of scheduled events, and determine which data is expected in each event.</p><p>For more documentation and guidance in creating an XNAT-formatted study protocol, visit our <b><a href=\"https://wiki.xnat.org/pages/viewpage.action?pageId=21037137\">Visits and Protocols Documentation</a>.</b></p>"),
		startButton: "Start New Protocol",
		importButton: "Import Protocol JSON File"
	};
	
  s.labels.protocolEditSettings = "EDIT SETTINGS";
  s.labels.protocolEditUsers = "EDIT USERS";
  s.labels.protocolViewProjects = "VIEW PROJECTS";
  s.labels.protocolNotifications = "NOTIFICATIONS";
  s.labels.protocolOptionsTitle = "PROTOCOL SCHEDULE OPTIONS";
  s.labels.protocolOptionsCollapse = "COLLAPSE";
	s.labels.protocolOptionsExpand = "EXPAND";
	s.labels.protocolOptionsDisplay = s.labels.protocolOptionsCollapse;
	
	s.labels.protocolOptions = {
		allowUnexpectedExperiments: "Allow Unexpected Experiments in Visits",
		allowUnexpectedAdHocVisits: "Allow Unexpected (Ad Hoc) Visits Other Than Terminal Visits",
		allowMultipleOpenVisits: "Allow Data Entry Into Multiple Open Visits",
		allowExceptions: "Allow Deviations for Missing Visit Data",
		enableNonVisitDataCollection: "Enable Non-Visit Data Collection",
		enableMultipleArms: "Enable Multiple Arms Support in this Protocol"
	};
	
  s.labels.resetDefaults = "Reset Defaults";
  
	s.labels.help = "Help";
	s.labels.add = "Add";
	s.labels.edit = "Edit";
	s.labels.open = "Open";
	s.labels.expected = "Expected";
	s.labels.protocol = "Protocol";
	s.labels.arm = "Arm";
	s.labels.arms = "Arms";
	s.labels.experiment = "Experiment";
	s.labels.type = "Type";
	s.labels.subtype = "Subtype";
	s.labels.assessor = "Assessor";
	s.labels.experiment = "Experiment";
	s.labels.experiments = "Experiments";
	s.labels.visit = "Visit";
	s.labels.visits = "Visits";
	s.labels.addAllExperimentsRow = "ADD ALL EXPERIMENTS";
	s.labels.addFirstVisit = s.labels.add+" Your First "+s.labels.visit;
	s.labels.addFirstExperiment = s.labels.add+" Your First "+s.labels.experiment;
	s.labels.addNewExperimentsBubble = $sce.trustAsHtml("Click below to create<br>your first experiment<br>to add to visits.");
	s.labels.addNewVisitBubble = $sce.trustAsHtml("Define your protocol by<br>creating visits. Click<br>above to get started!");
	s.labels.unsaved = "UNSAVED";
	s.labels.required = "Required";
	
	s.labels.tableFooterHeading = "VISIT INTERVALS";
	s.labels.tableFooterOffset = "Offset";
	s.labels.tableFooterWindow = "Window";
	
  s.labels.deleteProtocol = "Delete Protocol";
  s.labels.exportProtocol = "Export Protocol";
  s.labels.updateProtocol = {action:"Save Protocol", save:"Save Protocol", update:"Update Protocol"};
	s.labels.importing = "Importing...";
	s.labels.validating = "Validating...";
	s.labels.importAction = s.labels.importing;
	s.labels.bytes = "Bytes";
	s.labels.lastModified = "Last Modified";
  s.labels.protocols = {};
	s.labels.protocols.name = "PROTOCOL NAME"
	s.labels.protocols.uniqueName = "(MUST BE UNIQUE)";
	s.labels.protocols.required = "REQUIRED";
  s.labels.protocols.description = "DESCRIPTION";
  s.labels.protocols.versionDescription = "VERSION DESCRIPTION";
  s.labels.protocols.protocolEditors = "PROTOCOL EDITORS: USER LIST";
  s.labels.protocols.addEditor = "Add";
  s.labels.protocols.removeEditor = "Remove";
  s.labels.protocols.dataEntrySettings = "DATA ENTRY SETTINGS";
  s.labels.protocols.ongoingExperiments = "ONGOING EXPERIMENTS";
  s.labels.visitTypes = {};
  s.labels.visitTypes.definedVisitTypes = "DEFINED VISIT TYPES";
  s.labels.visitTypes.visitTypeProperties = "VISIT TYPE PROPERTIES";
  s.labels.visitTypes.emptyProperties = "SELECT OR CREATE A VISIT TYPE TO EDIT";
  s.labels.visitTypes.createVisitType = "Create "+s.labels.visit+" "+s.labels.type;
  s.labels.visitTypes.visitTypeName = "Name of "+s.labels.visit+" "+s.labels.type+" (Required)";
  s.labels.visitTypes.initial = "This can be an Initial "+s.labels.visit;
  s.labels.visitTypes.followUp = "This can be a Follow Up "+s.labels.visit;
  s.labels.visitTypes.terminal = "This can be a Terminal "+s.labels.visit;
	
	s.labels.visits = {}
	s.labels.visits.createVisit = "OPEN VISIT";
	s.labels.visits.reopenVisit = "REOPEN VISIT";
	s.labels.visits.closeVisit = "CLOSE VISIT";
	s.labels.visits.editVisit = "EDIT VISIT";
	s.labels.visits.deleteVisit = "DELETE VISIT";
	s.labels.visits.createVisitDeviation = "CREATE VISIT DEVIATION";
	s.labels.visits.createAdHocVisit = "CREATE AD HOC VISIT";
	s.labels.visits.createTerminalVisit = "CREATE TERMINAL VISIT";
	s.labels.visits.reassignArm = "REASSIGN SUBJECT ARM";
	s.labels.visits.viewProtocolSchedule = "VIEW PROTOCOL SCHEDULE";
	s.labels.visits.viewAsExperimentList = "VIEW AS EXPERIMENT LIST";
	
	s.labels.project = {}
	s.labels.project.selectProtocol = "SELECT PROTOCOL";
	s.labels.project.version = "VERSION";
	s.labels.project.versionDescription = "VERSION DESCRIPTION";
	s.labels.project.setProtocol = "SET PROTOCOL";
	s.labels.project.sendVisitReportNotificiation = "SEND VISIT REPORT NOTIFICATION";
	
  
  s.dialog = {};
	s.dialog.defaults = {
		height: 180,
		width: 400,
		buttons: {
			okay: {
				label: "Okay",
				isDefault: true,
				close: true,
			}
		},
		errors: {
			labels: {
				dialogObjectNotFound: {
					title: "Dialog Object Not Found",
					description: $sce.trustAsHtml("Dialog Object Not Found")
				}
			}
		}
	};
	s.dialog.newProtocol = {
		labels: {
			title: "New Protocol",
			header: "Create a New Protocol",
		}				
	};
	s.dialog.savedProtocol = {
		id: 'savedProtocol',
		height: 150,
		width: 350,
		labels: {
			title: "Protocol Saved",
			header: "saved successfully",
		}				
	};
	s.dialog.editProtocolSettings = {
		src: XNAT.url.buildUrl('scripts/protocols/templates/editProtocolSettings.html'),
		height: 330,
		width: 400,
		buttons: {
			okay: {
				label: "Okay",
				isDefault: true,
				close: false
			}
		},
		labels: {
			title: "Settings",
			header: "Edit Protocol Settings",
		}				
	};
	s.dialog.editProtocolUsers = {
		height: 420,
		width: 600,
		labels: {
			title: "Users",
			header: "Edit Protocol Users",
			directions: $sce.trustAsHtml("Specify who can make changes to or delete this protocol.<br><b>Note:</b> You cannot remove yourself as a protocol owner."),
			protocolOwners: "PROTOCOL OWNERS",
			availableUsers: "ADD USER(S) TO PROTOCOL",
			removeUser: "Remove",
			addUser: "Add",
			selectAll: "Select All",
			unselectAll: "Unselect All",
            userListAccessDeniedWarning: "Non-admin users on this site do not have access to the user list, so you cannot add new users to this protocol."
		}				
	};
	s.dialog.viewProtocolProjects = {
		height: 450,
		width: 600,
		labels: {
			title: "View Projects",
			header: "Projects using this protocol",
			directions: $sce.trustAsHtml("As projects are assigned to this protocol, they will be listed here, along with which version of the protocol is in usage. You can select a project in this list and update it to the latest version of this protocol, for any project that you have ownership permissions."),
			projectHeader: "Project",
			versionHeader: "Protocol Version",
			modifiedHeader: "Last Modified",
			updateProtocolVersionButton: "Update Protocol Version",
			applyProtocolToProjectsButton: "Apply Protocol To Projects",
			removeProtocolFromProjectsButton: "Remove Protocol From Projects",
			noAssociatedProjects: "There are currently no projects this protocol is associated with.",
			selectProjectToRemove: "Select project checkboxes above to remove them."
		}				
	};
	s.dialog.selectProjects = {
		height: 380,
		width: 370,
		labels: {
			title: "Select Projects",
			directions: $sce.trustAsHtml("Select projects to apply this protocol to."),
			projectHeader: "Project"
		}				
	};
	s.dialog.editProtocolNotifications = {
		height: 400,
		width: 600,
		labels: {
			title: "Notifications",
			header: "Set Notification Defaults",
			directions: $sce.trustAsHtml("These notification settings will be applied by default for any project. However, each project can override these settings if desired by the project owner."),
			protocolEvent: "Protocol Event",
			notifyInHeader: "Notify In Header",
			notifyInEmail: "Notify In Email",
			visitApproachingWindow: "Visit Approaching Window",
			visitInWindowNotOpen: "Visit In Window, Not Open",
			overdueMissedVisit: "Overdue / Missed Visit",
			exceptionEntered: "Deviation Entered",
			defaultNotificationEmails: "DEFAULT NOTIFICATION EMAIL(S)",
			defaultNotificationEmailsTip: $sce.trustAsHtml("<project-owner> ...or a comma-separated list of email addresses."),
			invalidEmails: "The following notification email addresse(s) are invalid"
		}				
	};
	s.dialog.editExperiment = {
		height: 345,
		width: 650,
		buttons: {
			okay: {
				label: "Okay",
				isDefault: true,
				close: false
			}
		},
		labels: {
			title: s.labels.experiment,
			type: {
				field: "EXPERIMENT",
				directions: $sce.trustAsHtml("Select an experiment type from the dropdown."),
				validation: { required: true,
					error: "An experiment data type is required."
				}
			},
			subtype: {
				field: "SUBTYPE (OPTIONAL)",
				directions: $sce.trustAsHtml("This type of experiment might appear twice in a visit. You may want to add a subtype label to differentiate this instance of this experiment.")
			},
			imageAssessors: {
				field: "IMAGE ASSESSORS",
				directions: $sce.trustAsHtml("Click here to optionally add image assessors."),
				button: "Add Assessor"
			}
		}				
	};
	s.dialog.editVisitType = {
		height: 465,
		width: 730,
		buttons: {
			okay: {
				label: "Okay",
				isDefault: true,
				close: false
			}
		},
		labels: {
			title: s.labels.visit+" Type",
			header: s.labels.visit + " Name and Schedule",
			firstHeader: "Define Baseline " + s.labels.visit,
			sortOrder: {
				field: "ORDER",
				directions: $sce.trustAsHtml("Specify the order in which this visit will occur.")
			},
			name: {
				field: "VISIT LABEL",
				directions: $sce.trustAsHtml("Specify the visit name."),
				validation: { required: true,
					error: "A visit type label is required."
				}
			},
			description: {
				field: "VISIT DESCRIPTION",
				directions: $sce.trustAsHtml("Description of the visit.")
			},
			delta: {
				field: "VISIT OFFSET",
				directions: $sce.trustAsHtml("Specify the number of days since the subject's first visit that this visit should occur."),
				validation: { required: true,
					error: "A visit offset is required.",
				}
			},
			window: {
				field: "VISIT WINDOW",
				directions: $sce.trustAsHtml("Specify the number of days before and after the visit offset above. Visits that fall outside of this window will trigger \"subject out of protocol\" notifications.")
			},
			copyFromVisit: {
				field: "COPY EXPERIMENTS FROM VISIT",
				directions: $sce.trustAsHtml("Select an optional existing visit to copy required experiments from.")
			},
			deltaUnits: "days"
		}
	};
	s.dialog.singleArmWarning = {
		labels: {
			title: "Switch to single arm?",
			header: "Warning!",
			description: $sce.trustAsHtml("This will delete all arms that have been created except for the default one!")
		}
	};
	s.dialog.editArm = {
		height: 445,
		width: 730,
		labels: {
			title: s.labels.arm,
			header: "Define Protocol " + s.labels.arm,
			sortOrder: {
				field: "ORDER",
				directions: $sce.trustAsHtml("Specify the order in which this arm will appear.")
			},
			name: {
				field: "NAME",
				directions: $sce.trustAsHtml("Specify the name.")
			},
			description: {
				field: "DESCRIPTION",
				directions: $sce.trustAsHtml("Description of the arm.")
			},
			copyFromArm: {
				field: "COPY VISITS FROM EXISTING ARM",
				directions: $sce.trustAsHtml("Select an optional existing arm to copy visits from.")
			}
		}
	};
	s.dialog.missingDataTypes = {
		src: XNAT.url.buildUrl('scripts/protocols/templates/missingDataTypeWarning.html'),
		height: 300,
		width: 450,
		labels: {
			title: "Missing Data Types!",
			header: "Warning the following data types referenced in this protocol cannot be found in the system:"
		}
	};
	s.dialog.missingUsers = {
		height: 280,
		width: 400,
		labels: {
			title: "Missing Users!",
			header: "Warning the following user(s) referenced in this protocol's white list cannot be found in the system and will be removed:"
		}
	};
	s.dialog.preexisitingTerminalVisit = {
//		width: 360,
		labels: {
			title: "Preexisiting Terminal Visit",
			description: $sce.trustAsHtml("A terminal visit has already been recorded for this subject.")
		}
	};
	s.dialog.preexisitingOpenVisits = {
		width: 415,
		height: 200,
		labels: {
			title: "Multiple open visits not allowed",
			description: $sce.trustAsHtml("Multiple open visits are not allowed by the project protocol.<br><br>You must close any open visits before opening this visit.")
		}
	};
	s.dialog.adHocPreexisitingOpenVisits = {
		width: 415,
		labels: {
			title: "Multiple open visits not allowed",
			description: $sce.trustAsHtml("Multiple open visits are not allowed by the project protocol.<br><br>You must close any open visits before creating an ad hoc visit.")
		}
	};
	s.dialog.openVisit = {
		height: 355,
		width: 500,
		buttons: {
			okay: {
				label: s.labels.open+" "+s.labels.visit,
				isDefault: true,
				close: false
			}
		},
		labels: {
			title: s.labels.visit,
			date: {
				field: "VISIT DATE",
				directions: $sce.trustAsHtml("Specify the date of the subject's visit."),
				validation: { required: true,
					error: "A visit date is required.",
				}
			},
			offset: {
				field: "VISIT OFFSET",
				directions: $sce.trustAsHtml("Calculated from subject's baseline.")
			},
			terminal: {
				field: "MAKE THIS A TERMINAL VISIT",
				directions: $sce.trustAsHtml("If checked, this will be the subject's final visit.")
			},
			lastVisitTerminalWarning: "This is a Terminal Visit. It will be the last data entered for this subject.",
			deltaUnits: "days"
		}
	};
	s.dialog.editVisit = {
		id: "openVisit",
		height: 355,
		width: 500,
		buttons: {
			okay: {
				label: s.labels.edit+" "+s.labels.visit,
				isDefault: true,
				close: false
			}
		},
		labels: {
			title: s.labels.visit,
			date: {
				field: "VISIT DATE",
				directions: $sce.trustAsHtml("Specify the date of the subject's visit."),
				validation: { required: true,
					error: "A visit date is required.",
				}
			},
			offset: {
				field: "VISIT OFFSET",
				directions: $sce.trustAsHtml("Calculated from subject's baseline.")
			},
			terminal: {
				field: "MAKE THIS A TERMINAL VISIT",
				directions: $sce.trustAsHtml("If checked, this will be the subject's final visit.")
			},
			lastVisitTerminalWarning: "This is a Terminal Visit. It will be the last data entered for this subject.",
			deltaUnits: "days"
		}
	};
	s.dialog.adHocSubjectVisit = {
		height: 400,
		width: 600,
		buttons: {
			okay: {
				label: "Open Ad Hoc "+s.labels.visit,
				isDefault: true,
				close: true
			}
		},
		labels: {
			title: "Ad Hoc "+s.labels.visit,
			header: "Define Ad Hoc "+s.labels.visit,
			description: $sce.trustAsHtml("An ad hoc visit can be created to account for any data that is collected out of protocol or out of cycle. Any data type can be added, and there is not a defined window."),
			name: {
				field: "VISIT LABEL",
				directions: $sce.trustAsHtml("Specify the visit name.")
			},
			date: {
				field: "VISIT DATE",
				directions: $sce.trustAsHtml("Specify the date of the visit.")
			},
			offset: {
				field: "VISIT OFFSET",
				directions: $sce.trustAsHtml("Calculated from subject's baseline.")
			},
			terminal: {
				field: "MAKE THIS A TERMINAL VISIT",
				directions: $sce.trustAsHtml("If checked, this will be the subject's final visit.")
			},
			terminalWarning: "You have marked this as a Terminal Visit. This will be the last data entered for this subject.",
			deltaUnits: "days"
		}
	};
	
	s.dialog.confirm = {
		defaults: {
			buttons: {
				ok: {
					label: 'Yes',
					isDefault: true,
					close: true
				},
				cancel: {
					label: 'No',
					isDefault: false,
					close: true
				}
			}
		},
		protocolSaved: {
			id: "protocolSaved",
			height: 170,
			width: 360,
			labels: {
				title: "Unsaved "+s.labels.protocol+"?",
				question: $sce.trustAsHtml("The currently loaded protocol has not been saved.<br/><br/>Do you wish to save your changes?")
			}
		},
		deleteProtocol: {
			id: "deleteProtocol",
			height: 170,
			width: 360,
			labels: {
				title: "Delete "+s.labels.protocol+"?",
				question: $sce.trustAsHtml("Are you sure you wish to delete this protocol<br><br><b>All associated projects will loose their constraints.</b>")
			}
		},
		deleteExperiment: {
			id: "deleteExperiment",
			height: 210,
			width: 330,
			labels: {
				title: "Delete "+s.labels.experiment+"?",
				question: $sce.trustAsHtml("Are you sure you wish to delete this experiment and any of its assessors?<br><br><b>It will be removed from all associated visits.</b>")
			}
		},
		deleteVisit: {
			id: "deleteVisit",
			height: 150,
			width: 290,
			labels: {
				title: "Delete "+s.labels.visit+"?",
				question: $sce.trustAsHtml("Are you sure you wish to delete this visit?")
			}
		},
		removeAllExperimentsFromVisit: {
			id: "removeAllExperimentsFromVisit",
			height: 155,
			width: 280,
			labels: {
				title: "Remove All "+s.labels.experiments+" From "+s.labels.visit+"?",
				question: $sce.trustAsHtml("Are you sure you wish to remove all experiments from this visit?")
			}
		},
		unassociateProjects: {
			id: 'unassociateProjects',
			src: 'template.dialog.confirm.unassociateProjects',
			height: 300,
			width: 440,
			labels: {
				title: "Confirm Protocol Association Removal",
				question: $sce.trustAsHtml("Are you sure?"),
				directions: $sce.trustAsHtml("This protocol will no longer be associated with the following projects:"),
			}
		},
		unusedExperiments: {
			id: 'unusedExperiments',
			src: 'template.dialog.confirm.unusedExperiments',
			height: 300,
			width: 440,
			buttons: {
				ok: {
					label: "Save Anyway",
					isDefault: true,
					close: true
				},
				cancel: {
					label: "Continue Editing Protocol",
					isDefault: false,
					close: true
				}
			},
			labels: {
				title: "Unassociated Experiments?",
				question: "Protocol has unassociated experiments",
				directions: $sce.trustAsHtml("The following are experiments that have been explicitly added but not been required under any of the defined visit types and will not be included in the protocol unless checked:")
			}
		},
		removeExperimentDeviation:{
			id: 'removeExperimentDeviation',
			src: 'template.dialog.confirm.removeExperimentDeviation',
			height: 250,
			width: 450,
			buttons: {
				ok: {
					label: "Remove Deviation",
					isDefault: true,
					close: true
				},
				cancel: {
					label: "Cancel",
					isDefault: false,
					close: true
				}
			},
			labels: {
				title: "Remove Experiment Deviation?",
				question: "Are you sure you wish to remove the following deviation?",
				directions: {
					visit: s.labels.visit,
					experiment: s.labels.experiment,
					subtype: s.labels.subtype,
					assessor: s.labels.assessor
				}
			}
		}
	}
  
	s.defaultHelpTemplate = 'template.help';
  s.helpInfo = {};
  s.helpInfo.allowUnexpectedExperiments = {
		id: 'allowUnexpectedExperimentsHelp',
		labels: {
			title:'Allow Unexpected Experiments',
			content: $sce.trustAsHtml('<p>Your protocol will allow for unexpected experiments to be added to any of the defined visits if this option is checked as opposed to just the required experiments. These experiments will not show up in expected visit reports or project summary dashboards however.</p>')
		}
	};
  s.helpInfo.allowUnexpectedAdHocVisits = {
		id: 'allowUnexpectedAdHocVisitsHelp',
		labels: {
			title:'Allow Unexpected (Ad Hoc) Visits',
			content: $sce.trustAsHtml('<p>Your protocol will allow for ad hoc or unscheduled visits if this option is checked. Such events will also show up in visit reports and project dashboards. An ad hoc visit can allow any valid data type.</p><p>Notes:<br><ul>'+
			'<li>"Allow unexpected experiments" must be true.</li>'+
			'<li>Unexpected Terminal Visits (ie. Subject withdraws from study, or passes away) will always be allowed.</li>'+
			'</ul></p>')
		}
	};
  s.helpInfo.allowMultipleOpenVisits = {
		id: 'allowMultipleOpenVisitsHelp',
		labels: {
			title:'Allow Multiple Open Visits',
			content: $sce.trustAsHtml('<p>Your protocol will allow for more than one visit to be open at a time. If this is not checked the default behavior requires that the current visit be closed out after all required experiments have been satisfied or deviations have been supplied in their place before the next visit or a new ad hoc visit can be opened.</p>')
		}
	};
  s.helpInfo.allowExceptions = {
		id: 'allowExceptionsHelp',
		labels: {
			title:'Allow Deviations for Missing Visit Data',
			content: $sce.trustAsHtml('<p>Your protocol will allow deviations for otherwise required missing experiments or assessors. Entering a deviation will require the user to input a deviation explanation which is stored with the visit for future reference and will show up on project reports and dashboards. If this is not checked the default behavior requires that all expected experiments on a visit be fulfilled before the visit can be closed and the subject can remain within the protocol.</p>')
		}
	};
  s.helpInfo.enableNonVisitDataCollection = {
		id: 'enableNonVisitDataCollectionHelp',
		labels: {
			title:'Enable Non-Visit Data Collection',
			content: $sce.trustAsHtml('Help content for <b>Enable Non-Visit Data Collection</b>.')
		}
	};
  s.helpInfo.enableMultipleArms = {
		id: 'enableMultipleArmsHelp',
		height: 450,
		width: 650,
		labels: {
			title:'Enable Multiple Arms',
			content: $sce.trustAsHtml('Help content for <b>Enable Multiple Arms</b>.')
		}
	};  
};