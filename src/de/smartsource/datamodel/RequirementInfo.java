package de.smartsource.datamodel;

public class RequirementInfo {
	private String id;
	private String name;
	String[] connectedRequirements;
	String[] connectedComponents;
	
	
	public RequirementInfo(){
	}
	
	public RequirementInfo(String reqId, String reqName, String[] reqConnectedRequirements){
		this.id = reqId;
		this.name = reqName;
		this.connectedRequirements = reqConnectedRequirements;
	}
	
	
	
	
	public String[] getConnectedComponents() {
		return connectedComponents;
	}

	public void setConnectedComponents(String[] connectedComponents) {
		this.connectedComponents = connectedComponents;
	}

	public String[] getConnectedRequirements() {
		return connectedRequirements;
	}
	public void setConnectedRequirements(String[] connectedRequirements) {
		this.connectedRequirements = connectedRequirements;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	

}
