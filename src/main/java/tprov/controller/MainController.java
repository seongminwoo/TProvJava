package tprov.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import tprov.model.TomcatInstance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.ConflictException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Container.Port;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.SearchItem;
import com.github.dockerjava.core.DockerClientBuilder;

@Controller
public class MainController {
	private static final String DOCKER_SERVER_URL = "http://192.168.59.103:2375";
	Logger log = LoggerFactory.getLogger(this.getClass());
	DockerClient docker = DockerClientBuilder.getInstance(DOCKER_SERVER_URL).build();
	ObjectMapper objectMapper = new ObjectMapper(); 
	
	@RequestMapping("/")
    public String home() {
        return "index";
    }
	
	@RequestMapping("/docker/{cmd}")
	@ResponseBody
	public String commandDocker(@PathVariable("cmd") String cmd, @RequestParam(value="query", required=false) String query) throws JsonProcessingException {
		switch(cmd) {
			case "info":
				Info info = docker.infoCmd().exec();
				return objectMapper.writeValueAsString(info);
			case "search":
				List<SearchItem> searchResult = docker.searchImagesCmd(query).exec();
				return objectMapper.writeValueAsString(searchResult);
			default:
				return cmd + "is not supported now.";
		}
	}
	
	@RequestMapping("/tomcat/create")
	public String createTomcat(@RequestParam(value="name", required=true) String name,
			@RequestParam(value="url", required = true) String url) {
		log.info("name : {}", name);
		log.info("url : {}", url);
		
		getWar(url, name);
		createTomcatInstance(name);
		return "redirect:/tomcat/list";
	}
	
	@RequestMapping("/tomcat/list.json")
	@ResponseBody
	public String listTomcatJson() throws JsonProcessingException {
		List<TomcatInstance> tomcatInstances = listTomcatInstances();
		return objectMapper.writeValueAsString(tomcatInstances);
	}
	
	@RequestMapping("/tomcat/list")
	public String listTomcat(Model model) throws JsonProcessingException {
		List<TomcatInstance> tomcatInstances = listTomcatInstances();
		model.addAttribute("tomcats", tomcatInstances);
		return "tomcatList";
	}
	
	@RequestMapping("/tomcat/delete")
	public String deleteTomcat(Model model, @RequestParam("id") List<String> ids) throws JsonProcessingException {
		for(String id : ids) {
			deleteTomcatInstance(id);
		}
		
		List<TomcatInstance> tomcatInstances = listTomcatInstances();
		model.addAttribute("tomcats", tomcatInstances);
		return "redirect:/tomcat/list";
	}

	private List<TomcatInstance> listTomcatInstances() {
		List<TomcatInstance> list = new ArrayList<TomcatInstance>();
		
		List<Container> containers = docker.listContainersCmd().exec();
		for(Container container : containers) {
			log.info("container : {}", container);
			Port[] ports = container.getPorts();
			
			for(Port port : ports) {
				Integer privatePort = port.getPrivatePort();
				
				if(privatePort == 8080) {
					TomcatInstance tomcat = new TomcatInstance();
					InspectContainerResponse inspectResult = docker.inspectContainerCmd(container.getId()).exec();
					tomcat.setContainerId(container.getId());
					tomcat.setHostname(inspectResult.getConfig().getHostName());
					tomcat.setIp(inspectResult.getNetworkSettings().getIpAddress());
					tomcat.setPort(port.getIp() + ":" + port.getPublicPort());
					list.add(tomcat);
				}
			}
		}
		
		return list;
	}

	private void createTomcatInstance(String name) {
		CreateContainerResponse container;
		container = docker.createContainerCmd("jamtur01/tomcat7").exec();
		docker.startContainerCmd(container.getId()).withPublishAllPorts(true).withVolumesFrom(name).exec();
	}

	private void getWar(String url, String name) {
		CreateContainerResponse container;
		
		try {
			container = docker.createContainerCmd("jamtur01/fetcher").withName(name).withCmd(url).exec();
		} catch (ConflictException ce) {
			deleteTomcatInstanceByName(name);
			container = docker.createContainerCmd("jamtur01/fetcher").withName(name).withCmd(url).exec();
		}
		
		docker.startContainerCmd(container.getId()).exec();
	}
	
	private void deleteTomcatInstance(String id) {
		docker.killContainerCmd(id).exec();
		docker.removeContainerCmd(id).exec();
	}

	private void deleteTomcatInstanceByName(String name) {
		List<Container> containers = docker.listContainersCmd().withShowAll(true).exec();
		
		for(Container con : containers) {
			if(Arrays.asList(con.getNames()).contains("/" + name)) {
				deleteTomcatInstance(con.getId());
			}
		}
	}
}
