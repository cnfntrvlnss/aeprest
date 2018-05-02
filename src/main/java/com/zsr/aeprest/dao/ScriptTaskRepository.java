package com.zsr.aeprest.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import com.zsr.aeprest.entity.ItmsScriptTask;


@Repository
public class ScriptTaskRepository {

	@Autowired
	public JdbcTemplate jdbc;

	public void save(ItmsScriptTask task) {
		
		String INSERT_TASK = "insert into itmsscripttask(task_id, project_name, test_case_number, status) "
				+ "values(?,?,?,?)";
		String INSERT_SCRIPT_PARAM = "insert into itmsscriptparam(task_id, machine_name, param_name, param_value) values(?,?,?,?)";
		
		if(hasTask(task.getTaskId())) {
			throw new RuntimeException("save failed, as task " + task.getTaskId() + " already exists");
		}
		
		//erase dirty data in itmsscriptparam table.
		String DELETE_SCRIPT_PARAM_BY_ID = "delete from itmsscriptparam where task_id = ?";
		jdbc.update(DELETE_SCRIPT_PARAM_BY_ID, task.getTaskId());
		
		//insert itmsscriptparam table.
		//rearrange scriptparam field in list format.
		List<List<String>> lis = new ArrayList<>();
		String taskId = task.getTaskId();
		List<Map<String, String>> scriptParam = task.getScriptParam();
		for(Map<String, String> group: scriptParam) {
			String machineName = group.get("machine_name");
			for(Map.Entry<String, String> entry: group.entrySet()) {
				if(entry.getKey().equals("machine_name")) continue;
				List<String> li = new ArrayList<>();
				li.add(taskId);
				li.add(machineName);
				li.add(entry.getKey());
				li.add(entry.getValue());
				lis.add(li);
			}
		}
		String insert_script_param = INSERT_SCRIPT_PARAM;
		if(lis.size() > 1) {
			StringBuilder sb  = new StringBuilder();
			for(int i=0; i< lis.size() - 1; i++) {
				sb.append(",(?,?,?,?)");
			}
			insert_script_param += sb.toString();
		}
		PreparedStatementSetter pss = new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement arg0) throws SQLException {
				int idx = 1;
				for(List<String> li: lis) {
					Iterator<String> it = li.iterator();
					while(it.hasNext()) {
						arg0.setString(idx++, it.next());
					}
				}
			}
		};
		jdbc.update(insert_script_param, pss);
		
		//insert itmsscripttask table.
		jdbc.update(INSERT_TASK, task.getTaskId(), task.getProjectName(), task.getTestCaseNumber(), task.getStatus().toString());
		
	}
	
	public boolean hasTask(String id) {
		String SELECT_TASK_ID = "select task_id from itmsscripttask where task_id=?";

		List<String> retIds = jdbc.query(SELECT_TASK_ID, (rs, rn)->{
			return rs.getString(1);
		}, id);
		if(retIds.size() == 0) {
			return false;
		}
		return true;
	}
	
	public ItmsScriptTask findById(String id) {
		ItmsScriptTask task = findTaskById(id);
		Collection<Map<String, String>> params = findParamById(task.getTaskId()).values();
		task.setScriptParam(new ArrayList<>(params));
		
		return task;
	}
	
	public ItmsScriptTask findTaskById(String id) {
		String SELECT_TASK_BY_ID = "select task_id, project_name, test_case_number, start_time, end_time, status, result_url "
				+ "from itmsscripttask where task_id = ?";
				
		List<ItmsScriptTask> tasks = jdbc.query(SELECT_TASK_BY_ID, (rs, rn)->{
			ItmsScriptTask ret = new ItmsScriptTask();
			ret.setTaskId(rs.getString(1));
			ret.setProjectName(rs.getString(2));
			ret.setTestCaseNumber(rs.getString(3));
			ret.setStartTime(rs.getTimestamp(4));
			ret.setEndTime(rs.getTimestamp(5));
			ret.setStatus(ItmsScriptTask.Status.valueOf(rs.getString(6)));
			ret.setResultUrl(rs.getString(7));
			
			return ret;
		}, id);
		
		if(tasks.size() == 0) {
			throw new RuntimeException("findById failed, as task " + id + " not exist");
		}
		ItmsScriptTask task = tasks.get(0);
		
		return task;
	}
	
	public Map<String, Map<String, String>> findParamById(String string) {
		
		String SELECT_SCRIPT_PARAM_BY_ID = "select task_id, machine_name, param_name, param_value "
				+ "from itmsscriptparam where task_id = ?";
		List<List<String>> paramsList = jdbc.query(SELECT_SCRIPT_PARAM_BY_ID, (rs, rn)->{
			List<String> ret = new ArrayList<>();
			ret.add(rs.getString(1));
			ret.add(rs.getString(2));
			ret.add(rs.getString(3));
			ret.add(rs.getString(4));
			return ret;
		}, string);
		
		Map<String, Map<String, String>> scriptParams = new HashMap<>();
		for(List<String> li: paramsList) {
			Iterator<String> it = li.iterator();
			it.next();
			String machineName = it.next();
			String paraName = it.next();
			String paraVal = it.next();
			if(!scriptParams.containsKey(machineName)) {
				scriptParams.put(machineName, new HashMap<>());
				scriptParams.get(machineName).put("machine_name", machineName);
			}
			scriptParams.get(machineName).put(paraName, paraVal);
		}
		
		return scriptParams;
	}
	
	List<String> findAllIds(){
		String SELECT_TASK_IDS = "select task_id from itmsscripttask;";
		List<String> ret = jdbc.query(SELECT_TASK_IDS, (rs, rn)->{
			return rs.getString(1);
		});
		return ret;
	}
	
	public List<ItmsScriptTask> findAll(){
		List<ItmsScriptTask> ret = new ArrayList<>();
		for(String id: findAllIds()) {
			ret.add(findById(id));
		}
		return ret;
	}
	
	public void deleteById(String id) {
		deleteParamsById(id);
		String DELETE_SCRIPT_TASK_BY_ID = "delete from itmsscripttask where task_id = ?";
		jdbc.update(DELETE_SCRIPT_TASK_BY_ID, id);
	}
	
	public void deleteParamsById(String id) {
		String DELETE_SCRIPT_PARAM_BY_ID = "delete from itmsscriptparam where task_id = ?";
		jdbc.update(DELETE_SCRIPT_PARAM_BY_ID, id);
	}
	
	public List<ItmsScriptTask> findByStatus(List<ItmsScriptTask.Status> stats) {
		String FIND_BY_STATUS = "select task_id from itmsscripttask where status in ";
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		Iterator<ItmsScriptTask.Status> it = stats.iterator();
		sb.append("'" + it.next() + "'");
		while(it.hasNext()) {
			sb.append(", '" + it.next() + "'");
		}
		sb.append(")");
		String findByStatus = FIND_BY_STATUS + sb.toString();
		
		
		List<String> ids = jdbc.query(findByStatus, (rs, rn)->{
			return rs.getString(1);
		});
		
		List<ItmsScriptTask> ret = new ArrayList<>();
		for(String id: ids) {
			ret.add(findById(id));
		}
		
		return ret;
	}
	
	public void updateTask(ItmsScriptTask task) {
		String UPDATE_TASK = "update itmsscripttask set start_time=?, end_time=?, status=?, result_url=? where task_id=?;";
		jdbc.update(UPDATE_TASK, task.getStartTime(), task.getEndTime(), task.getStatus().toString(), task.getResultUrl(), task.getTaskId());
	}
}

















