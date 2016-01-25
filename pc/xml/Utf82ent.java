package pc.xml;
import java.io.*;
import java.util.*;
import java.lang.StringBuffer;

public class Utf82ent {
    byte inbuf[]=null;
	byte outbuf[]=null;
	int outbufLength=0;
	boolean starter[]=new boolean[256];
	int maxLen=0;
    Properties entProp=null, utfProp=null;

    public Properties load(String arg) {
		Properties p=new Properties();
        try {
        	FileInputStream fs=new FileInputStream(new File(arg));
        	p.load(fs);
        	fs.close();
        } catch (Exception e) {
        	System.err.println("Failed to open Property file "+arg);
        	p=null;
        }
        return p;
	}

	public void read(String p1) {
		entProp=load("c:\\tdconfig\\utf.ini");
        for (int i=0;i<starter.length;i++) starter[i]=false;
        Enumeration en = entProp.propertyNames();
        while(en.hasMoreElements()) {
            String name = (String)en.nextElement();
            if (maxLen<name.length()) maxLen=name.length();
            char ch=name.charAt(0);
            if (ch<256) starter[ch]=true;
        }

		String s=null;
		try {
		   File f1=new File(p1);
		   outbuf=new byte[2*(int)f1.length()];
		   inbuf=new byte[(int)f1.length()];
           FileInputStream ps=new FileInputStream(f1);
           ps.read(inbuf);
           ps.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void write(String arg) {
		try {
           FileOutputStream ps=new FileOutputStream(arg);
           ps.write(outbuf,0,outbufLength);
           ps.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void convert() {
		for (int i=0;i<inbuf.length;i++) {
			char ch=(char)(inbuf[i]&0xff);
			String r=null;
			String s=null;
			if (starter[ch]) {
				int len=maxLen;
				if (i+maxLen>inbuf.length) len=inbuf.length-i;
				s="";
				for (int i3=i;i3<i+len;i3++) s=s+(char)(inbuf[i3] & 0xff);
				if (s.charAt(0)>128) {
					int ii=0;
				}
				while (s.length()>0 && r==null) {
					r=entProp.getProperty(s);
					if (r!=null) break;
					s=s.substring(0,s.length()-1);
				}
			}
			if (r==null && (ch&0xc0)==0xc0) {
				int b=2;
				int ic=ch&0x3f;
				s="12";
				if ((ch&0xf0)==0xf0) {
					b=4;
					ic=ch&0x0f;
					s="1234";
				} else if ((ch&0xE0)==0xE0) {
					b=3;
					ic=ch&0x0f;
					s="123";
				}
				for (int i2=1;i2<b;i2++) {
					ic=ic*64+(char)(inbuf[i+i2] & 0x3f);
				}
				r="&#x"+Integer.toHexString(ic)+";";
			}
		    if (r!=null) {
				i=i+s.length()-1;
				for (int i2=0;i2<r.length();i2++) outbuf[outbufLength++]=(byte)r.charAt(i2);
			} else {
				outbuf[outbufLength++]=(byte)ch;
			}
		}
	}

	public void process(String p1) {
		read(p1);
		convert();
		new File(p1).delete();
		write(p1);
	}
}
