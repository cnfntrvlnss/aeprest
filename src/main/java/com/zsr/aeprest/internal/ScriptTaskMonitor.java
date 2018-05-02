package com.zsr.aeprest.internal;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zsr.aeprest.dao.ScriptTaskRepository;
import com.zsr.aeprest.entity.ItmsScriptTask;

@Component
public class ScriptTaskMonitor implements Runnable, Closeable{
	static Logger logger = LoggerFactory.getLogger(TestTaskMonitor.class);
	
	private ScriptTaskRepository repo;
	private ExecutorService execService;
	
	@Autowired
	public ScriptTaskMonitor(ScriptTaskRepository repo) {
		this.repo = repo;
		execService = Executors.newCachedThreadPool();
		execService.execute(this);
	}
	
	@Override
	public void close() {
		logger.info("!!!!!! !! shutdown my ExecutorService.");
		execService.shutdown();
		try {
			while(true) {
				if(execService.awaitTermination(1, TimeUnit.SECONDS)) {
					break;
				}
				logger.info("executorService waiting for worker thread to complete execution...");
				execService.shutdownNow();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected void finalize() {
		close();
	}
	
	@Override
	public void run() {
		logger.info("start monitoring script tasks.");
		//已经开始过的判定失败
		List<ItmsScriptTask> tasks = repo.findByStatus(Arrays.asList(ItmsScriptTask.Status.START));
		for(ItmsScriptTask task: tasks) {
			logger.debug("history unfinished task {}, be re-assigned as BLOCK");
			task.setStatus(ItmsScriptTask.Status.BLOCK);
			repo.updateTask(task);
		}
		//已经调度过但未开始的，重新设定WAIT
		tasks = repo.findByStatus(Arrays.asList(ItmsScriptTask.Status.READY));
		for(ItmsScriptTask task: tasks) {
			logger.debug("task having been scheduled, without reaching to START, be re-assigned as WAIT");
			task.setStatus(ItmsScriptTask.Status.WAIT);
			repo.updateTask(task);
		}
		
		try {
			while(true) {
				logger.debug("~~~~~~~in loop to find new scriptTask to execute.");
				int thdcnt = ((ThreadPoolExecutor)execService).getActiveCount();
				logger.debug("~~~~~~{} thread pool statistic, num of active threads: {}", this.toString(), thdcnt);
				tasks = repo.findByStatus(Arrays.asList(ItmsScriptTask.Status.WAIT));
				for(ItmsScriptTask task: tasks) {
					task.setStatus(ItmsScriptTask.Status.READY);
					repo.updateTask(task);
					execService.execute(new ScriptTaskConsumer(task, repo));
				}
				synchronized(this) {
					this.wait(60000);	
				}

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
