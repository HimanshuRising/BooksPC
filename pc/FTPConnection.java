package pc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class FTPConnection {

	private String serverName;
	private String userName;
	private String password;
	private FTPClient thisFTPConn;
	private List<FTPFile> serverFiles;
	private List<String> contents;
	
	public FTPConnection(String serverName,String userName,String password){
		
		this.serverName=serverName;
		this.userName=userName;
		this.password=password;
		this.thisFTPConn=new FTPClient();
	}
	
	public void connect() throws SocketException, IOException,LoginFailedException{
		
		//connecting to server
		this.thisFTPConn.connect(this.serverName);
		
		//authenticate the username and passowrd if not throws the exception 
		boolean isConnected=this.thisFTPConn.login(this.userName, this.password);
		if(!isConnected){
			
			throw new LoginFailedException("Unable to Logged in FTP error "+thisFTPConn.getReplyCode());
		}
		//connection is made
		
		this.serverFiles=new ArrayList<FTPFile>();
		this.contents=new ArrayList<String>();
	}
	
	public String getReply(){
		
		return this.thisFTPConn.getReplyString();
	}
	
	public void listServerFiles(String directory) throws IOException,ZeroFileException{
		
		this.thisFTPConn.sendNoOp();
		this.thisFTPConn.changeWorkingDirectory(directory);
		String currentDir=this.thisFTPConn.printWorkingDirectory();
		System.out.println("Current working directory is "+currentDir);
		FTPFile[] files=this.thisFTPConn.listFiles();
		if(files.length==0)
			throw new ZeroFileException("No files inside "+currentDir + " "+ErrorCode.FTP_ZERO_FILE_FOUND);
		for(FTPFile aFile:files){
			
			if(aFile.isDirectory())
				continue;
			this.serverFiles.add(aFile);
			this.contents.add(aFile.getName().toUpperCase());
		}
	}
	
	public boolean checkConnection(){
		
		return this.thisFTPConn.isConnected();
	}
	
	public List<FTPFile> getFiles(String directory) throws IOException, ZeroFileException{
		
		listServerFiles(directory);
		return this.serverFiles;
	}
	
	public List<FTPFile> filterFiles(String directory,String searchCriteria) throws IOException, ZeroFileException	{
		List<FTPFile> ftpFiles=getFiles(directory);
		List<FTPFile> mcFiles=new ArrayList<FTPFile>();
		for(FTPFile aFile:ftpFiles){
			if(aFile.getName().indexOf("-"+searchCriteria+"-")!=-1)
				mcFiles.add(aFile);
		}
		return mcFiles;

	}
	public List<String> filterFileNames(String directory,String searchCriteria) throws IOException, ZeroFileException	{
		List<String> fileNames=new ArrayList<String>();
		List<FTPFile> allFiles=filterFiles(directory,searchCriteria);
		for(FTPFile aFile:allFiles)
			fileNames.add(aFile.getName());
		return fileNames;
	}
	
	public List<String> getFileNames(String directory) throws IOException, ZeroFileException{
		
		listServerFiles(directory);
		return this.contents;
	}
	
	public void logout() throws IOException{
		
		this.thisFTPConn.logout();
	}
	public void printFTPFiles(List<FTPFile> list){
		for(FTPFile aFile:list)
			System.out.println("PRINT : "+aFile.getName());
	}
	
	public boolean ftpDownload(FTPFile file,String outputName) throws IOException{
		OutputStream os=null;
		try
		{
		os=new FileOutputStream(new File(outputName));
		boolean isDownloaded=this.thisFTPConn.retrieveFile(file.getName(), os);
		if(isDownloaded)
			System.out.println(file.getName() +" has been downloaded successfully.");
		return isDownloaded;
		}finally{
			if(os!=null)
				os.close();
		}
		}
	
	public static void main(String[] args) throws SocketException, IOException, LoginFailedException, ZeroFileException {
		
		FTPConnection newCon=new FTPConnection("ftp.elsevierproofcentral.com", "tombk", "xEWKW3e2h");
		if(!newCon.checkConnection())
			newCon.connect();
		System.out.println("Connected to ");
		List<FTPFile> allFiles=newCon.filterFiles("Signals","MC");
		for(FTPFile aFile:allFiles)
		{
			System.out.println(aFile);
			newCon.ftpDownload(aFile, "D:\\1\\"+aFile.getName());
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
