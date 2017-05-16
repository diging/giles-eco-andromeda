<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>

<h1>System Configuration</h1>

<form:form modelAttribute="systemConfigPage"
	action="${pageContext.request.contextPath}/admin/system/config"
	method="POST">

	<input type="hidden" name="${_csrf.parameterName}"
		value="${_csrf.token}" />

	<div class="page-header">
		<h3>Giles Ecosystem Integration</h3>
	</div>

	<div class="form-group">
		<label for="baseUrl">Andromeda Base URL</label>
		<form:input type="text" class="form-control" id="baseUrl"
			placeholder="Andromeda Base URL" path="baseUrl" value="${baseUrl}"></form:input>
		<small><form:errors class="error" path="baseUrl"></form:errors></small>
	</div>
	
	<button class="btn btn-primary btn-md pull-right" type="submit">Save
		Changes!</button>
</form:form>