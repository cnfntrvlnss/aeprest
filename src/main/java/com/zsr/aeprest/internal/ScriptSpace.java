package com.zsr.aeprest.internal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ScriptSpace {
	static Logger logger = LoggerFactory.getLogger(ScriptSpace.class);
	static final String scriptDir = "test/script";
	static final String scriptBackupDir = "test/script_bck";
	static final String ftp_ip = "100.2.36.172";
	static final String ftp_usr = "iauto";
	static final String ftp_pwd = "iauto";
	static {
		checkdir(scriptDir, scriptBackupDir);
	}
	
	static void checkdir(String... dirs) {
		for(String dir: dirs) {
			File file = new File(dir);
			if(!file.exists()) {
				if(!file.mkdirs()) {
					System.err.println("failed to create directory " + file.getPath());
				}
			}
		}
	}
	
	static boolean prepareScriptDir(String ftpPath, String rootDir) {
		
		int lastsep = ftpPath.lastIndexOf('/');
		String dirName = ftpPath.substring(lastsep + 1);
		ftpPath = ftpPath.substring(0, lastsep);
		
		File rootFile = new File(rootDir);
		File recordFile = new File(rootDir, dirName + "_record.txt");
		File tmpFile = new File(rootDir, dirName + "_record.txt.tmp");
		
		List<String> recLi = new ArrayList<>();
		List<String> nrecLi = new ArrayList<>();
		
		if(recordFile.exists()) {
			//if(dirFile.exists()) return true;
			try(BufferedReader br = new BufferedReader(new FileReader(recordFile))){
				String tmpline;
				while((tmpline = br.readLine()) != null) {
					recLi.add(tmpline);
				}
			} catch (IOException e) {
				logger.error("failed to read record file {}. ", recordFile.getPath(), e);
				return false;
			}
		}
		
		MyFTPClient ftp = MyFTPClient.genMyFTPClient(ftp_ip, ftp_usr, ftp_pwd, ftpPath);
		if(ftp == null) return false;
		boolean bsucc = true;
		long startms = System.currentTimeMillis();
		if(!ftp.updateLocalDir(dirName, rootFile, recLi, nrecLi)) {
			logger.error("failed to copy directory from ftp, {} ===> {}/{}", dirName, rootFile.getPath(), dirName);
			bsucc = false;
		}else {
			
		}
		long endms = System.currentTimeMillis();
		logger.info("in prepareScriptDir, ftp.updateLocalDir elapse {} seconds", (float)(endms - startms) / 1000); 
		if(!bsucc) {
			return false;
		}
		
		try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile)))){
			for(String s: nrecLi) {
				bw.write(s);
				bw.newLine();
			}
		} catch (IOException e) {
			logger.error("failed to write record of directory {} to file {}. {}", dirName, tmpFile.getPath(), e);
			return false;
		}
		
		if(recordFile.exists() && !recordFile.delete()) {
			logger.error("failed to delete old record file {}", recordFile.getPath());
			return false;
		}
		if(!tmpFile.renameTo(recordFile)) {
			logger.error("failed to rename record file, from {} to {}", tmpFile.getPath(), recordFile.getPath());
			return false;
		}
		
		return true;
	}
	
	public static String prepareScriptSpace(String taskname, String path) {
		File myScriptDir = new File(scriptDir, taskname);
		if(!myScriptDir.mkdir()) {
			return null;
		}
		
		return "";
	}

}