package com.zsr.aeprest.internal;

import java.io.Closeable;
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
import com.zsr.aeprest.entity.TestTask;


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
