/*
 * protocols: src/main/resources/META-INF/resources/scripts/protocols/project.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

var projectProtocolController = angular.module('projectProtocol', ['ngResource', 'ui.bootstrap', 'protocol.services', 'protocol.directives'])
.controller('ProjectProtocolController', function($scope, $compile, $timeout, $sce, $location, Protocol, Visit, VisitReport, Projects, User){
	$scope.protocol = XNAT.data.page.protocol;
	ngxModal.$scope = $scope;
	ngxModal.$compile = $compile;
  initializeContent($scope, $sce);
//	initializeDataTypes($scope);
//	initializeExperiments($scope);

//$scope.debug = true;
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
});
