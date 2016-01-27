package de.smartsource.webservice;


import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;

import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;


import com.intland.codebeamer.persistence.dto.AssociationDto;
import com.intland.codebeamer.persistence.dto.AssociationTypeDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.remoting.GroupType;
import com.intland.codebeamer.remoting.RemoteApi;
import com.intland.codebeamer.remoting.RemoteApiFactory;
import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.api.uri.UriComponent.Type;
import com.sun.jersey.core.header.FormDataContentDisposition;


import de.smartsource.datamodel.ComponentInfo;
import de.smartsource.datamodel.ProjectInfo;
import de.smartsource.datamodel.RequirementInfo;

/**
 * @author Robert Lorenz Törmer
 * Web Service that returns Data fetched from CodeBeamer
 *
 */

@Path("/DataFetcher")
public class DataFetcher {
	
	
	
	@GET
	@Path("/clientHello")
	@Produces(MediaType.APPLICATION_JSON)
	public String[] clientHello(){
		
		String[] out = {"serverHello"};
		return out;
	}
	
	
	@GET
	@Path("/checkLoginData")
	@Produces(MediaType.APPLICATION_JSON)
	public String[] checkLoginData(@QueryParam("url") @Encoded String url, @QueryParam("login") String login, @QueryParam("password") String password){
		
		// connecting
		RemoteApi api = null;
		String codeBeamerUrl = "";
		
		//decoding codebeamer url
		try {
			codeBeamerUrl = URLDecoder.decode(url, "UTF-8");
		} catch (Exception e) {
			//if decoding fails, return simple error
			String[] out = {"error"};
			return out;
		}
		
		// connect to remote api of codebeamer instance
		try {
			api = RemoteApiFactory.getInstance().connect(codeBeamerUrl);
		} catch (MalformedURLException e1) {
			//if url malformed, simple error
			String[] out = new String[1];
			out[0] = "error";
			return out;
			
		}

			
		// signing in
		String token;
		try{
			token = api.login(login, password);
		} catch (AccessControlException e){
			String[] out = new String[1];
			out[0] = "wrongLogin";
			return out;
		} catch (Exception e){
			String[] out = new String[1];
			out[0] = "wrongCodeBeamerUrl";
			return out;
		}  
		
		//if everything works --> success
		String[] out = {"success"};
		return out;
			

	
	}
	
	
	/**
	 * Method returns all Projects from a platform
	 * @param url : url of the codebeamer instance - needs to be encoded by UTF-8
	 * @param login : username of codebeamer user
	 * @param password : password of codebeamer user
	 * @return : Stringarray of all projects from the platform
	 *
	 */
	@GET
	@Path("/getAllProjects")
	@Produces(MediaType.APPLICATION_JSON)
	public String[][] getAllProjects(@QueryParam("url") @Encoded String url, @QueryParam("login") String login, @QueryParam("password") String password){


		// connecting
		RemoteApi api;
		try {

			String codeBeamerUrl = "";
			
			try {
				codeBeamerUrl = URLDecoder.decode(url, "UTF-8");
				
			} catch (Exception e) {
				
			}
			
			

			api = RemoteApiFactory.getInstance().connect(codeBeamerUrl);
			if (api == null) {
				String[][] out = new String[1][1];
				out[0][0] = "wrong url";
				return out;
			}
			
			
			
			// signing in
			String token;
			try{
				token = api.login(login, password);
			} catch (AccessControlException e){
				String[][] out = new String[1][1];
				out[0][0] = "wrong login";
				return out;
			}
			
			if (token == null) {
				String[][] out = new String[1][1];
				out[0][0] = "token null";
				return out;
			}
			

			// retrieving project names
			ProjectDto projects[] = api.findAllProjects(token);
			String[][] aus = new String[projects.length][3];
			for (int i = 0; i < projects.length; i++) {
				ProjectDto currentProject = projects[i];
				aus[i][0] = currentProject.getId().toString();
				aus[i][1] = currentProject.getName();
				aus[i][2] = currentProject.getDescription();
			}
			return aus;

		} catch (MalformedURLException e) {
			String[][] out = new String[1][1];
			out[0][0] = "malformed url";
			return out;
		}

	}
	
	
	/**
	 * Method that returns all components that belong to a project
	 * @param url : url of the codebeamer instance - needs to be encoded by UTF-8
	 * @param login : username of codebeamer user
	 * @param password : password of codebeamer user
	 * @param projectID : id of the appropriate project
	 * @return : array of componentinfo objects of all components that belong to the project
	 *
	 */
	@GET
	@Path("/getAllComponentsForProject")
	@Produces(MediaType.APPLICATION_JSON)
	public ComponentInfo[] getAllComponentsForProject(@QueryParam("url") @Encoded String url, @QueryParam("login") String login, @QueryParam("password") String password, @QueryParam("projectID") String projectID){
		//connecting
		RemoteApi api;
		try {
			String codeBeamerUrl = "";
			try{
				codeBeamerUrl = URLDecoder.decode(url, "UTF-8");
			} catch (Exception e) {
			}
				
			api = RemoteApiFactory.getInstance().connect(codeBeamerUrl);			
			if(api == null) {
				return null;
			}
			//signing in
			String token = api.login(login, password);
			
			//retrieving project names
			TrackerDto[] tracker = api.findTrackersByProject(token, Integer.parseInt(projectID));
			int indexOfComponents = 0;
			boolean found = false;
			for (int i=0; i<tracker.length; i++){
				if (tracker[i].getName().equalsIgnoreCase("Components") || tracker[i].getName().equalsIgnoreCase("Softwarekomponenten") || tracker[i].getKeyName().equalsIgnoreCase("COMP")) {
					indexOfComponents = i;
					found = true;
					continue;
				}
			}
			if (found = false) {
				return null;
			}
			//retreive components
			TrackerItemDto[] componentArray = api.findTrackerItemsByTrackerId(token, tracker[indexOfComponents].getId());
			//array of components to array list -> used for output
			ArrayList<TrackerItemDto> componentList = new ArrayList<TrackerItemDto>();
			Collections.addAll(componentList, componentArray);
			//iterate components
			for (int i=0; i<componentArray.length; i++) {
				//get children of current component
				ArrayList<TrackerItemDto> childrenOfCurrentComponent = DataFetcher.getChildComponentsOfComponent(componentArray[i], api, token);
				//remove children from list
				for (int y=0; y<childrenOfCurrentComponent.size(); y++) {
					componentList.remove(childrenOfCurrentComponent.get(y));
				}
			}
			
			//prepare output
			ComponentInfo[] output = new ComponentInfo[componentList.size()];
			for (int i=0; i<componentList.size(); i++){
				output[i] = new ComponentInfo();
				try{
					output[i].setId(componentList.get(i).getId().toString());
				} catch (Exception e){
					output[i].setId("");
				}
				try{
					output[i].setName(componentList.get(i).getName());
				} catch (Exception e){
					output[i].setName("");
				}
				try{
					output[i].setDescription(componentList.get(i).getDescription());
				} catch (Exception e){
					output[i].setDescription("");
				}
				try{
					output[i].setEstimatedhours(componentList.get(i).getEstimatedHours().toString());
				} catch (Exception e){
					output[i].setEstimatedhours("");
				}
				try{
					output[i].setShortdescription(componentList.get(i).getShortDescription());
				} catch (Exception e){
					output[i].setDescription("");
				}
				try{
					output[i].setPriority(componentList.get(i).getPriority().toString());
				} catch (Exception e){
					output[i].setPriority("");
				}
				try{
					output[i].setModifier(componentList.get(i).getModifier().getName());
				} catch (Exception e){
					output[i].setModifier("");
				}
			}
			return output;
			
			

		} catch (MalformedURLException e) {
			return null;
		}
	}
	

	
	
	/**
	 * Method that returns info about a certain project
	 * @param url : url of the codebeamer instance - needs to be encoded by UTF-8
	 * @param login : username of codebeamer user
	 * @param password : password of codebeamer user
	 * @param projectID : id of the appropriate project
	 * @return : array of projectinfo objects with information of any project
	 *
	 */
	@GET
	@Path("/getInfoForProjectObject")
	@Produces(MediaType.APPLICATION_JSON)
	public ProjectInfo getInfoForProjectObject(@QueryParam("url") @Encoded String url, @QueryParam("login") String login, @QueryParam("password") String password, @QueryParam("projectID") String projectID){
		//connecting
		RemoteApi api;
		try {
			
			String codeBeamerUrl = "";
			try{
				codeBeamerUrl = URLDecoder.decode(url, "UTF-8");
			} catch (Exception e) {
			}
			
			api = RemoteApiFactory.getInstance().connect(codeBeamerUrl);
			if(api == null) {
				return null;
			}
			//signing in
			String token = api.login(login, password);
			
			//retrieving project names
			ProjectDto project = api.findProjectById(token, Integer.parseInt(projectID));
			ProjectInfo output = new ProjectInfo();
			output.setId(projectID);
			try{
				output.setName(project.getName());
			} catch (Exception e){
				output.setName("");
			}
			try{
				output.setDescription(project.getDescription());
			} catch (Exception e){
				output.setDescription("");
			}
			try{
				output.setCategory(project.getCategory());
			} catch (Exception e){
				output.setCategory("");
			}
			try{
				output.setStart(project.getStartDate().toString());
			} catch (Exception e){
				output.setStart("");
			}
			try{
				output.setEnd(project.getEndDate().toString());
			} catch (Exception e){
				output.setEnd("");
			}
			try{
				output.setStatus(project.getStatus().toString());
			} catch (Exception e){
				output.setStatus("");
			}
			try{
				output.setCreator(project.getCreatedBy().getName());
			} catch (Exception e){
				output.setCreator("");
			}
			
			
			return output;

		} catch (MalformedURLException e) {
			return null;
		}

}
	
	/**
	 * Method that returns info about a certain component
	 * @param url : url of the codebeamer instance - needs to be encoded by UTF-8
	 * @param login : username of codebeamer user
	 * @param password : password of codebeamer user
	 * @param componentID : id of the appropriate component
	 * @return : array of componentinfo objects with information of any component
	 *
	 */
	@GET
	@Path("/getComponentInfo")
	@Produces(MediaType.APPLICATION_JSON)
	public ComponentInfo getComponentInfo(@QueryParam("url") @Encoded String url, @QueryParam("login") String login, @QueryParam("password") String password, @QueryParam("componentID") String componentID){
		//connecting
		RemoteApi api;
		try {
			
			String codeBeamerUrl = "";
			try{
				codeBeamerUrl = URLDecoder.decode(url, "UTF-8");
			} catch (Exception e) {
			}
				
			api = RemoteApiFactory.getInstance().connect(codeBeamerUrl);
			if(api == null) {
				return null;
			}
			//signing in
			String token = api.login(login, password);
			
			//retrieving component information
			TrackerItemDto component = api.findTrackerItemById(token, Integer.parseInt(componentID));
			ComponentInfo output = new ComponentInfo();
			
			try{
				output.setId(component.getId().toString());
			} catch (Exception e){
				output.setId("");
			}
			try{
				output.setName(component.getName());
			} catch (Exception e){
				output.setName("");
			}
			try{					
				output.setDescription(component.getDescription());
			} catch (Exception e){
				output.setDescription("");
			}
			try{
				output.setEstimatedhours(component.getEstimatedHours().toString());
			} catch (Exception e){
				output.setEstimatedhours("");
			}
			try{
				output.setShortdescription(component.getShortDescription());
			} catch (Exception e){
				output.setDescription("");
			}
			try{
				output.setPriority(component.getPriority().toString());
			} catch (Exception e){
				output.setPriority("");
			}
			try{
				output.setModifier(component.getModifier().getName());
			} catch (Exception e){
				output.setModifier("");
			}
			
			return output;
			
			

		} catch (MalformedURLException e) {
			return null;
		}
	}
	
	
	
	/**
	 * Method that returns all requirements for a project
	 * @param url : url of the codebeamer instance - needs to be encoded by UTF-8
	 * @param login : username of codebeamer user
	 * @param password : password of codebeamer user
	 * @param projectID : id of the appropriate project
	 * @return : array of requirements that also contains the graph
	 * 				of requirement associations
	 *
	 */
	@GET
	@Path("/getAllRequirementsForProject")
	@Produces(MediaType.APPLICATION_JSON)
	public RequirementInfo[] getAllRequirementsForProject(@QueryParam("url") @Encoded String url, @QueryParam("login") String login, @QueryParam("password") String password, @QueryParam("projectID") String projectID){
		//output list
		ArrayList<RequirementInfo> output = new ArrayList<RequirementInfo>();
		// connecting
		RemoteApi api;
		try {
			api = RemoteApiFactory.getInstance().connect(url);
			// signing in
			String token = "";
			try{
				token = api.login(login, password);
			} catch (AccessControlException e){
				RequirementInfo[] finalOutput = new RequirementInfo[0];
				return finalOutput;
			}
			
			if (!token.equals("")) {
				TrackerDto[] tracker = api.findTrackersByProject(token, Integer.parseInt(projectID));
				int idOfRequirements = 0;
				//get all trackers
				for (int b=0; b<tracker.length; b++){
					//get components
					if (tracker[b].getKeyName().equalsIgnoreCase("REQ")){
						//find associations from one requirement to another
						idOfRequirements = tracker[b].getId();

					}
				}
				//iterate all requirements
				TrackerItemDto[] items = api.findTrackerItemsByTrackerId(token, idOfRequirements);
				for (int c=0; c<items.length; c++) {
					//one requirement
					TrackerItemDto oneRequirement = items[c];
					ArrayList<TrackerItemDto> connectedRequirements = new ArrayList<TrackerItemDto>();
					
					//iterate all associations to other requirements
					AssociationDto[] associations = api.findAssociationsByEntity(token, new Integer(GroupType.TRACKER_ITEM), oneRequirement.getId(), false);
					for (int d=0; d<associations.length; d++) {
						AssociationDto oneAssociation = associations[d];
						
						try {
							TrackerItemDto connectedTrackerItem = api.findTrackerItemById(token, oneAssociation.getTo().getId());
							//check if connectedTrackerItem is requirement
							if (connectedTrackerItem.getTracker().getId().toString().equalsIgnoreCase("" + idOfRequirements)) {
								connectedRequirements.add(connectedTrackerItem);
							}
						} catch (NullPointerException e){
							RequirementInfo[] finalOutput = new RequirementInfo[0];
							return finalOutput;
						}
					}
					//trackeritem arraylist --> string array with ids
					String[] connectedRequirementNames = new String[connectedRequirements.size()];
					for (int i=0; i<connectedRequirements.size(); i++){
						connectedRequirementNames[i] = connectedRequirements.get(i).getId().toString();
					}
					//build output transfer object
					RequirementInfo currentRequirement = new RequirementInfo(oneRequirement.getId().toString(), oneRequirement.getName(), connectedRequirementNames);
					//set connected components
					ArrayList<TrackerItemDto> connectedComponents = DataFetcher.getIdsOfRelatedComponentsForRequirement(oneRequirement, api, token);
					//build string array of connectedComponent's ids
					String[] connectedCompIds = new String[connectedComponents.size()];
					for (int po=0; po<connectedComponents.size(); po++){
						connectedCompIds[po] = connectedComponents.get(po).getId().toString();
					}
					currentRequirement.setConnectedComponents(connectedCompIds);
					output.add(currentRequirement);
				}
				
				//output list to array
				RequirementInfo[] finalOutput = new RequirementInfo[output.size()];
				for (int i=0; i<output.size(); i++){
					finalOutput[i] = output.get(i);
				}
				
				return finalOutput;
			}
				
			} catch (MalformedURLException e1) {
				RequirementInfo[] finalOutput = new RequirementInfo[0];
				return finalOutput;
		} finally {}
		RequirementInfo[] finalOutput = new RequirementInfo[0];
		return finalOutput;
	}
	
	
	
	
	
	
	/**
	 * 
	 * helper method that searches for all components that are related to a passed requirement
	 *  - since the getAllComponentsForProject method only returns the parent nodes of components
	 *  	this one also only references parent components
	 * parameters: requirement under consideration, api and token
	 * return value: String array with ids of related components in the same project
	 */
private static ArrayList<TrackerItemDto> getIdsOfRelatedComponentsForRequirement(TrackerItemDto requirement, RemoteApi api, String token){
		
		
		TrackerDto[] tracker = api.findTrackersByProject(token, requirement.getProject().getId());
		
		//get id of component trackers
		int idOfComponents = 0;
		int idOfRequirements = 0;
		for (int b=0; b<tracker.length; b++){
			//get components
			if (tracker[b].getKeyName().equalsIgnoreCase("COMP")){
				//find associations from one component to another
				idOfComponents = tracker[b].getId();
			} else if (tracker[b].getKeyName().equalsIgnoreCase("REQ")) {
				idOfRequirements = tracker[b].getId();
			}
		}	
		
		//iterate all associations to requirements
		ArrayList<TrackerItemDto> connectedComponents = new ArrayList<TrackerItemDto>();
		AssociationDto[] associations = api.findAssociationsByEntity(token, new Integer(GroupType.TRACKER_ITEM), requirement.getId(), true);
		for (int d=0; d<associations.length; d++) {
			AssociationDto oneAssociation = associations[d];
			
			//look for related components
			try {
				//don't know which of the two sides is the requirement and which one is the component
				TrackerItemDto v1 = api.findTrackerItemById(token, oneAssociation.getFrom().getId());
				TrackerItemDto v2 = api.findTrackerItemById(token, oneAssociation.getTo().getId());
				//check if connectedTrackerItem is requirement
				if (v1.getTracker().getId().toString().equalsIgnoreCase("" + idOfComponents)) {
					connectedComponents.add(v1);
				} else if (v2.getTracker().getId().toString().equalsIgnoreCase("" + idOfComponents)) {
					connectedComponents.add(v2);
				}
			} catch (NullPointerException e){
				System.out.println("NullpointerException");
			}
			
			//look for parent requirements and add their related components
			if (oneAssociation.getTypeId() == AssociationTypeDto.CHILD) {
				try {
					//if child is a component, add it to output
					TrackerItemDto childRequirement = api.findTrackerItemById(token, oneAssociation.getTo().getId());
					TrackerItemDto parentRequirement = api.findTrackerItemById(token, oneAssociation.getFrom().getId());
					if (childRequirement.getId().toString().equalsIgnoreCase("" + requirement.getId())) {
						if (parentRequirement.getTracker().getId().toString().equalsIgnoreCase("" + idOfRequirements)) {
							ArrayList<TrackerItemDto> relatedComponentsOfParentRequirement = DataFetcher.getIdsOfRelatedComponentsForRequirement(parentRequirement, api, token);
							//only add those that are not already in there
							for (int al=0; al<relatedComponentsOfParentRequirement.size(); al++) {
								TrackerItemDto oneComponent = relatedComponentsOfParentRequirement.get(al);
								if (!connectedComponents.contains(oneComponent)) {
									connectedComponents.add(oneComponent);
								}
							}
						}
					}
					
					
				} catch (NullPointerException e){
					System.out.println("NullpointerException");
				}
				
			}
		}
		
		/*
		 * get only father components as references
		 */
		//child components will be removed in the end
		ArrayList<TrackerItemDto> componentsToRemove = new ArrayList<TrackerItemDto>();
		//index of components in connectedComponents --> will grow when components are added
		int maxIndex = connectedComponents.size();
		for (int i=0; i<maxIndex; i++){
			TrackerItemDto oneComponent = connectedComponents.get(i);
			//find all related components
			associations = api.findAssociationsByEntity(token, new Integer(GroupType.TRACKER_ITEM), oneComponent.getId(), false);
			for (int d=0; d<associations.length; d++) {
				AssociationDto oneAssociation = associations[d];
				//filter parent components
				if (oneAssociation.getTypeId() == AssociationTypeDto.CHILD) {
					try {
						//if child is a component, add it to output
						TrackerItemDto childComponent = api.findTrackerItemById(token, oneAssociation.getTo().getId());
						TrackerItemDto parentComponent = api.findTrackerItemById(token, oneAssociation.getFrom().getId());
						if (childComponent.getTracker().getId().toString().equalsIgnoreCase(oneComponent.getTracker().getId().toString())) {
							//add parent components to connectedcomponents
							if (!componentsToRemove.contains(childComponent)) {
								componentsToRemove.add(childComponent);
							}
							if (!connectedComponents.contains(parentComponent)) {
								connectedComponents.add(parentComponent);
								maxIndex++;
							}
							
							
						}
					} catch (NullPointerException e){
						System.out.println("NullpointerException");
					}
				}
			}
		}
		//remove child components
		connectedComponents.removeAll(componentsToRemove);
		return connectedComponents;
	}
							
	
	
	/**
	 * 
	 * helper method that checks if the passed component has any child components
	 * parameters: the component, remote api and token
	 * return value: true or false
	 */
	private static boolean componentHasAtLeastOneChild(TrackerItemDto component, RemoteApi api, String token) {
		//get all associations of component
		AssociationDto[] associations = api.findAssociationsByEntity(token, new Integer(GroupType.TRACKER_ITEM), component.getId(), false);
		for (int d=0; d<associations.length; d++) {
			AssociationDto oneAssociation = associations[d];
			
			if (oneAssociation.getTypeId() == AssociationTypeDto.CHILD) {
				try {
					//if child is a component, add it to output
					TrackerItemDto child = api.findTrackerItemById(token, oneAssociation.getTo().getId());
					if (child.getTracker().getId().toString().equalsIgnoreCase(component.getTracker().getId().toString())) {
						return true;
					}
				} catch (NullPointerException e){
					System.out.println("NullpointerException");
				}
			}
		}
		//if no assosiation with type child has been found
		return false;
		
	}
	
	/**
	 * 
	 * helper method that checks if the passed component has any child components
	 * parameters: the component, remote api and token
	 * return value: ArrayList of child components, if there are
	 */
	private static ArrayList<TrackerItemDto>getChildComponentsOfComponent(TrackerItemDto component, RemoteApi api, String token) {
		//initialize output array
		ArrayList<TrackerItemDto> outputArrayList = new ArrayList<TrackerItemDto>();
		//get all associations of component
		AssociationDto[] associations = api.findAssociationsByEntity(token, new Integer(GroupType.TRACKER_ITEM), component.getId(), false);
		for (int d=0; d<associations.length; d++) {
			AssociationDto oneAssociation = associations[d];
			
			if (oneAssociation.getTypeId() == AssociationTypeDto.CHILD) {
				try {
					//if child is a component, add it to output
					TrackerItemDto child = api.findTrackerItemById(token, oneAssociation.getTo().getId());
					if (child.getTracker().getId().toString().equalsIgnoreCase(component.getTracker().getId().toString())) {
						outputArrayList.add(child);
					}
				} catch (NullPointerException e){
					System.out.println("NullpointerException");
				}
			}
		}
		return outputArrayList;
	}
}


