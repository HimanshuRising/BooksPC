package pc.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import pc.BookHandler;
import pc.OutXmlNotFoundException;

public class OutXmlFilter {

	File xmlFile;
	BookHandler book;
	public OutXmlFilter() {
		
	}
	
	public OutXmlFilter(File xmlFile,BookHandler book) throws OutXmlNotFoundException, UnsupportedEncodingException, FileNotFoundException, IOException, ParserConfigurationException, SAXException{
		this.xmlFile=xmlFile;
		this.book=book;
		if(!xmlFile.exists())
			throw new OutXmlNotFoundException(xmlFile.getName()+" Not Found inside "+this.xmlFile.getParent());
		System.out.println(xmlFile.getAbsolutePath());
		StringBuilder xmlContent=new StringBuilder(readFileUTF8(xmlFile.getAbsolutePath()));
		filterAndCopyXML(xmlContent, xmlFile);
	}
	
	public String readFileUTF8(String str) throws UnsupportedEncodingException, FileNotFoundException,IOException{
		try
		{
		String line="";
		BufferedReader buffReader = new BufferedReader(new InputStreamReader(new FileInputStream(str),"UTF8"));
		line=buffReader.readLine();
		buffReader.close();
		return line;
		}catch(Exception ex){ex.printStackTrace();
		return "";
		}
	}
	
	public void filterAndCopyXML(StringBuilder xmlContent,File inputFile) throws ParserConfigurationException, FileNotFoundException, IOException, SAXException{
		
			boolean commentStatus=false;
			String local=xmlContent.toString();
			if(commentStatus==false)
			{
				local=local.replaceAll("<opt_INS>","");
	    		local=local.replaceAll("</opt_INS>","");
	    		xmlContent=new StringBuilder(local);
				while(xmlContent.indexOf("<opt_DEL")>0){
					xmlContent.replace(xmlContent.indexOf("<opt_DEL"), (xmlContent.indexOf("</opt_DEL>",xmlContent.indexOf("<opt_DEL"))+10), "");
		
				}
				while(xmlContent.indexOf("<DEL")>0){
					xmlContent.replace(xmlContent.indexOf("<DEL"), (xmlContent.indexOf("</DEL>",xmlContent.indexOf("<DEL"))+6), "");
				}
				while(xmlContent.indexOf("<!--total")>0){
					xmlContent.replace(xmlContent.indexOf("<!--total"), (xmlContent.indexOf("-->",xmlContent.indexOf("<!--total"))+3), "");
				}
				while(xmlContent.indexOf("<!--Q")>0){
					xmlContent.replace(xmlContent.indexOf("<!--Q"), (xmlContent.indexOf("-->",xmlContent.indexOf("<!--Q"))+3), "");
				}
			}
			String temp=removeEmptyTag(xmlContent.toString()).replaceAll("  "," ");
			temp=temp.replaceAll("<ce:abstract-sec id=\"abst([0-9]+)\"><ce:simple-para id=\"spar([0-9]+)\">dd</ce:simple-para></ce:abstract-sec>","<ce:abstract-sec id=\"abst$1\"><ce:simple-para id=\"spar$2\"></ce:simple-para></ce:abstract-sec>");
			xmlContent=new StringBuilder(temp);
			File orgXML=new File(inputFile.getParent()+File.separator+inputFile.getName().substring(0, inputFile.getName().indexOf(".xml"))+"_org.xml");
			copyFile(inputFile,orgXML);
			WriteFileUTF8(inputFile.getAbsolutePath(),xmlContent);
			System.out.println("file writtern");
			new Utf82ent().process(inputFile.getAbsolutePath());
			System.out.println("dest file "+orgXML);
			File tempFile=new File(inputFile.getAbsolutePath());
			System.out.println("tm "+tempFile);
			new VTool().runVtool(inputFile.getAbsolutePath());
			boolean errorStatus=new VTool().isVtoolError(xmlContent+File.separator+tempFile.getName()+".log.xml");
			System.out.println("Vtool Error Status ");
			tempFile.delete();
			}

		public static String removeEmptyTag(String XMLtext) throws FileNotFoundException, IOException{
			Properties properties=new Properties();
			properties.load(new FileInputStream("d:\\fms_queue\\classes\\XMLTAGNAME.ini"));
			Set<Entry<Object,Object>> setObj=properties.entrySet();
			Iterator<Entry<Object, Object>> lIterator = setObj.iterator();  
			int setSize=setObj.size();
			int count=0;
			while (lIterator.hasNext()){  
				Map.Entry lEntry = (Map.Entry) lIterator.next();  
				String lKey = (String)lEntry.getKey();  
				String lValue = (String)lEntry.getValue();  
				XMLtext=XMLtext.replaceAll(" <"+lValue+"(>| ([^<>]+)>) "," <"+lValue+"$1");
				XMLtext=XMLtext.replaceAll("<"+lValue+"(>| ([^>\\/]+)>) ","<"+lValue+"$1");
				XMLtext=XMLtext.replaceAll(" </"+lValue+"> ","</"+lValue+"> ");
				XMLtext=XMLtext.replaceAll(" </"+lValue+">","</"+lValue+">");
				if(!lValue.startsWith("mml")){
					XMLtext=XMLtext.replaceAll("<"+lValue+"(>| ([^<>]+)>)&nbsp;","<"+lValue+"$1");
					XMLtext=XMLtext.replaceAll("&nbsp;</"+lValue+">","</"+lValue+">");		         
				}
				Pattern p1 = Pattern.compile("<"+lValue+"(>| ([^<>]+)>)</"+lValue+">");
				Matcher m1 = p1.matcher(XMLtext);
				if(m1.find()){
					XMLtext=XMLtext.replaceAll("<"+lValue+"(>| ([^<>]+)>)</"+lValue+">","");
					XMLtext=removeEmptyTag(XMLtext);
				}
				if(count==(setSize-1))
					break;	
			} 
			return XMLtext;
	}
	
	public void WriteFileUTF8(String xmlfilename,StringBuilder str) throws IOException{
			StringBuilder finalString=null;
			finalString=str;
			File f=new File(xmlfilename);
			if(f.exists())
				f.delete();
			OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream(new File(xmlfilename)),"UTF-8") ;
			os.flush();
			os.write(finalString.toString());
			os.close();
	}
	
	public static void copyFile(File src, File dest)throws IOException{
		if(src.isFile()){
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest); 
			byte[] buffer = new byte[1024];
			int length;
			while ((length = in.read(buffer)) > 0){
				out.write(buffer, 0, length);
			}
			System.out.println("copied "+dest.getAbsolutePath());
			in.close();
			out.close();
		}
	}
}
