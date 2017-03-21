/*
 * protocols: src/main/resources/META-INF/resources/scripts/protocols/
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

var subjectVisitController = angular.module('subjectVisits', ['ngResource', 'ui.bootstrap', 'protocol.services', 'protocol.directives'])
.controller('SubjectVisitController', function($scope, $compile, $timeout, $sce, $location, Protocol, Visit, VisitReport, Projects, User){
	$scope.protocol = XNAT.data.page.protocol;
	$scope.visits = XNAT.data.page.subject.visits;
	ngxModal.$scope = $scope;
	ngxModal.$compile = $compile;
  initializeContent($scope, $sce);
	initializeDataTypes($scope);
	initializeExperiments($scope);
	
	$scope.protocolCompleted = false;
/* TODO:	Check each expected visittype in the protocol object with the actual visits and make sure they're all closed
	for(var v in $scope.visits){
		if($scope.visits[v].closed){
			$scope.protocolCompleted = true;
		}
	}
*/
$scope.debug = true;
	$scope.sendVisitReportNotificiation = function(){
		VisitReport.get(
			{id: XNAT.data.page.projectID, sendNotificationEmail: true}, function(data){
				
				console.log(data);
				
			}
		);
	};
	
  $scope.showHelpLink = function($event){
    var helpInfo = $scope.helpInfo[$event.target.parentElement.parentElement.id];
    if(helpInfo){
			ngxModal.help(helpInfo, null);
    } else {
			console.error('Help content not defined: '+$event.target.parentElement.parentElement.id);
		}
  };

	$scope.createTerminalVisit = function($event){
		// Check protocol for allowance of multiple open visits
		// And for a pre-existing terminal visit
		// (...and if not, make sure others aren't already open)
		if($scope.visits){
			for(var i=0; i<$scope.visits.length; i++){
				if(!$scope.visits[i].closed && !$scope.protocol.allowMultipleOpenVisits){
					ngxModal.dialog('adHocPreexisitingOpenVisits');
					return;
				}
				if($scope.visits[i].terminal){
					ngxModal.dialog('preexisitingTerminalVisit');
					return;
				}
			}
		}
		
		if(!$scope.cachedVisit){
			$scope.cachedVisit = {};
		}
		$scope.cachedVisit.terminal = true;

		var xm = ngxModal.dialog('adHocSubjectVisit', function(){
			Visit.save($scope.cachedVisit, $scope.refreshPage);
            $scope.refreshPage();//This refreshes the page immediately before confirming that the request succeeded. This prevents a long delay of sometimes 15 seconds upon modifying a visit. If we want to later add a check that the request succeeded, we will probably want to remove this line.
		}, 'add');
		$timeout(function(){
			XNAT.app.datePicker.init("#adHocDatePicker");
			$('button.ez_cal').remove();
			$('#cal1-container').css({'top':'80px', 'left':'374px'});
		}, 10, true);
	};

	$scope.createVisit = function(visitType, lastVisit){
		// Check protocol for allowance of multiple open visits
		// (...and if not, make sure others aren't already open)
		if(!$scope.protocol.allowMultipleOpenVisits){
			if($scope.visits){
				for(var i=0; i<$scope.visits.length; i++){
					if(!$scope.visits[i].closed){
						ngxModal.dialog('preexisitingOpenVisits');		// TODO: create this dialog!
						return;
					}
				}
			}
		}
		if(!$scope.cachedVisit){
			$scope.cachedVisit = {};
		}
		$scope.cachedVisit.type = visitType;
		if(lastVisit){
			$scope.cachedVisit.terminal = true;
		}
		
		var xm = ngxModal.dialog('openVisit', function(){
			if(!$scope.cachedVisit.name){
				$scope.cachedVisit.name = ''+$scope.visits.length;
			}
			// The stupid calendar date picker totally messes up the angular binding so we need to reset the date manually
			$scope.cachedVisit.date = $("#adHocDateInput").val();
			$scope.errors = $scope.validateVisit($scope.cachedVisit);
			if(!$scope.errors){
				Visit.save($scope.cachedVisit, $scope.refreshPage);
                $scope.refreshPage();//This refreshes the page immediately before confirming that the request succeeded. This prevents a long delay of sometimes 15 seconds upon modifying a visit. If we want to later add a check that the request succeeded, we will probably want to remove this line.
				xmodal.close(xm.$modal);
				delete $scope.cachedVisit;
			}
			$scope.$apply();
		}, 'open');
		$timeout(function(){
			XNAT.app.datePicker.init("#adHocDatePicker");		// TODO: rename this visitDatePicker
			$("#adHocDatePicker").val($scope.cachedVisit.date);
			$('button.ez_cal').remove();
			$('#cal1-container').css({'top':'25px', 'left':'280px'});
			$("#adHocDateInput").val($('#expectedDate').text());
		}, 10, true);
	};
	$scope.closeVisit = function(visitId){
		console.log('Closing Visit: '+visitId);
		closeVisit(visitId);
	};
	$scope.reopenVisit = function(visitId){
		console.log('Reopening Visit: '+visitId);
		openVisit(visitId);
	};
	$scope.editVisit = function(visitId){
		var found = findVisit(visitId, $scope.visits);
		if(found){
			$scope.cachedVisit = angular.copy(found.visit);
			var xm = ngxModal.dialog('editVisit', function(){
			// The stupid calendar date picker totally messes up the angular binding so we need to reset the date manually
				$scope.cachedVisit.date = $("#adHocDateInput").val();
				$scope.errors = $scope.validateVisit($scope.cachedVisit);
				if(!$scope.errors){
					Visit.save($scope.cachedVisit, $scope.refreshPage);
                    $scope.refreshPage();//This refreshes the page immediately before confirming that the request succeeded. This prevents a long delay of sometimes 15 seconds upon modifying a visit. If we want to later add a check that the request succeeded, we will probably want to remove this line.
					xmodal.close(xm.$modal);
					delete $scope.cachedVisit;
				}
				$scope.$apply();
			}, 'edit');
			$timeout(function(){
				XNAT.app.datePicker.init("#adHocDatePicker");		// TODO: rename this visitDatePicker
				$('button.ez_cal').remove();
				$('#cal1-container').css({'top':'25px', 'left':'280px'});
				var df = new Date($scope.cachedVisit.date);
				var sd = (df.getMonth()+1)+'/'+(df.getDate()+1)+'/'+df.getFullYear();
				$("#adHocDatePicker").val(sd);
				$("#adHocDateInput").val(sd);
			}, 10, true);
		}
	};
	$scope.deleteVisit = function(visitId){
		console.log('Deleting Visit: '+visitId);
		confirmDeleteVisit(visitId);
	};

  $scope.addDeviation = function($event){ // TODO: wrap or convert old visit function.
		$scope.cachedDeviation = {};
		var xm = ngxModal.dialog('editDeviation', function(){
		});
  };

	$scope.removeExperimentDeviation = function(visitId, expType, expIdOrSubType){
		delete $scope.deviation;
		var found = findVisit(visitId, $scope.visits);
		if(found){
			$scope.deviation = {visit: found.visit};
			found = findDataType(expType, $scope.allDataTypes);
			if(found){
				$scope.deviation.experiment = found.dataType;
			}
			if(expIdOrSubType){
				found = findDataType(expType, $scope.assessorTypeOptionsArray);
				if(found){
					$scope.deviation.assessor = found.dataType;
				} else {
					$scope.deviation.subtype = expIdOrSubType;
				}
			}
			ngxModal.confirm($scope.dialog.confirm.removeExperimentDeviation, function(){
				removeExperimentDeviationConfirmed(visitId, expType, expIdOrSubType);
				$scope.$apply();
			}, null, null);
		}
	};
	
	$scope.validateVisit = function(visit){
		var errors = validateRequiredFields($scope, visit, $scope.dialog.openVisit.labels);
		if(!errors){
			// More complex validation of business logic specific to the data object goes here
//			return truthy value or an error object if there are more validation errors;
		}
		return errors;
	};
	$scope.createNewAdHocVisit = function($event){
		// Check protocol for allowance of multiple open visits
		// (...and if not, make sure others aren't already open)
		if(!$scope.protocol.allowMultipleOpenVisits){
			if($scope.visits){
				for(var i=0; i<$scope.visits.length; i++){
					if(!$scope.visits[i].closed){
						ngxModal.dialog('adHocPreexisitingOpenVisits');
						return;
					}
				}
			}
		}
		if(!$scope.cachedVisit){
			$scope.cachedVisit = {};
		}
		var xm = ngxModal.dialog('adHocSubjectVisit', function(){
			Visit.save($scope.cachedVisit, $scope.refreshPage);
            $scope.refreshPage();//This refreshes the page immediately before confirming that the request succeeded. This prevents a long delay of sometimes 15 seconds upon modifying a visit. If we want to later add a check that the request succeeded, we will probably want to remove this line.
		}, 'add');
		$timeout(function(){
			XNAT.app.datePicker.init("#adHocDatePicker");
			$('button.ez_cal').remove();
			$('#cal1-container').css({'top':'25px', 'left':'374px'});
		}, 10, true);
	};
	
	$scope.openCalendar = function($event){
		$('#cal1-container').fadeIn(100);
	};

  $scope.checkProtocolNotifications = function($event){
		if(!$scope.protocol.headerNotifications){
			for(var i=0; i<$scope.protocol.headerNotifications.length; i++){
				$scope.cachedNotifications.headerNotifications[$scope.protocol.headerNotifications[i]] = true;
			}
		}
		// TODO: display warnings in header by default...
		if(true){ // check all notification warnings for this subject
			var xm = ngxModal.dialog('protocolNotification', function(){
				// TODO: build warning dialog once notification restlet is complete
			});
		}
  };

	$scope.moveExperimentToNewArm = function($event){
		ngxModal.dialog($scope.dialog.moveExperimentToNewArm, null, 'add');
  };
	
	$scope.refreshPage = function(){
		window.location = window.location;
	}

	$scope.printToConsole = function(){
		console.log($scope.visits);
		console.log($scope.protocol);
	};
});
