package pc.workflow;

import java.util.*;
import java.io.*;
//Creator Edward Bowe May-2006

//This Method manages the Workflow for FMS
//It keeps Workflow.ini and the individual user Tokens in sync

public class WorkFlow3
{
	static final int SET_WORKFLOW=1;
	static final int CREATE_WORKFLOW=2;
	static final int QUEUE_MOVE_TOKEN=3;
	static final int ALLOCATE_TOKEN=4;
	static final int DEALLOCATE_TOKEN=5;
	static final int FINISH_TOKEN=6;
	static final int LIST_WORKFLOW=7;
	static final int LIST_FINISHED=8;
	static final int DELETE_WORKFLOW=9;
	static final int SET_SKIP=10;
	static final int GET_WORKFLOW=11;
	static final int STORE_WORKFLOW=12;
	static final int CHECK_SKIP=13;
	static final int GETREMARK_WORKFLOW=14;
	static final int REMARK_WORKFLOW=15;
	static final int ROUTE=16;
	static final int CREATE_COMPLETED_WORKFLOW=17;
	static final int HOLD=18;
	static final int REPORT_STATUS=19;
	static final int DUMPSTATUS=20;
	static final int ABORT=21;
	int SYS_OPTION=-1;
	String SYS_PARAMS[]=null;
	private String stage="";

	String month[]={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

	Properties JidAidProp=null;
	Properties userProperties=null;
	Vector finishedList=null;

	class lnk {  // Basic link list for recursive procedures
		String s;
		lnk next;
		lnk(String str,lnk n) {
			this.s=str;
			this.next=n;
		}
	}
	int errorcount=0;
	String tFMSRoot=null;
	Properties prop=null;
	Properties token=null;
	String RTN;
	String ALTWF=null;
	String ALTWF_WILEY=null;
	String initFile="c:\\tdconfig\\fms.ini";

	public Properties getProp() {
		return this.prop;
	}

	private int submit(String s,String p) {
		System.out.println("SUBMIT "+s+" "+p);
		return errorcount;
	}

	private void println(String s) {
		System.out.println(s);
	}

	private String statusString(String s) {
		if (s.equalsIgnoreCase("r")) return "Ready to download";
		if (s.equalsIgnoreCase("p")) return "Downloaded/on PC";
		if (s.equalsIgnoreCase("f")) return "Failed";
		if (s.equalsIgnoreCase("q")) return "On queue";
		if (s.equalsIgnoreCase("d")) return "Done/pass from TIS upload complete";
		if (s.equalsIgnoreCase("w")) return "Finished, waiting for other workflow node to complete";
		return "Not Allocated";
	}

	public WorkFlow3(String s) {
		if (s!=null && s.length()>0)
			this.initFile=s;
	}

	public WorkFlow3() {
	}

	public String reset_duedate(String cust,String cat,String jid, String aid){
		Properties ad=new Properties();
		loadProp(ad,FMSRoot()+"\\centralized_server\\"+cust+"\\"+cat+"\\"+jid+"\\"+aid+"\\attributes\\aid.ini",false);
		String[] tmp=nonNull(ad.getProperty("due-date")).split(":");
		System.out.println(""+tmp.length+" "+nonNull(ad.getProperty("due-date")));
		String duedate=tmp[2]+tmp[1]+tmp[0];
		return duedate;
	}
	private String FMSRoot() {
		RTN="FMSRoot";
		if (tFMSRoot==null) {
			Properties p=new Properties();
			loadProp(p,initFile,false);
			tFMSRoot=p.getProperty("fmsroot");
		}
		return tFMSRoot;
	}

	private int errorOut(String s) {
		errorcount++;
		System.err.println(RTN+":"+errorcount+" "+s);
		int i=1;
		i=i+10;
		return errorcount;
	}

	private void setGlobalProp(String s,String v) {
		RTN="setGlobalProp";
		String val=v;
		if (v==null) val="";
		setProp(prop,s,val);
		setProp(token,s,val);
	}
	private void setProp(Properties p, String s, String v) {
		RTN="setProp";
		String val=v;
		if (v==null) val="";
		p.setProperty(s.toUpperCase(),val.toUpperCase());
	}
	private String readProp(Properties p, String s) {
		RTN="getProp";
		String a=p.getProperty(s.toUpperCase());
		return a;
	}
	private String readProp(Properties p, String s,String r) {
		RTN="getProp";
		String a=p.getProperty(s);
		if (a!=null) return a;
		return r;
	}
	private boolean getLock(String pFile) {
		RTN="getLock";
		File f=new File(pFile);
		File f2=new File(pFile+"_lock");
		if(f2.exists()&& f.exists())//Added by Ravi on 16-02-2013
			f2.delete();//Added by Ravi on 16-02-2013

		for (int i=0; i<3; i++) {
			if (f.renameTo(f2))
				return true;
			try {Thread.sleep(1000);}
			catch (Exception e2) {;}
			System.out.println(RTN+" Lock failed "+f.getAbsolutePath()+" "+f2.getAbsolutePath());
		}
		errorOut("Failed to lock "+pFile);
		return false;
	}

	private boolean getUnLock(String pFile) {
		RTN="getUnlock";
		File f=new File(pFile);
		File f2=new File(pFile+"_lock");
		for (int i=0; i<20; i++) {
			if (f2.renameTo(f))
				return true;
			try {Thread.sleep(1000);}
			catch (Exception e2) {;}
		}
		errorOut("Failed to Unlock "+pFile);
		return false;
	}

	public int loadProp(Properties p,String arg,boolean lock) {
		RTN="loadProp";
		String Extra="";
		if (lock) {
			Extra="_lock";
			getLock(arg);
		}
		try
		{
			FileInputStream fs=new FileInputStream(new File(arg+Extra));
			p.load(fs);
			fs.close();
		}
		catch (Exception e)
		{
			errorOut("Failed to open Property file "+arg);
			p=null;
		}
		return errorcount;
	}

	public void storeProp(Properties p,String arg,boolean lock) {
		if (errorcount > 0) return;
		RTN="storeProp";
		String Extra="";
		if (lock) Extra="_lock";
		try
		{
			FileOutputStream fs=new FileOutputStream(arg+Extra);
			p.store(fs,"WORKFLOW");
			fs.close();
			if (lock) getUnLock(arg);
		}
		catch (Exception e)
		{
			errorOut("Failed to close/unlock Property file "+arg+" Extra="+Extra+"\n"+e);
		}
	}
	public boolean CopyFile(String in, String out) {
		File fin=new File(in);
		File fout=new File(out);
		System.out.println("Copy file "+in+"->"+out);
		if (!fin.canRead())
			return false;
		if (fout.getParent() != null) {
			File dir = new File( fout.getParent() );
			dir.mkdirs();
		}
		try
		{
			FileInputStream fins=new FileInputStream(fin);
			FileOutputStream fouts=new FileOutputStream(fout);
			byte buffer[] = new byte[0xffff];
			int b;
			while((b=fins.read(buffer))!=-1) fouts.write(buffer, 0, b);
			fins.close();
			fouts.close();
		}
		catch (IOException e)
		{
			System.err.println("Error copying file "+in+"->"+out);
			return false;
		}
		return true;
	}

	private int valBack(String s){
		RTN="valBack";
		int i=0;
		int success=1;
		String val,p;
		val=readProp(prop,s+".STATUS");
		if (val==null || !val.equalsIgnoreCase("FINISHED")) {
			println("FAILED VALIDATE..."+s+".STATUS"+" "+val);
			return 0;
		}

		for (i=1; (p=readProp(prop,s+".PREV["+i+"]"))!=null; i++) {
			if (valBack(p) != 1) 
				return 0;
		}
		return success;
	}
	private int validateBackward(String s){
		RTN="validateBackward";
		int i=0;
		int success=1;
		String p;

		for (i=1; (p=readProp(prop,s+".PREV["+i+"]"))!=null; i++) {
			if (valBack(p) != 1) 
				return 0;
		}
		return success;
	}

	private int clearForward(String s){
		RTN="clearForward";
		int i=0;
		int success=1;
		String val,p;
		for (i=1; (p=readProp(prop,s+".NEXT["+i+"]"))!=null; i++)
			clearForward2(p);
		return success;
	}
	private int clearForward2(String s){
		RTN="clearForward";
		int i=0;
		int success=1;
		String val,p;
		String tp=readProp(prop,s+".TOKENPATH");
		String userdir=nonNull(readProp(prop,s+".USERDIR"));
		if (userdir.length() > 0 && !userdir.equalsIgnoreCase("r"))
			errorOut("Token cannot be deleted, its allocated to user "+readProp(prop,s+".USER"));
		if (errorcount==0 && isNotEmpty(tp)) {
			File f=new File(FMSRoot()+tp);
			if (f.delete()) {
				setProp(prop,s+".TOKENPATH","");
				setProp(prop,s+".USER","");
				setProp(prop,s+".USERDIR","");
			} else {
				errorOut("Token not deleted "+f.getAbsolutePath());
			}
		}
		setProp(prop,s+".STATUS","FALSE");

		for (i=1; (p=readProp(prop,s+".NEXT["+i+"]"))!=null; i++)
			clearForward2(p);
		return success;
	}

	private int setForward(String s){
		RTN="setForward";
		int i=0;
		int success=1;
		String val,p;
		for (i=1; (p=readProp(prop,s+".NEXT["+i+"]"))!=null; i++)
			setForward2(p);
		return success;
	}

	private int setForward2(String s){
		RTN="setForward";
		int i=0;
		int success=1;
		String val,p;
		setProp(prop,s+".STATUS","FINISHED");

		for (i=1; (p=readProp(prop,s+".NEXT["+i+"]"))!=null; i++)
			setForward(p);
		return success;
	}

	private int removeToken(Properties prop,String dept) {
		RTN="removeToken";
		String filename=readProp(prop,dept+".TOKENPATH");
		System.out.println("Asked to remove "+dept+" Token "+filename);
		if (filename==null || filename.length()==0)
			return errorcount;
		System.out.println("Removing "+filename);
		setProp(prop,dept+".USER","");
		setProp(prop,dept+".USERDIR","");
		setProp(prop,dept+".TOKENPATH","");
		if (!new File(FMSRoot()+filename).delete())
			return errorOut("Failed to delete "+filename);
		return errorcount;
	}

	private String tokenPath(String dept,String user,String dest,String fname) {
		RTN="tokenPath";
		String myDest="\\db\\dept\\"+dept+"\\"+user+"\\"+dest+"\\"+fname;
		if (user==null || user.length()==0)
			myDest="\\db\\dept\\"+dept+"\\"+fname;
		return myDest;
	}

	private int copyToken(String tokenFile,String dept) {
		RTN="copyToken";
		Properties newtoken=new Properties();
		loadProp(newtoken,tokenFile,false);
		String filename=readProp(newtoken,"FILENAME");
		setProp(prop,dept+".USER","");
		setProp(prop,dept+".USERDIR","");
		setProp(prop,dept+".TOKENPATH",tokenPath(dept,null,null,filename));

		setProp(newtoken,"DEPARTMENT",dept);
		setProp(newtoken,"USER","");
		setProp(newtoken,"USERDIR","");
		setProp(newtoken,"TOKENPATH",tokenPath(dept,null,null,filename));
		storeProp(newtoken,FMSRoot()+tokenPath(dept,null,null,filename),false);
		System.out.println("Creating "+FMSRoot()+tokenPath(dept,null,null,filename));
		return errorcount;
	}

	private boolean isProperty(Properties nodes,String s,boolean errout) {
		RTN="isProperty";
		if (nodes.getProperty(s) != null)
			return true;
		if (errout)
			errorOut("Not a node "+s);
		return false;
	}

	private boolean startNext(String s){
		RTN="startNext";
		int i1,i2;
		boolean success=true;
		String p1,p2;
		for (i1=1; (p1=readProp(prop,s+".NEXT["+i1+"]"))!=null; i1++)
			if (validateBackward(p1)!=1)
				return false;
		System.out.println("Startnext ... all valid"+s);
		for (i1=1; (p1=readProp(prop,s+".NEXT["+i1+"]"))!=null; i1++) {
			System.out.println("copyToken ... "+readProp(prop,s+".TOKENPATH")+" "+p1);
			copyToken(FMSRoot()+readProp(prop,s+".TOKENPATH"),p1);
		}
		for (i1=1; (p1=readProp(prop,s+".NEXT["+i1+"]"))!=null; i1++) {
			System.out.println("Next="+p1);
			for (i2=1; (p2=readProp(prop,p1+".PREV["+i2+"]"))!=null; i2++) {
				System.out.println("Next="+p1+" Prev="+p2);
				removeToken(prop,p2);
			}
		}
		return success;
	}

	private int updateNewToken(Properties w, Properties t,String dept) {

		String cust,cat,due,jid,aid,fname;
		setProp(t,"DEPARTMENT",dept);
		setProp(t,"CUSTOMER",(cust=readProp(w,"CUSTOMER")));
		setProp(t,"CATEGORY",(cat=readProp(w,"CATEGORY")));
		setProp(t,"JID",(jid=readProp(w,"JID")));
		setProp(t,"AID",(aid=readProp(w,"AID")));
		due=readProp(w,"DUEDATE");
		String dur_for_Token=reset_duedate(cust,cat,jid,aid);
		//setProp(t,"DUEDATE",(due=readProp(w,"DUEDATE")));
		setProp(t,"DUEDATE",dur_for_Token);
		setProp(t,"FILENAME",(fname=dur_for_Token+"_"+cust+"_"+cat+"_"+jid+"_"+aid+".ini"));
		setProp(t,"TOKENPATH","\\DB\\dept\\"+dept+"\\"+fname);
		setProp(t,"USER","");
		setProp(t,"USERDIR","");
		setProp(w,dept+".USER","");
		setProp(w,dept+".USERDIR","");
		setProp(w,"DUEDATE",dur_for_Token);
		setProp(w,dept+".TOKENPATH","\\DB\\dept\\"+dept+"\\"+fname);
		setProp(w,"FILENAME",fname);		
		return errorcount;
	}

	private int recreateTheToken(Properties w, Properties t,String dept) {
		String user,cust,cat,due,jid,aid,fname,dest;
		if(new File(FMSRoot()+"\\"+readProp(w,dept+".TOKENPATH")).exists())
			new File(FMSRoot()+"\\"+readProp(w,dept+".TOKENPATH")).delete();
		setProp(t,"DEPARTMENT",dept);
		setProp(t,"CUSTOMER",(cust=readProp(w,"CUSTOMER")));
		setProp(t,"CATEGORY",(cat=readProp(w,"CATEGORY")));
		setProp(t,"JID",(jid=readProp(w,"JID")));
		setProp(t,"AID",(aid=readProp(w,"AID")));

		/**/
		due=readProp(w,"DUEDATE");
		String dur_for_Token=reset_duedate(cust,cat,jid,aid);
		//setProp(t,"DUEDATE",(due=readProp(w,"DUEDATE")));
		setProp(t,"DUEDATE",dur_for_Token);
		/**/
		setProp(t,"FILENAME",(fname=dur_for_Token+"_"+cust+"_"+cat+"_"+jid+"_"+aid+".ini"));
		setProp(t,"TOKENPATH","\\DB\\dept\\"+dept+"\\"+fname);
		setProp(t,"USER",(user=nonNull(readProp(w,dept+".USER"))));
		setProp(t,"USERDIR",(dest=nonNull(readProp(w,dept+".USERDIR"))));////
		if (user.length()==0) user=null;
		if (dest.length()==0) dest=null;
		setProp(t,"TOKENPATH",tokenPath(dept,user,dest,fname));
		setProp(w,"DUEDATE",dur_for_Token);
		setProp(w,dept+".TOKENPATH",tokenPath(dept,user,dest,fname));
		setProp(w,"FILENAME",fname);
		return errorcount;
	}

	private String nonNull(String s) {
		RTN="nonNull";
		if (s==null)
			return "";
		return s;
	}
	private boolean isNotEmpty(String s) {
		RTN="isNotEmpty";
		if (s==null)
			return false;
		if (s.length()==0)
			return false;
		return true;
	}
	private boolean FileExists(String s) {
		File f=new File(s);
		return (f.exists());
	}
	private int rename(String s,String t) {
		RTN="rename";
		if (s.equalsIgnoreCase(t))
			return errorcount;
		File f1=new File(s);
		if (!f1.exists()) {
			return errorOut("Rename failed "+f1.getAbsolutePath());
		}
		File f2=new File(t);
		if (f2.exists()) {
			f2.delete();
		}
		if (!f1.renameTo(f2)) {
			return errorOut("Rename failed "+f1.getAbsolutePath()+"->"+f2.getAbsolutePath());
		}
		return errorcount;
	}

	private int intCreateUser(Properties p,String user, String pass, String dept) {
		System.out.println("Inserting user "+user);
		RTN="intCreateUser";
		if (readProp(p,"password")==null)
			setProp(p,"PASSWORD",pass);
		String p1;
		int i;
		for (i=0;(p1=readProp(p,"DEPT["+i+"]"))!=null &&
				!p1.equalsIgnoreCase(dept) ;i++);
		File f=new File(FMSRoot()+"\\db\\dept\\"+dept+"\\"+user);
		if (p1==null) {
			setProp(p,"DEPT["+i+"]",dept);
			if (f.exists())
				return errorOut("Dept file already exists "+dept);
		}
		if (f.exists()) 
			return errorcount;
		f.mkdirs();
		new File(f.getAbsolutePath()+"\\p").mkdir();
		new File(f.getAbsolutePath()+"\\d").mkdir();
		new File(f.getAbsolutePath()+"\\q").mkdir();
		new File(f.getAbsolutePath()+"\\w").mkdir();
		new File(f.getAbsolutePath()+"\\r").mkdir();
		new File(f.getAbsolutePath()+"\\f").mkdir();
		return errorcount;
	}

	public int createUser(String user, String dept) {
		RTN="createUser";System.out.println(RTN);
		File u=new File(FMSRoot()+"\\db\\users\\"+user+".ini");
		File d=new File(FMSRoot()+"\\db\\dept\\"+dept);
		if (!d.exists()) return errorOut("Department does not exist");
		if (!u.exists()) return errorOut("User does not exists, for new user provide password");
		Properties up=new Properties();
		loadProp(up,u.getAbsolutePath(),false);
		intCreateUser(up,user,null,dept);
		storeProp(up,u.getAbsolutePath(),false);
		return errorcount;
	}

	public int createUser(String user, String pass, String dept) {
		RTN="createUser";System.out.println(RTN);
		File u=new File(FMSRoot()+"\\db\\users\\"+user+".ini");
		File d=new File(FMSRoot()+"\\db\\dept\\"+dept);
		if (!d.exists()) return errorOut("Department does not exist");
		if (u.exists()) return errorOut("User already exists, don't supply password");
		Properties up=new Properties();
		intCreateUser(up,user,pass,dept);
		storeProp(up,u.getAbsolutePath(),false);
		return errorcount;
	}

	public int createDepts(String cust, String cat, String jid) {
		RTN="createDepts";
		Properties fmsJid=new Properties();
		loadProp(fmsJid,FMSRoot()+"\\"+cust+"\\"+cat+"\\"+jid+"\\attributes\\workflow.ini",false);
		String p1;
		for (int i=1; (p1=fmsJid.getProperty("SYS.NODE["+i+"]"))!=null;i++) {
			new File(FMSRoot()+"\\db\\dept\\"+p1).mkdirs();
		}
		return errorcount;
	}
	public int createDefaultUsers(String cust, String cat, String jid) {
		RTN="createDepts";
		Properties fmsJid=new Properties();
		loadProp(fmsJid,FMSRoot()+"\\centralized_server\\"+cust+"\\"+cat+"\\"+jid+"\\attributes\\workflow.ini",false);
		String p1;
		for (int i=1; (p1=fmsJid.getProperty("SYS.NODE["+i+"]"))!=null;i++) {
			createUser("USER1",p1);
		}
		return errorcount;
	}

	public int Abort(String propFile) {
		int i1;
		String dept;
		for (i1=1;(dept=readProp(prop,"SYS.NODE["+i1+"]"))!=null; i1++) {
			String tf=nonNull(readProp(prop,dept+".TOKENPATH"));
			if (tf.length() > 1) {
				System.out.println("Deleting..."+FMSRoot()+tf);
				File f=new File(FMSRoot()+tf);
				if (f.exists())
					f.delete();
			}
		}
		getUnLock(propFile);
		prop=new Properties();
		loadProp(prop,propFile,true);
		for (i1=1;(dept=readProp(prop,"SYS.NODE["+i1+"]"))!=null; i1++) {
			String tf=nonNull(readProp(prop,dept+".TOKENPATH"));
			if (tf.length()>0)
			{
				Properties t=new Properties();
				String cust,cat,due,jid,aid,fname;
				setProp(t,"DEPARTMENT",dept);
				setProp(t,"CUSTOMER",(cust=readProp(prop,"CUSTOMER")));
				setProp(t,"CATEGORY",(cat=readProp(prop,"CATEGORY")));
				setProp(t,"JID",(jid=readProp(prop,"JID")));
				setProp(t,"AID",(aid=readProp(prop,"AID")));

				/**/
				due=readProp(prop,"DUEDATE");
				String dur_for_Token=reset_duedate(cust,cat,jid,aid);
				//setProp(t,"DUEDATE",(due=readProp(prop,"DUEDATE")));
				setProp(t,"DUEDATE",dur_for_Token);				
				/**/
				setProp(t,"FILENAME",(fname=dur_for_Token+"_"+cust+"_"+cat+"_"+jid+"_"+aid+".ini"));
				setProp(t,"TOKENPATH",tf);
				setProp(t,"USER",nonNull(readProp(prop,dept+".USER")));
				setProp(t,"USERDIR",nonNull(readProp(prop,dept+".USERDIR")));
				System.out.println("Making token"+FMSRoot()+readProp(t,"TOKENPATH"));
				storeProp(t,FMSRoot()+readProp(t,"TOKENPATH"),false);
			}
		}
		storeProp(prop,propFile,true);
		return errorcount;
	}

	private int counter(String p1,String p2) {
		System.out.println("In Ctx.counter");
		boolean success=false;
		String s=p1+"\\"+p2;
		if (!(new File(s)).exists()) return errorOut("Counter directory does not exist");
		while (!success)
		{
			File[] f=new File(s).listFiles();
			if (f.length == 1 && f[0].isDirectory())
			{
				int i=Integer.parseInt(f[0].getName());
				if (f[0].renameTo(new File(f[0].getParent()+"\\"+(i+1))))
					return i;
				try {Thread.sleep(1000);} catch (Exception e){};
			}
		}
		System.out.println("Out Ctx.counter");
		return -1;
	}

	private String transDueDate(String s) {
		String da[]=s.split("[:]");
		Calendar rightNow = Calendar.getInstance();
		try {
			rightNow.set(Integer.parseInt(da[2]), Integer.parseInt(da[1])-1, Integer.parseInt(da[0]));
		} catch (Exception e) {
			System.out.println("Error in Date "+s+" "+"processing");
			return "19990101";
		}
		/*
		if (rightNow.get(rightNow.DAY_OF_WEEK)==rightNow.MONDAY || rightNow.get(rightNow.DAY_OF_WEEK)==rightNow.TUESDAY)
		   rightNow.add(Calendar.DATE, -4);
		else
		   rightNow.add(Calendar.DATE, -2);
		 */ 
		if (rightNow.get(rightNow.DAY_OF_WEEK)==rightNow.MONDAY || rightNow.get(rightNow.DAY_OF_WEEK)==rightNow.TUESDAY)
			rightNow.add(Calendar.DATE, -4);
		else
			if (rightNow.get(rightNow.DAY_OF_WEEK)==rightNow.SUNDAY)
				rightNow.add(Calendar.DATE, -2);
			else
				rightNow.add(Calendar.DATE, -1);		  
		return ""+(rightNow.get(Calendar.YEAR)*10000+(rightNow.get(Calendar.MONTH)+1)*100+rightNow.get(Calendar.DAY_OF_MONTH));
	}

	private String internalDate(String s) {
		String da[]=s.split("[:]");
		Calendar rightNow = Calendar.getInstance();
		rightNow.set(Integer.parseInt(da[2]), Integer.parseInt(da[1])-1, Integer.parseInt(da[0]));
		return ""+(rightNow.get(Calendar.YEAR)*10000+(rightNow.get(Calendar.MONTH)+1)*100+rightNow.get(Calendar.DAY_OF_MONTH));
	}

	private String reverseLongInternalDate(String s) {
		String da[]=s.split("[:]");
		Calendar rightNow = Calendar.getInstance();
		rightNow.set(Integer.parseInt(da[0]), Integer.parseInt(da[1])-1, Integer.parseInt(da[2]));
		return ""+(rightNow.get(Calendar.YEAR)*10000+(rightNow.get(Calendar.MONTH)+1)*100+rightNow.get(Calendar.DAY_OF_MONTH))+
				":"+da[3]+da[4];
	}
	private String reverseInternalDate(String s) {
		String da[]=s.split("[:]");
		Calendar rightNow = Calendar.getInstance();
		rightNow.set(Integer.parseInt(da[0]), Integer.parseInt(da[1])-1, Integer.parseInt(da[2]));
		return ""+(rightNow.get(Calendar.YEAR)*10000+(rightNow.get(Calendar.MONTH)+1)*100+rightNow.get(Calendar.DAY_OF_MONTH));
	}

	private int reportDump(String customer, String category, String jid, String aid,String r) {
		Properties np=new Properties();
		String fname=customer+"_"+category+"_"+jid+"_"+aid+"_00000";
		setProp(np,"CUSTOMER",customer);
		setProp(np,"CATEGORY",category);
		setProp(np,"JID_AID",jid+"_"+aid);
		setProp(np,"STAGE","???");
		setProp(np,"DEPARTMENT","???");
		setProp(np,"STATE","");
		setProp(np,"USER","");
		setProp(np,"DEPARTMENT","");
		setProp(np,"DUE-DATE","");
		setProp(np,"INT-DUE-DATE","");
		setProp(np,"CUSTOMER-IN-DATE","");
		setProp(np,"FIGS","");
		setProp(np,"MSS","");
		setProp(np,"REVISE","");
		setProp(np,"PIT","");
		setProp(np,"SITE","");
		setProp(np,"PTSIII_DUE","");
		setProp(np,"PHYSICALPAGES","");
		setProp(np,"ORDER_STATUS","ERROR");
		setProp(np,"EMAIL","ERROR");
		setProp(np,"CANCEL_REMARK",r);
		setProp(np,"CONTAINS_TEX_FILE","0");
		setProp(np,"PLATFORM","");
		System.out.println("dummy Storing report "+FMSRoot()+"\\db\\updates\\"+fname+".ini");
		//storeProp(np,FMSRoot()+"\\db\\updates\\"+fname+".ini",false);
		return 1;
	}

	public int dumpStatusNoLoad(String customer, String category, String jid, String aid, String r) {
		/*if(customer.startsWith("JW"))
			return 0;*/
		String jidaid=FMSRoot()+"\\centralized_server\\"+customer+"\\"+category+"\\"+jid+"\\"+aid;
		File f=new File(jidaid);
		if (!f.exists()) return reportDump(customer, category, jid, aid,"Not in FMS");
		String wfFile=FMSRoot()+"\\centralized_server\\"+customer+"\\"+category+"\\"+jid+"\\"+aid+"\\attributes\\workflow.ini";
		if (r!=null) return reportDump(customer, category, jid, aid,r);
		loadProp(prop,wfFile,false);
		if (prop==null) loadProp(prop,wfFile+"_lock",false);
		if (prop==null) loadProp(prop,wfFile,false);
		if (prop==null) return reportDump(customer, category, jid, aid,"Record Locked");
		reportDump(customer, category, jid, aid,"");
		return 1;
	}

	private int dumpStatus(Properties p) {
		/*if(readProp(prop,"CUSTOMER").startsWith("JW"))
			return 0;*/
		Properties np=new Properties();
		String fname=readProp(prop,"CUSTOMER")+"_"+readProp(prop,"CATEGORY")+"_"+readProp(prop,"JID")+"_"+readProp(prop,"AID");
		String s="",d="",s1="",u1="",st="",c1="",c2="";
		try{
			Properties aid=JidAidProp;
			if (aid==null) {
				aid=new Properties();
				loadProp(aid,FMSRoot()+"\\centralized_server\\"+readProp(prop,"CUSTOMER")+"\\"+readProp(prop,"CATEGORY")+"\\"+readProp(prop,"JID")+"\\"+readProp(prop,"AID")+"\\attributes\\aid.ini",false);
			}
			System.out.println("creating report");
			System.out.println(p.getProperty("CUSTOMER"));
			System.out.println(p.getProperty("CATEGORY"));
			System.out.println(aid.getProperty("stage"));
			System.out.println(p.getProperty("JID")+"_"+p.getProperty("AID"));
			setProp(np,"CUSTOMER",p.getProperty("CUSTOMER"));
			setProp(np,"CATEGORY",p.getProperty("CATEGORY"));
			setProp(np,"JID_AID",p.getProperty("JID")+"_"+p.getProperty("AID"));
			setProp(np,"STAGE",aid.getProperty("stage"));

			for(int i1=1; (s=p.getProperty("SYS.NODE["+i1+"]"))!=null; i1++) {
				if (nonNull(p.getProperty(s+".TOKENPATH")).length()>1) {
					c2=nonNull(p.getProperty("JOB."+s+".CANCEL_REMARK"));
					if (c2.length()>0) {
						if (c1.length()>0) c1=c1+",";
						c1=c1+s+":"+c2;
					}
					d=p.getProperty(s+".TOKENPATH");
					if (s1.length()>0) s1=s1+",";
					s1=s1+s;
					setProp(np,"DEPARTMENT",s1);
					if (nonNull(d=p.getProperty(s+".USER")).length()>1) {
						if (u1.length()>0) u1=u1+",";
						u1=u1+s+":"+d;
					}
					if (nonNull(d=p.getProperty(s+".STATUS")).length()>1) {
						if (st.length()>0) st=st+",";
						st=st+s+":"+d;
					}
				}
			}
			setProp(np,"CANCEL_REMARK",c1);
			setProp(np,"STATE",st);
			setProp(np,"USER",u1);
			setProp(np,"DEPARTMENT",s1);
			System.out.println("creating 1");
			//	time.idate, DND, ORDER_STATUS,
			{
				String s5due=readProp(p,"S5DUEDATE","");
				if (s5due.length()>0) s5due=internalDate(s5due);
				setProp(np,"S5DUEDATE",s5due);
			}
			setProp(np,"DUE-DATE",internalDate(aid.getProperty("due-date")));
			setProp(np,"INT-DUE-DATE",transDueDate(aid.getProperty("due-date")));
			setProp(np,"CUSTOMER-IN-DATE",reverseLongInternalDate(aid.getProperty("time")));
			setProp(np,"FIGS",aid.getProperty("no-phys-figs"));
			setProp(np,"MSS",readProp(p,"JOB.MSS_PAGES","0"));
			String tmpstr1=readProp(np,"MSS","0");
			if (!tmpstr1.equals(readProp(aid,"no-mns-pages","0")))
				setProp(np,"MSS",readProp(aid,"no-mns-pages","0"));
			setProp(np,"MSS",readProp(aid,"no-mns-pages","0"));
			setProp(np,"REVISE","0");
			setProp(np,"PIT",aid.getProperty("pit"));
			setProp(np,"SITE",aid.getProperty("prod-site"));
			setProp(np,"EMAIL",aid.getProperty("ead"));
			setProp(np,"PTSIII_DUE",internalDate(aid.getProperty("due-date")));
			setProp(np,"PHYSICALPAGES",p.getProperty("JOB.PDFPAGES"));
			setProp(np,"FASTTRACE",readProp(p,"JOB.FASTTRACE","FALSE"));
			setProp(np,"TIFFLIKECOUNT",readProp(p,"JOB.TIFFJPEGCOUNT","0"));
			setProp(np,"CONTAINS_TEX_FILE",readProp(p,"JOB.CONTAINS_TEX_FILE","0"));
			setProp(np,"PLATFORM",readProp(p,"JOB.PLATFORM",""));
			setProp(np,"GULLIVER",readProp(p,"JOB.GULLIVER",""));
			if(aid.getProperty("stage","").endsWith("RESUPPLY"))
			{
				setProp(np,"REMARK-TYPE-ITEM",readProp(aid,"remark-type-item[1]","NULL"));	
				setProp(np,"REMARK-ITEM",readProp(aid,"remark-item[1]","NULL"));
			}
			setProp(np,"APPROVAL",""+getValue(aid));					
			setProp(np,"CORRECTIONTYPE",readProp(aid,"corrections",""));
			setProp(np,"ANNOTCOUNT",readProp(p,"JOB.ANNOTCOUNT",""));
			if (SYS_OPTION==FINISH_TOKEN && SYS_PARAMS!=null && SYS_PARAMS.length>0 &&
					(SYS_PARAMS[0].endsWith("_LOGIN")))
				setProp(np,"INFLOW",aid.getProperty("time"));

			System.out.println("creating 2");
			s=aid.getProperty("order.status");
			if (s==null) s=""; else {s=s+":"+aid.getProperty("order_receive_count");}
			setProp(np,"ORDER_STATUS",s);
			if (true) {
				String Q1="S100_QC",lastQ="", lastStage="", lastState="",t;
				String Q2="S200_QC";
				int Qc=0;

				String t1="";
				if ((t1=p.getProperty("M100_QC.NEXT[1]"))!=null) {
					Q1="M100_QC"; 
					Q2="M200_QC";
				}
//				String lastTs[]=null;
				for (int i9=1;(t=p.getProperty("SYS.STAMP["+i9+"]"))!=null;i9++) {
					String ts[]=t.split("[;]");
					System.out.println("--------- "+ts.length);
					if (ts!=null && ts.length>4) {
//						lastTs=ts;
						if ((lastQ.equals(Q1)||lastQ.equals(Q2)) && !ts[4].equals(lastQ)){
							if (!ts[4].equals(""+p.getProperty(lastQ+".NEXT[1]"))){
								if(lastState!=null && !lastState.equalsIgnoreCase("R")){
									Qc++;
								}
							} 
						}
						
						String currentStage = ts[4];
						if(currentStage!=null && !currentStage.equals(lastStage))
								Qc=0;
						
						if ((ts[4].equals(Q1)||ts[4].equals(Q2)) && ts[3].equals("NOTALLOCATED") &&
								(p.getProperty("SYS.STAMP["+(i9+1)+"]")==null) &&
								(""+p.getProperty(ts[4]+".TOKENPATH")).length()<6){
							Qc++;
						}
						
						lastQ=ts[4];
						lastStage=ts[4];
						lastState=ts[1];
						
						if (ts[4].endsWith("_EP") && ts[3].equals("FINISHED")) Qc=0;
						if (ts[3].equals("FINISHED")) setProp(np,"DEPT_DATE",ts[0]);
					}
				}
				
				/**
				 * Reset counter if stage mismatch
				 */
				String currentStage = aid.getProperty("stage", "");
				System.out.println("currentStage ::> "+currentStage);
				System.out.println("lastStage ::> "+lastStage);
				if(lastStage!=null && lastStage.length()>0 && currentStage.length()>0){
					if(!lastStage.equalsIgnoreCase(currentStage))
						Qc=0;
				}
				
				if (Qc > 0)
					setProp(np,"REVISE",""+Qc);
			}
			/* Blocked by Ravi 16-02-2013
			//np.store(new FileInputStream(new File(FMSRoot()+"\\db\\updates\\"+fname+".ini")));
	    	System.out.println("Storing report "+FMSRoot()+"\\db\\updates\\"+fname+".ini");
			storeProp(np,FMSRoot()+"\\db\\updates\\"+fname+"_"+counter(FMSRoot()+"\\DB\\counters","dbcounter")+".ini",false);			
//			storeProp(np,FMSRoot()+"\\db\\tmp\\"+fname+"_"+counter(FMSRoot()+"\\DB\\counters","dbcounter")+".ini",false);	
			 */
			//New process Added on 16-02-2013 Bu Ravi As we have multiple updates folder as per stage and JID Character letters.
			String countVal=""+counter("d:\\fms\\DB\\counters","dbcounter");
			setProp(np,"DBCOUNTER",countVal);			

			System.out.println("Token Stage : "+np.getProperty("STAGE",""));
			System.out.println("Token Department : "+np.getProperty("DEPARTMENT","")); 
			System.out.println("Token fname : "+fname); 
			if(!fname.startsWith("ELSEVIER"))
			{
				System.out.println("Storing report d:\\fms\\db\\updates3\\"+fname+"_"+countVal+".ini"); //to print token name 15-1-2013
				storeProp(np,"d:\\fms\\db\\updates3\\"+fname+"_"+countVal+".ini",false);		
			}
			else if(fname.startsWith("ELSEVIER") && np.getProperty("STAGE","").equals("CU"))
			{
				System.out.println("Storing report d:\\fms\\db\\updates4\\"+fname+"_"+countVal+".ini"); //to print token name 15-1-2013
				storeProp(np,"d:\\fms\\db\\updates4\\"+fname+"_"+countVal+".ini",false);
			}
			else if(fname.startsWith("ELSEVIER") && 
					(np.getProperty("STAGE","").equals("S200")
							||np.getProperty("STAGE","").equals("S200RESUPPLY")))
			{
				System.out.println("Storing report d:\\fms\\db\\updates5\\"+fname+"_"+countVal+".ini"); //to print token name 15-1-2013
				storeProp(np,"d:\\fms\\db\\updates5\\"+fname+"_"+countVal+".ini",false);
			}						
			//else if(fname.startsWith("ELSEVIER")&&(np.getProperty("DEPARTMENT","").indexOf("P100")==-1)&&						
			/*else if(fname.startsWith("ELSEVIER")&&						
				(np.getProperty("STAGE","").equals("S5")	//FOR S5 to S100 stage second planner queue
				||np.getProperty("STAGE","").equals("S5RESUPPLY")))
				{
					System.out.println("Storing report d:\\fms\\db\\updates2\\"+fname+"_"+countVal+".ini"); //to print token name 15-1-2013
					storeProp(np,"d:\\fms\\db\\updates2\\"+fname+"_"+countVal+".ini",false);		
				}
			else if(fname.startsWith("ELSEVIER")&&						
				(np.getProperty("STAGE","").equals("S100")
				||np.getProperty("STAGE","").equals("S100RESUPPLY")))
				{
					System.out.println("Storing report d:\\fms\\db\\updates6\\"+fname+"_"+countVal+".ini"); //to print token name 15-1-2013
					storeProp(np,"d:\\fms\\db\\updates6\\"+fname+"_"+countVal+".ini",false);		
				}*/
			else if(fname.startsWith("ELSEVIER")&&						
					(np.getProperty("STAGE","").equals("S5")	//FOR S5 to S100 stage second planner queue
							||np.getProperty("STAGE","").equals("S5RESUPPLY")
							||np.getProperty("STAGE","").equals("S100")
							||np.getProperty("STAGE","").equals("S100RESUPPLY"))
					)
			{
				String localJID=readProp(prop,"JID");
				System.out.println("localJID "+localJID);
				if(localJID.charAt(0)=='A'||localJID.charAt(0)=='B'||localJID.charAt(0)=='C'||localJID.charAt(0)=='D'||localJID.charAt(0)=='E'||localJID.charAt(0)=='F')
				{
					storeProp(np,"d:\\fms\\db\\updatesA-F\\"+fname+"_"+countVal+".ini",false);
				}
				if(localJID.charAt(0)=='G'||localJID.charAt(0)=='H'||localJID.charAt(0)=='I'||localJID.charAt(0)=='J'||localJID.charAt(0)=='K'||localJID.charAt(0)=='L')
				{
					storeProp(np,"d:\\fms\\db\\updatesG-L\\"+fname+"_"+countVal+".ini",false);
				}
				if(localJID.charAt(0)=='M'||localJID.charAt(0)=='N'||localJID.charAt(0)=='O'||localJID.charAt(0)=='P'||localJID.charAt(0)=='Q'||localJID.charAt(0)=='R')
				{
					storeProp(np,"d:\\fms\\db\\updatesM-R\\"+fname+"_"+countVal+".ini",false);
				}
				if(localJID.charAt(0)=='S'||localJID.charAt(0)=='T'||localJID.charAt(0)=='U'||localJID.charAt(0)=='V'||localJID.charAt(0)=='W'||localJID.charAt(0)=='X'||localJID.charAt(0)=='Y'||localJID.charAt(0)=='Z')
				{
					storeProp(np,"d:\\fms\\db\\updatesS-Z\\"+fname+"_"+countVal+".ini",false);
				}
			}
			else if(fname.startsWith("ELSEVIER") && 
					(np.getProperty("STAGE","").equals("S250")
							||np.getProperty("STAGE","").equals("S250RESUPPLY")))
			{
				System.out.println("Storing report d:\\fms\\db\\updates5\\"+fname+"_"+countVal+".ini"); //to print token name 15-1-2013
				storeProp(np,"d:\\fms\\db\\updates7\\"+fname+"_"+countVal+".ini",false);
			}	
			else
			{
				System.out.println("Storing report d:\\fms\\db\\updates\\"+fname+"_"+countVal+".ini");
				storeProp(np,"d:\\fms\\db\\updates\\"+fname+"_"+countVal+".ini",false);			
			}
			//******************************************************************************************************************
		}catch(Exception e)	
		{		
			e.printStackTrace();
			errorOut("Exception "+e);			
		}
		return 0;
	}


	/*
    public int initDB() {
    	Properties a=new Properties();
		loadProp(a,FMSRoot()+"\\centralized_server\\"+customer+"\\"+category+"\\"+jid+"\\"+aid+"\\attributes\\aid.ini",false);
    	Properties db=new Properties();
		db.setProperty("customer",prop.getProperty("customer"));
		db.setProperty("category",prop.getProperty("category"));
		db.setProperty("jid",prop.getProperty("jid"));
		db.setProperty("aid",prop.getProperty("aid"));
		db.setProperty("stage",a.getProperty("stage"));
		db.setProperty("time",a.getProperty("time"));
		db.setProperty("due-date",a.getProperty("due-date"));
		db.setProperty("expiry-date",a.getProperty("expiry-date"));
		db.setProperty("prd-type-as-sent",a.getProperty("prd-type-as-sent"));
		db.setProperty("no-mns-pages",a.getProperty("no-mns-pages"));
		db.setProperty("no-phys-figs",a.getProperty("no-phys-figs"));
		db.setProperty("prod-site",a.getProperty("prod-site"));
		db.setProperty("received-date",a.getProperty("received-date"));
    	return errorcount;
    }
	 */
	private int readWorkflow(String customer, String category, String jid, String aid,
			int option, String[] oparams) {
		SYS_OPTION=option;
		SYS_PARAMS=oparams;
		RTN="readWorkflow";
		int i;
		finishedList=null;
		String propFile=FMSRoot()+"\\centralized_server\\"+customer+"\\"+category+"\\"+jid+"\\"+aid+"\\attributes\\workflow.ini";
		//19-02-2013 Added By Ravi
		if(!new File(propFile).exists())
		{
			String propFile1=FMSRoot()+"\\centralized_server\\"+customer+"\\"+category+"\\"+jid+"\\"+aid+"\\attributes\\workflow.ini_lock";
			if(new File(propFile1).exists())
			{
				new File(propFile1).renameTo(new File(propFile));
			}
			else
			{
				String propFile2=FMSRoot()+"\\centralized_server\\"+customer+"\\"+category+"\\"+jid+"\\"+aid+"\\attributes\\workflow.ini__bck";
				if(new File(propFile2).exists())
				{
					CopyFile(propFile2,propFile);
				}
			}
		}
		//**********************
		if ((option==CREATE_COMPLETED_WORKFLOW) || (option==CREATE_WORKFLOW)) {
			RTN="readWorkflow:CREATE_WORKFLOW";
			//Check if workflow already exists -- if it does exit
			File fl[]=new File(FMSRoot()+"\\centralized_server\\"+customer+"\\"+category+"\\"+jid+"\\"+aid+"\\attributes").listFiles();
			for (i=0;fl!=null && i<fl.length;i++) {
				String ans=fl[i].getName().toUpperCase();
				if (ans.indexOf("WORKFLOW.INI") > -1)
					return errorOut("Workflow file exists ");
			}
			File f=null;
			//Check if JID workflow exists -- if not exit
			f=new File(FMSRoot()+"\\centralized_server\\"+customer+"\\"+category+"\\"+jid+"\\attributes\\workflow.ini");
			if(this.stage.equalsIgnoreCase("S200"))
				f=new File(FMSRoot()+"\\centralized_server\\"+customer+"\\"+category+"\\"+jid+"\\attributes\\revised\\workflow.ini");
			//if (ALTWF_WILEY!=null) f=new File(FMSRoot()+"\\centralized_server\\"+customer+"\\"+category+"\\"+jid+"\\attributes\\"+ALTWF_WILEY+".ini");
			if (!f.exists())
				return errorOut("JID Workflow file does not exist "+f.getAbsolutePath());

			//Load JID workflow file and update it to start workflow
			this.prop=new Properties();
			loadProp(prop,f.getAbsolutePath(),false);
			Properties fmsAid=new Properties();
			String fmsAidStr=FMSRoot()+"\\centralized_server\\"+customer+"\\"+category+"\\"+jid+"\\"+aid+"\\attributes\\aid.ini";
			loadProp(fmsAid,fmsAidStr,false);
			if (fmsAid.getProperty("aid")==null) {
				fmsAid.setProperty("aid",aid);
				storeProp(fmsAid,fmsAidStr,false);
			}
			String dept=readProp(prop,"SYS.NODE[1]");
			if (option==CREATE_COMPLETED_WORKFLOW) {
				dept=oparams[0];
				String s;
				for (int i1=1;(s=readProp(prop,"SYS.NODE["+i1+"]"))!=null; i1++)
					setProp(prop,s+".STATUS","FINISHED");
			}
			this.token=new Properties();
			if (errorcount > 0) return errorcount;
			setProp(prop,"CUSTOMER",customer);
			setProp(prop,"CATEGORY",category);
			setProp(prop,"JID",jid);
			setProp(prop,"AID",aid);
			String[] tmp=nonNull(fmsAid.getProperty("due-date")).split(":");
			System.out.println(""+tmp.length+" "+nonNull(fmsAid.getProperty("due-date")));
			String duedate=tmp[2]+tmp[1]+tmp[0];
			setProp(prop,"DUEDATE",duedate);
			setProp(prop,"FILENAME",duedate+"_"+customer+"_"+category+"_"+jid+"_"+aid+".ini");
			updateNewToken(prop,token,dept);
			storeProp(token,FMSRoot()+readProp(token,"TOKENPATH"),false);
			clearForward(dept);
			setProp(prop,dept+".STATUS","FALSE");
			storeProp(prop,propFile,false);
			//			initDB(fmsAid);
			return errorcount;
		}

		if (option==DELETE_WORKFLOW) {
			RTN="readWorkflow:CREATE_WORKFLOW";
			//Check if workflow already exists -- if it does exit
			Properties tp=new Properties();
			File f=new File(FMSRoot()+"\\centralized_server\\"+customer+"\\"+category+"\\"+jid+"\\"+aid+"\\attributes");
			if (!f.exists())
				return errorOut("File not found "+f.getAbsolutePath());
			File fl[]=f.listFiles();
			for (i=0;i<fl.length;i++) {
				String ans=fl[i].getName().toUpperCase();
				if (ans.indexOf("WORKFLOW.INI") > -1) {
					loadProp(tp,fl[i].getAbsolutePath(),false);
					String s="";
					for (int i1=1;(s=readProp(tp,"SYS.NODE["+i1+"]"))!=null; i1++) {
						String t=nonNull(readProp(tp,s+".TOKENPATH"));
						if (t.length()>0)
						{
							f=new File(FMSRoot()+t);
							if (f.exists())
							{
								f.delete();
							}
						}
					}
					fl[i].delete();
				}
			}
			return errorcount;
		}


		RTN="readWorkflow";
		this.prop=new Properties();
		Properties nodes=new Properties();
		loadProp(prop,propFile,true);
		String start=readProp(prop,"SYS.NODE[1]");
		String s;
		for (i=1;(s=readProp(prop,"SYS.NODE["+i+"]"))!=null; i++) {
			nodes.setProperty(s,"TRUE");
		}
		for (i=1;(s=readProp(prop,"SYS.NODE["+i+"]"))!=null; i++) {
			String tp=readProp(prop,s+".TOKENPATH");
			if (tp!=null && tp.length() > 0) {
				File f=new File(FMSRoot()+tp);
				if (!f.exists())
					errorOut("Token not found "+f.getAbsolutePath());
				if (validateBackward(s)!=1)
					errorOut("Error -> Not all steps complete "+s);
			}
		}
		if (start==null)
			errorOut("Error -> Start node is NULL");
		switch (option) {
		case LIST_WORKFLOW:
			for (i=1;(s=readProp(prop,"SYS.NODE["+i+"]"))!=null; i++) {
				String tp=nonNull(readProp(prop,s+".TOKENPATH"));
				String tp2;
				if (tp.length() > 0) {
					tp=s+": "+nonNull(readProp(prop,s+".USER"))+": "+statusString(nonNull(readProp(prop,s+".USERDIR")));
					if (finishedList==null) finishedList=new Vector();
					finishedList.add(tp);
				}
			}	    
			break;
		case HOLD:
			setProp(prop,"SYS.HOLD",oparams[0]);
			break;
		case ABORT:
			Abort(propFile);
			return errorcount;        	    
		case GET_WORKFLOW:
			break;
		case STORE_WORKFLOW:
			System.out.println("Enum...");
			Properties pr=new Properties();
			for (Enumeration en=userProperties.propertyNames() ; en.hasMoreElements() ; ) {
				String val=(String)en.nextElement();
				System.out.println("Enum..."+val);
				setProp(prop,"JOB."+val,userProperties.getProperty(val));
			}
			break;
		case REMARK_WORKFLOW:
			Calendar c = Calendar.getInstance();
			for (i=1;(s=readProp(prop,"SYS.REMARK["+i+"]"))!=null; i++);
			s=c.get(c.DATE)+"-"+month[c.get(c.MONTH)]+"-"+c.get(c.YEAR)+":"+
					c.get(c.HOUR_OF_DAY)+":"+c.get(c.MINUTE)+";"+oparams[0]+":"+oparams[1]+":"+oparams[2];
			prop.setProperty("SYS.REMARK["+i+"]",s);
			break;
		case GETREMARK_WORKFLOW:
			for (i=1;(s=readProp(prop,"SYS.REMARK["+i+"]"))!=null; i++) {
				if (finishedList==null) finishedList=new Vector();
				finishedList.add(s);        	    	
			}
			break;
		case LIST_FINISHED:
			for (i=1;(s=readProp(prop,"SYS.NODE["+i+"]"))!=null; i++) {
				String tp=readProp(prop,s+".status");
				if (tp.equalsIgnoreCase("FINISHED")) {
					if (finishedList==null)
						finishedList=new Vector();
					finishedList.add(s);
				}
			}        	    
			break;
		case CHECK_SKIP: {
			boolean contin=false;
			storeProp(prop,propFile,true);
			for (i=1; errorcount==0 && (s=readProp(prop,"SYS.NODE["+i+"]"))!=null;i++) {
				if ((nonNull(readProp(prop,s+".AUTOSTART")).length() > 0) &&
						(nonNull(readProp(prop,s+".STATUS")).equalsIgnoreCase("FALSE")) &&
						(nonNull(readProp(prop,s+".TOKENPATH")).length() > 0) &&
						(nonNull(readProp(prop,s+".USER")).length()==0)) {
					contin=true;
					WorkFlow3 x=new WorkFlow3(initFile);
					x.allocateToken(readProp(prop,"CUSTOMER"),
							readProp(prop,"CATEGORY"),
							readProp(prop,"JID"),
							readProp(prop,"AID"),s,"USER1");
					submit(readProp(prop,s+".AUTOSTART"),
							FMSRoot()+readProp(x.getProp(),s+".TOKENPATH"));
				}
				if ((nonNull(readProp(prop,s+".SKIP")).equalsIgnoreCase("ONCE") ||
						nonNull(readProp(prop,s+".SKIP")).equalsIgnoreCase("TRUE")) &&
						(!nonNull(readProp(prop,s+".STATUS")).equalsIgnoreCase("FINISHED")) &&
						(nonNull(readProp(prop,s+".TOKENPATH")).length() > 0) &&
						(nonNull(readProp(prop,s+".USER")).length()==0)) {
					WorkFlow3 x=new WorkFlow3(initFile);
					contin=true;
					System.out.println("SKIP ONCE alloc..."+s);
					x.allocateToken(readProp(prop,"CUSTOMER"),
							readProp(prop,"CATEGORY"),
							readProp(prop,"JID"),
							readProp(prop,"AID"),s,"USER1");
					System.out.println("SKIP ONCE finish..."+s);
					x.finishThisToken(readProp(prop,"CUSTOMER"),
							readProp(prop,"CATEGORY"),
							readProp(prop,"JID"),
							readProp(prop,"AID"),s);
					setProp(prop,s+".SKIP","");
				}
			}
			if (errorcount==0 && contin)
				errorcount=-1;
			return errorcount;
		}

		case SET_SKIP:
			for (i=0; i<oparams.length-1;i++) {
				if (isProperty(nodes,oparams[i],true))
					setProp(prop,oparams[i]+".SKIP",oparams[oparams.length-1]);
			}
			break;
		case SET_WORKFLOW:
			boolean contin=true;
			for (i=0;i<oparams.length;i++) {
				String gp=nodes.getProperty(oparams[i]);
				if (gp==null || gp.length()==0)
					errorOut("Department "+oparams[i]+" not in workflow");
				if (errorcount==0)
					if (validateBackward(oparams[i])!=1)
						errorOut("Department "+oparams[i]+" workflow is incomplete");
			}
			for (i=0;i<oparams.length;i++) { // check workflows not allocated
				String p1=null;
				for (i=1; (p1=prop.getProperty("SYS.NODE["+i+"]"))!=null;i++)
					if (nonNull(prop.getProperty(p1+".USER")).length()>0)
						errorOut("Cannot set workflow, token allocated Dept="+p1+" User="+
								prop.getProperty(p1+".USER")+" Directory="+prop.getProperty(p1+".USERDIR"));
			}
			if (errorcount==0) {
				for (i=0;i<oparams.length && errorcount==0; i++) {
					Properties t=new Properties();
					if (validateBackward(oparams[i])==1) {
						clearForward(oparams[i]);
						updateNewToken(prop,t,oparams[i]);
						setProp(prop,oparams[i]+".STATUS","FALSE");
						storeProp(t,FMSRoot()+readProp(t,"TOKENPATH"),false);
					}
				}
				if (errorcount==0) {
					for (i=0;i<oparams.length; i++) {
						int vb=validateBackward(oparams[i]);
						System.out.println(oparams[i]+" == "+vb);
						if (vb!=1) {
							errorOut("Workflow not valid for node ... restore old workflow"+oparams[i]);
							Abort(propFile);
							dumpStatus(prop);
							return errorcount;
						}
					}
				} else {
					Abort(propFile);
				}
			}
			dumpStatus(prop);
			break;

		case REPORT_STATUS:
			dumpStatus(prop);
			break;
		case ALLOCATE_TOKEN:
			if (nonNull(readProp(prop,"SYS.HOLD")).equalsIgnoreCase("TRUE")) {
				storeProp(prop,propFile,true);
				errorcount++;
				return errorcount;			
			}
		case QUEUE_MOVE_TOKEN:
		case DEALLOCATE_TOKEN:
		case FINISH_TOKEN:
			System.out.println("FINISHING THE TOKEN");
			RTN="queueMoveToken";
			isProperty(nodes,oparams[0],true);
			String dept=null,target=null,user=null,status="FALSE";

			if (!isNotEmpty(oparams[0]))
				errorOut("Invalid Parameter 1");
			else if ((option==ALLOCATE_TOKEN ||
					option==QUEUE_MOVE_TOKEN) && !isNotEmpty(oparams[0]))
				errorOut("Invalid Parameter 2");
			else {
				dept=oparams[0];
				if (option==FINISH_TOKEN) {
					System.out.println("OK TILL HERE");
					target="w";
					user=null;
					status="FINISHED";
				}
				if (option==ALLOCATE_TOKEN) {
					if (nonNull(readProp(prop,dept+".USER")).length() > 0)
						errorOut("User already set Allocate failed "+readProp(prop,dept+".TOKENPATH"));
					target="r";
					user=oparams[1];
					status="ALLOCATED";
				}
				if (option==DEALLOCATE_TOKEN) {
					target=null;
					user=null;
					status="NOTALLOCATED";
				}
				if (option==QUEUE_MOVE_TOKEN) {
					//user=?
					user=null;
					target=oparams[1];
					status="INPROGRESS";
				}
			}
			String tokenFile=null;
			if (errorcount==0) {
				System.out.println("OK TILL HERE TOO");
				tokenFile=readProp(prop,dept+".TOKENPATH");
				if (!isNotEmpty(tokenFile))
					errorOut("TOKENPATH is empty for "+readProp(prop,"FILENAME"));
				if (errorcount==0 && !FileExists(FMSRoot()+tokenFile))
					errorOut("TOKEN does not exist "+tokenFile);
				if (errorcount==0) {
					this.token=new Properties();
					loadProp(this.token,FMSRoot()+tokenFile,true);
				}
			}
			if (errorcount==0) {
				if (target!=null && user==null)
					user=readProp(this.token,"USER");
				String dest=tokenPath(dept,user,target,readProp(this.token,"FILENAME"));
				if (rename(FMSRoot()+tokenFile+"_lock",FMSRoot()+dest+"_lock")==0) {
					setProp(token,"TOKENPATH",dest);
					setProp(token,"USERDIR",target);		
					setProp(token,"USER",user);
					setProp(token,"DEPARTMENT",dept);
					setProp(token,"STATUS",status);
					setProp(prop,dept+".TOKENPATH",dest);
					setProp(prop,dept+".USERDIR",target);
					setProp(prop,dept+".USER",user);
					setProp(prop,dept+".STATUS",status);
					storeProp(token,FMSRoot()+dest,true);
				} else {
					getUnLock(FMSRoot()+tokenFile);
				}
				clearForward(dept);
				if (option==FINISH_TOKEN) {
					System.out.println("Start next..."+dept);
					startNext(dept);
					if (nonNull(readProp(prop,dept+".SKIP")).equalsIgnoreCase("ONCE")) {
						setProp(prop,dept+".SKIP","DONE");
						System.out.println("OneSkip Finish");
					}
				}
			}
			dumpStatus(prop);
			break;

		}
		storeProp(prop,propFile,true);
		return errorcount;			
	}

	public int recreateTokens(String cust,String cat,String jid,String aid) {
		prop=new Properties();
		loadProp(prop,FMSRoot()+"\\centralized_server\\"+cust+"\\"+cat+"\\"+jid+"\\"+aid+"\\attributes\\workflow.ini",false);
		String p1;String dept;
		for (int i1=1;(dept=readProp(prop,"SYS.NODE["+i1+"]"))!=null; i1++) {
			String tf=nonNull(readProp(prop,dept+".TOKENPATH"));
			if (tf.length() > 1) {
				System.out.println(tf);
				Properties token=new Properties();
				recreateTheToken(prop,token,dept);
				storeProp(token,FMSRoot()+token.getProperty("TOKENPATH"),false);
				storeProp(prop,FMSRoot()+"\\centralized_server\\"+cust+"\\"+cat+"\\"+jid+"\\"+aid+"\\attributes\\workflow.ini",false);
			}
		}
		return errorcount;
	}

	public int writeDB(Properties np,String fname) {
		storeProp(np,FMSRoot()+"\\db\\updates\\"+fname+"_"+counter(FMSRoot()+"\\DB\\counters","dbcounter")+".ini",false);			
		return errorcount;
	}
	public int setAltWorkflow(String s) {
		ALTWF=s;
		return errorcount;
	}
	public int setAltWorkflow_wiley(String s) {
		ALTWF_WILEY=s;
		return errorcount;
	}	
	public int createWorkflow(String cust,String cat,String jid,String aid) {
		readWorkflow(cust,cat,jid,aid,CREATE_WORKFLOW,new String[]{});
		return errorcount;
	}
	public int createAltWorkflow(String cust,String cat,String jid,String aid,String alt) {
		setAltWorkflow(alt);
		readWorkflow(cust,cat,jid,aid,CREATE_WORKFLOW,new String[]{});
		return errorcount;
	}
	public int createAltWorkflow_wiley(String cust,String cat,String jid,String aid,String alt) {
		if(alt.contains(":"))
		{
			String[] info=alt.split(":");
			this.stage=info[1];
			setAltWorkflow_wiley(info[0]);
		}
		setAltWorkflow_wiley(alt);
		readWorkflow(cust,cat,jid,aid,CREATE_WORKFLOW,new String[]{});
		return errorcount;
	}	
	public int createCompleteWorkflow(String cust,String cat,String jid,String aid,String dept) {
		System.out.println(cust+" "+cat+" "+jid+" "+aid+" "+dept);
		readWorkflow(cust,cat,jid,aid,CREATE_COMPLETED_WORKFLOW,new String[]{dept});
		return errorcount;
	}
	public int deleteWorkflow(String cust,String cat,String jid,String aid) {
		readWorkflow(cust,cat,jid,aid,DELETE_WORKFLOW,new String[]{});
		return errorcount;
	}
	public Vector listToken(String cust,String cat,String jid,String aid) {
		readWorkflow(cust,cat,jid,aid,LIST_WORKFLOW,new String[]{});
		if (errorcount>0) return null;
		return finishedList;
	}
	public int holdToken(String cust,String cat,String jid,String aid,String remark) {
		readWorkflow(cust,cat,jid,aid,HOLD,new String[]{remark});
		return errorcount;
	}
	public int abortToken(String cust,String cat,String jid,String aid) {
		readWorkflow(cust,cat,jid,aid,ABORT,new String[]{});
		return errorcount;
	}
	public Vector listFinished(String cust,String cat,String jid,String aid) {
		readWorkflow(cust,cat,jid,aid,LIST_FINISHED,new String[]{});
		if (errorcount>0) return null;
		return finishedList;
	}
	public int setSkip(String cust,String cat,String jid,String aid,String dept,String val) {
		readWorkflow(cust,cat,jid,aid,SET_SKIP,new String[]{dept,val});
		return errorcount;
	}
	public int setSkips(String cust,String cat,String jid,String aid,String[] dept,String val) {
		String dept_val[]=new String[dept.length+1];
		int i=0;
		for (i=0;i<dept.length;i++)
			dept_val[i]=dept[i];
		dept_val[i]=val;
		readWorkflow(cust,cat,jid,aid,SET_SKIP,dept_val);
		return errorcount;
	}
	public int allocateToken(String cust,String cat,String jid,String aid,String dept,String user) {
		readWorkflow(cust,cat,jid,aid,ALLOCATE_TOKEN,new String[]{dept,user});
		return errorcount;
	}
	public int reportStatus(String cust,String cat,String jid,String aid) {
		readWorkflow(cust,cat,jid,aid,REPORT_STATUS,new String[]{});
		return errorcount;
	}
	public int deAllocateToken(String cust,String cat,String jid,String aid,String dept,String user) {
		readWorkflow(cust,cat,jid,aid,DEALLOCATE_TOKEN,new String[]{dept,user});
		return errorcount;
	}
	public int finishThisToken(String cust,String cat,String jid,String aid,String dept) {
		readWorkflow(cust,cat,jid,aid,FINISH_TOKEN,new String[]{dept});
		return errorcount;
	}
	public int finishToken(String cust,String cat,String jid,String aid,String dept) {
		readWorkflow(cust,cat,jid,aid,FINISH_TOKEN,new String[]{dept});
		int sk=-1;
		while (sk==-1) {
			WorkFlow3 x=new WorkFlow3(initFile);
			sk=x.checkSkip(cust,cat,jid,aid);
		}
		return errorcount;
	}
	public int checkSkip(String cust,String cat,String jid,String aid) {
		readWorkflow(cust,cat,jid,aid,CHECK_SKIP,new String[]{});
		return errorcount;
	}
	public int queueMoveToken(String cust,String cat,String jid,String aid,String dept,String dest) {
		readWorkflow(cust,cat,jid,aid,QUEUE_MOVE_TOKEN,new String[]{dept,dest});
		return errorcount;
	}
	public int setWorkFlow(String cust,String cat,String jid,String aid,String[] depts) {
		readWorkflow(cust,cat,jid,aid,SET_WORKFLOW,depts);
		return errorcount;
	}
	public Properties getWorkflow(String cust,String cat,String jid,String aid) {
		readWorkflow(cust,cat,jid,aid,GET_WORKFLOW,new String[]{});
		if (errorcount >0) return null;
		return prop;
	}
	public int storeWorkflow(String cust,String cat,String jid,String aid,Properties p) {
		userProperties=p;
		readWorkflow(cust,cat,jid,aid,STORE_WORKFLOW,new String[]{});
		userProperties=null;
		return errorcount;
	}
	public int addRemark(String cust,String cat,String jid,String aid,String user,String dept,String remark) {
		readWorkflow(cust,cat,jid,aid,REMARK_WORKFLOW,new String[]{user,dept,remark});
		return errorcount;
	}
	public Vector getRemarks(String cust,String cat,String jid,String aid) {
		readWorkflow(cust,cat,jid,aid,GETREMARK_WORKFLOW,new String[]{});
		if (errorcount>0) return null;
		return finishedList;
	}

	public String sub1(String s1,String s2,String s3) {
		int i1=s1.indexOf(s2);
		String s4=s1;
		if (i1 > -1)
			s4=s1.substring(0,i1)+s3+s1.substring(i1+s2.length());
		System.out.println("--"+s4+" "+s2+" "+i1);
		return s4;
	}
	public String sub(String s1,String s2,String s3) {
		if (s1.indexOf(s2) > -1)
			System.out.println("Sub "+s1+" "+s2+" "+s3);
		while (s1.indexOf(s2) > -1)
			s1=sub1(s1,s2,s3);
		return s1;
	}
	public String SR(String val) {
		String s=val;
		s=sub(s,"S100_MCT","M100_MCT");
		s=sub(s,"S100_FS","M100_FS");
		s=sub(s,"S100_CE","M100_CE");
		s=sub(s,"S200_IE","M200_IE");
		s=sub(s,"S200_QC","M200_QC");
		return s; 
	}

	public void stat(Properties p,String s,String s2) {
		String fin=p.getProperty(s+".STATUS");
		if (fin !=null && fin.equals("FINISHED")) {
			String tp=p.getProperty(s+".TOKENPATH");
			if (tp==null || tp.length()==0) {
				p.setProperty(s2+".STATUS","FINISHED");
			}       		
		}
	}



	public int update2(String f1) {
		Properties aidWf=new Properties();
		Properties tmpWf=new Properties();
		loadProp(aidWf,f1,false);
		int i;
		for (i=1;aidWf.getProperty("SYS.NODE["+i+"]")!=null;i++) {
			String np=aidWf.getProperty("SYS.NODE["+i+"]");
			System.out.println("Dept-"+np);
			String tp=aidWf.getProperty(np+".TOKENPATH");
			if (tp!=null && tp.length() > 0) {
				System.out.println("Token-"+FMSRoot()+tp);
				File tpf=new File(FMSRoot()+tp);
				Properties tpp=new Properties();
				loadProp(tpp,tpf.getAbsolutePath(),false);
				tpf.delete();
				recreateTheToken(tmpWf,tpp,tmpWf.getProperty("SYS.NODE["+i+"]"));
				System.out.println("Store--"+FMSRoot()+tpp.getProperty("TOKENPATH"));
				storeProp(tpp,FMSRoot()+tpp.getProperty("TOKENPATH"),false);
			}
		}

		Enumeration en;
		for (en=aidWf.propertyNames(); en.hasMoreElements() ;) {
			String val=(String)en.nextElement();
			tmpWf.setProperty(SR(val),SR(aidWf.getProperty(val)));
		}
		for (i=1;aidWf.getProperty("SYS.NODE["+i+"]")!=null;i++);

		tmpWf.setProperty("SYS.NODE["+(i++)+"]","S100_CEC");
		tmpWf.setProperty("SYS.NODE["+(i++)+"]","S100_QC");			
		tmpWf.setProperty("SYS.NODE["+(i++)+"]","S200_QC");			

		tmpWf.setProperty("M100_CE.NEXT[1]","S100_CEC");		
		tmpWf.setProperty("S100_CEC.PREV[1]","M100_CE");
		tmpWf.setProperty("S100_CEC.NEXT[1]","S100_FX");
		tmpWf.setProperty("S100_FX.PREV[1]","S100_CEC");

		tmpWf.setProperty("M100_QC.NEXT[1]","S100_QC");		
		tmpWf.setProperty("S100_QC.PREV[1]","M100_QC");
		tmpWf.setProperty("S100_QC.NEXT[1]","S100_XMLTEX");
		tmpWf.setProperty("S100_XMLTEX.PREV[1]","S100_QC");

		tmpWf.setProperty("M200_QC.NEXT[1]","S200_QC");		
		tmpWf.setProperty("S200_QC.PREV[1]","M200_QC");
		tmpWf.setProperty("S200_QC.NEXT[1]","S200_XMLTEX");
		tmpWf.setProperty("S200_XMLTEX.PREV[1]","S200_QC");

		stat(tmpWf,"M100_CE","S100_CEC");
		stat(tmpWf,"M100_QC","S100_QC");
		stat(tmpWf,"M200_QC","S200_QC");

		File f=new File(f1);
		File fr=new File(f1+"_N");
		if (fr.exists()) fr.delete();
		f.renameTo(fr);
		storeProp(tmpWf,f1,false);
		for (i=1;tmpWf.getProperty("SYS.NODE["+i+"]")!=null;i++) {
			String np=tmpWf.getProperty("SYS.NODE["+i+"]");
			System.out.println("Dept-"+np);
			String tp=tmpWf.getProperty(np+".TOKENPATH");
			if (tp!=null && tp.length() > 0) {
				System.out.println("Token-"+FMSRoot()+tp);
				File tpf=new File(FMSRoot()+tp);
				Properties tpp=new Properties();
				loadProp(tpp,tpf.getAbsolutePath(),false);
				tpf.delete();
				recreateTheToken(tmpWf,tpp,tmpWf.getProperty("SYS.NODE["+i+"]"));
				System.out.println("Store--"+FMSRoot()+tpp.getProperty("TOKENPATH"));
				storeProp(tpp,FMSRoot()+tpp.getProperty("TOKENPATH"),false);
			}
		}
		return errorcount;
	}	

	public int updateWorkflows(String p1,String p2,String p3) {
		String jid=FMSRoot()+"\\centralized_server\\"+p1+"\\"+p2+"\\"+p3;
		String wf=jid+"\\attributes\\workflow.ini";
		System.out.println(wf);
		if (FileExists(wf)) {   		
			update2(wf);
			File j[]=new File(jid).listFiles();
			for (int i=0;i<j.length;i++) {
				wf=j[i].getAbsolutePath()+"\\attributes\\workflow.ini";
				if (FileExists(wf))
					update2(wf);
			}
		}
		return errorcount;
	}

	public int update(String f1, String f2, String f3) {
		Properties oldWf=new Properties();
		Properties newWf=new Properties();
		Properties aidWf=new Properties();
		Properties tmpWf=new Properties();
		loadProp(oldWf,f1,false);
		loadProp(newWf,f2,false);
		loadProp(aidWf,f3,false);
		Enumeration en;
		for (en=aidWf.propertyNames(); en.hasMoreElements() ;) {
			String val=(String)en.nextElement();
			if (oldWf.getProperty(val)==null) 
				tmpWf.setProperty(val,oldWf.getProperty(val));
		}
		for (en=newWf.propertyNames(); en.hasMoreElements() ;) {
			String val=(String)en.nextElement();
			tmpWf.setProperty(val,newWf.getProperty(val));
		}
		storeProp(tmpWf,f3,false);
		return errorcount;
	}	

	public int del(String p1, String p2, String p3, String p4) {

		CopyFile(
				"M:\\FMS\\centralized_server\\"+p1+"\\"+p2+"\\"+p3+"\\"+p4+"\\attributes\\workflow.ini",
				"M:\\FMS\\centralized_server\\"+p1+"\\"+p2+"\\___trash\\workflow.ini_"+p3+"_"+p4);

		WorkFlow3 x1=new WorkFlow3(initFile);
		x1.deleteWorkflow(p1,p2,p3,p4);
		File f1 = new File("M:\\FMS\\centralized_server\\"+p1+"\\"+p2+"\\"+p3+"\\"+p4);
		File f2 = new File("M:\\FMS\\centralized_server\\"+p1+"\\"+p2+"\\___trash\\"+p3+"_"+p4);
		f1.renameTo(f2);
		return errorcount;
	}	

	public int doDump(String p1, String p2, String p3, String p4) {
		try {
			doDumpB(p1,p2,p3,p4);
		} catch (Exception e) {
		}
		return errorcount;
	}	
	public int doDumpB(String p1, String p2, String p3, String p4) {
		if (!(new File("D:\\FMS\\centralized_server\\"+p1+"\\"+p2+"\\"+p3+"\\"+p4+"\\attributes\\workflow.ini").exists()))
			return errorcount++;
		new WorkFlow3("c:\\fms\\orders\\bin\\workflow.ini").reportStatus(p1,p2,p3,p4);
		return errorcount;
	}	

	private String getValue(Properties aidp){
		String val="N";
		String s="";
		for(int i1=1; (s=aidp.getProperty("remark-item["+i1+"]"))!=null; i1++) {
			if((s.toUpperCase().indexOf("APPROVAL")>-1)||(s.toUpperCase().indexOf("REVISED")>-1)||(s.toUpperCase().indexOf("F39")>-1))
				val="Y";
		}
		for(int i1=1; (s=aidp.getProperty("response-item["+i1+"]"))!=null; i1++) {
			if((s.toUpperCase().indexOf("APPROVAL")>-1)||(s.toUpperCase().indexOf("REVISED")>-1)||(s.toUpperCase().indexOf("F39")>-1))
				val="Y";
		}		
		return val;
	} 	
	public static void main(String [] args) {
		int i;
		/* r=ready to download
		 * p=on PC
		 * f=failed
		 * q=on queue
		 * d=pass from TIS upload complete
		 * w=finished workflow
		 */		
		//new WorkFlow3().listToken("ELSEVIER","JOURNAL","MSA","23569");
		//if (true) return;
/*		WorkFlow3 x=new WorkFlow3();
		String workflowIni = "D:/fms/Random_workflow.ini";
		Properties prop = new Properties();
		x.loadProp(prop, workflowIni, false);
		
		for(Object k : prop.keySet() ){
			System.out.println(""+k+"--> "+prop.getProperty((String)k));
		}*/
		//x.doDump("ELSEVIER","JOURNAL","ACA","229346");

		WorkFlow3 wf=new WorkFlow3();
		wf.setWorkFlow("ELSEVIER","JOURNAL","CARP","9990",new String[]{"S200_IE"});
	}
}