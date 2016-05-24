var protocolUrl = XNAT.url.dataUrl('protocol');
var visitUrl = XNAT.url.buildUrl('app/action/ModifyPvisit');
var projectUrl = XNAT.url.dataUrl('projects');
var userUrl = XNAT.url.buildUrl('REST/users');

angular.module('protocol.services', []).factory('Protocol', function($resource) {
  return $resource(protocolUrl + '/:id', {id: '@_id'},{
    'update': {
      method: 'POST',
      params: {'inbody': 'true', 'version':'@version', 'XNAT_CSRF':getCsrf},
      transformRequest: function(data, headers){
          return angular.toJson(removeParentExperiments(data.protocol));  // Do this for initial saves too obviously
      }
    },
    'delete': {
      method: 'DELETE',
      params: {'inbody': 'true', 'XNAT_CSRF':getCsrf}
    }
  });
}).factory('Visit', function($resource) {
	// /visits/{VISIT_ID}", "/projects/{PROJECT_ID}/visits/{VISIT_ID}", "/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/visits/{VISIT_ID}
  return $resource(visitUrl,
		{
			'xnat:pVisitData/id': '@id',
			'xnat:pVisitData/project': XNAT.data.page.projectID,
			'xnat:pVisitData/subject_id': XNAT.data.page.subjectID,
			'xnat:pVisitData/visit_type': '@type',		// undefined or ad-hoc??
			'xnat:pVisitData/visit_name': '@name',
			'xnat:pVisitData/date': '@date',
			'xnat:pVisitData/terminal': '@terminal',
			'format': 'json',
			'XNAT_CSRF':getCsrf
		},
		{
			'query': {
				url: projectUrl + '/:project/subjects/:subject/visits/:visit',
				params: {'requested_format': 'json', 'XNAT_CSRF':getCsrf},
				transformResponse: function(data, headers){
					return {Result: angular.fromJson(data)};
				}
			}
		}
	);
}).factory('VisitReport', function($resource) {
  return $resource(projectUrl + '/:id/visit/report',
		{
			'format': 'json',
			'XNAT_CSRF':getCsrf
		},
		{
			'query': {
				params: {'sendNotificationEmail': '@sendNotificationEmail', 'XNAT_CSRF':getCsrf},
				transformResponse: function(data, headers){
					console.log(data);
					return {Result: angular.fromJson(data)};
				}
			}
		}
	);
}).factory('ProjectsUsingProtocol', function($resource) {
  return $resource(protocolUrl + '/:id', {id: '@_id'},{
    'query': {
      params: {'projectsUsing': 'true', 'XNAT_CSRF':getCsrf},
			transformResponse: function(data, headers){
          return {Result: angular.fromJson(data)};
      }
    }
  });
}).factory('Projects', function($resource) {
  return $resource(projectUrl + '/:id/protocol/:protocol', {id: '@id', protocol: '@protocol', 'XNAT_CSRF':getCsrf},{
    'query': {
			url: projectUrl + '/:id',
      transformResponse: function(data, headers){
          return angular.fromJson(data).ResultSet;
      }
    },
		'update': {
			url: projectUrl + '/:id/protocol/:protocol',
      method: 'PUT',
      params: {'version':'@revision', 'XNAT_CSRF':getCsrf},
      transformRequest: function(data, headers){
          return data;
      }
    }
  });
}).factory('User', function($resource) {
  return $resource(userUrl, {id: '@_id'}, {
    'query': {
      params: {'sortBy': 'login'},
      transformResponse: function(data, headers){
          return angular.fromJson(data).ResultSet;
      }
    }
  });
});

function getCsrf(){
  return window.csrfToken;
};

// test this real good with a js unit test suite