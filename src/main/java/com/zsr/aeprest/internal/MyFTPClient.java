package com.zsr.aeprest.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MyFTPClient {

	static Logger logger = LoggerFactory.getLogger(MyFTPClient.class);
	
	public static MyFTPClient genMyFTPClient(String host, String user, String passwd, String path) {
		
		logger.info(">>>>>> start login to ftp, {};{};{};{};", host, user, passwd, path);
		MyFTPClient ret = new MyFTPClient();
		FTPClient ftp = new FTPClient();
        //ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));
		
		try {
			
			ftp.connect(host);
			int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply))
            {
                ftp.disconnect();
                logger.error("FTP server refused connection.");
                return null;
            }

		}  catch (IOException e) {
            if (ftp.isConnected())
            {
                try
                {
                    ftp.disconnect();
                }
                catch (IOException f)    {}
            }
            logger.error("Could not connect to server. {}" , e);
            return null;
		}
		
		try {
			if(!ftp.login(user,  passwd)) {
				logger.error("failed to login to ftp. {}:{}. ", user, passwd);
                try
                {
                    ftp.disconnect();
                } catch (IOException f) {}
                return null;
			}
		} catch (IOException e1) {
			logger.error("failed to login to ftp. {}:{}. ", user, passwd, e1);
            try
            {
                ftp.disconnect();
            }
            catch (IOException f) {}
            return null;
		}

		ret.ftpClient = ftp;
		try {
			logger.info("Remote system is {}.", ftp.getSystemType());
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.enterLocalPassiveMode();
            ftp.setUseEPSVwithIPv4(true);
            ftp.changeWorkingDirectory(path);
            
			return ret;
		} catch(IOException e2) {
			logger.error("ftp session failed.", e2);
		}
		
		ret.close();	
		return null;
	}
	
	private FTPClient ftpClient;
	
	private MyFTPClient() {}
	
	void close() {
		try {
			ftpClient.logout();
		} catch (IOException e1) {}
        try {
            ftpClient.disconnect();
        } catch (IOException f) {}
	}

	
	void printAllFiles(String name) {
		
		try {
			FTPFile[] fs = ftpClient.listFiles(name);
			for(FTPFile f: fs) {
				System.out.println(f.getRawListing());
				logger.debug("{}:{}:{}", f.getName(), f.getSize(), f.getTimestamp().getTime());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	class FileInfo{
		List<String> subdir;
		Date ts;
		long size;
		String getFtpPath() {
			StringBuilder sb = new StringBuilder();
			boolean bs = false;
			for(String s: subdir) {
				if(bs) sb.append("/");
				bs = true;
				sb.append(s);
			}
			return sb.toString();
		}
		String getPath() {
			StringBuilder sb = new StringBuilder();
			Iterator<String> it = subdir.iterator();
			if(it.hasNext()) sb.append(it.next());
			while(it.hasNext()) {
				sb.append("/");
				sb.append(it.next());
			}
			return sb.toString();
		}
	}
	
	boolean copyAll(String src, File desFile) {
		
		try {
			FTPFile[] fs = ftpClient.listFiles(src);
			for(FTPFile f: fs) {
				if(f.getName().equals("..")) {
					continue;
				}
				if(f.getName().equals(".")) {
					logger.debug("enter directory " + src);
					if(!desFile.exists()) {
						if(!desFile.mkdir()) {
							throw new IOException("failed to create directory " + desFile.getPath());
						}
					}
					continue;
				}
				
				String subname = src + "/" + f.getName();
				File subDesFile = new File(desFile, f.getName());
				if(f.isFile()) {
					logger.debug("copy file from {} to {}", subname, subDesFile);
					try(OutputStream out = new FileOutputStream(subDesFile)) {
						ftpClient.retrieveFile(subname, out);
					}
					
				} else if(f.isDirectory()) {
					if(!copyAll(subname, subDesFile)) return false;
				}
			}
			
			return true;
		} catch (IOException e) {
			logger.error("failed to copy dir from ftp. ", e);
		}
		return false;
	}
	
	
	List<String> getFtpFileList(String name) {
		
		//System.out.println("debug: " + name);
		try {
			List<String> ret = new ArrayList<String>();
			FTPFile[] fs = ftpClient.listFiles(name);
			for(FTPFile f: fs) {
				//System.out.println("debug: " + f.getName());
				if(f.getName().equals("..")) {
					continue;
				}
				
				if(f.getName().equals(".")) {
					String curline = name + "," + f.getTimestamp().getTime() + ",";
					//System.out.println(curline);
					ret.add(curline);
					continue;
				}
				
				String subname = name + "/" + f.getName();	
				if(f.isFile()) {
					String curline = subname + "," + f.getTimestamp().getTime() + "," + f.getSize();
					//System.out.println(curline);
					ret.add(curline);
				} else if(f.isDirectory()) {
					List<String> curlines = getFtpFileList(subname);
					if(curlines == null) return null;
					ret.addAll(curlines);
				}
			}
			return ret;
		} catch (IOException e) {
			logger.error("failed to getFtpFileList, at {}", e);
		}
		
		return null;
	}
	
	/**
	 *  return l1 - l2, which represent updated files in ftp.
	 *  exclude all directories, as a directory changes as a file in it changes.
	 *  
	 * @param l1
	 * @param l2
	 * @return
	 */
	static List<String> getDiffList(List<String> li1, List<String> li2) {
		
		List<String> ret = new ArrayList<String>();
		li1.sort(null);
		li2.sort(null);
		
		Iterator<String> it = li2.iterator();
		String l2 = "";
		for(String l1: li1) {
			//ignore directory.
			if(l1.split(",").length <3 ) continue;
			while(l1.compareTo(l2) > 0) {
				if(it.hasNext()) {
					l2 = it.next();
				}else {
					break;
				}
			}
			if(l1.compareTo(l2) != 0) {
				ret.add(l1);
			}
		}
		
		return ret;
	}
	
	/**
	 * get all lines whose first token(file path) is in myLi but conLi.
	 * 
	 * @param conLi
	 * @param myLi
	 * @param sep
	 * @return
	 */
	static List<String> getDiffListByFirstToken(List<String> conLi, List<String> myLi, String sep) {
		
		TreeMap<String, List<String>> mp1 = new TreeMap<>();
		for(String s: conLi) {
			String tok = s.split(sep, 2)[0];
			if(!mp1.containsKey(tok)) {
				mp1.put(tok, new ArrayList<String>());
			}
			mp1.get(tok).add(s);
		}
		TreeMap<String, List<String>> mp2 = new TreeMap<>();
		for(String s: myLi) {
			String tok = s.split(sep, 2)[0];
			if(!mp2.containsKey(tok)) {
				mp2.put(tok, new ArrayList<String>());
			}
			mp2.get(tok).add(s);
		}
		
		Set<String> retset = new TreeSet<>(mp2.keySet());
		retset.removeAll(mp1.keySet());
		List<String> ret = new ArrayList<String>();
		for(String s: retset) {
			ret.addAll(mp2.get(s));
		}
		return ret;
	}
	
 	static List<String> eraseDupFile(List<String> li){
		List<String> ret = new ArrayList<String>();
		String last = "";
		for(String l: li) {
			if(last.equals("")) {
				last = l.split(",", 2)[0];
				ret.add(last);
			}
			if(l.startsWith(last)) continue;
			last = l.split(",", 2)[0];
			ret.add(last);
		}
		
		return ret;
	}
	
	static File getFileByFtpPath(String src) {
		File ret = null;
		for(String t: src.split("/")) {
			if(ret == null) {
				ret = new File(t);
			} else {
				ret = new File(ret, t);
			}
		}
		return ret;
	}
	
	boolean updateLocalDir(String remoteDir, File localDirFile, List<String> li2, List<String> nli) {
		List<String> li1 = getFtpFileList(remoteDir);
		if(li1 == null) return false;
		if(li1.size() == 0) {
			logger.error("remoteDir not exist in ftp, {}", remoteDir);
			return false;
		}
		if(nli != null) {
			nli.addAll(li1);
		}
		
		List<String> onlyli2 = getDiffListByFirstToken(li1, li2, ",");
		List<String> deled = eraseDupFile(onlyli2);
		
		List<String> srcfiles =getDiffList(li1, li2);
		//List<String> srcfiles = eraseDupFile(difli);
		// add files.
		for(String src: srcfiles) {
			src = src.split(",")[0];
			File des = new File(localDirFile, getFileByFtpPath(src).getPath());
			logger.info("get file from ftp, {} ==> {}", src, des);
			File desDir = des.getParentFile();
			if(!desDir.exists()) {
				if(!desDir.mkdirs()) {
					logger.error("failed to mkdir directory {}", desDir.getPath());
					return false;
				}
			}
			
			try(OutputStream out = new FileOutputStream(des)) {
				ftpClient.retrieveFile(src, out);
			} catch(IOException e) {
				logger.error("failed to get file from ftp, {} ==> {}", src, des);
				return false;
			}
		}
		
		//remove files or directories.
		for(String s: deled) {
			String ftpPath = s.split(",", 2)[0];
			File curFile = new File(localDirFile, getFileByFtpPath(ftpPath).getPath());
			logger.info("remove file not existing in ftp, {};{}; {}", s, localDirFile.getPath(), curFile.getPath());
			if(!curFile.exists()) {
				logger.warn("the local directory is not consistent with local record, as file to delete does not exist. {}", 
						curFile.getPath());
			}
			
			if(curFile.isDirectory()) {
				if(!Utilities.deleteFile(curFile)) {
					logger.warn("failed to delete directory {}", curFile);
				}
			} else if(curFile.isFile()) {
				if(!curFile.delete()) {
					logger.warn("failed to delete file {}", curFile);
				}
			}
		}
		
		return true;
	}
	
	
	public static void main(String[] args) {
		__main:
		try {
			MyFTPClient ftp = genMyFTPClient("100.3.16.103", "iauto", "iauto", "IAUTO/TS");
			if(ftp == null) break __main;
			//ftp.printAllFiles("SERVER/BMC");
//			ftp.listAll("BMC");
//			System.out.println("==========================================");
//			ftp.listAll("SERVER");
//			ftp.copyAll("SERVER", new File("SERVER"));
//
//			List<String> lines = ftp.getFtpFileList("SERVER");
//			System.out.println("filelist from ftp: " + (lines == null ? null : lines.size()));
//			
//			if(lines != null) {
//				for(String l : lines) {
//					System.out.println(l);
//					
//				}
//				
//				List<String> sec_lines = ftp.getFtpFileList("SERVER");
//				List<String> diflines =getDiffList(lines, sec_lines);
//				System.out.println("===== lines - sec_lines: " + diflines.size());
//				
//			}
			
			List<String> li2 = new ArrayList<String>();
			List<String> li1 = new ArrayList<String>();
			System.out.println("====== first invoke ftp.updateLocalDir");
			if(!ftp.updateLocalDir("SERVER", new File("test/script_bck"), li2, li1)) {
				break __main;
			}
			
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try {
				br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			System.out.println();
			System.out.println("====== second invoke ftp.updateLocalDir");
			li2 = li1;
			li1 = new ArrayList<String>();
			if(!ftp.updateLocalDir("SERVER", new File("test/script_bck"), li2, li1)) {
				break __main;
			}
			
		}finally {
			
		}
	}

}
