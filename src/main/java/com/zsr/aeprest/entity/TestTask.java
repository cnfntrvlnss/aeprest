package com.zsr.aeprest.entity;

import java.util.Date;
import java.util.List;

public class TestTask {
	int id;
	String name;
	List<TestCase> testCase;
	Date startTime;
	Date endTime;
	Status status;
	String logPath;
	
	public static enum Status{
		RUNNING, FINISHED, FAILED,
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<TestCase> getTestCase() {
		return testCase;
	}

	public void setTestCase(List<TestCase> tcs) {
		this.testCase = tcs;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getLogPath() {
		return logPath;
	}

	public void setLogPath(String logPath) {
		this.logPath = logPath;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	
}
