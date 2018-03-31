package com.zsr.aeprest.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zsr.aeprest.entity.TestCase;
import com.zsr.aeprest.entity.TestTask;
import com.zsr.aeprest.internal.ScriptSpace;


class TestTaskConsumer implements Runnable{

	static Logger logger = LoggerFactory.getLogger(TestTaskConsumer.class);
	CachedTestTask task;
	
	public TestTaskConsumer(CachedTestTask task) {
		this.task = task;
	}
	
	
	/** faked run procedure, only test execution flow.
	@Override
	public void run() {
		//follow interface of CachedTestTask, have task being executed.
		logger.debug("start to execute task, taskId {},", task.getTestTask().getId());
		task.beginTask();
		try {
			Thread.sleep(3000);
			while(true) {
				TestCase cas = task.getNextTestCase();
				if(cas == null) break;
				Thread.sleep(3000);
				task.finishTestCase(TestCase.Result.PASS, 3000);
			}
			
			task.endTask("C:\\Users\\zhengshr\\Downloads\\20180118150059_1_111_111_BMC_111_log.zip");
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			
		}
		
	}*/
	
	static String testLogDirDir = "C:\\Users\\zhengshr\\aeptask\\";
	
	static {
		File file = new File(testLogDirDir);
		if(!file.exists()) {
			if(!file.mkdir()) {
				logger.error("failed to create logDir for testTask, {}.", testLogDirDir);
			}
		}
	}
	
	boolean prepareDir(File DirFile) {
		if(DirFile.exists()) {
			if(!Utilities.deleteFile(DirFile)) {
				return false;
			}
		}
		return DirFile.mkdir();
	}
	
	static String prepareCommandLine(String name, String param) {
		StringBuilder cmd = new StringBuilder();
		String scPath = new File("./", name).getPath();
		if(scPath.endsWith(".jar")) {
			cmd.append("java -jar ");
		} else if(scPath.endsWith(".ps1")) {
			cmd.append("powershell ");
		}
//		else if(scPath.endsWith(".py")) {
//			cmd.append("python ");
//		}
		cmd.append(scPath);
		cmd.append(" ");
		cmd.append(param);
		return cmd.toString();
	}
	

	
	@Override
	public void run() {
		logger.debug("start to execute task, taskId {}.", task.getTestTask().getId());
		task.beginTask();
		TestTask testTask = task.getTestTask();
		String taskname = String.format("task_%d_%s", testTask.getId(), testTask.getName());
		File testLogDir = new File(testLogDirDir, taskname + "_log");
		if(!prepareDir(testLogDir)) {
			logger.debug("cannot create testLogDir, {}.", testLogDir.toString());
			task.endTask(false);
			return;
		}
		
		boolean[] stopped = new boolean[1];
		stopped[0] = false;
		while(true) {
			TestCase cas = task.getNextTestCase();
			if(cas == null) break;
			String workDir = ScriptSpace.prepareScriptSpace(taskname, cas.getSrcPath());
			if(workDir == null) {
				task.endTask(false);
				logger.debug("cannot prepare script space, {},{}.", taskname, cas.getSrcPath());
				return;
			}
			String cmd = prepareCommandLine(cas.getScriptName(), cas.getScriptParam());
			File outPath = new File(testLogDir, cas.getId() + ".log");
			int[] res = new int[1];
			res[0] = -1;
			task.startTestCase();
			if(execCmd(workDir, cmd, outPath.toString(), stopped, res)) {
			}
			if(res[0] == 0) {
				task.finishTestCase(TestCase.Result.PASS);
			}else {
				task.finishTestCase(TestCase.Result.FAIL);
			}
			
			File srcDir = new File(workDir, "log");
			if (srcDir.exists()) {
				File desDir = new File(testLogDir, cas.getId() + "_log");
				Utilities.copyDir(srcDir.getPath(), desDir.getPath());
			}
		}
		
		Utilities.zipcompress(testLogDir);
		File zipFile = new File(testLogDir.getParentFile(), testLogDir.getName() + ".zip");
		task.endTask(zipFile.getPath());
	}
	
	
	
	
	/**
	 * execute one script, store stderr/stdout to outPath, return exitValue in results.
	 * @param workDir
	 *            cwd when executing scripts.
	 * @param cmd
	 *            command to execute.
	 * @param outPath
	 *            output file for log from terminal.
	 * @param results
	 *            represent exit code.
	 * @return true if success.
	 */
	private static boolean execCmd(String workDir, String cmd, String outPath, boolean[] stopRef, int[] results) {

		try (RandomAccessFile outin = new RandomAccessFile(outPath, "rw")) {
			long startPos = outin.length();
			outin.seek(startPos);
			ProcessBuilder pb = new ProcessBuilder();
			pb.directory(new File(workDir));
			pb.command(Arrays.asList(cmd.split(" ")));
			pb.redirectErrorStream(true);
			Process p = null;
			try {
				p = pb.start();

			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}

			InputStream procOut = p.getInputStream();
			boolean bconti = true;// turn false if child exits.
			byte[] tmpData = new byte[1024];
			while (bconti) {
				try {
					if (p.waitFor(1, TimeUnit.SECONDS)) {
						bconti = false;
					}
				} catch (InterruptedException e) {
					logger.info("cmd: {}, interrupted while waitFor child process.", cmd);
				}

				try {
					int avilen;
					while ((avilen = procOut.available()) > 0) {
						if (avilen > tmpData.length) {
							avilen = tmpData.length;
						}
						procOut.read(tmpData, 0, avilen);
						outin.write(tmpData, 0, avilen);
						logger.trace("yeild output from child process {} bytes", avilen);
					}
				} catch (IOException e) {
					logger.error("failed to transfer output of child process", e);
				}
				
				boolean bterm = false;
				if (bconti) {
					logger.trace("periodicly check indication for stopping child process.");
					synchronized (stopRef) {
						if (stopRef[0]) {
							bterm = true;
						}
					}
				}

				if (bterm) {
					logger.info("the child process should be terminated, cmd: {}", cmd);
					p.destroy();// terminate the child process.
					while(bconti) {
						try {
							p.waitFor();
							bconti = false;
						} catch (InterruptedException e) {
							
						}
					}
				}
			}

			results[0] = p.exitValue();
			return true;

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}

		return false;

	}


	
}
