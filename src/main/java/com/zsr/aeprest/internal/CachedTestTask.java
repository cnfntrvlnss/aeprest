package com.zsr.aeprest.internal;


import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.zsr.aeprest.TestTaskRepository;
import com.zsr.aeprest.entity.TestCase;
import com.zsr.aeprest.entity.TestTask;


/**
 * synchronize  memory data with  persistent data; manage logic of task execution.
 * @author zhengshr
 *
 */
public class CachedTestTask {

	TestTaskRepository repo;
	TestTask testTask;
	Iterator<TestCase> testCaseIter;
	TestCase testCase;
	long caseStartTime;
	
	
	public CachedTestTask(TestTaskRepository repo, TestTask task) {
		this.repo = repo;
		this.testTask = task;
		testCaseIter = testTask.getTestCase().iterator();
		testCase = null;
	}
	
	public TestTask getTestTask() {
		return testTask;
	}
	
	public void beginTask() {
		if(testTask.getStartTime() == null) {
			testTask.setStartTime(new Date());
			repo.updateTask(testTask);		
		}
	}
	
	public void endTask(String logPath) {
		testTask.setEndTime(new Date());
		testTask.setStatus(TestTask.Status.FINISHED);
		testTask.setLogPath(logPath);
		repo.updateTask(testTask);
	}
	
	public void endTask(boolean b) {
		testTask.setStatus(TestTask.Status.FAILED);
		repo.updateTask(testTask);
	}
	
	public void setStatus(TestTask.Status status) {
		if(testTask.getStatus() != status) {
			testTask.setStatus(status);
			repo.updateTask(testTask);			
		}
	}
	
	public TestCase getNextTestCase() {
		while(testCaseIter.hasNext()) {
			testCase = testCaseIter.next();
			if(testCase.getResult()!= null && !testCase.getResult().toString().equals("")) {
				continue;
			}
			return testCase;
		}
		return null;
	}
	
	public void startTestCase() {
		caseStartTime = System.currentTimeMillis();
	}
	
	public void finishTestCase(TestCase.Result res) {
		long caseEndTime = System.currentTimeMillis();
		int elapsed = (int) ((caseEndTime - caseStartTime) / 1000);
		testCase.setElapsed(elapsed);
		testCase.setResult(res);
		repo.updateTaskCase(testCase, testTask.getId());
	}

}
