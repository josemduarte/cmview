package cmview.sadpAdapter;

import cmview.datasources.Model;
import cmview.toolUtils.AlignmentToolResult;

/**
 * @author Lars Petzold
 * 
 * Designed to collect the results of a SADP run.
 */
public class SADPResult extends AlignmentToolResult {
	
	double compTime = 0.0;
	int    numIterations = 0;
	int    numCommonContacts = 0;
	Model  firstOutputModel;
	Model  secondOutputModel;
	String firstName;
	String secondName;
	
	public double getCompTime() {
		return compTime;
	}
	
	public void setCompTime(double compTime) {
		this.compTime = compTime;
	}
	
	public int getNumIterations() {
		return numIterations;
	}
	
	public void setNumIterations(int numIterations) {
		this.numIterations = numIterations;
	}
	
	public int getNumCommonContacts() {
		return numIterations;
	}
	
	public void setNumCommonContacts(int numCommonContacts) {
		this.numCommonContacts = numCommonContacts;
	}
	
	public Model getFirstOutputModel() {
		return firstOutputModel;
	}

	public void setFirstOutputModel(Model mod) {
		this.firstOutputModel = mod;
	}
	
	public Model getSecondOutputModel() {
		return secondOutputModel;
	}
	
	public void setSecondOutputModel(Model mod) {
		this.secondOutputModel = mod;
	}
	
	public String getFirstName() {
		return firstName;
	}
	
	public void setFirstName(String name) {
		this.firstName = name;
	}
	
	public String getSecondName() {
		return secondName;
	}
	
	public void setSecondName(String name) {
		this.secondName = name;
	}
}
