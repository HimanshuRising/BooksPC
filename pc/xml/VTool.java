package pc.xml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class VTool {
	SingletonDomParser sd;
	public VTool() throws ParserConfigurationException {
		sd=SingletonDomParser.getInstance();
		
	}
	public boolean isVtoolError(String xml) throws SAXException, IOException{
		boolean status=false;
		int errorCount=0;
		Document xmlDoc=sd.getDocument(xml);
		NodeList resultsTag=xmlDoc.getElementsByTagName("results");
		NodeList ListOfResultTag1=resultsTag.item(1).getChildNodes();
		for(int base=0;base<ListOfResultTag1.getLength();base++){
			if(ListOfResultTag1.item(base) instanceof Text)
				continue;
			Node aNode=ListOfResultTag1.item(base);
			System.out.println(aNode.getNodeName());
			if(!aNode.getNodeName().equalsIgnoreCase("message"))
				continue;
			String type=SingletonDomParser.getAttibuteValue(aNode, "type");
				if(type.equalsIgnoreCase("error")){
					status=true;
					errorCount++;
				}
			}
		System.out.println(errorCount);
		if(errorCount>0 || status)
			return true;
		return false;
		}
	
	public void runVtool(String datasetpath){
		Process p=null;
		try
		{
			Runtime rt=Runtime.getRuntime();
			String	temp="cmd.exe /c start C:/Vtool5/V5.bat "+datasetpath;
			System.out.println("vtool command-->"+temp);
			p=rt.exec(temp);
			p.waitFor();
			InputStream is=p.getInputStream();
			BufferedReader br=new BufferedReader(new InputStreamReader(is));
			while(true){
				String str=br.readLine();
				if(str==null)
					break;
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			if(p!=null)p.destroy();
		}
	}
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		boolean h= new VTool().isVtoolError("D:/1/extracted/B9780128044230000020_out.xml.log.xml");
		System.out.println(h);
	}
}
