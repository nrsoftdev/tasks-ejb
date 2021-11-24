package nrsoft.tasks.ejb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.bind.JAXBException;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.spi.ValueReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.schema.beans.Beans;

import nrsoft.tasks.ProcessData;
import nrsoft.tasks.Task;
import nrsoft.tasks.TaskResult;
import nrsoft.tasks.dto.ProcessDTO;
import nrsoft.tasks.dto.ProcessDefinitionDTO;
import nrsoft.tasks.dto.TaskDefinitionDTO;
import nrsoft.tasks.dto.TextConnectorDTO;
import nrsoft.tasks.logger.LoggerProvider;
import nrsoft.tasks.logger.LoggersProvider;
import nrsoft.tasks.model.ProcessDefinition;
import nrsoft.tasks.model.TaskDefinition;
import nrsoft.tasks.persistance.TasksDaoJPA;
import nrsoft.tasks.runtime.Process;
import nrsoft.tasks.runtime.ProcessObserver;
import nrsoft.tasks.runtime.ProcessObserverPersistance;
import nrsoft.tasks.runtime.TaskProviderSpringFilesystem;
import nrsoft.tasks.spring.BeanCreator;
import nrsoft.tasks.spring.XmlSpringConfigurationBuilder;


@Stateless
public class ProcessDefinitionBean implements nrsoft.tasks.ejb.ProcessDefinition {
	
	private static Logger logger = LoggerFactory.getLogger(ProcessDefinitionBean.class);
	
	@PersistenceContext(unitName="processDefinition") 
	protected EntityManager em;
	
	// private UserTransaction ut;
	
	private ModelMapper modelMapper = new ModelMapper();

	
	public ProcessDefinitionBean()
	{
		/*InputStream stream = TaskSvc.class.getResourceAsStream("/config.properties");
		Properties properties = new Properties();
		try {
			properties.load(stream);
		} catch (IOException e) {
			log.warn("Errore in lettura di config.properties",e);
		}
		*/
		
		modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
		TypeMap<nrsoft.tasks.model.ProcessDefinition, ProcessDefinitionDTO> typeMap = modelMapper.createTypeMap(nrsoft.tasks.model.ProcessDefinition.class, ProcessDefinitionDTO.class);
		
		
		typeMap.addMappings(mapper -> mapper.skip(ProcessDefinitionDTO::setTaskDefinitionDescription));
		typeMap.addMappings(mapper -> mapper.skip(ProcessDefinitionDTO::setTaskDefinitionId));
		typeMap.addMappings(mapper -> mapper.skip(ProcessDefinitionDTO::setTaskDefinitionName));
		

	}
	
	@Override
	public List<ProcessDefinitionDTO> getProcessDefinitionList() {
		
	
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		List<nrsoft.tasks.model.ProcessDefinition> list = processDAO.getProcessDefinitionList();
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing processDAO",e);
		}

		return list.stream()
		        .map(this::convertToDto)
		        .collect(Collectors.toList());

	}

	
	

	@Override
	public ProcessDefinitionDTO getProcessDefinition(long processId, long version) {
		
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		
		nrsoft.tasks.model.ProcessDefinition processDefinition = processDAO.getProcessDefinitionById(processId, version);
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing processDAO",e);
		}
		
		return convertToDto(processDefinition);
	}

	
	@Override
	public ProcessDefinitionDTO saveProcessDefinition(ProcessDefinitionDTO processDefinition) {
		TasksDaoJPA dao = new TasksDaoJPA(em);
		
		nrsoft.tasks.model.ProcessDefinition procDef = null;
		
		
		
		if(processDefinition.getProcessId()==0) {
			procDef = new nrsoft.tasks.model.ProcessDefinition();
			procDef.setCreationUser(processDefinition.getCreationUser());
			procDef.setCreationTime(OffsetDateTime.now());
			procDef.setName(processDefinition.getName());

		} else {
			procDef = dao.getProcessDefinitionById(processDefinition.getProcessId(), processDefinition.getVersion());
			procDef.setChangeUser(processDefinition.getChangeUser());
			procDef.setChangeTime(OffsetDateTime.now());

		}
		
		if(procDef!=null) {
			
			procDef.setDescription(processDefinition.getDescription());
			procDef.setStartBeanName(processDefinition.getStartBeanName());
			
			TaskDefinition taskDefinition = dao.getTaskDefinitionById(processDefinition.getTaskDefinitionId());
			
			procDef.setTaskDefinition(taskDefinition);
			
			
			dao.saveProcessDefinition(procDef);
		}

		try {
			dao.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}
		return convertToDto(procDef);
	}


	@Override
	public boolean removeProcessDefinitionById(long processId, long version) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		boolean ok = false;
		
		nrsoft.tasks.model.ProcessDefinition processDefinition = null;
		try {
			processDefinition = processDAO.getProcessDefinitionById(processId, version);
		} catch(javax.persistence.NoResultException e) {
			logger.info("Process Definition not found " + processId, e);
		}
		if(processDefinition!=null) {
			processDAO.removeProcessDefinition(processDefinition);
			ok = true;
		}
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}
		
		return ok;
	}
	
	public boolean generate(long processId, long version, String user) {
		
		boolean generated = true;
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		
		Beans beans = new Beans();
		BeanCreator creator = new BeanCreator();

		ProcessDefinition processDefinition = processDAO.getProcessDefinitionById(processId, version);
		for(Object bean: creator.createBeans(processDefinition.getTaskDefinition()))
			beans.getImportOrAliasOrBean().add(bean);


		try {
			String xml =  XmlSpringConfigurationBuilder.buildXml(beans);
			processDefinition.setGeneratedCode(xml);
			processDefinition.setGenerationUser(user);
			processDefinition.setGenerationTime(OffsetDateTime.now());
			
			processDAO.saveProcessDefinition(processDefinition);
			
		} catch (JAXBException | IOException e) {
			e.printStackTrace();
			generated = false;
		}
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}
		
		
		return generated;
		
	}
	
	
	
	private ProcessDefinitionDTO convertToDto(nrsoft.tasks.model.ProcessDefinition processDefinition) {
		if(processDefinition==null)
			return null;
				
		ProcessDefinitionDTO dto = modelMapper.map(processDefinition, ProcessDefinitionDTO.class);
		dto.setTaskDefinitionDescription(processDefinition.getTaskDefinition().getDescription());
		dto.setTaskDefinitionName(processDefinition.getTaskDefinition().getName());
		dto.setTaskDefinitionId(processDefinition.getTaskDefinition().getTaskId());
	    return dto;
	}

	@Override
	public TaskResult run(long processId, long version, String user) {
		
		
		TaskResult result = null;
		
		TasksDaoJPA processDAO = new TasksDaoJPA(em);

		ProcessDefinition processDef = processDAO.getProcessDefinitionById(processId, version);
		

		/*
		tempFile = File.createTempFile("task", ".xml");
		FileWriter writer = new FileWriter(tempFile);
		writer.write(processDef.getGeneratedCode());
		writer.close();

		String taskName = processDef.getStartBeanName();
		TaskProviderSpringFilesystem taskProvider = new TaskProviderSpringFilesystem(tempFile.getAbsolutePath(), taskName);
		
		Task task = taskProvider.load();
		
		ProcessData processData = new ProcessData();
		
		nrsoft.tasks.runtime.Process process = new Process(task, processData);
		nrsoft.tasks.model.Process processModel = processDAO.createProcess(process.getUUID(),  user, processDef);
		
		ProcessObserverPersistance observer = new ProcessObserverPersistance(processDAO,processModel);
		
		process.addObserver(observer);
		*/
		
		ProcessObserverPersistance processObserver = new ProcessObserverPersistance(processDAO, processDef);
		nrsoft.tasks.runtime.Process process = nrsoft.tasks.runtime.Process.create(user, processDef
				, Arrays.asList(new ProcessObserver[] {  processObserver } ));
		
		LoggersProvider.buildLogger(process.getUUID());
		
		process.setLoggerProvider( new LoggerProvider(process.getUUID()) );
		
		process.run();
		result = process.getResult();

		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}
			
			
		return result;
	}

	@Override
	public ProcessDTO getProcessResult(String processId) {

		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		
		UUID uuid = UUID.fromString(processId);
		nrsoft.tasks.model.Process process = processDAO.getProcess(uuid );
		
		ProcessDTO dto = modelMapper.map(process, ProcessDTO.class);
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}
		
		
		
		return dto;
	}


}

