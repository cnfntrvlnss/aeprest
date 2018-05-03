package com.zsr.aeprest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("aep")
public class LogZipController {
	
	@Value("${aep.result-root}")
	String resultRoot;
	
	@RequestMapping("download/{filename:.+}")
	public ResponseEntity<InputStreamResource> downloadLogZip(@PathVariable String filename){
		File file = new File(resultRoot, filename);
		InputStream in = null;
		int lengthOfStream = 0;
		try {
			in = new FileInputStream(file);
			lengthOfStream = in.available();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setContentDispositionFormData("attachment", filename);
		headers.setContentLength(lengthOfStream);
		InputStreamResource resource = new InputStreamResource(in);
		return new ResponseEntity<>(resource, headers, HttpStatus.OK);
	}
}
