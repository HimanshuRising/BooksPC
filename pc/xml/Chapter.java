package pc.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="chapter-info")
@XmlAccessorType(XmlAccessType.FIELD)

public class Chapter {

	@XmlElement(name="pii")
	private String pii;
	@XmlElement(name="aid")
	private String aid;
	public String getPii() {
		return pii;
	}
	public void setPii(String pii) {
		this.pii = pii;
	}
	public String getAid() {
		return aid;
	}
	public void setAid(String aid) {
		this.aid = aid;
	}

}
