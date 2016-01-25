package pc.main;

import java.io.*;
import java.util.*;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import pc.BookHandler;
import pc.ChapterIsAllocatedException;
import pc.FTPConnection;
import pc.LoginFailedException;
import pc.OutXmlNotFoundException;
import pc.UnzipUtility;
import pc.ZeroFileException;
import pc.workflow.WorkFlow3;
import pc.xml.OutXmlFilter;
public class DoBookWork extends TimerTask{

	static final Logger log = Logger.getLogger(DoBookWork.class);
	private static BookHandler book;
	private static boolean isFtpXmlDownloaded=true;
	private static boolean isFtpZipDownloaded=true;
	static FTPConnection newCon=null;
	static final String bookOutXmlPath="D:"+File.separator+"BooksOutXml";
	static final String unzipDir=bookOutXmlPath+File.separator+"Extracted";
	public static void main(String[] args) throws ZeroFileException, IOException, JAXBException, ParserConfigurationException, SAXException, LoginFailedException {
		
		book=new BookHandler();
		Timer time=new Timer();
		time.schedule(new DoBookWork(),0,100000);
	}
	
	private static String searchCriteria(BookHandler bookhandler){
		return "B9780128044230000020-MC";
	}
	
	private static void  initDownloading() {
		try
		{
			List<BookHandler> bookList=book.getListOfBooks();
			newCon=new FTPConnection("ftp.elsevierproofcentral.com", "tombk", "xEWKW3e2h");
			if(!newCon.checkConnection())
				newCon.connect();
			System.out.println("Connected to ftp.elsevierproofcentral.com");
			List<String> mcFiles=newCon.filterFileNames("Signals", "MC");
			System.out.println("MC "+mcFiles.size());
			for(BookHandler bh:bookList){
				if(bh.isChapterNotAllocated()){
					FTPFile[] ftpFile=searchXMLandZIP(mcFiles, bh);
					if(ftpFile[0]==null && ftpFile[1]==null ){
						log.error("XML And Zip Both Not Found On FTP Server For "+bh.getJid().toUpperCase()+" "+bh.getAid().toUpperCase());
						return;
					}
					isFtpXmlDownloaded=newCon.ftpDownload(ftpFile[0], bookOutXmlPath+ftpFile[0].getName());
					isFtpZipDownloaded=newCon.ftpDownload(ftpFile[1], bookOutXmlPath+ftpFile[1].getName());
					if(isFtpXmlDownloaded && isFtpZipDownloaded){
						String zipOnDisk=bookOutXmlPath+ftpFile[1].getName();
						new UnzipUtility().unzip(zipOnDisk, unzipDir);
					}
					new OutXmlFilter(new File("D:/1/extracted/B9780128044230000020_out.xml"), bh);
	
				}
			}
		}
		catch(ZeroFileException zfe)
		{
			log.error(zfe.getLocalizedMessage());
		} catch (IOException ie) {
			log.error(ie.getLocalizedMessage());
			ie.printStackTrace();
		} catch (LoginFailedException lfe) {
			log.error(lfe.getLocalizedMessage());
		} catch (JAXBException jbe) {
			log.error(jbe.getLocalizedMessage());
		} catch (ParserConfigurationException pce) {
			log.error(pce.getLocalizedMessage());
		} catch (SAXException se) {
			log.error(se.getLocalizedMessage());
		}catch (ChapterIsAllocatedException ciae) {
			log.error(ciae.getLocalizedMessage());
		}catch (OutXmlNotFoundException oxnf) {
			log.error(oxnf.getLocalizedMessage());
		}
		
		}
	
	private static FTPFile[] searchXMLandZIP(List<String> ftpFiles,BookHandler bh){
		boolean isXMLFound=false;
		boolean isZIPFound=false;
		
		FTPFile xmlFile=new FTPFile();
		FTPFile zipFile=new FTPFile();
		for(String aFile:ftpFiles){
			if(aFile.indexOf(searchCriteria(bh))!=-1){
				System.out.println("XML found on ftp");
				isXMLFound=true;
				xmlFile.setName(aFile);
				break;
			}else{
				System.out.println("XML not found");
			}
		}
		if(isXMLFound){
			String xmlNameWE=xmlFile.getName().substring(0, xmlFile.getName().lastIndexOf("."));
			System.out.println(xmlNameWE+".zip");
			if(ftpFiles.contains(xmlNameWE+".zip")){
				isZIPFound=true;
				zipFile.setName(xmlNameWE+".zip");
				System.out.println("Zip file also found");
			}else{
				System.out.println("Zip file not found");
			}
		}
		if(isXMLFound && isZIPFound){
			return new FTPFile[]{xmlFile,zipFile};
		}else{
			System.out.println("Both not found");
			return new FTPFile[]{null,null};
		}
	}
	boolean finishDepartment(BookHandler bk){
		WorkFlow3 wf=new WorkFlow3();
		int isAllocated=wf.allocateToken(bk.getAccount(), bk.getCategory(), bk.getJid(), bk.getAid(), bk.getDept(), "USER1");
		int isFinished=wf.finishToken(bk.getAccount(), bk.getCategory(), bk.getJid(), bk.getAid(), bk.getDept());
		if(isAllocated==0 && isFinished==0)
			return true;
		return false;
	}
	@Override
	public void run() {
		initDownloading();
		
	}
}
