package pc;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import pc.xml.BooksShemaBuilder;
public class BookHandler {
	
	private String account;
	private String category;
	private String jid;
	private String aid;
	private String absJournalPath;
	private String dept="";
	
	private String dbPath;
	private String pii="";
	private String server;
	private static int fileCount=0;
	private String isbn;
	private String bookOrderPath;
	private boolean isXMLOrderFound=true;
	private boolean isChapterNotAllocated=true;
	
	public boolean isXMLOrderFound() {
		return isXMLOrderFound;
	}

	public void setXMLOrderFound(boolean isXMLOrderFound) {
		this.isXMLOrderFound = isXMLOrderFound;
	}

	public boolean isChapterNotAllocated() {
		return isChapterNotAllocated;
	}

	public void setChapterNotAllocated(boolean isChapterNotAllocated) {
		this.isChapterNotAllocated = isChapterNotAllocated;
	}

	static BooksShemaBuilder schemaBuilder;
	
	public String getPii() {
		
		return pii;
	}
	public String getDept() {
		return dept;
	}


	public void setPii(String pii) {
		this.pii = pii;
	}

	public int getFileCount() {
		return fileCount;
	}

	public void setFileCount(int fileCount) {
		this.fileCount = fileCount;
	}

	public String getDbPath() {
		return "D:"+File.separator+"FMS"+File.separator+"DB"+File.separator+"DEPT"+File.separator+dept;
	}

	public void setDbPath(String dbPath) {
		this.dbPath = dbPath;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}
	
	public BookHandler() throws JAXBException, ParserConfigurationException, SAXException {
		dept="NPD_WAIT";
		account="ELSEVIER";
		category="BOOKS";
		schemaBuilder=BooksShemaBuilder.getInstance();

	}
	
	public BookHandler(String server,String dept) throws JAXBException, ParserConfigurationException, SAXException {
		this();
		this.dept=dept;
		if(server.isEmpty())
			this.server="";
		this.server=server;
	}
	
	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

	public String getAid() {
		return aid;
	}

	public void setAid(String aid) {
		this.aid = aid;
	}

	public String getAbsJournalPath() {
		//return "\\\\Fmsbooks\\D$"+File.separator+"fms"+File.separator+"centralized_server"+File.separator+account+File.separator+category+File.separator+jid+File.separator+aid;
		return "D:"+File.separator+"fms"+File.separator+"centralized_server"+File.separator+account+File.separator+category+File.separator+jid+File.separator+aid;

	}

	public void setAbsJournalPath(String absJournalPath) {
		this.absJournalPath = absJournalPath;
	}

	private  List<String> findChaptersInDept()throws ZeroFileException, IOException{
		
		if(!new File(this.getDbPath()).exists())
			throw new FileNotFoundException(this.dept +" not found");
		List<String> listedFiles=null;
		File[] iniFiles=new File(this.getDbPath()).listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if(pathname.isDirectory() || (!pathname.getName().endsWith(".ini") && !pathname.getName().endsWith(".INI")))
					return false;
				if(pathname.getName().split("_").length==5)
					return true;
				else
					return false;
			}
		});
		
		if(iniFiles.length==0)
			throw new ZeroFileException("No file(s) exist inside "+ErrorCode.ZERO_FILE_FOUND +" "+dept);
		this.setFileCount(iniFiles.length);
		listedFiles=new ArrayList<String>();
		for(File aFile:iniFiles)
		{
			
				listedFiles.add(aFile.getName().substring(0,aFile.getName().lastIndexOf('.')));
		}
		return listedFiles.size()>0 ? listedFiles : null;
	}
	
	public boolean isChapterNotAllocatedInWait(BookHandler bk) throws IOException, ChapterIsAllocatedException{
		InputStream is=null;
		String wf=bk.getAbsJournalPath()+File.separator+"Attributes"+File.separator+"workflow.ini";
		if(!new File(wf).exists())
			throw new FileNotFoundException(wf +" not found "+ErrorCode.WORKFLOW_NOT_FOUND);
		Properties wp=new Properties();
		is=new FileInputStream(wf);
		wp.load(is);
		is.close();
		if(wp.getProperty(bk.dept+".TOKENPATH", "")!=null){
			if(!wp.getProperty(bk.dept+".TOKENPATH", "").equalsIgnoreCase("")){
				if(wp.getProperty(bk.dept+".TOKENPATH", "").split("\\\\").length==5 && (wp.getProperty(this.dept+".STATUS", "").indexOf("NOTALLOCATED")!=-1 || wp.getProperty(this.dept+"STATUS", "").indexOf("FALSE")!=-1)){
					return true;
				}else
					throw new ChapterIsAllocatedException("Chapter "+bk.getAid()+" Of "+bk.getJid()+" Is Allocated To Someone");
				
			}
		}
		return false;
	}
	
	public BookHandler instanceInfo(int pos) throws ZeroFileException, IOException, JAXBException, ParserConfigurationException, SAXException, ChapterIsAllocatedException{
		
		
		BookHandler book=new BookHandler();
		List<String> inputList=findChaptersInDept();
		String aToken=inputList.get(pos);
		String[] token=aToken.split("_");
		book.setAccount(token[1]);
		book.setCategory(token[2]);
		book.setJid(token[3]);
		book.setAid(token[4]);
		book.setIsbn(getIsbn(book));
		book.setDbPath("D:"+File.separator+"FMS"+File.separator+"DB"+File.separator+"DEPT"+File.separator+dept);
		book.setAbsJournalPath("D:"+File.separator+"fms"+File.separator+"centralized_server"+File.separator+account+File.separator+category+File.separator+book.jid+File.separator+book.aid);
		book.setChapterNotAllocated(isChapterNotAllocatedInWait(book));
		System.out.println(book.getBookOrderPath());
		try{
			schemaBuilder.initUnmarshaller(book.getBookOrderPath());
		}catch(FileNotFoundException fne){
			if(fne.getLocalizedMessage().indexOf(ErrorCode.XML_ORDER_NOT_FOUND)!=-1){
				book.setXMLOrderFound(false);
			}
		}
		book.setPii(schemaBuilder.getPII(Integer.parseInt(book.getAid())));
		return book;
	}
	
	public List<BookHandler> getListOfBooks() throws ZeroFileException, IOException, JAXBException, ParserConfigurationException, SAXException, ChapterIsAllocatedException{
		List<BookHandler> list=new ArrayList<>();
		for(int index=0;index<findChaptersInDept().size();index++){
			try{
			
			list.add(instanceInfo(index));
			}catch(FileNotFoundException ex){
				System.out.println(ex.getLocalizedMessage());
				if(ex.getLocalizedMessage().indexOf(ErrorCode.XML_ORDER_NOT_FOUND)!=-1){
					continue;
				}
			}
		}
		return list;
		
		
	}
	
	public String getBookOrderPath() throws FileNotFoundException {
		String tdnas="\\\\td-nas"+File.separator+"Elsinpt"+File.separator+"ElsBook"+File.separator+"Orders"+File.separator;
		if(!new File(tdnas).exists())
			throw new FileNotFoundException("TD-NAS(172.16.0.44) is disconnected from FMSBOOKS(172.16.0.46) "+ErrorCode.PATH_NOT_CONNECTED);
		String tdorder=tdnas+"S&T"+File.separator+getIsbn()+File.separator+"Q300"+File.separator+"Current_Order"+File.separator+"S&T_"+getIsbn()+"_Q300.xml";
		if(!new File(tdorder).exists())
			tdorder=tdnas+"EHS"+File.separator+getIsbn()+File.separator+"Q300"+File.separator+"Current_Order"+File.separator+"S&T_"+getIsbn()+"_Q300.xml";
		if(!new File(tdorder).exists())
			tdorder=tdnas+"PPM"+File.separator+getIsbn()+File.separator+"Q300"+File.separator+"Current_Order"+File.separator+"S&T_"+getIsbn()+"_Q300.xml";
		if(!new File(tdorder).exists())
			throw new FileNotFoundException(new File(tdorder).getName() +" is not found on TD-NAS for "+this.getJid()+" "+this.getAid()+".Hence skipping downloading ProofCentral package for this chapter.Thanks! "+ErrorCode.XML_ORDER_NOT_FOUND+" "+ErrorCode.SKIP_PC_XML);
		return tdorder;
	}

	public void setBookOrderPath(String bookOrderPath) {
		this.bookOrderPath = bookOrderPath;
	}

	public String getIsbn() {
		return isbn.replace("-", "");
	}
	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}
	
	public String toString()
	{
		try {
			return new StringBuilder().append("Account: ").append(getAccount()).append("\nCategory: ").append(getCategory())
					.append("\nBOOK: ").append(getJid()).append("\nCHAPTER: ").append(getAid()).append("\nJOB PATH: ").append(getAbsJournalPath())
					.append("\nDB PATH: ").append(getDbPath()).append("\nISBN: ").append(getIsbn()).append("\nPII: ").append(getPii()).append("\nORDER PATH: ").append(getBookOrderPath()).toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return "";
			
		}
	}
	
	public String getIsbn(BookHandler bh)throws IOException{
		
		InputStream is=null;
		File aid=new File(bh.getAbsJournalPath()+File.separator+"Attributes"+File.separator+"aid.ini");
		if(!aid.exists())
			throw new FileNotFoundException(aid.getAbsolutePath() +" not found "+ErrorCode.AID_FILE_NOT_FOUND);
		Properties aidProp=new Properties();
		is=new FileInputStream(aid);
		aidProp.load(is);
		is.close();
		return aidProp.getProperty("isbn", "");
	}
	
	public static void main(String[] args) throws ZeroFileException, IOException {
		//BookHandler book1=new BookHandler();
	
		
	}
}
