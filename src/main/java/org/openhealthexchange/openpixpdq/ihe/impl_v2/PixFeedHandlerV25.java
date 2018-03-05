/* Copyright 2009 Misys PLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing 
 * permissions and limitations under the License. 
 */
package org.openhealthexchange.openpixpdq.ihe.impl_v2;

import java.util.List;

import org.openhealthexchange.openpixpdq.data.MessageHeader;
import org.openhealthexchange.openpixpdq.data.Patient;
import org.openhealthexchange.openpixpdq.data.PatientIdentifier;
import org.openhealthexchange.openpixpdq.ihe.IPixManagerAdapter;
import org.openhealthexchange.openpixpdq.ihe.IPixUpdateNotificationRequest;
import org.openhealthexchange.openpixpdq.ihe.PixManagerException;
import org.openhealthexchange.openpixpdq.ihe.PixUpdateNotifier;
import org.openhealthexchange.openpixpdq.ihe.audit.ParticipantObject;
import org.openhealthexchange.openpixpdq.ihe.impl_v2.hl7.HL7Header;
import org.openhealthexchange.openpixpdq.ihe.impl_v2.hl7.HL7v25;
import org.openhealthexchange.openpixpdq.ihe.impl_v2.hl7.HL7v25ToBaseConvertor;
import org.openhealthexchange.openpixpdq.util.AssigningAuthorityUtil;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.app.ApplicationException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.datatype.CX;
import ca.uhn.hl7v2.model.v25.message.ACK;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.message.ADT_A05;
import ca.uhn.hl7v2.model.v25.segment.PID;

import com.misyshealthcare.connect.base.audit.ActiveParticipant;
import com.misyshealthcare.connect.base.audit.AuditCodeMappings;
import com.misyshealthcare.connect.base.audit.AuditCodeMappings.EventActionCode;
import com.misyshealthcare.connect.net.Identifier;

/**
 * PixFeedHandlerV25 is a message handler similar to 
 * {@link PixFeedHandler}. However, it processes
 * HL7v2.5 PIX Feed message instead of HL7v2.3.1.
 *  
 * @author Wenzhi Li
 * @version 1.0 Jan 22, 2009
 * @see PixFeedHandler
 */
class PixFeedHandlerV25 extends BaseHandler {
	private PixManager actor = null;
	private IPixManagerAdapter pixAdapter = null;

	/**
	 * Constructor
	 * 
	 * @param actor the {@link PixManager} actor
	 */
	PixFeedHandlerV25(PixManager actor) {
		super(actor.getConnection());
		this.actor = actor;
		this.pixAdapter = actor.getPixManagerAdapter(); 
		assert this.connection != null;
		assert this.pixAdapter != null;
	}

	/**
	 * Processes PIX Feed Create Patient message in HL7v2.5.
	 * 
	 * @param msgIn the PIX Feed request message
	 * @return a response message for PIX Feed
	 * @throws ApplicationException If Application has trouble
	 * @throws HL7Exception if something is wrong with HL7 message 
	 */
	Message processCreate(Message msgIn) 
	throws ApplicationException, HL7Exception {
        //ADT^A04 message is processed by ADT^A01
		assert msgIn instanceof ADT_A01 || 
			   msgIn instanceof ADT_A05;
	
		HL7Header hl7Header = new HL7Header(msgIn);
		
		//Create Acknowledgment and its Header
		ACK reply = initAcknowledgment(hl7Header);

		//Validate incoming message first
		PID pid = (PID)msgIn.get("PID");
		PatientIdentifier patientId = getPatientIdentifiers(pid);				
		boolean isValidMessage = validateMessage(reply, hl7Header, patientId, true);
		if (!isValidMessage) return reply;
		
		//Invoke eMPI function
		MessageHeader header = hl7Header.toMessageHeader();
		Patient patient = getPatient(msgIn);
		try {
			// 创建一个病人信息的数据操作数据
			List<PatientIdentifier> matching = pixAdapter.createPatient(patient, header);
			
			//Send PIX Update Notification
			if (matching != null && matching.size() > 0) {
				IPixUpdateNotificationRequest request = 
					new PixUpdateNotificationRequest(actor, matching);
				PixUpdateNotifier.getInstance().accept(request);
			}			
		} catch (PixManagerException e) {
			throw new ApplicationException(e);
		} 

		HL7v25.populateMSA(reply.getMSA(), "AA", hl7Header.getMessageControlId());

		//Finally, Audit Log PIX Feed Success 
	    auditLog(hl7Header, patient, AuditCodeMappings.EventActionCode.Create);

		return reply; 
	}

	/**
	 * Audit Logging of PIX Feed message.
	 * 
	 * @param hl7Header the header message from the source application
	 * @param patient the patient to create, update or merged
	 * @param eventActionCode the {@link EventActionCode}
	 */
	private void auditLog(HL7Header hl7Header, Patient patient, AuditCodeMappings.EventActionCode eventActionCode) {
		if (actor.getAuditTrail() == null)
			return;
		
		String userId = hl7Header.getSendingFacility().getNamespaceId() + "|" +
						hl7Header.getSendingApplication().getNamespaceId();
		String messageId = hl7Header.getMessageControlId();
		//TODO: Get the ip address of the source application
		String sourceIp = "127.0.0.1";
		ActiveParticipant source = new ActiveParticipant(userId, messageId, sourceIp);
		
		ParticipantObject patientObj = new ParticipantObject(patient);
		patientObj.setDetail(hl7Header.getMessageControlId());
		
		actor.getAuditTrail().logPixFeed(source, patientObj, eventActionCode);		
	}
	
	/**
	 * Initiates an acknowledgment instance for the incoming message.
	 * 
	 * @param hl7Header the message header of the incoming message
	 * @return an {@link ACK} instance
	 * @throws HL7Exception if something is wrong with HL7 message 
	 * @throws ApplicationException If Application has trouble
	 */
	private ACK initAcknowledgment(HL7Header hl7Header) 
	throws HL7Exception, ApplicationException {
		//Send Response
		ACK reply = new ACK();
		
		//For the response message, the ReceivingApplication and ReceivingFacility 
		//will become the sendingApplication and sendingFacility;
		//Also the sendingApplication and sendingFacility will become the 
		//receivingApplication and receivingFacility.
		Identifier serverApplication = getServerApplication();
		Identifier serverFacility = getServerFacility();
		Identifier sendingApplication = hl7Header.getSendingApplication();
		Identifier sendingFacility = hl7Header.getSendingFacility();

		String event = hl7Header.getTriggerEvent();
		HL7v25.populateMSH(reply.getMSH(), "ACK", event, getMessageControlId(), 
 			serverApplication, serverFacility, sendingApplication, sendingFacility);
		
		return reply;
	}
	
	/**
	 * Validates a patient identifier domain, namely, assigning authority.
	 * 
	 * @param reply the reply message to be populated if the validation fails
	 * @param patientId the patient id
	 * @param incomingMessageId the incoming message id
	 * @return <code>true</code> if the patient domain is validated successfully;
	 *         otherwise <code>false</code>.
	 * @throws HL7Exception if something is wrong with HL7 message 
	 */
	private boolean validateDomain(ACK reply, PatientIdentifier patientId, String incomingMessageId) 
	throws HL7Exception {
		boolean  isTest = Boolean.parseBoolean(connection.getProperty("test"));
		if (isTest) return true;
		Identifier domain = patientId.getAssigningAuthority();
		boolean domainOk = AssigningAuthorityUtil.validateDomain(
				domain, connection);
		if (!domainOk) {
			HL7v25.populateMSA(reply.getMSA(), "AE", incomingMessageId);
			//segmentId=PID, sequence=1, fieldPosition=3, fieldRepetition=1,componentNubmer=4
			HL7v25.populateERR(reply.getERR(), "PID", "1", "3", "1", "4",
					"204", "Unknown Key Identifier");
			return false;
		}
		return true;
	}
	
	
	/**
     * Validates the receiving facility and receiving application of an incoming message.
	 * 
     * @param reply the reply message to be populated if any validation is failed
	 * @param receivingApplication the receiving application of the incoming message
	 * @param receivingFacility the receiving facility of the incoming message
	 * @param expectedApplication the expected receiving application
	 * @param expectedFacility the expected receiving facility
	 * @param incomingMessageId the incoming message
	 * @return <code>true</code> if validation is passed;
	 *         otherwise <code>false</code>.
	 * @throws HL7Exception if something is wrong with HL7 message 
	 * @throws ApplicationException if something is wrong with the application
	 */
	private boolean validateReceivingFacilityApplication(ACK reply, Identifier receivingApplication,
			Identifier receivingFacility, Identifier expectedApplication, Identifier expectedFacility,
		    String incomingMessageId) 
		    throws HL7Exception, ApplicationException
	{
		//In case of tests, don't validate receiving application and facility,
		//It is not easy to switch to different receiving applications and facilities
		boolean  isTest = Boolean.parseBoolean(connection.getProperty("test"));
		if (isTest) return true;
	
		//We first need to validate ReceivingApplication and ReceivingFacility.
		//Currently we are not validating SendingApplication and SendingFacility
		if (!receivingApplication.equals(expectedApplication)) {
			HL7v25.populateMSA(reply.getMSA(), "AE", incomingMessageId);
			//segmentId=MSH, sequence=1, fieldPosition=5, fieldRepetition=1, componentNubmer=1
			HL7v25.populateERR(reply.getERR(), "MSH", "1", "5", "1", "1",
					null, "Unknown Receiving Application");
			return false;
		}
		if (!receivingFacility.equals(expectedFacility)) {
			HL7v25.populateMSA(reply.getMSA(), "AE", incomingMessageId);
			//segmentId=MSH, sequence=1, fieldPosition=6, fieldRepetition=1, componentNubmer=1
			HL7v25.populateERR(reply.getERR(), "MSH", "1", "6", "1", "1",
					null, "Unknown Receiving Facility");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Validates the incoming Message in this order:
	 * 
	 * <ul>
	 * <li> Validate Receiving Facility and Receiving Application</li>
	 * <li> Validate Domain </li>
	 * <li> Validate patient Id <li>
	 * </ul>
	 * 
     * @param reply the reply message to be populated if any validation is failed
	 * @param hl7Header the message header of the incoming message
	 * @param patientId the id of the patient to be validated
	 * @param isPixCreate Whether this validation is for pix patient creation
	 * @return <code>true</code> if the message is correct; <code>false</code>otherwise.
	 * @throws HL7Exception if something is wrong with HL7 message 
	 * @throws ApplicationException if something is wrong with the application
	 */
	private boolean validateMessage(ACK reply, HL7Header hl7Header, PatientIdentifier patientId, boolean isPixCreate) 
	throws HL7Exception, ApplicationException {
		Identifier serverApplication = getServerApplication();
		Identifier serverFacility = getServerFacility();
		Identifier receivingApplication = hl7Header.getReceivingApplication();
		Identifier receivingFacility = hl7Header.getReceivingFacility();

		String incomingMessageId = hl7Header.getMessageControlId();
		//1. validate receiving facility and receiving application
		boolean isValidFacilityApplication = validateReceivingFacilityApplication(reply, 
				receivingApplication, receivingFacility, 
				serverApplication, serverFacility, incomingMessageId );
		if (!isValidFacilityApplication) return false;		
		
		//2.validate the domain
		boolean isValidDomain = validateDomain(reply, patientId, incomingMessageId);
		if (!isValidDomain) return false;
		
		//3. validate ID itself 
		if (!isPixCreate) { 
			//Do not valid patient id for PIX patient creation
			boolean isValidPid = validatePatientId(reply, patientId, hl7Header.toMessageHeader(), incomingMessageId);
			if (!isValidPid) return false;
		}
		
		//Finally, it must be true when it reaches here
		return true;
	}
	
	/**
	 * Gets the patient identifier from a Patient PID segment.
	 * 
	 * @param pid the PID segment
	 * @return a {@link PatientIdentifier}
	 */
	private PatientIdentifier getPatientIdentifiers(PID pid) {
		PatientIdentifier identifier = new PatientIdentifier();
		CX[] cxs = pid.getPatientIdentifierList();
		for (CX cx : cxs) {
			Identifier assignAuth = new Identifier(cx.getAssigningAuthority()
					.getNamespaceID().getValue(), cx.getAssigningAuthority()
					.getUniversalID().getValue(), cx.getAssigningAuthority()
					.getUniversalIDType().getValue());
			Identifier assignFac = new Identifier(cx.getAssigningFacility()
					.getNamespaceID().getValue(), cx.getAssigningFacility()
					.getUniversalID().getValue(), cx.getAssigningFacility()
					.getUniversalIDType().getValue());
			identifier.setAssigningAuthority(assignAuth);
			identifier.setAssigningFacility(assignFac);
			identifier.setId(cx.getIDNumber().getValue());
			identifier.setIdentifierTypeCode(cx.getIdentifierTypeCode()
					.getValue());
		}
		return identifier;
	}
	
	/**
	 * Checks the given whether the given patient id is a valid patient id.
	 * 
     * @param reply the reply message to be populated if any validation is failed
	 * @param patientId the patient id to be checked
	 * @param header the incoming message header 
	 * @param incomingMessageId the incoming message id.
	 * @return <code>true</code> if the patientId is valid; otherwise <code>false</code>.
	 * @throws HL7Exception if something is wrong with HL7 message 
	 * @throws ApplicationException if something is wrong with the application
	 */
	private boolean validatePatientId(ACK reply, PatientIdentifier patientId, MessageHeader header, String incomingMessageId)
	throws HL7Exception, ApplicationException{
		boolean validPatient;
		try {
			validPatient = pixAdapter.isValidPatient(patientId, header);
		} catch (PixManagerException e) {
			throw new ApplicationException(e);
		}
		if (!validPatient) {
			HL7v25.populateMSA(reply.getMSA(), "AE", incomingMessageId);
			//segmentId=QPD, sequence=1, fieldPosition=3, fieldRepetition=1, componentNubmer=1
			HL7v25.populateERR(reply.getERR(), "PID", "1", "3", "1", "1",
					"204", "Unknown Key Identifier");
		}
		return validPatient;
	}
	
	/**
	 * Converts a PIX Feed Patient message to a {@link Patient} object.
	 * 
	 * @param msgIn the incoming PIX Feed message
	 * @return a {@link Patient} object
	 * @throws ApplicationException if something is wrong with the application
	 */
	private Patient getPatient(Message msgIn) throws ApplicationException,HL7Exception {
		HL7v25ToBaseConvertor convertor = null;
		if (msgIn.getVersion().equals("2.5")) {
			convertor = new HL7v25ToBaseConvertor(msgIn, connection);
		} else {
			throw new ApplicationException("Unexpected HL7 version");
		}
		Patient patientDesc = new Patient();
		patientDesc.setPatientIds(convertor.getPatientIds());
		patientDesc.setPatientName(convertor.getPatientName());
		patientDesc.setMonthersMaidenName(convertor.getMotherMaidenName());
		patientDesc.setBirthDateTime(convertor.getBirthDate());
		patientDesc.setAdministrativeSex(convertor.getSexType());
		patientDesc.setPatientAlias(convertor.getPatientAliasName());
		patientDesc.setRace(convertor.getRace());
		patientDesc.setPrimaryLanguage(convertor.getPrimaryLanguage());
		patientDesc.setMaritalStatus(convertor.getMartialStatus());
		patientDesc.setReligion(convertor.getReligion());
		patientDesc.setPatientAccountNumber(convertor.getpatientAccountNumber());
		patientDesc.setSsn(convertor.getSsn());
		patientDesc.setDriversLicense(convertor.getDriversLicense());
		patientDesc.setMonthersId(convertor.getMonthersId());
		patientDesc.setEthnicGroup(convertor.getEthnicGroup());
		patientDesc.setBirthPlace(convertor.getBirthPlace());
		patientDesc.setBirthOrder(convertor.getBirthOrder());
		patientDesc.setCitizenship(convertor.getCitizenShip());
		patientDesc.setDeathDate(convertor.getDeathDate());
		patientDesc.setDeathIndicator(convertor.getDeathIndicator());
		patientDesc.setPhoneNumbers(convertor.getPhoneList());
		patientDesc.setAddresses(convertor.getAddressList());
		patientDesc.setVisits(convertor.getVisitList());
		return patientDesc;
	}
	
}
