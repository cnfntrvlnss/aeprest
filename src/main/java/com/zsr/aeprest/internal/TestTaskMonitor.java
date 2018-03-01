package com.zsr.aeprest.internal;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zsr.aeprest.TestTaskRepository;
import com.zsr.aeprest.entity.TestCase;
import com.zsr.aeprest.entity.TestTask;

class TestTaskConsumer implements Runnable{

	Logger logger = LoggerFactory.getLogger(TestTaskConsumer.class);
	CachedTestTask task;
	
	public TestTaskConsumer(CachedTestTask task) {
		this.task = task;
	}
	
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
		
		
	}
	
}

/**
 * monitor task set, make all pending tasks to be finished, 
 * @author zhengshr
 *
 */
@Component
public class TestTaskMonitor implements Runnable, Closeable {

	static Logger logger = LoggerFactory.getLogger(TestTaskMonitor.class);
	private TestTaskRepository repo;
	
	private ExecutorService execService;
	
	@Autowired
	public TestTaskMonitor(TestTaskRepository repo) {
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
		logger.info("~~~~~~start to loop task of TestTask.");
		List<TestTask> tasks = repo.findUnfinshedTask();
		//logger.debug("return from findUnfinshedTask, size: {}", tasks.size());
		for(TestTask task: tasks) {
			CachedTestTask cached = new CachedTestTask(repo, task);
			cached.setStatus(TestTask.Status.RUNNING);
			execService.execute(new TestTaskConsumer(cached));
		}
		try {
			while(true) {
				logger.debug("~~~~~~~in loop to find new testTask to execute.");
					synchronized(this) {
						this.wait(60000);	
					}
				int thdcnt = ((ThreadPoolExecutor)execService).getActiveCount();
				logger.debug("~~~~~~{} thread pool statistic, num of active threads: {}", this.toString(), thdcnt);
				tasks = repo.findNewTask();
				for(TestTask task: tasks) {
					CachedTestTask cached = new CachedTestTask(repo, task);
					cached.setStatus(TestTask.Status.RUNNING);
					execService.execute(new TestTaskConsumer(cached));
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
