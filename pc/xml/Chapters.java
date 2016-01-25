package pc.xml;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="chapters")
@XmlAccessorType(XmlAccessType.FIELD)
public class Chapters {
	
	@XmlElement(name="chapter-info")
	private List<Chapter> chapter;

	public List<Chapter> getChapter() {
		return chapter;
	}

	public void setChapter(List<Chapter> chapter) {
		this.chapter = chapter;
	}
}
