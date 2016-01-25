package pc;

public class FileObject {

	private String fileName;
	private String absPath;
	private boolean isDirectory;
	
	public FileObject()
	{
		this.fileName=null;
		this.absPath=null;
		this.isDirectory=false;
	}
	
	public FileObject(String fileName,String absPath,boolean isDirectory)
	{
		this.fileName=fileName;
		this.absPath=absPath;
		this.isDirectory=isDirectory;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public boolean hasFileName()
	{
		return (this.fileName!=null ? true : false);
	}

	public String getAbsPath() {
		return absPath;
	}

	public void setAbsPath(String absPath) {
		this.absPath = absPath;
	}
	public boolean hasAbsolutePath()
	{
		return (this.absPath!=null ? true : false);
	}

	public boolean isDirectory() {
		return (this.isDirectory==true ? true : false);
	}
	public boolean isEqual(FileObject aFile)
	{
		return (getFileName().equalsIgnoreCase(aFile.getFileName()) && getAbsPath().equalsIgnoreCase(aFile.getAbsPath())
				&& isDirectory==aFile.isDirectory ? true : false );
	}
	public boolean search(String criteria)
	{
		return (this.fileName.equalsIgnoreCase(criteria) ? true :false);
	}
	public String toString()
	{
		return "\nName: "+this.fileName+"\nAbsolute Path: "+this.absPath+"\nDir? "+this.isDirectory;
	}

}
