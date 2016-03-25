/**
 * OpenCPS is the open source Core Public Services software
 * Copyright (C) 2016-present OpenCPS community

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package org.opencps.usermgt.portlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.opencps.usermgt.DuplicatEgovAgencyCodeException;
import org.opencps.usermgt.DuplicateEmployeeEmailException;
import org.opencps.usermgt.DuplicateEmployeeNoException;
import org.opencps.usermgt.EmptyEmployeeEmailException;
import org.opencps.usermgt.EmptyEmployeeNameException;
import org.opencps.usermgt.EmptyEmployeeNoException;
import org.opencps.usermgt.NoSuchEmployeeException;
import org.opencps.usermgt.NoSuchJobPosException;
import org.opencps.usermgt.NoSuchWorkingUnitException;
import org.opencps.usermgt.OutOfLengthEmployeeEmailException;
import org.opencps.usermgt.OutOfLengthFullNameException;
import org.opencps.usermgt.OutOfLengthUnitEnNameException;
import org.opencps.usermgt.OutOfLengthUnitNameException;
import org.opencps.usermgt.OutOfScopeException;
import org.opencps.usermgt.model.Employee;
import org.opencps.usermgt.model.JobPos;
import org.opencps.usermgt.model.WorkingUnit;
import org.opencps.usermgt.search.EmployeeDisplayTerm;
import org.opencps.usermgt.search.JobPosDisplayTerms;
import org.opencps.usermgt.search.JobPosSearchTerms;
import org.opencps.usermgt.search.WorkingUnitDisplayTerms;
import org.opencps.usermgt.service.EmployeeLocalServiceUtil;
import org.opencps.usermgt.service.JobPosLocalServiceUtil;
import org.opencps.usermgt.service.WorkingUnitLocalServiceUtil;
import org.opencps.util.MessageKeys;
import org.opencps.util.PortletPropsValues;
import org.opencps.util.WebKeys;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroup;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextFactory;
import com.liferay.portal.service.UserGroupLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;

/**
 * @author trungnt
 */

public class UserMgtPortlet extends MVCPortlet {

	public void deleteWorkingUnit(ActionRequest request, ActionResponse response)
		throws NoSuchWorkingUnitException, SystemException {

		long workingUnitId =
			ParamUtil.getLong(request, WorkingUnitDisplayTerms.WORKINGUNIT_ID);
		WorkingUnitLocalServiceUtil.deleteWorkingUnitByWorkingUnitId(workingUnitId);

	}

	public void updateJobPos(ActionRequest request, ActionResponse response)
		throws NumberFormatException, PortalException, SystemException {

		String rowIndexes = request.getParameter("rowIndexes");
		String[] indexOfRows = rowIndexes.split(",");

		ServiceContext serviceContext =
			ServiceContextFactory.getInstance(request);
		for (int index = 0; index < indexOfRows.length; index++) {
			String title =
				request.getParameter(JobPosSearchTerms.TITLE_JOBPOS +
					indexOfRows[index].trim());
			String leader =
				request.getParameter(JobPosSearchTerms.LEADER_JOBPOS +
					indexOfRows[index].trim());
			long workingUnitId =
				ParamUtil.getLong(request, "workingUnitId" + 0);
			JobPosLocalServiceUtil.addJobPos(
				serviceContext.getUserId(), serviceContext, title, "",
				workingUnitId, Integer.valueOf(leader));
		}

	}

	public void editJobPos(ActionRequest request, ActionResponse response)
		throws PortalException, SystemException {

		long jobPosId =
			ParamUtil.getLong(request, JobPosDisplayTerms.ID_JOBPOS);

		int leader =
			ParamUtil.getInteger(request, JobPosDisplayTerms.LEADER_JOBPOS);
		String title =
			ParamUtil.getString(request, JobPosDisplayTerms.TITLE_JOBPOS);
		ServiceContext serviceContext =
			ServiceContextFactory.getInstance(request);
		JobPos jobPos = null;
		if (jobPosId > 0) {
			jobPos = JobPosLocalServiceUtil.fetchJobPos(jobPosId);
			jobPos =
				JobPosLocalServiceUtil.updateJobPos(
					jobPosId, serviceContext.getUserId(), serviceContext,
					title, "", jobPos.getWorkingUnitId(), leader);
		}
		else {
			SessionErrors.add(request, "UPDATE_JOBPOS_ERROR");
		}
	}

	public void deleteJobPos(ActionRequest request, ActionResponse response)
		throws NoSuchJobPosException, SystemException {

		long jobPosId =
			ParamUtil.getLong(request, JobPosDisplayTerms.ID_JOBPOS);
		if (jobPosId > 0) {
			JobPosLocalServiceUtil.deletejobPos(jobPosId);;
		}
	}

	public void updateEmployee(
		ActionRequest actionRequest, ActionResponse actionResponse)
		throws IOException {

		long employeeId =
			ParamUtil.getLong(actionRequest, EmployeeDisplayTerm.EMPLOYEE_ID);
		long workingUnitId =
			ParamUtil.getLong(
				actionRequest, EmployeeDisplayTerm.WORKING_UNIT_ID);
		long mainJobPosId =
			ParamUtil.getLong(actionRequest, EmployeeDisplayTerm.MAIN_JOBPOS_ID);

		long companyId =
			ParamUtil.getLong(actionRequest, EmployeeDisplayTerm.COMPANY_ID);
		long groupId =
			ParamUtil.getLong(actionRequest, EmployeeDisplayTerm.GROUP_ID);

		String email =
			ParamUtil.getString(actionRequest, EmployeeDisplayTerm.EMAIL);
		String userAccountEmail =
			ParamUtil.getString(actionRequest, EmployeeDisplayTerm.USER_EMAIL);
		String employeeNo =
			ParamUtil.getString(actionRequest, EmployeeDisplayTerm.EMPLOYEE_NO);
		String fullName =
			ParamUtil.getString(actionRequest, EmployeeDisplayTerm.FULL_NAME);
		String mobile =
			ParamUtil.getString(actionRequest, EmployeeDisplayTerm.MOBILE);
		String telNo =
			ParamUtil.getString(actionRequest, EmployeeDisplayTerm.TEL_NO);
		String screenName =
			ParamUtil.getString(actionRequest, EmployeeDisplayTerm.SCREEN_NAME);
		String passWord =
			ParamUtil.getString(actionRequest, EmployeeDisplayTerm.PASS_WORD);
		String rePassWord =
			ParamUtil.getString(actionRequest, EmployeeDisplayTerm.RE_PASS_WORD);

		int gender =
			ParamUtil.getInteger(actionRequest, EmployeeDisplayTerm.GENDER);
		int birthDateDay =
			ParamUtil.getInteger(
				actionRequest, EmployeeDisplayTerm.BIRTH_DATE_DAY);
		int birthDateMonth =
			ParamUtil.getInteger(
				actionRequest, EmployeeDisplayTerm.BIRTH_DATE_MONTH);
		int birthDateYear =
			ParamUtil.getInteger(
				actionRequest, EmployeeDisplayTerm.BIRTH_DATE_YEAR);
		int workingStatus =
			ParamUtil.getInteger(
				actionRequest, EmployeeDisplayTerm.WORKING_STATUS);

		String redirectURL = ParamUtil.getString(actionRequest, "redirectURL");

		String returnURL = ParamUtil.getString(actionRequest, "returnURL");

		int[] jobPosIndexes =
			StringUtil.split(
				ParamUtil.getString(actionRequest, "jobPosIndexes"), -1);

		UserGroup userGroup = null;
		List<Long> jobPosIds = new ArrayList<Long>();

		if (jobPosIndexes != null && jobPosIndexes.length > 0) {
			for (int i = 0; i < jobPosIndexes.length; i++) {
				if (jobPosIndexes[i] >= 0) {
					long jobPosIdTemp =
						ParamUtil.getLong(
							actionRequest, EmployeeDisplayTerm.JOBPOS_ID +
								jobPosIndexes[i]);
					jobPosIds.add(jobPosIdTemp);
				}

			}
		}
		try {
			userGroup =
				UserGroupLocalServiceUtil.getUserGroup(
					companyId,
					PortletPropsValues.USERMGT_USERGROUP_NAME_EMPLOYEE);
		}
		catch (Exception e) {
			_log.warn(e);
		}

		try {
			ServiceContext serviceContext =
				ServiceContextFactory.getInstance(actionRequest);
			// validatetDictItem(dictItemId, itemName, itemCode,
			// serviceContext);

			// Add site for user. Default current site
			long[] groupIds = new long[] {
				groupId
			};
			// Add user group
			if (userGroup == null) {
				userGroup =
					UserGroupLocalServiceUtil.addUserGroup(
						serviceContext.getUserId(), companyId,
						PortletPropsValues.USERMGT_USERGROUP_NAME_EMPLOYEE,
						StringPool.BLANK, serviceContext);
			}

			long[] userGroupIds = new long[] {
				userGroup.getUserGroupId()
			};

			// Validate before update
			validateEmployee(
				employeeId, fullName, userAccountEmail, employeeNo,
				workingUnitId, mainJobPosId, serviceContext);

			if (employeeId == 0) {
				EmployeeLocalServiceUtil.addEmployee(
					serviceContext.getUserId(), workingUnitId, employeeNo,
					fullName, gender, telNo, mobile, email, screenName,
					workingStatus, mainJobPosId,
					ArrayUtil.toLongArray(jobPosIds), birthDateDay,
					birthDateMonth, birthDateYear, passWord, rePassWord,
					groupIds, userGroupIds, serviceContext);
				SessionMessages.add(
					actionRequest, MessageKeys.USERMGT_ADD_SUCCESS);
			}
			else {

				SessionMessages.add(
					actionRequest, MessageKeys.USERMGT_UPDATE_SUCCESS);
			}
		}
		catch (Exception e) {

			if (e instanceof EmptyEmployeeEmailException) {
				SessionErrors.add(
					actionRequest, EmptyEmployeeEmailException.class);
			}
			else if (e instanceof OutOfLengthEmployeeEmailException) {
				SessionErrors.add(
					actionRequest, OutOfLengthEmployeeEmailException.class);
			}
			else if (e instanceof EmptyEmployeeNoException) {
				SessionErrors.add(actionRequest, EmptyEmployeeNoException.class);
			}
			else if (e instanceof EmptyEmployeeNameException) {
				SessionErrors.add(
					actionRequest, EmptyEmployeeNameException.class);
			}
			else if (e instanceof OutOfLengthFullNameException) {
				SessionErrors.add(
					actionRequest, OutOfLengthFullNameException.class);
			}
			else if (e instanceof NoSuchWorkingUnitException) {
				SessionErrors.add(
					actionRequest, NoSuchWorkingUnitException.class);
			}
			else if (e instanceof NoSuchJobPosException) {
				SessionErrors.add(actionRequest, NoSuchJobPosException.class);
			}
			else if (e instanceof DuplicateEmployeeEmailException) {
				SessionErrors.add(
					actionRequest, DuplicateEmployeeEmailException.class);
			}
			else if (e instanceof NoSuchEmployeeException) {
				SessionErrors.add(actionRequest, NoSuchEmployeeException.class);
			}
			else if (e instanceof PortalException) {
				SessionErrors.add(actionRequest, PortalException.class);
			}
			else if (e instanceof SystemException) {
				SessionErrors.add(actionRequest, SystemException.class);
			}
			else {
				SessionErrors.add(
					actionRequest,
					MessageKeys.USERMGT_SYSTEM_EXCEPTION_OCCURRED);
			}
			redirectURL = returnURL;
			SessionErrors.add(
				actionRequest, MessageKeys.USERMGT_SYSTEM_EXCEPTION_OCCURRED);

		}
		finally {
			if (Validator.isNotNull(redirectURL)) {
				actionResponse.sendRedirect(redirectURL);
			}
		}

	}

	public void updateWorkingUnit(ActionRequest request, ActionResponse response)
		throws PortalException, SystemException {

		long managerWorkingUnitId =
			ParamUtil.getLong(
				request,
				WorkingUnitDisplayTerms.WORKINGUNIT_MANAGERWORKINGUNITID);
		long workingUnitId =
			ParamUtil.getLong(request, WorkingUnitDisplayTerms.WORKINGUNIT_ID);
		long parentWorkingUnitId =
			ParamUtil.getLong(
				request,
				WorkingUnitDisplayTerms.WORKINGUNIT_PARENTWORKINGUNITID);
		String name =
			ParamUtil.getString(
				request, WorkingUnitDisplayTerms.WORKINGUNIT_NAME);
		String enName =
			ParamUtil.getString(
				request, WorkingUnitDisplayTerms.WORKINGUNIT_ENNAME);
		String address =
			ParamUtil.getString(
				request, WorkingUnitDisplayTerms.WORKINGUNIT_ADDRESS);
		String telNo =
			ParamUtil.getString(
				request, WorkingUnitDisplayTerms.WORKINGUNIT_TELNO);
		String faxNo =
			ParamUtil.getString(
				request, WorkingUnitDisplayTerms.WORKINGUNIT_FAXNO);
		String email =
			ParamUtil.getString(
				request, WorkingUnitDisplayTerms.WORKINGUNIT_EMAIL);
		String website =
			ParamUtil.getString(
				request, WorkingUnitDisplayTerms.WORKINGUNIT_WEBSITE);
		String govAgencyCode =
			ParamUtil.getString(
				request, WorkingUnitDisplayTerms.WORKINGUNIT_GOVAGENCYCODE);
		String cityCode =
			ParamUtil.getString(
				request, WorkingUnitDisplayTerms.WORKINGUNIT_CITYCODE);
		String districtCode =
			ParamUtil.getString(
				request, WorkingUnitDisplayTerms.WORKINGUNIT_DISTRICTCODE);
		String wardCode =
			ParamUtil.getString(
				request, WorkingUnitDisplayTerms.WORKINGUNIT_WARDCODE);
		ServiceContext serviceContext =
			ServiceContextFactory.getInstance(request);
		boolean isEmployer =
			ParamUtil.getBoolean(
				request, WorkingUnitDisplayTerms.WORKINGUNIT_ISEMPLOYER);
		String redirectURL = ParamUtil.getString(request, "redirectURL");
		_log.info("go here");
		validateWorkingUnit(
			workingUnitId, name, govAgencyCode, enName, address, faxNo, email,
			website, serviceContext.getScopeGroupId(), parentWorkingUnitId);

		try {
			if (workingUnitId == 0) {
				_log.info("go here add");
				WorkingUnitLocalServiceUtil.addWorkingUnit(
					serviceContext.getUserId(), serviceContext, name, enName,
					govAgencyCode, parentWorkingUnitId, address, cityCode,
					districtCode, wardCode, telNo, faxNo, email, website,
					isEmployer, managerWorkingUnitId);
				SessionMessages.add(
					request, MessageKeys.WORKINGUNIT_UPDATE_SUCESS);
			}
			else {
				WorkingUnitLocalServiceUtil.updateWorkingUnit(
					workingUnitId, serviceContext.getUserId(), serviceContext,
					name, enName, govAgencyCode, parentWorkingUnitId, address,
					cityCode, districtCode, wardCode, telNo, faxNo, email,
					website, isEmployer, managerWorkingUnitId);
				SessionMessages.add(
					request, MessageKeys.WORKINGUNIT_UPDATE_SUCESS);
			}
		}
		catch (Exception e) {
			if (e instanceof OutOfLengthUnitNameException) {
				SessionErrors.add(request, OutOfLengthUnitNameException.class);
			}
			else if (e instanceof OutOfLengthUnitEnNameException) {
				SessionErrors.add(request, OutOfLengthUnitEnNameException.class);
			}
			else if (e instanceof DuplicatEgovAgencyCodeException) {
				SessionErrors.add(
					request, DuplicatEgovAgencyCodeException.class);
			}
			else if (e instanceof OutOfScopeException) {
				SessionErrors.add(request, OutOfScopeException.class);
			}
		}
		finally {
			if (Validator.isNotNull(redirectURL)) {
				try {
					response.sendRedirect(redirectURL);
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	protected void validateWorkingUnit(
		long workingUnitId, String name, String govAgencyCode, String enName,
		String adress, String faxNo, String email, String website,
		long groupId, long parentWorkingUnitId)
		throws OutOfLengthUnitNameException, OutOfLengthUnitEnNameException,
		NoSuchWorkingUnitException, SystemException,
		DuplicatEgovAgencyCodeException, OutOfScopeException {

		WorkingUnit workingUnit =
			WorkingUnitLocalServiceUtil.getWorkingUnit(groupId, govAgencyCode);
		WorkingUnit workingUnit2 = null;
		workingUnit2 = WorkingUnitLocalServiceUtil.fetchWorkingUnit(parentWorkingUnitId);
		if (name.trim().length() > PortletPropsValues.USERMGT_WORKINGUNIT_NAME_LENGTH) {
			throw new OutOfLengthUnitNameException();
		}
		else if (enName.trim().length() > PortletPropsValues.USERMGT_WORKINGUNIT_ENNAME_LENGTH) {
			throw new OutOfLengthUnitEnNameException();
		}
		else if (workingUnit != null && workingUnitId != 0) {
			throw new DuplicatEgovAgencyCodeException();
		}
		else if (workingUnit2 !=null && workingUnit2.getIsEmployer() == false) {
			throw new OutOfScopeException();
		}
	}

	@Override
	public void render(
		RenderRequest renderRequest, RenderResponse renderResponse)
		throws PortletException, IOException {

		long workingUnitId =
			ParamUtil.getLong(
				renderRequest, WorkingUnitDisplayTerms.WORKINGUNIT_ID);

		long employeeId =
			ParamUtil.getLong(renderRequest, EmployeeDisplayTerm.EMPLOYEE_ID);

		try {
			if (workingUnitId > 0) {
				WorkingUnit workingUnit =
					WorkingUnitLocalServiceUtil.getWorkingUnit(workingUnitId);
				renderRequest.setAttribute(
					WebKeys.WORKING_UNIT_ENTRY, workingUnit);
			}

			if (employeeId > 0) {
				Employee employee =
					EmployeeLocalServiceUtil.getEmployee(employeeId);

				if (employee != null) {
					long mappingUserId = employee.getMappingUserId();

					if (mappingUserId > 0) {
						User mappingUser =
							UserLocalServiceUtil.getUser(mappingUserId);

						renderRequest.setAttribute(
							WebKeys.USER_MAPPING_ENTRY, mappingUser);
					}

					long mappingWorkingUnitId = employee.getWorkingUnitId();

					if (mappingWorkingUnitId > 0) {
						WorkingUnit mappingWorkingUnit =
							WorkingUnitLocalServiceUtil.getWorkingUnit(mappingWorkingUnitId);

						renderRequest.setAttribute(
							WebKeys.WORKING_UNIT_MAPPING_ENTRY,
							mappingWorkingUnit);

					}

					long mainJobPosId = employee.getMainJobPosId();

					if (mainJobPosId > 0) {
						JobPos mainJobPos =
							JobPosLocalServiceUtil.getJobPos(mainJobPosId);
						renderRequest.setAttribute(
							WebKeys.MAIN_JOB_POS_ENTRY, mainJobPos);
					}
				}

				renderRequest.setAttribute(WebKeys.EMPLOYEE_ENTRY, employee);
			}
		}
		catch (Exception e) {
			_log.error(e);
		}

		super.render(renderRequest, renderResponse);
	}
	private Log _log =
		LogFactoryUtil.getLog(UserMgtEditProfilePortlet.class.getName());

	protected void validateEmployee(
		long employeeId, String fullName, String email, String employeeNo,
		long workingUnitId, long mainJobPosId, ServiceContext serviceContext)
		throws EmptyEmployeeEmailException, OutOfLengthEmployeeEmailException,
		EmptyEmployeeNoException, EmptyEmployeeNameException,
		OutOfLengthFullNameException, NoSuchWorkingUnitException,
		NoSuchJobPosException, DuplicateEmployeeEmailException,
		NoSuchEmployeeException, PortalException, SystemException {

		if (Validator.isNull(email)) {
			throw new EmptyEmployeeEmailException();
		}

		if (email.length() > PortletPropsValues.USERMGT_EMPLOYEE_EMAIL_LENGTH) {
			throw new OutOfLengthEmployeeEmailException();
		}

		if (Validator.isNull(employeeNo)) {
			throw new EmptyEmployeeNoException();
		}

		if (Validator.isNull(fullName)) {
			throw new EmptyEmployeeNameException();
		}

		if (fullName.length() > PortletPropsValues.USERMGT_EMPLOYEE_FULLNAME_LENGTH) {
			throw new OutOfLengthFullNameException();
		}

		if (workingUnitId <= 0) {
			throw new NoSuchWorkingUnitException();
		}

		if (mainJobPosId <= 0) {
			throw new NoSuchJobPosException();
		}

		Employee employee = null;

		Employee employeeByEmail = null;

		Employee employeeByEmployeeNo = null;

		if (employeeId > 0) {
			employee = EmployeeLocalServiceUtil.getEmployee(employeeId);
		}

		try {
			employeeByEmail =
				EmployeeLocalServiceUtil.getEmployeeByEmail(
					serviceContext.getScopeGroupId(), email);
		}
		catch (Exception e) {
			// Nothing todo
		}

		try {
			employeeByEmployeeNo =
				EmployeeLocalServiceUtil.getEmployeeByEmployeeNo(
					serviceContext.getScopeGroupId(), employeeNo);
		}
		catch (Exception e) {
			// Nothing todo
		}

		if (employee == null) {
			if (employeeByEmail != null) {
				throw new DuplicateEmployeeEmailException();
			}

			if (employeeByEmployeeNo != null) {
				throw new DuplicateEmployeeNoException();
			}
		}
		else {
			if (employeeByEmail.getEmployeeId() != employeeId) {
				throw new DuplicateEmployeeEmailException();
			}

			if (employeeByEmployeeNo.getEmployeeId() != employeeId) {
				throw new DuplicateEmployeeNoException();
			}
		}

	}
}
