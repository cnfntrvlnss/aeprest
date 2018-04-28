package com.zsr.aeprest.entity;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ItmsScriptTask {

	int taskId;
	String projectName;
	String testCaseNumber;
	Date startTime;
	Date endTime;
	Status status;
	String resultUrl;
	public static enum Status{
		WAIT, BLOCK, START, FAILED, SUCCESS
	}
	List<Map<String, String>> scriptParam;
	
	
	public int getTaskId() {
		return taskId;
	}
	public void setTaskId(int taskId) {
		this.taskId = taskId;
	}
	public String getProjectName() {
		return projectName;
	}
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public String getTestCaseNumber() {
		return testCaseNumber;
	}
	public void setTestCaseNumber(String testCaseNumber) {
		this.testCaseNumber = testCaseNumber;
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
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public String getResultUrl() {
		return resultUrl;
	}
	public void setResultUrl(String resultUrl) {
		this.resultUrl = resultUrl;
	}
	public List<Map<String, String>> getScriptParam() {
		return scriptParam;
	}
	public void setScriptParam(List<Map<String, String>> scriptParam) {
		this.scriptParam = scriptParam;
	}
	
	
}
