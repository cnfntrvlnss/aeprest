package com.zsr.aeprest.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zsr.aeprest.dao.ScriptTaskRepository;
import com.zsr.aeprest.entity.ItmsScriptTask;

public class ScriptTaskConsumer implements Runnable{
	Logger logger = LoggerFactory.getLogger(ScriptTaskConsumer.class);
	
	//TODO 存放result log的目录，要通过注入得到.
	String testLogDirDir = "C:\\Users\\zhengshr\\aeptask\\";
	ItmsScriptTask scriptTask;
	ScriptTaskRepository repository;
	
	public ScriptTaskConsumer(ItmsScriptTask task, ScriptTaskRepository repo) {
		scriptTask = task;
		repository = repo;
	}
	
	boolean cleanDir(File DirFile) {
		if(DirFile.exists()) {
			if(!Utilities.deleteFile(DirFile)) {
				return false;
			}
		}
		return DirFile.mkdir();
	}

	
	/**
	 * 从脚本约束文件中取出命令行，并替换变量.
	 * @param scriptDir
	 * @return
	 */
	String fetchCommandLine(String scriptDir) {
		
		Map<String, Map<String, String>> paramMap = repository.findParamById(scriptTask.getTaskId());
		String cmdline = null;
		File file = new File(scriptDir);
		file = new File(file, "readme.txt");
		try(BufferedReader reader = new BufferedReader(new FileReader(file));){
			String line = null;
			while((line = reader.readLine()) != null) {
				if(line.startsWith("脚本执行方式:")) {
					cmdline = line.split(": +")[1];
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		StringBuffer sb = new StringBuffer();
		Pattern pattern = Pattern.compile("#\\{([^}]+)\\}");
		Matcher matcher = pattern.matcher(cmdline);
		while(matcher.find()) {
			String param = matcher.group(1);
			int sepidx = param.indexOf('.');
			String machineName = param.substring(0, sepidx);
			String subparam = param.substring(sepidx+1);
			Map<String, String> params = paramMap.get(machineName);
			if(params == null) {
				logger.info("cannot assign with param {} in commandline {}", param, cmdline);
				return null;
			}
			String value = params.get(subparam);
			if(value == null) {
				logger.info("cannot assign with param {} in commandline {}", param, cmdline);
				return null;				
			}
			matcher.appendReplacement(sb, value);
		}
		matcher.appendTail(sb);
		return sb.toString();
	}
	
	@Override
	public void run() {
		
		String ftpPath = "/" + scriptTask.getProjectName() + "/" + scriptTask.getTestCaseNumber();
		String taskName = scriptTask.getProjectName() + "_" + scriptTask.getTaskId();
		
		File testLogDir = new File(testLogDirDir, taskName + "_log");
		if(!cleanDir(testLogDir)) {
			logger.info("cannot create testLogDir, {}.", testLogDir.toString());
			scriptTask.setStatus(ItmsScriptTask.Status.BLOCK);
			repository.updateTask(scriptTask);
			return;
		}

		String workDir = ScriptSpace.prepareScriptSpace(taskName, ftpPath);
		if(workDir == null) {
			//ftp download failed.
			logger.info("failed to prepare work directory, ftppath {}", ftpPath);
			scriptTask.setStatus(ItmsScriptTask.Status.BLOCK);
			repository.updateTask(scriptTask);
			return;
		}
		
		String cmdline = fetchCommandLine(workDir);
		if(cmdline == null) {
			scriptTask.setStatus(ItmsScriptTask.Status.BLOCK);
			repository.updateTask(scriptTask);
			return;
		}
		File outFile = new File(testLogDir, "console.log");
		int[] res = new int[1];
		res[0] = -1;
		boolean[] stopped = new boolean[1];
		stopped[0] = false;
		scriptTask.setStartTime(new Date());
		logger.debug("start executing script task, task: {}, workdir: {}, cmdline: {}", taskName, workDir, cmdline);
		if(!CommandLineExecutor.execCmd(workDir, cmdline, outFile.toString(), stopped, res)) {
			logger.info("failed to execute command line, workDir {}, commandline {}", workDir, cmdline);
			scriptTask.setStatus(ItmsScriptTask.Status.BLOCK);
			repository.updateTask(scriptTask);
			return;
		}
		
		logger.debug("finish executing script task, task: {}, exitvalue: {}", taskName, res[0]);
		scriptTask.setEndTime(new Date());
		if(res[0] == 0) {
			scriptTask.setStatus(ItmsScriptTask.Status.SUCCESS);
		}else {
			scriptTask.setStatus(ItmsScriptTask.Status.FAILED);
		}
		File srcDir = new File(workDir, "log");
		if (srcDir.exists()) {
			File desDir = new File(testLogDir, scriptTask.getTestCaseNumber() + "_log");
			Utilities.copyDir(srcDir.getPath(), desDir.getPath());
		}
		Utilities.zipcompress(testLogDir);
		File zipFile = new File(testLogDir.getParentFile(), testLogDir.getName() + ".zip");
		scriptTask.setResultUrl(zipFile.getName());
		repository.updateTask(scriptTask);
	}
}
