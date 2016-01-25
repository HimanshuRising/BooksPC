package pc.xml;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
public class SingletonDomParser{
	
	static DocumentBuilder dBuilder=null;
	static DocumentBuilderFactory dbFactory=null;
	private static SingletonDomParser instance;

	private SingletonDomParser() throws ParserConfigurationException{
		if(dbFactory==null){
			dbFactory = DocumentBuilderFactory.newInstance();
			System.out.println("Document builder factory created");
		}
		if(dBuilder==null){
			dBuilder = dbFactory.newDocumentBuilder();
			System.out.println("Document builder created");
		}
	}
	public Document getDocument(String xml) throws SAXException, IOException{
		Document doc = dBuilder.parse(xml);
		doc.getDocumentElement().normalize();
		return doc;
	}
	public static SingletonDomParser getInstance() throws ParserConfigurationException {
		if(instance==null){
			instance= new SingletonDomParser();
		}
		return instance;
	}
	
	 public static Node getNodeFromNode(Node aNode,String searchName){
		  if(aNode instanceof Element){
		  NodeList nodeList=aNode.getChildNodes();
		  if(nodeList!=null && nodeList.getLength()>0){
		  for(int i=0;i<nodeList.getLength();i++){
			  Node childNode=nodeList.item(i);
			  if(childNode instanceof Text)
				  continue;
			  String nodeName=childNode.getNodeName();
			  if(nodeName==null)
				  continue;
			  if(nodeName.equalsIgnoreCase(searchName))
				  return childNode;
		  }
		  }}
		  return null;
	  }
	   
	  public static String getAttibuteValue(Node aNode,String searchAttrib){
			 NamedNodeMap attrib=aNode.getAttributes();
			 if(attrib!=null && attrib.getLength()>0){
				for(int base=0;base<attrib.getLength();base++){
					Node att=attrib.item(base);
					if(att.getNodeName().equals(searchAttrib))
						return att.getNodeValue();
				}
			 }
			 return "";
		 }
}