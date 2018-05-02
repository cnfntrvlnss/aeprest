package com.zsr.aeprest.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandLineExecutor {
	static Logger logger = LoggerFactory.getLogger(CommandLineExecutor.class);
	
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
	public static boolean execCmd(String workDir, String cmd, String outPath, boolean[] stopRef, int[] results) {

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
