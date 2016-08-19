/*
 * XNAT http://www.xnat.org
 * Copyright (c) 2016, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Author: Justin Cleveland <clevelandj@wustl.edu> (jcleve01)
 */

var editProtocolModule = angular.module('editProtocol', ['ngResource', 'ui.bootstrap', 'protocol.services', 'protocol.directives'])
.controller('EditProtocolController', function($scope, $compile, $timeout, $sce, Protocol, ProjectsUsingProtocol, Projects, User){
	$scope.ProtocolService = Protocol;
	ngxModal.$scope = $scope;
	ngxModal.$compile = $compile;

  $scope.showHelpLink = function($event){
    var helpInfo = $scope.helpInfo[$event.target.parentElement.parentElement.id];
    if(helpInfo){
			ngxModal.help(helpInfo, null);
    } else {
			console.error('Help content not defined: '+$event.target.parentElement.parentElement.id);
		}
  };
	
	$scope.expandCollapseProtocolOptions = function(){
		if($scope.display.protocolOptions){
			$scope.labels.protocolOptionsDisplay = $scope.labels.protocolOptionsExpand;
			$scope.display.protocolOptions = false;
		} else {
			$scope.labels.protocolOptionsDisplay = $scope.labels.protocolOptionsCollapse;
			$scope.display.protocolOptions = true;
		}
	};

	/***********************************************/
	/**** Protocol Version Selection Operations ****/
	/***********************************************/
	$scope.populateRevisions = function(){
		$scope.revisions = [];
		var maxVersion = $scope.protocol.version;
		if($scope.pWrapper && $scope.pWrapper.maxVersion){
			maxVersion = $scope.pWrapper.maxVersion;
		}
		for(var i=maxVersion; i>0; i--){
			$scope.revisions.push(i);
		}
		$scope.currentVersion = $scope.protocol.version;
	};
	$scope.selectRevision = function(){
		if($scope.currentVersion != $scope.protocol.version){
			// Confirm current changes have been saved before switching to another version of this protocol
			if($scope.confirmProtocolSaved($scope.actuallySelectRevision)){
				$scope.actuallySelectRevision();
			}
		}
	};
	$scope.actuallySelectRevision = function(){
		initializeProtocol($scope, Protocol, $scope.currentVersion);
	};

	/**************************************************/
	/**** Protocol Name and Description Operations ****/
	/**************************************************/
  $scope.editProtocolSettings = function($event) {
		$scope.cachedProtocol = $.extend({}, $scope.protocol);
		ngxModal.dialog('editProtocolSettings', validateNameAndDescription, 'edit');
  };

	/********************************************/
	/**** Protocol User Whitelist Operations ****/
	/********************************************/
  $scope.editProtocolUsers = function($event){
		$scope.dialog.editProtocolUsers.labels.selectAllOwners = $scope.dialog.editProtocolUsers.labels.selectAll;
		$scope.dialog.editProtocolUsers.labels.selectAllUsers = $scope.dialog.editProtocolUsers.labels.selectAll;
		$scope.cachedUserWhiteList = [];
		if(!$scope.protocol.userWhiteList){
			$scope.protocol.userWhiteList = [];
		}
		for(var i=0; i<$scope.protocol.userWhiteList.length; i++){
			$scope.cachedUserWhiteList.push({"login": $scope.protocol.userWhiteList[i]});
		}
      if(showUserList) {
          User.query(function (data) {
              $scope.users = data.Result;
              $scope.usersNotAlreadyOwners = [];
              $($scope.users).each(function (i, user) {
                  var notAlreadyAnOwner = true;
                  if ($scope.cachedUserWhiteList) {
                      $($scope.cachedUserWhiteList).each(function (j, powner) {
                          if (user.login == powner.login) {
                              notAlreadyAnOwner = false;
                          }
                      });
                      if (notAlreadyAnOwner) {
                          $scope.usersNotAlreadyOwners.push(user);
                      }
                  }
              });
          }, errorMessage);
      }
      else{
          $scope.userListAccessDenied = true;
      }

		var xm = ngxModal.dialog('editProtocolUsers', function(){
			$scope.protocol.userWhiteList = [];
			for(var i=0; i<$scope.cachedUserWhiteList.length; i++){
				$scope.protocol.userWhiteList.push($scope.cachedUserWhiteList[i].login);
			}
			xmodal.close(xm.$modal);
			delete $scope.cachedUserWhiteList;
		}, 'edit');
  };
	$scope.selectUser = function($event, element){
		if($event.target.checked){
			element.user.checked = true;
		} else {
			element.user.checked = false;
		}
	};
	$scope.removeUser = function($event){
		var whitelist = [];
		$($scope.cachedUserWhiteList).each(function(i,owner){
			// TODO: Prevent current user from removing themselves or main admin from client side even if it's being prevented on server?
			if(!owner.checked){
				whitelist.push(owner);
			} else {
				$scope.usersNotAlreadyOwners.push(owner);
			}
		});
		$scope.cachedUserWhiteList = whitelist;
	};
	$scope.addUser = function($event){
		var userlist = [];
		$($scope.usersNotAlreadyOwners).each(function(i,user){
			if(user.checked){
				$scope.cachedUserWhiteList.push({'login':user.login, 'checked': true});
			} else {
				userlist.push(user);
			}
		});
		$scope.usersNotAlreadyOwners = userlist;
	};
	$scope.checkForMissingUsers = function() {
        $scope.missingUsers = [];
            if(showUserList) {
                User.query(function (data) {
                    $scope.users = data.Result;
                    $($scope.protocol.userWhiteList).each(function (i, user) {
                        var found = findUser(user, $scope.users);
                        if (!found) {
                            $scope.missingUsers.push(user);
                        }
                    });
                    if ($scope.missingUsers.length > 0) {
                        var xm = ngxModal.dialog('missingUsers', function () {
                            $scope.removeUsers($scope.missingUsers);
                            xmodal.close(xm.$modal);
                        });
                    }
                }, errorMessage);
            }
            else{
                $scope.userListAccessDenied = true;
            }
	};
	$scope.removeUsers = function(usersToRemove) {
		$(usersToRemove).each(function(i, user){
			var found = $scope.protocol.userWhiteList.indexOf(user);
			if(found >= 0){
				$scope.protocol.userWhiteList.splice(found, 1);
			}
		});
	};
	$scope.selectAllUsers = function($event) {
		var label, list;
		if($event.target.id == "selectAllOwners"){
			label = $scope.dialog.editProtocolUsers.labels.selectAllOwners;
			list = $($scope.cachedUserWhiteList);
		} else {
			label = $scope.dialog.editProtocolUsers.labels.selectAllUsers;
			list = $($scope.usersNotAlreadyOwners);
		}
		if(label == $scope.dialog.editProtocolUsers.labels.selectAll){
			if($event.target.id == "selectAllOwners"){
				$scope.dialog.editProtocolUsers.labels.selectAllOwners = $scope.dialog.editProtocolUsers.labels.unselectAll;
			} else {
				$scope.dialog.editProtocolUsers.labels.selectAllUsers = $scope.dialog.editProtocolUsers.labels.unselectAll;
			}
			list.each(function(i, user){
				user.checked = true;
			});
		} else {
			if($event.target.id == "selectAllOwners"){
				$scope.dialog.editProtocolUsers.labels.selectAllOwners = $scope.dialog.editProtocolUsers.labels.selectAll;
			} else {
				$scope.dialog.editProtocolUsers.labels.selectAllUsers = $scope.dialog.editProtocolUsers.labels.selectAll;
			}
			list.each(function(i, user){
				user.checked = false;
			});
		}
	};

	/*************************************************/
	/**** Project Protocol Association Operations ****/
	/*************************************************/
  $scope.viewProtocolProjects = function($event){
		if($scope.pid != undefined){
			ProjectsUsingProtocol.query({id: $scope.pid}, function(data){
				$scope.projectsUsing = data.Result;
				$scope.refreshSafeProjectNames();
			}, errorMessage);
		}
    var xm = ngxModal.dialog('viewProtocolProjects');
  };
	$scope.projectLink = projectUrl; // referenced in href on project dialog
	$scope.selectProject = function($event, element){
		$scope.selectProjectToRemove = false;
		if($event.target.checked){
			element.project.checked = true;
		} else {
			element.project.checked = false;
		}
	};
	$scope.checkAllProjects = function($event, projects){
		$scope.selectProjectToRemove = false;
		if($event.target.checked){
			$(projects).each(function(i,p){
				p.checked = true;
			});
		} else {
			$(projects).each(function(i,p){
				p.checked = false;
			});
		}
	};
	$scope.updateProtocolVersion = function($event){
		$($scope.projectsUsing).each(function(i,pUsing){
			if(pUsing.checked){
//					var pid = pUsing.ID;
//					if(!pid){
//						pid = pUsing.id
//					}
					Projects.update({
							id: pUsing.projectId,
							protocol: $scope.protocol.protocolId,
							revision: $scope.protocol.version
						}, function(data){
							ProjectsUsingProtocol.query({id: $scope.pid}, function(data){
								$scope.projectsUsing = data.Result;
								$scope.refreshSafeProjectNames();
							}, errorMessage);
					}, errorMessage);
			}
		});
  };
	$scope.applyProtocolToProjects = function($event){
		Projects.query(function(data){
			$scope.allProjects = data.Result;
			// Compare with $scope.projectsUsing and only add
			// projects not already associated from this list
			$scope.unassociatedProjects = [];
			$($scope.allProjects).each(function(i,p){
				var unassociated = true;
				$($scope.projectsUsing).each(function(j,u){
					if(p.ID == u.projectId){
						unassociated = false;
					}
				});
				if(unassociated){					
					p.name = $sce.trustAsHtml(p.name);
					$scope.unassociatedProjects.push(p);
				}
			});
		}, errorMessage);
    var xm = ngxModal.dialog('selectProjects', function(){
			$($scope.unassociatedProjects).each(function(i,project){
				if(project.checked){
					var pid = project.ID;
					if(!pid){
						pid = project.id
					}
					
					// TODO: here we really should guard against reassigning a project to this protocol that already has a protocol assigned to it.
					
					Projects.update({
							id: pid,
							protocol: $scope.protocol.protocolId,
							revision: $scope.protocol.version
						}, function(data){
							ProjectsUsingProtocol.query({id: $scope.pid}, function(data){
								$scope.projectsUsing = data.Result;
								$scope.refreshSafeProjectNames();
							}, errorMessage);
					}, errorMessage);
				}
			});
			xmodal.close(xm.$modal);
		}, null);
  };
	
	$scope.removeProtocolFromProjects = function($event){
		$scope.projectsToRemove = [];
		$($scope.projectsUsing).each(function(i,project){
			if(project.checked){
				$scope.projectsToRemove.push(project.projectName);
			}
		});
		if($scope.projectsToRemove.length){
			var xm = ngxModal.confirm($scope.dialog.confirm.unassociateProjects, function(){
				$($scope.projectsUsing).each(function(i,project){
					if(project.checked){
						console.log("Removing Protocol Association: "+project.projectId);
						Projects.delete({
								id: project.projectId,
								protocol: $scope.protocol.protocolId
							}, function(data){
								ProjectsUsingProtocol.query({id: $scope.pid}, function(data){
									$scope.projectsUsing = data.Result;
									$scope.refreshSafeProjectNames();
								}, errorMessage);
						}, errorMessage);
					}
				});
				xmodal.close(xm.$modal);
			}, null);
		} else {
			$scope.selectProjectToRemove = true;
		}
	};
	$scope.refreshSafeProjectNames = function(){
		$($scope.projectsUsing).each(function(i,p){
			p.projectName = $sce.trustAsHtml(p.projectName);
		});
	}
	
  $scope.editProtocolNotifications = function($event){
		if(!$scope.protocol.defaultNotificationEmails){
			$scope.protocol.defaultNotificationEmails = [];
		}
		if(!$scope.protocol.headerNotifications){
			$scope.protocol.headerNotifications = [];
		}
		if(!$scope.protocol.emailNotifications){
			$scope.protocol.emailNotifications = [];
		}
		$scope.cachedNotifications = {
			headerNotifications: [], emailNotifications: [],
			defaultNotificationEmails: $scope.protocol.defaultNotificationEmails.join(', ')
		}
		for(var i=0; i<$scope.protocol.headerNotifications.length; i++){
			$scope.cachedNotifications.headerNotifications[$scope.protocol.headerNotifications[i]] = true;
		}
		for(var i=0; i<$scope.protocol.emailNotifications.length; i++){
			$scope.cachedNotifications.emailNotifications[$scope.protocol.emailNotifications[i]] = true;
		}
    var xm = ngxModal.dialog('editProtocolNotifications', function(){
			var emailArray = $scope.cachedNotifications.defaultNotificationEmails
			if(emailArray){
				$scope.dialog.editProtocolNotifications.invalidEmails = [];
				emailArray = emailArray.split(',');
				for(var i=0; i<emailArray.length; i++){
					emailArray[i] = emailArray[i].trim();
					if(!validEmailFormat(emailArray[i])){
						$scope.dialog.editProtocolNotifications.invalidEmails.push(emailArray[i]);
					}
				}
				if($scope.dialog.editProtocolNotifications.invalidEmails.length > 0){
					$scope.$apply();
					return;
				}
				$scope.protocol.defaultNotificationEmails = emailArray
			}
			$scope.protocol.headerNotifications = [];
			for(var n in $scope.cachedNotifications.headerNotifications) {
				if($scope.cachedNotifications.headerNotifications[n] == true){
					$scope.protocol.headerNotifications.push(n);
				}
			}
			$scope.protocol.emailNotifications = [];
			for(var n in $scope.cachedNotifications.emailNotifications) {
				if($scope.cachedNotifications.emailNotifications[n] == true){
					$scope.protocol.emailNotifications.push(n);
				}
			}
			xmodal.close(xm.$modal);
			delete $scope.cachedNotifications;
		}, 'edit');
  };

	/************************/
	/**** Arm Operations ****/
	/************************/
	$scope.addArm = function($event){
		ngxModal.dialog('editArm', null, 'add');
  };

	$scope.editArm = function($event){
		ngxModal.dialog('editArm', null, 'edit');
  };
	
	/*******************************/
	/**** Experiment Operations ****/
	/*******************************/
	$scope.experimentList = [];
	$scope.refreshExperimentList = function(){
		// Flattens the experiments array and assessor sub-objects into a displayable array
		$scope.experimentList = [];
		for(var i=0; i<$scope.experiments.length; i++){
			$scope.experimentList.push($scope.experiments[i]);
			if($scope.experiments[i].expectedAssessors){
				for(var j=0; j<$scope.experiments[i].expectedAssessors.length; j++){
					if($scope.experiments[i].expectedAssessors[j]){
						if(!$scope.experiments[i].expectedAssessors[j].parentExperiment){
							$scope.experiments[i].expectedAssessors[j].parentExperiment = $scope.experiments[i]
						}
						$scope.experimentList.push($scope.experiments[i].expectedAssessors[j]);
					}
				}
			}
		}
	};
	
  $scope.addExperiment = function(){
		$scope.cachedExperiment = {};
		if(!$scope.experiments){
			initializeExperiments($scope);
		}
		var xm = ngxModal.dialog('editExperiment', function(){
			$scope.errors = $scope.validateExperiment($scope.cachedExperiment);
			if(!$scope.errors){
				var experiment = $scope.cachedExperiment;
				// Validation: First check for an existing experiment with this exact subtype/assessors etc...
				var found = findExperiment(experiment, $scope.experiments);
				if(!found){
					$scope.validateAndAddAssessors(experiment.expectedAssessors, experiment);
					$scope.experiments.push(experiment);
				} else {
					$scope.validateAndAddAssessors(experiment.expectedAssessors, found.experiment, found.experiment.expectedAssessors);
				}
				$scope.refreshExperimentList();
				xmodal.close(xm.$modal);
				delete $scope.cachedExperiment;
			}
			$scope.$apply();
		}, 'add');
  };

	$scope.editExperiment = function(origExp){
    $scope.cachedExperiment = angular.copy(origExp);
    var xm = ngxModal.dialog('editExperiment',  function(){
			$scope.errors = $scope.validateExperiment($scope.cachedExperiment);
			if(!$scope.errors){
				var found = findExperiment($scope.cachedExperiment, $scope.experiments);
				if(!found){ // User is attempting to change the type or subtype of an existing experiment
					// We need to make the change on all instances of expected experiments under each VisitType currently checked
					// But first! Deal with assessor changes before the expected experiment type/subtype changes...
					$scope.validateAndAddAssessors($scope.cachedExperiment.expectedAssessors, origExp);
					$scope.updateExperimentsAndAssessorsForAllVisitTypes(origExp, $scope.cachedExperiment);
					// Make the change to the master list of experiments
					origExp.type = $scope.cachedExperiment.type;
					origExp.subtype = $scope.cachedExperiment.subtype;	
				} else {
					$scope.validateAndAddAssessors($scope.cachedExperiment.expectedAssessors, found.experiment);
				}
				$scope.refreshExperimentList();
				xmodal.close(xm.$modal);
				delete $scope.cachedExperiment;
			}
			$scope.$apply();
		}, 'edit');
		$timeout(function(){
			resizeExperimentDialog($scope.cachedExperiment.expectedAssessors.length, 140);
		}, 10, true);
  };
	
	$scope.validateExperiment = function(experiment){
		var errors = validateRequiredFields($scope, experiment, $scope.dialog.editExperiment.labels);
		if(!errors){
			// More complex validation of business logic specific to the data object goes here
//			return truthy value or an error object if there are more validation errors;
		}
		return errors;
	};

	// Actually add the assessors to the supplied experiment (if they don't already exist)
	$scope.validateAndAddAssessors = function(assessors, experiment, assessorsToKeep){
		if(!assessorsToKeep){
			assessorsToKeep = [];
		}
		$(assessors).each(function(i, assessor){
			if(assessor){
				if(typeof assessor == 'string'){
					assessor = {type:assessor, subtype:null};
				}
				if(assessor.type){
					if(!assessor.parentExperiment){
						assessor.parentExperiment = experiment;
					}
					var found = findExperiment(assessor, assessorsToKeep); // Prevent duplicate assessors???
					if(!found){
						assessorsToKeep.push(assessor);
					}
				}
			}
		});
		// Remove any corresponding experiments under the current protocol's VisitType's expected experiments
		// that aren't on the assessorsToKeep list
		$(experiment.expectedAssessors).each(function(i, expAss){
			if(expAss){
				var found = findExperiment(expAss, assessorsToKeep);
				if(!found){
					$scope.removeExperimentsOrAssessorsFromAllVisitTypes(experiment, expAss);
				}
			}
		});
		experiment.expectedAssessors = assessorsToKeep;
	};

	// Updates the expected experiments matching origExperiment for all VisitTypes in the current protocol
	// with the newExperiment info: type, subtype and expectedAssessor list.
	// Also verifies each assessor's parent experiment is appropriately updated.
	$scope.updateExperimentsAndAssessorsForAllVisitTypes = function(origExperiment, newExperiment){
		if(origExperiment && newExperiment){
			$($scope.protocol.visitTypes).each(function(i, vt){
				var vtExp = findExperiment(origExperiment, vt.expectedExperiments);
				if(vtExp){
					// Make the change to the VisitType's expected experiment
					vtExp.experiment.type = newExperiment.type;
					vtExp.experiment.subtype = newExperiment.subtype;
					$(vtExp.experiment.expectedAssessors).each(function(i, expAss){
						expAss.parentExperiment = vtExp.experiment;
					});
					// We do NOT want to automatically add all new assessors to VisitTypes with this experiment checked	$scope.validateAndAddAssessors(newExperiment.expectedAssessors, vtExp.experiment);
				}
			});
		}
	};
	
	// Removes the specified experiment from all VisitTypes in the current protocol if ONLY an experiment is provided.
  // If an assessor is also passed in, ONLY assessors belonging to the specified experiment will be removed
	// from all VisitTypes, the experiment will not be removed
	$scope.removeExperimentsOrAssessorsFromAllVisitTypes = function(experiment, assessor){
		if(experiment){
			$($scope.protocol.visitTypes).each(function(i, vt){
				var vtExp = findExperiment(experiment, vt.expectedExperiments);
				if(vtExp){
					if(assessor){
						var vtAss = findExperiment(assessor, vtExp.experiment.expectedAssessors);
						if(vtAss){
							vtExp.experiment.expectedAssessors.splice(vtAss.index, 1);
						}
					} else {
						vt.expectedExperiments.splice(vtExp.index, 1);
					}
				}
			});
		}
	};
	
	// Add assessor dropdown to experiment dialog
	$scope.addAssessorToDialog = function(){
		if(!$scope.cachedExperiment.expectedAssessors){
			$scope.cachedExperiment.expectedAssessors = [];
		}
		$scope.cachedExperiment.expectedAssessors.push({"type":"", "subtype":""});
		$timeout(function(){
			resizeExperimentDialog($scope.cachedExperiment.expectedAssessors.length, 140);
		}, 10, true);
	};
	
	// Remove assessor dropdown from experiment dialog
	$scope.removeAssessorFromDialog = function(i){
		$scope.cachedExperiment.expectedAssessors.splice(i, 1);
		resizeExperimentDialog($scope.cachedExperiment.expectedAssessors.length, 140);
	};

	$scope.deleteExperiment = function(experiment){
		ngxModal.confirm($scope.dialog.confirm.deleteExperiment, function(){
			// If it's an assessor row look for it's parent and delete any assessors under it
			if(experiment.parentExperiment){
				$scope.removeExperimentsOrAssessorsFromAllVisitTypes(experiment.parentExperiment, experiment);
			} else {
				$scope.removeExperimentsOrAssessorsFromAllVisitTypes(experiment);
			}
			// Remove the experiment from the master list (or it's parent experiment if it's an assessor row)
			// ...and refresh the experimentList display
			if(experiment.parentExperiment){
				var foundParent = findExperiment(experiment.parentExperiment, $scope.experiments);
				if(foundParent){
					var foundAss = findExperiment(experiment, foundParent.experiment.expectedAssessors);
					if(foundAss){
						foundParent.experiment.expectedAssessors.splice(foundAss.index, 1);
					}
				}
			} else {
				var found = findExperiment(experiment, $scope.experiments);
				if(found){
					$scope.experiments.splice(found.index, 1);
				}
			}
			$scope.refreshExperimentList();
			$scope.$apply();
		}, null, null);
  };
	
	/******************************/
	/**** VisitType Operations ****/
	/******************************/
  $scope.addVisit = function($event){
		$scope.cachedVisit = {expectedExperiments:[]};
		if(!$scope.protocol.visitTypes || $scope.protocol.visitTypes.length == 0){
			$scope.cachedVisit.sortOrder = 0;
			$scope.cachedVisit.delta = 0;
			$scope.cachedVisit.deltaHigh = 0;
			$scope.cachedVisit.deltaLow = 0;
		}
		$scope.copySettings = {};
		$scope.visitTypeOptions = [];
		$($scope.protocol.visitTypes).each(function(i, visit){
			$scope.visitTypeOptions.push({name:visit.name, value:''+i});
		});
		var xm = ngxModal.dialog('editVisitType', function(){
			if(!$scope.protocol.visitTypes){
				$scope.protocol.visitTypes = [];
				$scope.initializeSingleArmProtocol();
			}
			$scope.errors = $scope.validateVisit($scope.cachedVisit);
			if(!$scope.errors){
				$scope.copyExperimentsFromVisit();
				$scope.protocol.visitTypes.splice($scope.validateVisitSortOrder(), 0, $scope.cachedVisit);
				if($scope.newProtocol){
					$timeout(function(){$scope.addNewExperimentsBubble = 'visible-bubble';}, 1000, true);
					$timeout(function(){$scope.addNewExperimentsBubble = 'hidden-bubble';}, 7000, true);
					$timeout(function(){$scope.addNewExperimentsBubble = 'hidden';}, 10000, true);
				}
				xmodal.close();	// just close all modals because we were doing the following and it was not getting closed.
				// xmodal.close(xm.$modal);  ...somewhere perhaps we're losing the CORRECT reference to xm??? 
				delete $scope.cachedVisit;
			}
			$scope.$apply();
		}, 'add');
		$scope.addNewVisitBubble = 'hidden-bubble';
  };
	$scope.editVisitType = function(i){
		$scope.executeModalHideHack();
		$scope.cachedVisit = angular.copy($scope.protocol.visitTypes[i]);
		$scope.validateVisitSortOrder(i);
    var xm = ngxModal.dialog('editVisitType', function(){
			$scope.errors = $scope.validateVisit($scope.cachedVisit);
			if(!$scope.errors){
				var newOrder = $scope.validateVisitSortOrder($scope.cachedVisit.sortOrder);
				// Reposition visit index in array with...
				if($scope.protocol.visitTypes[i].sortOrder != newOrder) {
					$scope.protocol.visitTypes.splice(i, 1);
					$scope.protocol.visitTypes.splice(newOrder, 0, $scope.cachedVisit);
				} else {
					$scope.protocol.visitTypes[i] = $scope.cachedVisit;
				}
				xmodal.close(xm.$modal);
				delete $scope.cachedVisit;
			}
			$scope.$apply();
		}, 'edit');
  };
	$scope.validateVisit = function(visit){
		var errors = validateRequiredFields($scope, visit, $scope.dialog.editVisitType.labels);
		visit.delta = makeNumeric(visit.delta);					// Sometimes weird keystrokes get through
		visit.deltaLow = makeNumeric(visit.deltaLow);		// Apparently this is necessary
		visit.deltaHigh = makeNumeric(visit.deltaHigh);
		if(!errors){
			// More complex validation of business logic specific to the data object goes here
			// move validateVisitSortOrder() calls here!
//			return truthy value or an error object if there are more validation errors;
		}
		return errors;
	};
	$scope.validateVisitSortOrder = function(i){
		if(!(typeof $scope.cachedVisit.sortOrder === 'number')) {
			if(typeof $scope.cachedVisit.sortOrder === 'string') {
				try {
					$scope.cachedVisit.sortOrder = parseInt($scope.cachedVisit.sortOrder);
				} catch (e) {}
			}
		}
		if(typeof $scope.cachedVisit.sortOrder === 'undefined'){
			if(i) {
				$scope.cachedVisit.sortOrder = i;
			} else {
				$scope.cachedVisit.sortOrder = $scope.protocol.visitTypes.length;
			}
		}
		// Ensure any user entered value is between 0 and $scope.protocol.visitTypes.length;
		if($scope.cachedVisit.sortOrder < 0){
			$scope.cachedVisit.sortOrder = 0;
		} else if($scope.cachedVisit.sortOrder > $scope.protocol.visitTypes.length) {
			$scope.cachedVisit.sortOrder = $scope.protocol.visitTypes.length
		}
		return $scope.cachedVisit.sortOrder;
	};
	$scope.copyExperimentsFromVisit = function(){
		if($scope.copySettings.fromVisit){
			var visitToCopyFrom = $scope.protocol.visitTypes[$scope.copySettings.fromVisit];
			$(visitToCopyFrom.expectedExperiments).each(function(i, experiment){
				var assessors = [];
				$(experiment.expectedAssessors).each(function(i, assessor){
					assessors.push({
						type:assessor.type,
						subtype:assessor.subtype
					});
				});
				$scope.cachedVisit.expectedExperiments.push({
					type:experiment.type,
					subtype:experiment.subtype,
					expectedAssessors:assessors
				});
			});
// TODO: Copy other visit options??
		}
	};
	$scope.deleteVisit = function(i){
		$scope.executeModalHideHack();
		ngxModal.confirm($scope.dialog.confirm.deleteVisit, function(){
			$scope.protocol.visitTypes.splice(i, 1);
			$scope.$apply();
		}, null, null);
  };
	
	$scope.executeModalHideHack = function(){
		$scope.modalHideHack = true;
		$timeout(function(){
			$timeout(function(){
				$scope.modalHideHack = false; // set it back
			}, 100, true); // invokeApply = true
		}, 0, true); // invokeApply = true
	}

	/*************************************************/
	/**** VisitType/Experiment Mapping Operations ****/
	/*************************************************/
	$scope.areAllExperimentsChecked = function(element){
//		if(element.visitType.expectedExperiments.contains(element.$parent.experiment)){
//			return true;
//		}
		return false;
	};
	
	$scope.isExpVisitChecked = function(element){
		var vtExps = element.visitType.expectedExperiments;
		var availableExp = element.$parent.experiment;
		for(var i=0; i < vtExps.length; i++){
			if(vtExps[i].type == availableExp.type
			&& vtExps[i].subtype == availableExp.subtype){
				return true;
			}
			var rowParent = findExperiment(availableExp.parentExperiment, $scope.experiments);
			if(rowParent){
				var found = findExperiment(availableExp, vtExps[i].expectedAssessors);
				if(found &&
					rowParent.experiment.type == vtExps[i].type &&
					rowParent.experiment.subtype == vtExps[i].subtype){
					return true;
				}
			}
		}
		return false;
	};
	
	$scope.mapExpToVisit = function($event, element){
		var vtExps = element.visitType.expectedExperiments;
		var exp = element.$parent.experiment;
		if($event.target.checked){
			$scope.addExpToVisitType(exp, vtExps);
		} else {	// unchecked
			$scope.removeExpFromVisitType(exp, vtExps);
		}
	};
	
	$scope.addAllExpsToVisit = function($event, element){
		var vtExps = element.visitType.expectedExperiments;
		if($event.target.checked){
			$($scope.experiments).each(function(i,exp){
				$scope.addExpToVisitType(exp, vtExps);
			});
		} else {
			ngxModal.confirm($scope.dialog.confirm.removeAllExperimentsFromVisit, function(){
				vtExps = [];
				$scope.$apply();
			}, null, null);
		}
//		console.log("VisitType Id: "+$event.currentTarget.dataset.visitType);
	};

	$scope.addExpToVisitType = function(exp, vtExps){
		if(exp.parentExperiment){
			// Then it's an assessor row and we need to make sure it is pushed onto
			// this visitType's experiment list if it's not already on there
			var found = findExperiment(exp.parentExperiment, vtExps);
			var expAss = {
				type: exp.type,
				subtype: exp.subtype,
				required: true
			};
			if(!found){
				vtExps.push({
					type: exp.parentExperiment.type,
					subtype: exp.parentExperiment.subtype,
					required: true,
					expectedAssessors: [expAss]
				});
			} else {
				if(!found.experiment.expectedAssessors){
					found.experiment.expectedAssessors = [];
				}
				var foundAss = findExperiment(exp, found.experiment.expectedAssessors);
				if(!foundAss){
					found.experiment.expectedAssessors.push(expAss);
				}
			}
		} else {
			var found = findExperiment(exp, vtExps);
			if(!found){
				vtExps.push({
					type: exp.type,
					subtype: exp.subtype,
					required: true
				});
			}
		}
	};
	
	$scope.removeExpFromVisitType = function(exp, vtExps){
		if(exp.parentExperiment){
			// Then it's an assessor row
			var found = findExperiment(exp.parentExperiment, vtExps);
			if(found){ // And we only need to remove it from it's parent's expectedAssessors array
				var foundAss = findExperiment(exp, found.experiment.expectedAssessors);
				if(foundAss){
					found.experiment.expectedAssessors.splice(foundAss.index, 1);
				}
			}
		} else {
			var found = findExperiment(exp, vtExps);
			if(found){
				vtExps.splice(found.index, 1);
			}
		}
	};
  
	/*****************************/
	/**** Protocol Operations ****/
	/*****************************/
  $scope.deleteProtocol = function(){
		ngxModal.confirm($scope.dialog.confirm.deleteProtocol, function(){
			$scope.pWrapper.$delete(
				{id: $scope.pid},
				function() {
					window.onbeforeunload = angular.noop;
					window.location = XNAT.url.buildUrl('app/template/ManageProtocol.vm');
				}
			);
		});
  };

	$scope.exportProtocol = function(){
		if($scope.confirmProtocolSaved($scope.exportLocal)){
			$scope.exportFromServer();
		}
	};
	
	$scope.exportFiles = document.getElementById('exportFiles');
	$scope.exportFromServer = function(){
		var params = '?'+noCache();
		if($scope.protocol.version){
			params = '?version='+$scope.protocol.version+'&'+noCache();
		}
		params += '&export=true'
		var getProtocolUrl = protocolUrl+'/'+$scope.protocol.protocolId+params;
		$($scope.exportFiles).attr('href', getProtocolUrl);
		if(safari){
			$($scope.exportFiles).attr('target', '_blank');
			var evObj = document.createEvent('MouseEvents');
			evObj.initMouseEvent('click', true, true, window);
			$scope.exportFiles.dispatchEvent(evObj);
		} else {
			$scope.exportFiles.click();
		}
	};
	
	$scope.exportLocal = function(){
		var protocolToExport = removeParentExperiments($scope.protocol);
		// Remove all ids in the protocol to prevent reimporting this protocol over top of an existing one later
		// ...or overwriting child element foreign keys
		cleanIds(protocolToExport);
		var jsonProtocol = angular.toJson(protocolToExport);
		$($scope.exportFiles).attr('download', decodeURIComponent($scope.protocol.name)+'_'+$scope.labels.unsaved+'.json').attr('href', 'data:text/plain,'+jsonProtocol);
		if(safari){
			$($scope.exportFiles).attr('target', '_blank');
			var evObj = document.createEvent('MouseEvents');
			evObj.initMouseEvent('click', true, true, window);
			$scope.exportFiles.dispatchEvent(evObj);
		} else {
			$scope.exportFiles.click();
		}
	};

	$scope.checkForUnusedExperiments = function(){
		// TODO: search through all protocol arms as well!
		var unusedExperiments;
		var allAssociatedExps = [];
		if($scope.protocol.visitTypes){
			for(var i=0; i<$scope.protocol.visitTypes.length; i++){
				var ees = $scope.protocol.visitTypes[i].expectedExperiments;
				for(var j=0; j<ees.length; j++){
					allAssociatedExps.push(ees[j]);
				}
			}
			for(var i=0; i<$scope.experiments.length; i++){
				var exp = $scope.experiments[i];
				if(!exp.parentExperiment){
					var found = findExperiment(exp, allAssociatedExps);
					if(!found){
						if(!unusedExperiments){
							unusedExperiments = [];
						}
						if($scope.newProtocol){
							var found = findExperiment(exp, $scope.defaultExperiments);
							if(!found){
								unusedExperiments.push(exp);
							}
						} else {
							unusedExperiments.push(exp);
						}
					}
				}
			}
			if(unusedExperiments && unusedExperiments.length == 0){
				unusedExperiments = null;
			}
		}
		return unusedExperiments;
	};
	
  $scope.updateProtocol = function(){
    if($scope.pWrapper){
			if($scope.validateProtocol()){
				if($scope.isProtocolDirty()){
					$scope.actuallyUpdateProtocol();
				}
			}
    } else {
console.log('pWrapper is '+pWrapper);
    }
  };
	
	$scope.validateProtocol = function(){
		
		// TODO: add other higher priority protocol validation here...
		
		$scope.unusedExperiments = $scope.checkForUnusedExperiments();
		if($scope.unusedExperiments){
			ngxModal.confirm($scope.dialog.confirm.unusedExperiments, $scope.actuallyUpdateProtocol, null /*...continue editing protocol to resolve unused experiments*/);
			return false;
		}
		return true;
	};

	$scope.actuallyUpdateProtocol = function(){
		$scope.pWrapper.protocol = removeParentExperiments($scope.protocol);
		$scope.pWrapper.$update(
			{id: $scope.pid},
			function() {
				$scope.protocol = $scope.pWrapper.protocol;
				// Save a serialized copy for comparing initial settings with the
				// working protocol object so we can revert back if necessary
				$scope.defaultProtocol = angular.toJson($scope.protocol);
				var refreshNewProtocol = function(){
					// If parameters.protocolName exists then this was a brand new protocol and we need to
					// ...rewrite the URL with protocol ID so user does not come back to an empty protocol upon refreshing the page
					if(parameters.protocolName){
						window.location = XNAT.url.buildUrl('app/template/EditProtocol.vm?protocolId='+$scope.protocol.protocolId);
					}
				};
				$scope.populateRevisions();
				var xm = xmodal.confirm({
					title: $scope.dialog.savedProtocol.labels.title,
					content: '<ng-include id="'+$scope.dialog.savedProtocol.id+'" src="\'template.dialog.'+$scope.dialog.savedProtocol.id+'\'"></ng-include>',
					height: $scope.dialog.savedProtocol.height,
					width: $scope.dialog.savedProtocol.width,
					buttons: {
						ok: {
							label: 'Ok',
							isDefault: true,
							close: true,
							action: refreshNewProtocol
						},
						cancel: {
							label: 'Update Projects',
							isDefault: false,
							close: true,
							action: $scope.viewProtocolProjects  // TODO: should call refreshNewProtocol here too!
						}
					}
				});
				$compile($('#'+$scope.dialog.savedProtocol.id).parent().parent().parent())($scope);
			}
		);
	};

	/**********************************************/
	/**** Default Settings and Initializations ****/
	/**********************************************/
	$scope.protocolOptionsContent = 'template.protocolOptions';
  initializeContent($scope, $sce);
	initializeProtocol($scope, Protocol);

	$scope.defaultExperiments = [
	  {
      "type":"xnat:mrSessionData",
			"subtype":null,
			"expectedAssessors": [
				{	"type":"xnat:qcAssessmentData",	"subtype":null }
			]
    },
		{
			"type":"xnat:ctSessionData",
			"subtype":null
	  },
	];
	
	$scope.addDefaultExperiments = function(){
		$scope.experiments = angular.copy($scope.defaultExperiments);
		$scope.refreshExperimentList();
	};
	
	$scope.addNewVisitBubble = 'hidden-bubble';
	$scope.addNewExperimentsBubble = 'hidden-bubble';
  if(!$scope.pid){	// Can presume we're working with a brand new protocol
		$scope.newProtocol = true;
		$timeout(function(){$scope.addNewVisitBubble = 'visible-bubble';}, 1000, true);
		$timeout(function(){$scope.addNewVisitBubble = 'hidden-bubble';}, 7000, true);
		$timeout(function(){$scope.addNewVisitBubble = 'hidden';}, 10000, true);
		$scope.addDefaultExperiments();
	} else {
		$scope.addNewVisitBubble = 'hidden';
		$scope.addNewExperimentsBubble = 'hidden';
	}
	
	$scope.initializeSingleArmProtocol = function(){
		if($scope.protocol.arms && $scope.protocol.arms.length > 0){
			ngxModal.dialog('singleArmWarning');
			$scope.protocol.arms = [];
		}
		$scope.visitTypeExperimentContent = 'template.visitTypeExperimentTable';
	};
	$scope.initializeMultiArmProtocol = function(){
		$scope.visitTypeExperimentContent = 'template.multiArmVisitTypeExpirimentTable';
		$scope.armTabContent = ['template.visitTypeExperimentTable'];
		$($scope.protocol.arms).each(function(i, arm){
			$scope.armTabContent[arm.armOrder] = 'template.visitTypeExperimentTable';
		});
	};

  $scope.resetDefaultSettings = function($event){
		var dp = angular.fromJson($scope.defaultProtocol);
		if(dp){
			$scope.protocol.enabled = dp.enabled;
			$scope.protocol.allowUnexpectedExperiments = dp.allowUnexpectedExperiments;
			$scope.protocol.allowUnexpectedAdHocVisits = dp.allowUnexpectedAdHocVisits;
			$scope.protocol.allowMultipleOpenVisits = dp.allowMultipleOpenVisits;
			$scope.protocol.allowExceptions = dp.allowExceptions;
			$scope.protocol.enableNonVisitDataCollection = dp.enableNonVisitDataCollection;
		} else {
			$scope.protocol.enabled = true;
			$scope.protocol.allowUnexpectedExperiments = false;
			$scope.protocol.allowUnexpectedAdHocVisits = false;
			$scope.protocol.allowMultipleOpenVisits = false;
			$scope.protocol.allowExceptions = false;
			$scope.protocol.enableNonVisitDataCollection = false;
		}
  };
	
	/***************************/
	/**** Utility Functions ****/
  /***************************/
	$scope.isProtocolDirty = function(){
		var p = angular.toJson(removeParentExperiments(angular.copy($scope.protocol)));
		if(p == $scope.defaultProtocol){
			return false;
		}
		return true;
	};
	$scope.confirmProtocolSaved = function(cancelFunction) {
		if($scope.isProtocolDirty()){
			ngxModal.confirm($scope.dialog.confirm.protocolSaved, $scope.updateProtocol, function(){
				if(cancelFunction){
					$timeout(cancelFunction, 200, true);
				}
			});
			return false;
		} else {
			return true;
		}
	};
  window.onbeforeunload = function (event) {        
    if($scope.isProtocolDirty()){
      return 'The currently loaded protocol has not been saved.\n\nIf you wish to save your changes, stay on this page.';
    }
  }
	
	$scope.printToConsole = function(){
		if($scope.isProtocolDirty()){
			console.log("\nDirty Protocol!\n\nDefault Protocol:");
			console.log($scope.defaultProtocol);
			console.log("Current Protocol:");
		};
		console.log($scope.protocol);
	};
});
