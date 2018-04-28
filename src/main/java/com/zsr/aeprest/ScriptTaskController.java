package com.zsr.aeprest;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zsr.aeprest.dao.ScriptTaskRepository;
import com.zsr.aeprest.entity.ItmsScriptTask;

@RestController
@RequestMapping("aep")
public class ScriptTaskController {
	static Logger logger = LoggerFactory.getLogger(ScriptTaskController.class);
	
	@Autowired
	ScriptTaskRepository repo;
	
	@RequestMapping(value="executeTask", method=RequestMethod.POST)
	public String execTask(@RequestBody ItmsScriptTask task) {
		logger.info("receive task: {}", toString(task));
		repo.save(task);
		
		return "success";
	}
	
	@RequestMapping(value="taskStatus/{taskId}")
	public Map<String, Object> queryTaskStatus(@PathVariable("taskId") int id){
		logger.debug("queryTaskStatus {}", id);
		
		ItmsScriptTask task = repo.findTaskById(id);
		Map<String, Object> retm = new HashMap<>();
		retm.put("taskId", task.getTaskId());
		retm.put("projectName", task.getProjectName());
		retm.put("testCaseNumber", task.getTestCaseNumber());
		retm.put("status", task.getStatus());
		
		return retm;
	}
	
	@RequestMapping(value="task/{taskId}", method=RequestMethod.GET)
	public ItmsScriptTask queryTask(@PathVariable("taskId") int id) {
		logger.debug("queryTask {}", id);
		
		ItmsScriptTask task = repo.findById(id);
		return task;
	}
	
	@RequestMapping(value="task/{taskId}", method=RequestMethod.DELETE)
	public void deleteTask(@PathVariable("taskId") int id) {
		logger.debug("deleteTask {}", id);
		repo.deleteById(id);
	}

	
	String toString(Object o) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		String json = "";
		try {
			json = mapper.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return json;
	}

}