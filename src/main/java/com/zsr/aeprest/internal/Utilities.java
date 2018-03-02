package com.zsr.aeprest.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Utilities {
	static Logger logger = LoggerFactory.getLogger(Utilities.class);
	
	public static boolean zipcompress(File file) {
		String name = file.getName();
		File zipFile = new File(file.getParentFile(), name + ".zip");
		ZipOutputStream zo = null;
		try {
			zo = new ZipOutputStream(new FileOutputStream(zipFile));
			File[] fs = file.listFiles();
			if(fs.length == 0) {
				zo.putNextEntry(new ZipEntry(name));
				return true;
			}
			for(File f: fs) {
				if(f.isDirectory()) {
				}else {
					zo.putNextEntry(new ZipEntry(name + "/" + f.getName()));
					try(FileInputStream in_f = new FileInputStream(f);){
						int read;
						byte[] tmpbuf = new byte[4096];
						while((read = in_f.read(tmpbuf)) != -1) {
							zo.write(tmpbuf, 0, read);
						}	
						
					}
				}
			}
			return true;
			
		}catch(IOException e) {
			e.printStackTrace();
		}finally {
			if(zo != null) {
				try {
					zo.close();
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
		}
		
		return false;
	}
	
	public static boolean deleteFile(File file) {
		if(file.isDirectory()) {
			File[] fs = file.listFiles();
			for(int i=0; i< fs.length; i++) {
				if(!deleteFile(fs[i])) {
					return false;
				}
			}
			if(!file.delete()) {
				return false;
			}
		}else {
			if(!file.delete()) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean copyDir(String src, String des) {
		
		File srcFile = new File(src);
		File desFile = new File(des);
		
		if(!desFile.exists()) {
			desFile.mkdir();
		}
		
		File[] fs = srcFile.listFiles();
		if(fs == null) {
			System.out.println("ERROR return null from File.listFiles(), dir: " + src);
			return false;
		}
		for(File f: fs) {
			File subDesFile = new File(desFile, f.getName());
			if(f.isFile()) {
				if(!copyFile(f, subDesFile)) {
					System.out.println("ERROR failed to copy file from " + f.getPath() + " to " + subDesFile.getPath());
					return false;
				}
			} else if(f.isDirectory()){
				if(!copyDir(f.getPath(), subDesFile.getPath())) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	public static boolean copyFile(File src, File des) {
		try(FileInputStream in = new FileInputStream(src)){
			try(FileOutputStream out = new FileOutputStream(des)){
				byte[] tmpbuf = new byte[4096];
				int read;
				while((read = in.read(tmpbuf)) != -1) {
					out.write(tmpbuf, 0, read);
				}
				return true;
			}
		} catch(IOException e) {
			
		}
		return false;
		
	}
	
	public static boolean saveFile(String path, byte[] data) {
		try(FileOutputStream out = new FileOutputStream(path)){
			out.write(data);
			return true;
		} catch(IOException e) {
			
		}
		
		return false;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//zipcompress(new File("data"));
		
//		if(!copyDir("data", "data_bck")) {
//			System.out.println("failed to copy dir data to data_bck");
//		}
		
		File tmpFile = new File("test/script_bck/ics_iAutoDemo");
		if(tmpFile.exists()) {
			System.out.println("in dir: " + Arrays.asList(tmpFile.listFiles()));
			if(!deleteFile(tmpFile)) {
				System.out.println("failed to delete dir data_bck");
			}
		}
		if(!tmpFile.mkdir()) {
			System.out.println("failed to create " + tmpFile.getPath());
		}
	

		String tmp = ",bmc,asd,";
		System.out.println("array result: " + Arrays.toString(tmp.split(",")));
	}

}
