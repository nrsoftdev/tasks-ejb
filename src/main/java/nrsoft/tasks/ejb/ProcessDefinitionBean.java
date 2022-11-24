package nrsoft.tasks.ejb;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;


import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.convention.MatchingStrategies;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.schema.beans.Beans;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import nrsoft.tasks.dto.ProcessDefinitionDTO;
import nrsoft.tasks.dto.ProcessDefinitionVariableDTO;
import nrsoft.tasks.metadata.ProcessVariableType;
import nrsoft.tasks.model.ProcessDefinition;
import nrsoft.tasks.model.ProcessDefinitionVariable;
import nrsoft.tasks.model.TaskDefinition;
import nrsoft.tasks.persistance.TasksDaoJPA;
import nrsoft.tasks.spring.BeanCreator;
import nrsoft.tasks.spring.XmlSpringConfigurationBuilder;


@Stateless
public class ProcessDefinitionBean implements nrsoft.tasks.ejb.ProcessDefinition {
	
	private static Logger logger = LogManager.getLogger(ProcessDefinitionBean.class);
	
	@PersistenceContext(unitName="processDefinition") 
	protected EntityManager entityManager;
	
	@Resource(name = "DefaultManagedExecutorService")
	ManagedExecutorService executor;
	
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
		TypeMap<nrsoft.tasks.model.ProcessDefinition, ProcessDefinitionDTO> typeMapProcessDefinition = modelMapper.createTypeMap(nrsoft.tasks.model.ProcessDefinition.class, ProcessDefinitionDTO.class);
		
		
		typeMapProcessDefinition.addMappings(mapper -> mapper.skip(ProcessDefinitionDTO::setTaskDefinitionDescription));
		typeMapProcessDefinition.addMappings(mapper -> mapper.skip(ProcessDefinitionDTO::setTaskDefinitionId));
		typeMapProcessDefinition.addMappings(mapper -> mapper.skip(ProcessDefinitionDTO::setTaskDefinitionName));
		typeMapProcessDefinition.addMappings(mapper -> mapper.skip(ProcessDefinitionDTO::setVariables));
		

		

	}
	
	@Override
	public List<ProcessDefinitionDTO> getProcessDefinitionList() {
		
	
		TasksDaoJPA processDAO = new TasksDaoJPA(entityManager);
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
		
		TasksDaoJPA processDAO = new TasksDaoJPA(entityManager);
		
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
		TasksDaoJPA dao = new TasksDaoJPA(entityManager);
		
		nrsoft.tasks.model.ProcessDefinition procDef = null;
		
		boolean newBean = false;
		
		
		
		if(processDefinition.getProcessId()==0) {
			procDef = new nrsoft.tasks.model.ProcessDefinition();
			procDef.setCreationUser(processDefinition.getCreationUser());
			procDef.setCreationTime(OffsetDateTime.now());
			procDef.setName(processDefinition.getName());
			newBean = true;
			/*
			for(ProcessDefinitionVariableDTO variable : processDefinition.getVariables()) {
				
				ProcessDefinitionVariable pdv = new ProcessDefinitionVariable(
						variable.getName(), variable.getType(), variable.getValue());
				pdv.setProcessDefinition(procDef);
				procDef.getVariables().add(pdv);
				
			}
			*/
		} else {
			procDef = dao.getProcessDefinitionById(processDefinition.getProcessId(), processDefinition.getVersion());
			procDef.setChangeUser(processDefinition.getChangeUser());
			procDef.setChangeTime(OffsetDateTime.now());
			
			/*
			List<ProcessDefinitionVariableDTO> newVars = new LinkedList<>();
			for(ProcessDefinitionVariableDTO varDto : processDefinition.getVariables()) {
				boolean found = false;
				for(ProcessDefinitionVariable variable : procDef.getVariables()) {
					
					if(varDto.getName().equals(variable.getName())) {
						found = true;
					}
				}
				if(!found) {
					newVars.add(varDto);
				}
			}
			
			List<ProcessDefinitionVariable> obsoleteVars = new LinkedList<>();
			for(ProcessDefinitionVariable variable : procDef.getVariables()) {
			
				boolean found = false;
				
				for(ProcessDefinitionVariableDTO varDto : processDefinition.getVariables()) {		
					if(varDto.getName().equals(variable.getName())) {
						found = true;
					}
				}
				if(!found) {
					obsoleteVars.add(variable);
				}
			}
			
			for(ProcessDefinitionVariable obsoleteVar : obsoleteVars) {
				procDef.getVariables().remove(obsoleteVar);
			}
			
			
			
			for(ProcessDefinitionVariableDTO varDto : processDefinition.getVariables()) {
				for(ProcessDefinitionVariable variable : procDef.getVariables()) {
					
					if(varDto.getName().equals(variable.getName())) {
						variable.setValue(varDto.getValue());
					}
				}
			}
			
			procDef.getVariables().clear();
			for(ProcessDefinitionVariableDTO newVar : processDefinition.getVariables()) {
				ProcessDefinitionVariable var = new ProcessDefinitionVariable(newVar.getName(), newVar.getType(), newVar.getValue());
				var.setProcessDefinition(procDef);
				procDef.getVariables().add(var);
				
			}
			*/

			
			
		}
		
		if(procDef!=null) {
			
			procDef.setDescription(processDefinition.getDescription());
			
			if(newBean) {
				TaskDefinition taskDefinition = dao.getTaskDefinitionById(processDefinition.getTaskDefinitionId());
				
				procDef.setTaskDefinition(taskDefinition);
				procDef.setStartBeanName(taskDefinition.getName());

				dao.saveProcessDefinition(procDef);
			}
			else 
				dao.changeProcessDefinition(procDef);
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
		TasksDaoJPA processDAO = new TasksDaoJPA(entityManager);
		boolean ok = false;
		
		nrsoft.tasks.model.ProcessDefinition processDefinition = null;
		try {
			processDefinition = processDAO.getProcessDefinitionById(processId, version);
		} catch(jakarta.persistence.NoResultException e) {
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
		TasksDaoJPA processDAO = new TasksDaoJPA(entityManager);
		
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
			
		} catch (IOException | jakarta.xml.bind.JAXBException e) {
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
	
	@Override
	public boolean addVariable(long processId, long version, String name, ProcessVariableType type, String value) {
	
		TasksDaoJPA processDAO = new TasksDaoJPA(entityManager);
		
		ProcessDefinition pd = processDAO.getProcessDefinitionById(processId, version);
		
		
		ProcessDefinitionVariable variable = new ProcessDefinitionVariable(name, type.getJavaType(), value);
		
		variable.setProcessDefinition(pd);
		
		processDAO.createProcessDefinitionVariable(variable);
		
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}
		
		
		
		return true;
	}

	public boolean removeVariable(long processId, long version, String name) {
		
		TasksDaoJPA processDAO = new TasksDaoJPA(entityManager);
		
		boolean deleted = processDAO.removeProcessDefinitionVariable(processId, version, name);
		
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}

		return deleted;
	}
	
	@Override
	public boolean changeVariables(long processId, long version, List<ProcessDefinitionVariableDTO> variables) {
		
		TasksDaoJPA processDAO = new TasksDaoJPA(entityManager);
		
		ProcessDefinition pd = processDAO.getProcessDefinitionById(processId, version);
		
		Map<String,ProcessDefinitionVariable> actualVariables = new TreeMap<>();
		Map<String,ProcessDefinitionVariableDTO> newVariables = new TreeMap<>();
		
		for(ProcessDefinitionVariable var1 : pd.getVariables()) {
			actualVariables.put(var1.getName(), var1);
		}
		
		for(ProcessDefinitionVariableDTO var2 : variables) {
			newVariables.put(var2.getName(), var2);
		}
		
		LinkedList<String> actualVariablesNames = new LinkedList<String>(actualVariables.keySet());
		LinkedList<String> variablesNamesToRemove = new LinkedList<String>(actualVariables.keySet());
		LinkedList<String> newVariablesNames = new LinkedList<String>(newVariables.keySet());
		LinkedList<String> variablesNamesToAdd = new LinkedList<String>(newVariables.keySet());
		
		variablesNamesToRemove.removeAll(newVariablesNames);
		variablesNamesToAdd.removeAll(actualVariablesNames);
		
		Iterator<ProcessDefinitionVariable> it = pd.getVariables().iterator();
		for(String varName : variablesNamesToRemove) {
			
			while(it.hasNext()) {
				ProcessDefinitionVariable currentVar = it.next();
				if(currentVar.getName().equals(varName)) {
					it.remove();
					processDAO.removeProcessDefinitionVariable(currentVar);
					break;
				}
			}
		}
		for(String varName : variablesNamesToAdd) {
			
			ProcessDefinitionVariableDTO newVarDTO = newVariables.get(varName);
			ProcessDefinitionVariable newvar = new ProcessDefinitionVariable(
					varName
					, ProcessVariableType.valueOf(newVarDTO.getType()).getJavaType()
					, newVarDTO.getValue());
			newvar.setProcessDefinition(pd);
			pd.getVariables().add(newvar);
			entityManager.persist(newvar);
		}
		
		if(variablesNamesToRemove.size()>0 && variablesNamesToAdd.size()>0) {
			entityManager.merge(pd);
		}
		
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}
		
		return true;

	}
	
	
	
	
	private ProcessDefinitionDTO convertToDto(nrsoft.tasks.model.ProcessDefinition processDefinition) {
		if(processDefinition==null)
			return null;
				
		ProcessDefinitionDTO dto = modelMapper.map(processDefinition, ProcessDefinitionDTO.class);
		dto.setTaskDefinitionDescription(processDefinition.getTaskDefinition().getDescription());
		dto.setTaskDefinitionName(processDefinition.getTaskDefinition().getName());
		dto.setTaskDefinitionId(processDefinition.getTaskDefinition().getTaskId());
		

		if(processDefinition.getVariables()!=null) {
			for(ProcessDefinitionVariable variable : processDefinition.getVariables()) {
				dto.getVariables().add(new ProcessDefinitionVariableDTO(variable.getName(), variable.getValue()
						, ProcessVariableType.getType(variable.getType())));
			}
		}

		
	    return dto;
	}

	


}

