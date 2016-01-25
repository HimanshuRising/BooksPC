package pc.xml;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import pc.ErrorCode;

public class BooksShemaBuilder {

	Unmarshaller thisUnmarshaller;
	JAXBContext orderContext;
	Chapters chapters;
	SAXParserFactory spf;
	XMLReader xmlReader;
	private static BooksShemaBuilder instance=null;
	

	private BooksShemaBuilder()throws JAXBException, ParserConfigurationException, SAXException{
		
		if(orderContext==null){
			orderContext=JAXBContext.newInstance(Orders.class);
			System.out.println("JAXB created");
		}
		if(thisUnmarshaller==null && orderContext!=null){
			System.out.println("Unmarshaller created");
			thisUnmarshaller=this.orderContext.createUnmarshaller();
			spf = SAXParserFactory.newInstance();
			spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	        spf.setFeature("http://xml.org/sax/features/validation", false);
	        xmlReader = spf.newSAXParser().getXMLReader();
	  }
		
	}
	
	public static BooksShemaBuilder getInstance() throws JAXBException, ParserConfigurationException, SAXException {
		if(instance==null){
			instance=new BooksShemaBuilder();
		}
		return instance;
	}
	
	public void initUnmarshaller(String xmlFile) throws JAXBException, FileNotFoundException{
		
		if(!new File(xmlFile).exists())
			throw new FileNotFoundException(xmlFile +" not found "+ErrorCode.XML_ORDER_NOT_FOUND);
		InputSource inputSource = new InputSource(new FileInputStream(xmlFile));
		SAXSource source = new SAXSource(xmlReader, inputSource);
		Orders rootOrder=(Orders)thisUnmarshaller.unmarshal(source);
		chapters=rootOrder.getChapters();
	}
	
	public Map<Integer,String> getChapterInfo(){
		
		Map<Integer,String> chapterMap=new HashMap<Integer,String>();
		List<Chapter> chapterList=getChapterList();
		for(Chapter aChapter:chapterList){
			chapterMap.put(Integer.parseInt(aChapter.getAid()), aChapter.getPii());
		}
		return chapterMap;
		
	}
	
	public List<Chapter> getChapterList(){
		return chapters.getChapter();
	}
	
	public String getPII(Integer chapter){
		
		return getChapterInfo().get(chapter); 
	}
	
	//Test
	public static void main(String[] args) throws JAXBException, IOException, ParserConfigurationException, SAXException {
		
		URL url=new URL("file:////td-nas//elsinpt//ElsBook//Orders//S&T//9780000000187//O300//Current_Order//S&T_9780000000187_O300.xml");
		//url.openConnection().setConnectTimeout(1000);
		
		BooksShemaBuilder p=new BooksShemaBuilder();
		 SAXParserFactory spf = SAXParserFactory.newInstance();
        //spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
		 spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        spf.setFeature("http://xml.org/sax/features/validation", false);
        XMLReader xmlReader = spf.newSAXParser().getXMLReader();
        InputSource inputSource = new InputSource(url.openStream());
        SAXSource source = new SAXSource(xmlReader, inputSource);
       
			
		
		while(true){
		System.out.println(p.getChapterInfo().size());
		System.out.println(p.getPII(0002));
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		}
		}
	}
}
