angular.module('manageProtocols', ['ngResource', 'protocol.services'])
.controller('ProtocolsController', function($scope, $compile, $timeout, Protocol, $sce){
	$scope.ProtocolService = Protocol;
	ngxModal.$scope = $scope;
	ngxModal.$compile = $compile;
	initializeContent($scope, $sce);
  $scope.importProtocol = function(){
		delete $scope.fileImportList;
    $('#importFiles').trigger('click');
  };
  $scope.editProtocol = function(e){
    var pid = e.target.value;
    if(!pid){
      pid = $(e.target).parent()[0].value;
    }
    window.location = xnatUri+'/app/template/EditProtocol.vm?protocolId='+pid;
  };
	$scope.protocolDocumentation = function(){
		window.location = "http://wiki.xnat.org/pages/viewpage.action?pageId=21037137";
	};
	$scope.startNewProtocol = function(){
		$scope.dialog.editProtocolSettings.labels = $scope.dialog.newProtocol.labels;
		$scope.cachedProtocol = $.extend({}, $scope.protocol);
		var xm = ngxModal.dialog('editProtocolSettings', validateNameAndDescription, null);
	};
	$scope.handleFileSelect = function(fileInputElement){
		if(checkFileAPISupport()){
			$scope.fileImportList = [];
			for(var i = 0, f; f = fileInputElement.files[i]; i++) {
				$scope.fileImportList.push(f);
				$scope.$apply();
				$scope.processImportedProtocol(f)
			}
		}
	};
	$scope.processImportedProtocol = function(f){
		var reader = new FileReader();
		reader.onload=(function(file) {
			return function(e){
				var importedProtocol = e.target.result;
				try{
					importedProtocol = JSON.parse(importedProtocol);
					if(importedProtocol.protocol){
						importedProtocol = importedProtocol.protocol;
					}
					loadProtocols($scope, function(){ // Refresh the existing protocol list right before checking for non-unique names in case new ones were added to the system since the page loaded.
						try{
							$scope.incrementProtocolName(importedProtocol);
						} catch (e){
							f.failure = true;
							// TODO: Convert this to and ngxModal and internationalize it...
							// Better yet display error in red below the file listing
//							f.nameRequired = true;
							xModalMessage('ERROR: Invalid Protocol', 'Protocol name is missing in import file.');
							return;
						}
						// Remove all ids in the protocol to prevent reimporting this protocol over top of an existing one later ...or overwriting child element foreign keys
						cleanIds(importedProtocol);
						delete importedProtocol.protocolId;
						$scope.saveProtocol(importedProtocol, function() {
							f.success = true;
							$timeout(function(){
								var found = findFile(f.name, $scope.fileImportList);
								if(found){
									$scope.fileImportList.splice(found.index, 1);
								}
							}, 5000);
							loadProtocols($scope);
						});
					});
				} catch (e) {
					f.failure = true;
					// TODO: Convert this to and ngxModal and internationalize it...
					xModalMessage('ERROR: Invalid JSON', 'Failed to parse protocol file.<br/><br/>'+e.message);
					$scope.$apply();
				}
			};
		})(f);
		reader.readAsText(f);
	};
	$scope.incrementProtocolName = function(p){
		var inc = 1;
		while(!validateUniqueName(p, $scope.protocols)){
			var lastIdx = p.name.indexOf(')');
			if(lastIdx>=0){
				var firstIdx = p.name.indexOf('(');
				if(firstIdx>=0){
					var incStr = p.name.substring(firstIdx+1, lastIdx);
					inc = parseInt(incStr);  // Returns NaN if bad substring ...deal with this
					inc++;
					p.name = p.name.substring(0, firstIdx)+'('+inc+')';
				} else {
					p.name = p.name+' ('+inc+')';
				}
			} else {
				p.name = p.name+' ('+inc+')';
			}
		}
	};
	
	
	$scope.saveProtocol = function(protocol, afterSave) {
		var protocolId = '';
		if(protocol.protocolId){
			protocolId = protocol.protocolId;
		}
		var ppURL = postProtocolUrl + protocolId+'?inbody=true&XNAT_CSRF='+window.csrfToken;
		try{
			YAHOO.util.Connect.asyncRequest('POST', ppURL, {
				success:function(o) {
					try {
						protocolWrapper = JSON.parse(o.responseText);
						protocol=protocolWrapper.protocol;
						if($.isFunction(afterSave)){
							afterSave();
						}
					} catch (e) {
							var message = '';
							if(e.message) {message = e.message};
							// TODO: Convert this to and ngxModal and internationalize it...
							xModalMessage('ERROR: '+o.status, 'Failed to parse protocol.<br/><br/>'+message);
					}
				},
				failure:function(o) {
					$scope.handleProtocolError(o);		// TODO: use common errormessage function instead
				}
			}, JSON.stringify(protocol));
		} catch (e) {
			// TODO: Convert this to and ngxModal and internationalize it...
			xModalMessage('ERROR: Failed to save protocol.', e.message);
		}
	};
	$scope.handleProtocolError = function(err){		// TODO: use common errormessage function instead
		if(err){
			// TODO: Convert this to and ngxModal and internationalize it...
			xModalMessage('ERROR: '+err.status, 'Protocol operation failed.<br/><br/>'+err.statusText+'<br/><br/>'+err.responseText);
		} else {
			// TODO: Convert this to and ngxModal and internationalize it...
			xModalMessage('ERROR', 'Protocol operation failed.');
		}
	};
	
	
	loadProtocols($scope);
});

function handleFileSelect(el){
	var s = angular.element(el).scope();
	if(s){
		s.handleFileSelect(el);
	}
};