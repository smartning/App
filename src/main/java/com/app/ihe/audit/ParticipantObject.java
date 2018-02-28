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

package com.app.ihe.audit;

import com.app.ihe.data.Patient;
import com.app.ihe.data.PatientIdentifier;
import com.app.ihe.data.PersonName;
import com.app.ihe.registry.HL7;
import com.misyshealthcare.connect.base.audit.AuditCodeMappings.ParticipantObjectIdTypeCode;
import com.misyshealthcare.connect.base.audit.AuditCodeMappings.ParticipantObjectRoleCode;
import com.misyshealthcare.connect.base.audit.AuditCodeMappings.ParticipantObjectTypeCode;

import java.util.ArrayList;
import java.util.List;

/**
 * ParticipantObject get the participant info to the Audit Trail.
 * 
 * @author Wenzhi Li
 * @version 1.0 - Dec 30, 2008
 */
public class ParticipantObject {

	private String dataLifeCycle;
	private String sensitivity;
	private String id;
	private String name;
	private String query; //B64 encoded query;
	private String detail;
	
	ParticipantObjectIdTypeCode idTypeCode = ParticipantObjectIdTypeCode.Patient;
	ParticipantObjectTypeCode typeCode;
	ParticipantObjectRoleCode role;

	private List<PatientIdentifier> pIds = new ArrayList<PatientIdentifier>();
	private Patient patient;
	

	public ParticipantObject() {		
	}
	/** 
	 * Constructor for building patient participant objects.
	 * 
	 * @param patient The descriptor of the patient.
	 */
	public ParticipantObject(Patient patient) {
		this.name = null;
		this.patient = patient;
		
		if (patient != null && patient.getPatientIds().size() >0 ) {
		    setId(patient.getPatientIds());
		}
		
		typeCode = ParticipantObjectTypeCode.Person;
		role = ParticipantObjectRoleCode.Patient;
	}
	

	public ParticipantObject(String name, String id) {
		this.name = name;
		setId(id);
	}
	
	public ParticipantObject(String Name, PatientIdentifier pId) {
		this.name = Name;
		addId(pId);
	}
	
	/** 
	 * Adds an patient id to this PO. 
	 * 
	 * @param pid the id to be added
	 */
	public void addId(PatientIdentifier pid) {
		pIds.add(pid);
	}
	
	/** 
	 * Sets the ids for this PO. 
	 * 
	 * @param pids The list of patient ID
	 */
	public void setId(List<PatientIdentifier> pids) {
		pIds = pids;
	}

	/** 
	 * Sets the id for this PO to be a standard string.
	 * 
	 * @param inId
	 */
	public void setId(String inId) {
		id = inId;
	}
	
	/** 
	 * Looks for a standard string id, then looks us the id through the patient description.
	 * 
	 * Note the the asigning authority is ignored if the standard string 
	 * id is set for this PO.  Otherwise it uses the given class to do a 
	 * lookup.  If null it uses the Local authority.  Local authority is 
	 * only needed when encoding as HL7 and using no assigning authority.
	 *
	 * @param encoding The encoding to get the ID in, HL7 or null for no encoding.
	 * @return The retrieved ID, or "No ID Available"
	 */
	public String getId(String encoding)
	{
		//look for default id first
		String foundId = id;
		// Apply the encoding here
		if (foundId == null && pIds != null && pIds.size()>0 && encoding != null) {
			if (encoding.equalsIgnoreCase("HL7")) {
				foundId = HL7.toCX(pIds);
			}
			// other encodings here...
		}

		return foundId;
	}
	
	public String getName(String encoding) {
		// Look for default name first.
		String foundName = name;
		if (foundName == null && patient !=null && encoding != null) {
			// Get name from patient in specific encoding, if available.
			if (encoding.equalsIgnoreCase("HL7")) {
				foundName = HL7.toXPN(patient.getPatientName());
			}
			//other encoding...
		}
		// Get generic encoding name from patient.
		if (foundName == null && patient != null) {
			PersonName name = patient.getPatientName();
			if (name != null)
				foundName = patient.getPatientName().toString();
		}
		
		return foundName;
	}


	public String getDataLifeCycle() {
		return dataLifeCycle;
	}


	public void setDataLifeCycle(String dataLifeCycle) {
		this.dataLifeCycle = dataLifeCycle;
	}


	public String getSensitivity() {
		return sensitivity;
	}


	public void setSensitivity(String sensitivity) {
		this.sensitivity = sensitivity;
	}


	public String getQuery() {
		return query;
	}


	public void setQuery(String query) {
		this.query = query;
	}


	public String getDetail() {
		return detail;
	}


	public void setDetail(String detail) {
		this.detail = detail;
	}
	
	
}
