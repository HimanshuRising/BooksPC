package pc.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="order")
@XmlAccessorType(XmlAccessType.FIELD)
public class Order {
	
	@XmlElement(name="chapters")
	private Chapters chapters;

	public Chapters getChapters() {
		return chapters;
	}

	public void setChapters(Chapters chapters) {
		this.chapters = chapters;
	}
	
	
}
