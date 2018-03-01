package com.zsr.aeprest;


import java.util.HashMap;
import java.util.List;
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
import com.zsr.aeprest.entity.TestCase;
import com.zsr.aeprest.entity.TestTask;
import com.zsr.aeprest.internal.TestTaskMonitor;

@RestController
@RequestMapping("aep")
public class TestTaskController {

	static Logger logger = LoggerFactory.getLogger(TestTaskController.class);
	
	@Autowired
	TestTaskRepository repo;
	@Autowired
	TestTaskMonitor testTaskMonitor;
	
	@RequestMapping(value="executeTask", method=RequestMethod.POST)
	public String execTask(@RequestBody TestTask task) {
		
		logger.info("get task: {}.", toString(task));
		repo.save(task);
		synchronized(testTaskMonitor) {
			testTaskMonitor.notifyAll();			
		}
		return "success";
	}
	
	@RequestMapping(value="taskStatus/{taskId}")
	public Map<String, Object> queryTaskStatus(@PathVariable("taskId") int id){
		TestTask task = repo.findTask(id);
		if(task == null) {
			throw new RuntimeException("taskId " + id + " not exist");
		}
		Map<String, Object> retm = new HashMap<>();
		retm.put("id", task.getId());
		retm.put("name", task.getName());
		retm.put("status",  task.getStatus());
		List<TestCase> tcs = task.getTestCase();
		int cnt = 0;
		for(TestCase tc: tcs) {
			if(tc.getResult() != null && !"".equals(tc.getResult().toString())) {
				cnt ++;
			}
		}
		retm.put("progress",(int) ((float)cnt / tcs.size()) * 100);
		
		return retm;
	}
	
	@RequestMapping(value="task/{taskId}", method=RequestMethod.GET)
	public TestTask queryTask(@PathVariable("taskId") int id) {
		
		TestTask task = repo.findTask(id);
		if(task == null) {
			throw new RuntimeException(String.format("taskId %d not exist", id));
		}
		logger.info("after execution, testTask: {}", toString(task));
		return task;
	}
	
	@RequestMapping(value="task/{taskId}", method=RequestMethod.DELETE)
	public void deleteTask(@PathVariable("taskId") int id) {
		
		repo.deleteTask(id);
		
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
