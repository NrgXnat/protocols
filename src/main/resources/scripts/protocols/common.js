function loadProtocols(s, callback){
	s.ProtocolService.query(function(data){
		s.protocols = data;
		if(callback){
			callback();
		}
	}, errorMessage);
};

function initializeProtocol(s, ProtocolService, version){
  var pname = parameters.protocolName;
  var pdesc = parameters.protocolDescription;
	s.protocol = {name:pname, description:pdesc};
  s.pid = parameters.protocolId;
	initializeDataTypes(s);
  if(s.pid){
		var params = {id: s.pid};
		if(version){
			params.version = version;
		}
    var pWrapper = ProtocolService.get(
      params,
      function(){
        s.pWrapper = pWrapper;
        s.protocol = pWrapper.protocol;
        // Save a serialized copy for comparing initial settings with the
				// working protocol object so we can revert back if necessary
        s.defaultProtocol = angular.toJson(s.protocol);
        s.labels.updateProtocol.action = s.labels.updateProtocol.update;
				s.newProtocol = false;
				initializeExperiments(s);
				s.checkForMissingUsers();
				s.populateRevisions();
				if(s.protocol.arms && s.protocol.arms.length > 0){
					s.initializeMultiArmProtocol();
					s.enableMultipleArms = true;
				} else {
					s.initializeSingleArmProtocol();
					s.enableMultipleArms = false;
				}
      },
      errorMessage
    );
  } else {
		s.newProtocol = true;
	}
  s.pWrapper = new ProtocolService();
  s.pWrapper.protocol = s.protocol;
};

function initializeDataTypes(s){
	s.dataTypeOptions = dataTypeOptions;
	s.assessorTypeOptions = assessorTypeOptions;
	s.dataTypeOptionsArray = [], s.assessorTypeOptionsArray = [], s.allDataTypes = [];;
	for (var prop in dataTypeOptions) {
		if(prop){
			var option = {"type": prop, "name": dataTypeOptions[prop]};
			s.dataTypeOptionsArray.push(option);
			s.allDataTypes.push(option);
		}
	}
	for (var prop in assessorTypeOptions) {
		if(prop){
			var option = {"type": prop, "name": assessorTypeOptions[prop]};
			s.assessorTypeOptionsArray.push(option);
			s.allDataTypes.push(option);
		}
	}
};

var missingClass = 'missing-datatype-error';
function initializeExperiments(s){
	s.experiments = [], priExp = [], s.missingDataTypes = [];
	$(s.protocol.visitTypes).each(function(i, vt){
		$(vt.expectedExperiments).each(function(j, expExperiment){
			var exp, found = findExperiment(expExperiment, priExp);
			if(!found){
				var exp = {
					type:expExperiment.type,
					subtype:expExperiment.subtype,
					expectedAssessors:[]
				};
				addAssessors(s, exp, expExperiment);
				var found = findDataType(expExperiment.type, s.dataTypeOptionsArray);
				if(found){
					// To account for experiments with differing subtypes we
					// need to count them up in an array within this array
					if(!priExp[found.index]){
						priExp[found.index] = [];
					}
					var dupSubtype = findExperiment(exp, priExp[found.index]);
					if(!dupSubtype){
						priExp[found.index].push(exp);
					} else {
						addAssessors(s, dupSubtype.experiment, exp);
					}
				} else {
					var dupMissingtype = findExperiment(exp, s.missingDataTypes);
					if(!dupMissingtype){
						exp.missing = missingClass;
						s.missingDataTypes.push(exp);
					}
				}
			} else { // Add any additional assessors that may be on identical expected experiments but for subsequent visit types
				addAssessors(s, found.experiment, expExperiment);
			}
		});
	});
	// Collapse prioritized array (remove empty slots and flatten to 1 dimension)
	for(var i=0; i<priExp.length; i++){
		if(priExp[i]){
			for(var j=0; j<priExp[i].length; j++){
				s.experiments.push(priExp[i][j]);
			}
		}
	};
	// Concatenate prioritized array with missing types 
	s.experiments = s.experiments.concat(s.missingDataTypes);
  if(s.missingDataTypes.length > 0){ // Issue missing data types warning
		var xm = ngxModal.dialog('missingDataTypes');
  }
	if(s.refreshExperimentList){
		s.refreshExperimentList();
	}
};
function addAssessors(s, toExp, fromExp){
	$(fromExp.expectedAssessors).each(function(i, expAssessor){
		var foundAss = findExperiment(expAssessor, toExp.expectedAssessors);
		if(!foundAss){
			var newExpAssessor = {
				type:expAssessor.type,
				subtype:expAssessor.subtype,
				parentExperiment: toExp
			};
			reportMissingAssessors(s, newExpAssessor);
			toExp.expectedAssessors.push(newExpAssessor);
		}
	});
};
function reportMissingAssessors(s, expAssessor){
	var foundAss = findDataType(expAssessor.type, s.assessorTypeOptionsArray);
	if(!foundAss){
		expAssessor.missing = missingClass;
		if(!s.missingAssessors){
			s.missingAssessors = [];
		}
		var foundMissing = findExperiment(expAssessor, s.missingAssessors, true);
		if(!foundMissing){
			s.missingAssessors.push({
				type: expAssessor.type,
				subtype: expAssessor.subtype,
				parentExperiment: expAssessor.parentExperiment,
				missing: missingClass
			});
		}
	}
};

function validateRequiredFields(s, data, labels){
	if(!data || !resetValidationErrors(labels)){
		//ngxModal.dialog('formValidationFailed');
		alert('Form validation failed');
		return false;
	}
	var errors;
	for(var l in labels) {
    if(labels.hasOwnProperty(l)){ // check that this is it's own property not inherited from prototype
			if(labels[l].validation && labels[l].validation.required){
				if(typeof data[l] == 'string'){
					data[l] = data[l].trim();
				}
				// Test a string version of whatever the value is to make sure it's not empty
				if(data[l]==undefined || !(""+data[l])){ // ...this will let 0 pass)
					if(!labels.validationErrors){
						labels.validationErrors = {};
						errors = labels.validationErrors;
					}
					var error = labels[l].validation.error ? labels[l].validation.error : s.labels.required;  // need to access content in scope but don't want to have to pass scope into every damn function
					labels.validationErrors[l] = error;
				}
			}
    }
	}
	return errors;
};

function resetValidationErrors(labels){
	if(!labels){
		//ngxModal.dialog('formValidationFailed');
		alert('Form validation failed: labels object required.');
		return false;
	}
	delete labels.validationErrors;
	return true;
}


/*** Cleanse the supplied protocol object of parent experiment objects on  ***/
/*** assessors used for UI record keeping before serializing the protocol. ***/
function removeParentExperiments(p){
	var clean;
	if(p){
		clean = $.extend({}, p);
		for(var i in clean){
			if(i=='visitTypes'){
				for(var j in clean[i]){
					for(var k in clean[i][j]){
						if(k=='expectedExperiments'){
							for(var l in clean[i][j][k]){
								for(var m in clean[i][j][k][l]){
									if(m=='parentExperiment'){
										delete clean[i][j][k][l][m];
									}
									if(m=='expectedAssessors'){
										for(var n in clean[i][j][k][l][m]){
											for(var o in clean[i][j][k][l][m][n]){
												if(o=='parentExperiment'){
													delete clean[i][j][k][l][m][n][o];
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return clean;
	}
	throw "Invalid Protocol"
}
function cleanIds(p){
	if(p){
		delete p.id;
		$(p.visitTypes).each(function(i,vt){
			delete vt.id;
			$(vt.expectedExperiments).each(function(j,ee){
				delete ee.id;
				$(ee.expectedAssessors).each(function(k,ea){
					delete ea.id;
				});
			});
		});
	}
}

findUser = function(login, arr){
	if(arr){
		for(var i=0; i<arr.length; i++){
			if(arr[i]){
				if(arr[i].login == login){
					return {index:i, user:arr[i]};
				}
			}
		}
	}
	return null;
}
findDataType = function(name, arr){
	if(arr){
		for(var i=0; i<arr.length; i++){
			if(arr[i]){
				if(arr[i].type == name){
					return {index:i, dataType:arr[i]};
				}
			}
		}
	}
	return null;
}
findExperiment = function(exp, arr, matchParent){
	if(arr && exp){
		for(var i=0; i<arr.length; i++){
			if(arr[i]){
				var chk = arr[i];
				if(Array.isArray(chk)){ // This finds the first of duplicate experiments in the priority experiment array above.
					chk = arr[i][0];
				}
				if(chk.type == exp.type
				&& chk.subtype == exp.subtype){
					if(!matchParent){
						return {index:i, experiment:chk};
					} else {
						if(chk.parentExperiment && exp.parentExperiment
						&& chk.parentExperiment.type == exp.parentExperiment.type
						&& chk.parentExperiment.subtype == exp.parentExperiment.subtype){
							return {index:i, experiment:chk};
						}
					}
				}
			}
		}
	}
	return null;
};
findVisit = function(visitId, arr){
	if(arr && visitId){
		for(var i=0; i<arr.length; i++){
			if(arr[i]){
				if(arr[i].id == visitId){
					return {index:i, visit:arr[i]};
				}
			}
		}
	}
	return null;
};
findFile = function(filename, arr){
	if(arr){
		for(var i=0; i<arr.length; i++){
			if(arr[i]){
				if(arr[i].name == filename){
					return {index:i, file:arr[i]};
				}
			}
		}
	}
	return null;
}

function validateNameAndDescription(){
	delete ngxModal.$scope.cachedProtocol.uniqueNameRequired;
	delete ngxModal.$scope.cachedProtocol.nameRequired;
	if(ngxModal.$scope.cachedProtocol.name){
		var pdesc = "";
		if(ngxModal.$scope.cachedProtocol.description){
			pdesc = ngxModal.$scope.cachedProtocol.description.trim();
			pdesc = '&protocolDescription='+pdesc;
		}
		loadProtocols(ngxModal.$scope, function(){
			try{
				if(!validateUniqueName(ngxModal.$scope.cachedProtocol, ngxModal.$scope.protocols)){
					ngxModal.$scope.cachedProtocol.uniqueNameRequired = "validation-error";
					return;
				}
			}	catch (e){
				ngxModal.$scope.cachedProtocol.nameRequired = true;
				return;
			}
			xmodal.close(ngxModal.$modal);
			if(ngxModal.$scope.editProtocolSettings){
				ngxModal.$scope.protocol = ngxModal.$scope.cachedProtocol;
			} else {
				window.location = XNAT.url.buildUrl('/app/template/EditProtocol.vm?protocolName='+ngxModal.$scope.cachedProtocol.name+pdesc);
			}
		});
	} else {
		ngxModal.$scope.cachedProtocol.nameRequired = true;
		ngxModal.$scope.$apply();
	}
}

function validateUniqueName(protocol, existingProtocols){
	if(protocol.name){
		protocol.name = protocol.name.trim();
	}
	if(!protocol.name){
		throw "invalidProtocolName"; // TODO: make this an internationalized error string
	}	
	if(existingProtocols){
		for(var i=0; i<existingProtocols.length; i++){
			if(existingProtocols[i]){
				if(existingProtocols[i].name == protocol.name){
					if(existingProtocols[i].id != protocol.id){
						return false;
					}
				}
			}
		}
	}
	return true;
}

var ngxModal = {
	help: function(dialog, primaryAction) {
		var xm;
		var src = dialog.src ? dialog.src : this.$scope.defaultHelpTemplate;
		var action = primaryAction ? primaryAction : angular.noop;
    xm = xmodal.open({
			title: dialog.labels.title,
			content: '<ng-include id="'+dialog.id+'" src="\''+src+'\'"></ng-include>',
			height: dialog.height ? dialog.height : 300,
			width: dialog.width ? dialog.width : 500,
			buttons: {
				okay: {
					label: 'Okay',
					isDefault: true,
					close: true,
					action: action
				}
			}
		});
		this.$scope.helpInfo.labels = dialog.labels;
		// Compile and bind the scope of this new dialog in Angular
		this.$compile($('#'+dialog.id).parent().parent())(this.$scope);
		return xm;
  },
	dialog: function(dialog, primaryAction, addEdit) {
		var xm, src, buttons;
		if(typeof dialog == 'string'){
			xm = dialog;
			try{
				eval("dialog = ngxModal.$scope.dialog."+xm);
				if(dialog && !dialog.id){
					dialog.id = xm;
				}
			} catch (e) {
				dialog = {
					id:'dialogObjectNotFound',
					labels: ngxModal.$scope.dialog.defaults.errors.labels.dialogObjectNotFound
				};
				dialog.labels.header = xm;
			}
		}
		if(dialog.src) {
			src = dialog.src;
		} else {
			src = 'template.dialog.'+dialog.id;  // if dialog.id == 'undefined'
			var template = $("script[id='"+src+"']");
			if(template.length<1){
				src = XNAT.url.buildUrl('scripts/protocols/templates/defaultDialog.html')
				ngxModal.$scope.dialog.instance = dialog.labels;			// This is a terrible idea because if another dialog fails to set a certain property, previous values may show up.
			}
		}
		var defaultAction = function(){
			ngxModal.$scope.$apply();
			xmodal.close(xm.$modal);
		};
		var action = primaryAction ? primaryAction : defaultAction;
		if(addEdit == 'add'){
			this.$scope.addEdit = this.$scope.labels.add+' ';
		} else if(addEdit == 'edit'){
			this.$scope.addEdit = this.$scope.labels.edit+' ';
		} else if(addEdit == 'open'){
			this.$scope.addEdit = this.$scope.labels.open+' ';
		} else {
			this.$scope.addEdit = '';
		}
		if(dialog.buttons){
			buttons = angular.copy(dialog.buttons); // IMPORTANT: make a deep copy of these default objects so that properties and functions don't stick to the original and affect future usages of it.
		} else {
			buttons = $.extend(true, {}, ngxModal.$scope.dialog.defaults.buttons);
		}
		if(!buttons.okay){
			buttons.okay = {};
		}
		if(!buttons.okay.action){
			buttons.okay.action = action;
		}

    xm = xmodal.open({
			title: this.$scope.addEdit + dialog.labels.title,
			content: '<ng-include id="'+dialog.id+'" src="\''+src+'\'"></ng-include>',
			height: dialog.height ? dialog.height : ngxModal.$scope.dialog.defaults.height,
			width: dialog.width ? dialog.width : ngxModal.$scope.dialog.defaults.width,
			buttons: buttons
		});
		// Compile and bind the scope of this new dialog in Angular
		this.$compile($('#'+dialog.id).parent().parent().parent())(this.$scope);
		applyJQeventHandlers();
		return xm;
  },
	confirm: function(dialog, yesAction, noAction, cancelAction) {
		var xm, buttons, content, justClose = function(){	xmodal.close(xm.$modal); };
		if(dialog.src){
			content = '<ng-include id="'+dialog.id+'" src="\''+dialog.src+'\'"></ng-include>';
		} else {
			content = '<div id="'+dialog.id+'" class="confirm-dialog-content" ng-bind-html="dialog.confirm.'+dialog.id+'.labels.question"></div>';
		}
		if(dialog.buttons){
			buttons = dialog.buttons;
		} else {
			buttons = $.extend(true, {}, ngxModal.$scope.dialog.confirm.defaults.buttons);
		}
		if(!yesAction){ yesAction = justClose;	}
		if(!noAction){ noAction = justClose; }
		if(!cancelAction){ cancelAction = justClose; }
		if(!buttons.ok){
			buttons.okay = {};
		}
		if(!buttons.ok.action){
			buttons.ok.action = yesAction;
		}
/*
		if(!buttons.no){
			buttons.no = {};
		}
		if(!buttons.no.action){
			buttons.no.action = noAction;
		}
*/
		if(!buttons.cancel){
			buttons.cancel = {};
		}
		if(!buttons.cancel.action){
			buttons.cancel.action = noAction;
		}
    xm = xmodal.confirm({
			title: dialog.labels.title,
			content: content,
			height: dialog.height ? dialog.height : 200,
			width: dialog.width ? dialog.width : 400,
			buttons: buttons
		});
		// Compile and bind the scope of this new dialog in Angular
		this.$compile($('#'+dialog.id).parent().parent().parent())(this.$scope);
		return xm;
  }
};

function applyJQeventHandlers() {
	setTimeout(function(){
		$('.numeric').keyup(function () { this.value = makeNumeric(this.value); });
	}, 50);
}

function makeNumeric(val){
	if(typeof val === 'string') {
		val = val.replace(/[^0-9\.]/g,'');
	}
	return val;
}

function errorMessage(err) {
  if(err && err.data){
      xModalMessage('ERROR', err.data);
  } else {
      xModalMessage('ERROR', err);
  }
};

function checkFileAPISupport(){
  if (window.File && window.FileReader && window.FileList && window.Blob) {
    return true;
  }
  xModalMessage('HTML5 File API Unsupported', 'Import/Export file functions are not fully supported in your browser.<br/><br/>Try upgrading to a modern web browser.');
  return false;
};

function getUrlParams(){
  var vars = [], hash;
  var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
  for(var i = 0; i < hashes.length; i++){
    hash = hashes[i].split('=');
    hash[0]=decodeURIComponent(hash[0]);
    vars.push(hash[0]);
    vars[hash[0]] = decodeURIComponent(hash[1]);;
  }
  return vars;
};
var parameters = getUrlParams();

/****** Utility functions ******/
function isFunction(functionToCheck) {
 var getType = {};
 return functionToCheck && getType.toString.call(functionToCheck) === '[object Function]';
};
function noCache() {
 return 'rnd='+new Date().getTime();
};
function caseInsensitiveComparator(A,B){
  if(A && B){
    var a = A.toLowerCase();
    var b = B.toLowerCase();
    if(a < b) return -1;
    if(a > b) return 1;
  }
  return 0;
};