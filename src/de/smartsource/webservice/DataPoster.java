package de.smartsource.webservice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AccessControlException;

import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

import com.intland.codebeamer.manager.AccessRightsException;
import com.intland.codebeamer.manager.util.ArtifactNameConflictException;
import com.intland.codebeamer.manager.util.ChangeVetoedException;
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.remoting.DescriptionFormat;
import com.intland.codebeamer.remoting.RemoteApi;
import com.intland.codebeamer.remoting.RemoteApiFactory;
import com.intland.codebeamer.servlet.docs.DocumentSccException;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
 
@Path("/DataPoster")
public class DataPoster {
 
	@POST
	@Path("/pdfUpload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public String[] pdfUpload(
		@QueryParam("url") @Encoded String url, 
		@QueryParam("login") String login, 
		@QueryParam("password") String password,
		@QueryParam("projectID") String projectId,
		@FormDataParam("file") InputStream uploadedInputStream,
		@FormDataParam("file") FormDataContentDisposition fileDetail) {
 

		
		
		
 
		// save it
		if (uploadedInputStream != null){
			return uploadFileToCodeBeamer(url, login, password, projectId, uploadedInputStream);
		} else {
			String[] out = {"input is null"};
			return out;
		}
		
	}
	
	/**
	 * Method returns all Projects from a platform
	 * @param codeBeamerUrl : url of the codebeamer instance - needs to be encoded by UTF-8
	 * @param login : username of codebeamer user
	 * @param password : password of codebeamer user
	 * @param uploadedInputStream : inputStream with file to upload
	 * @return : Stringarray of all projects from the platform
	 *
	 */
	private String[] uploadFileToCodeBeamer(String codeBeamerUrl, String login, String password, String projectId,  InputStream uploadedInputStream) {
		
		//signing in
		RemoteApi api;
		try {
			api = RemoteApiFactory.getInstance().connect(codeBeamerUrl);
		} catch (MalformedURLException e) {
			String[] out = {"error"};
			return out;
		}
		// signing in
		String token = "";
		try{
			token = api.login(login, password);
		} catch (AccessControlException e){
		}
		
		//get right project
		ProjectDto project = api.findProjectById(token, Integer.parseInt(projectId));	
		//create directory
		ArtifactDto dir = new ArtifactDto();
		dir.setProject(project);
		dir.setName("SmartSourcer Results");
		dir.setDescription("Directory contains PDF reports of outsourcing decision recommendations generated by SmartSourcer.");
		dir.setOwner(api.getSessionUser(token));
		dir.setTypeId(2);
		
		//create artifact
		try {
			dir = api.createArtifact(token, dir);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ArtifactDto doc = new ArtifactDto();
		doc.setProject(project);
		doc.setParent(dir);
		String documentName = "SmartSourcer Results - " + project.getName() + ".pdf";
		doc.setName(documentName);
		doc.setDescription("Outsourcing Decision recommendations generated by SmartSourcer");
		doc.setOwner(api.getSessionUser(token));
		doc.setTypeId(1);
		
		//save file
		byte[] bytes = "".getBytes();
		try {
			bytes = IOUtils.toByteArray(uploadedInputStream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("started");
		try {
			doc = api.createAndUploadArtifact(
			          token,
			          doc,
			          bytes,
			          "Initial checkin.",
			          DescriptionFormat.PLAIN_TEXT);
		} catch (ArtifactNameConflictException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AccessRightsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DocumentSccException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ChangeVetoedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String[] out = {"Success"};
		return out;
		

	}
 
	// save uploaded file to new location
	private String[] writeToFile(InputStream uploadedInputStream,
		String uploadedFileLocation) {
 
		try {
			OutputStream out = new FileOutputStream(new File(
					uploadedFileLocation));
			int read = 0;
			byte[] bytes = new byte[1024];
 
			out = new FileOutputStream(new File(uploadedFileLocation));
			while ((read = uploadedInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();
			
			String[] output = {"Success"};
			return output;
		} catch (IOException e) {
 
			String[] output = {"Caught Exception"};
			e.printStackTrace();
			return output;
			
		}
 
	}
 
}