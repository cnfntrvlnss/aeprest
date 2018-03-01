package com.zsr.aeprest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.zsr.aeprest.entity.TestCase;
import com.zsr.aeprest.entity.TestTask;

@Repository
public class TestTaskRepository {
	
	@Autowired
	public JdbcTemplate jdbc;

	public boolean save(TestTask task) {
		String INSERT_TASK = "insert into testtask(id, name) values(?,?);";
		String INSERT_TASKCASE = "insert into testtaskcase(taskId, caseId, name, srcPath, scriptName, scriptParam) "
				+ "values(?,?,?,?,?,?);";
		if(hasTask(task.getId())) {
			throw new RuntimeException("taskId " + task.getId() + " already exist");
		}
		//erase dirty data.
		String DELETE_TESTCASE = "delete from testtaskcase where taskId = ?;";
		jdbc.update(DELETE_TESTCASE, task.getId());
		//insert taskcase at first.
		for(TestCase tc: task.getTestCase()) {
			jdbc.update(INSERT_TASKCASE, task.getId(), tc.getId(), tc.getName(), tc.getSrcPath(), tc.getScriptName()
					, tc.getScriptParam());
		}
		jdbc.update(INSERT_TASK, task.getId(), task.getName());
		
		return true;
	}
	
	public boolean hasTask(int id) {
		String SELECT_TASK_ID = "select id from testtask where id=?;";
		List<Integer> retId = jdbc.query(SELECT_TASK_ID, (rs, rn)->{
			return rs.getInt(1);
			}, id);
		if(retId == null || retId.size() == 0) {
			return false;
		}
		return true;
	}
	
	public TestTask findTask(int id) {
		String SELECT_TASKCASE_BYID = "select caseId, name, srcPath, scriptName, scriptParam, elapsedTime, result "
				+ "from testtaskcase where taskId = ?";
		String SELECT_TASK_BYID = "select id, name, startTime, endTime, status, logPath from testtask where id = ?;";
		List<TestCase> tcs = jdbc.query(SELECT_TASKCASE_BYID, (rs, rn)->{
			TestCase tc = new TestCase();
			tc.setId(rs.getInt(1));
			tc.setName(rs.getString(2));
			tc.setSrcPath(rs.getString(3));
			tc.setScriptName(rs.getString(4));
			tc.setScriptParam(rs.getString(5));
			tc.setElapsed(rs.getInt(6));
			if(rs.getString(7) != null) {
				tc.setResult(TestCase.Result.valueOf(rs.getString(7)));
			}
			
			return tc;
		}, id);
		List<TestTask> tasks = jdbc.query(SELECT_TASK_BYID, (rs, rn)->{
			TestTask t = new TestTask();
			t.setId(rs.getInt(1));
			t.setName(rs.getString(2));
			t.setStartTime(rs.getTimestamp(3));
			t.setEndTime(rs.getTimestamp(4));
			if(rs.getString(5) != null) {
				t.setStatus(TestTask.Status.valueOf(rs.getString(5)));
			}
			t.setLogPath(rs.getString(6));
			
			return t;
		}, id);
		if(tasks.size()== 0) {
			return null;
		}
		tasks.get(0).setTestCase(tcs);
		
		return tasks.get(0);
	}
	
	List<Integer> findAllId(){
		String SELECT_TASKID = "select id from testtask;";
		return jdbc.query(SELECT_TASKID, (rs, rn)->{
			return Integer.valueOf(0);
		});
	}
	
	public List<TestTask> findAll(){
		List<Integer> ids = findAllId();
		List<TestTask> ret = new ArrayList<>();
		for(int id: ids) {
			ret.add(findTask(id));
		}
		return ret;
	}
	
	public void deleteTask(int id) {
		final String DELETE_TASKCASE = "delete from testtaskcase where taskId = ?;";
		final String DELETE_TASK = "delete from testtask where id = ?;";
		jdbc.update(DELETE_TASKCASE, id);
		jdbc.update(DELETE_TASK, id);
	}
	
	/**
	 * called when the application starts, as no task is processed in separated thread.
	 * @return
	 */
	public List<TestTask> findUnfinshedTask(){
		final String SELECT_TASK_BYSTAT = "select id from testtask where status = ? or status = '' or status is null";
		List<Integer> ids = jdbc.query(SELECT_TASK_BYSTAT, (rs, rn)->{		
			return Integer.valueOf(rs.getInt(1));
		}, TestTask.Status.RUNNING.toString());
		List<TestTask> ret = new ArrayList<>();
		for(int id: ids) {
			ret.add(findTask(id));
		}
		return ret;
	}
	/**
	 * called at the point of new task arriving.
	 * @return
	 */
	public List<TestTask> findNewTask(){
		final String SELECT_TASK_BYNULLSTAT = "select id from testtask where status is null or status = ''";
		List<Integer> ids = jdbc.query(SELECT_TASK_BYNULLSTAT, (rs, rn)->{
			return Integer.valueOf(rs.getInt(1));
		});
		List<TestTask> ret = new ArrayList<>();
		for(int id: ids) {
			ret.add(findTask(id));
		}
		return ret;
	}
	
	public void updateTask(TestTask task) {
		final String UPDATE_TASK_PARTIAL = "update testtask set startTime=?, endTime=?, status=?, logPath=?  where id=?";
		jdbc.update(UPDATE_TASK_PARTIAL, task.getStartTime(), task.getEndTime(), task.getStatus().toString(), task.getLogPath(), task.getId());
	}
	
	public void updateTaskCase(TestCase cas, int taskId) {
		final String UPDATE_TASKCASE_PARTIAL = "update testtaskcase set elapsedTime=?, result=?  where taskId = ? and caseId = ?;";
		jdbc.update(UPDATE_TASKCASE_PARTIAL, cas.getElapsed(), cas.getResult().toString(), taskId, cas.getId());
	}
}
