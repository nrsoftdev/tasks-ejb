package nrsoft.tasks.ejb;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.convention.MatchingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nrsoft.tasks.dto.ProcessDefinitionDTO;
import nrsoft.tasks.dto.TaskDefinitionDTO;
import nrsoft.tasks.metadata.Metadata;
import nrsoft.tasks.metadata.MetadataDefinition;
import nrsoft.tasks.model.InitialProperty;
import nrsoft.tasks.model.TaskCollection;
import nrsoft.tasks.model.TaskCollectionMember;
import nrsoft.tasks.persistance.TasksDaoJPA;

@Stateless
public class TaskDefinitionBean implements TaskDefinition {
	
	public TaskDefinitionBean() {
		modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
		TypeMap<nrsoft.tasks.model.TaskDefinition, TaskDefinitionDTO> typeMap = modelMapper.createTypeMap(nrsoft.tasks.model.TaskDefinition.class
				, TaskDefinitionDTO.class);
		
		
		typeMap.addMappings(mapper -> mapper.skip(TaskDefinitionDTO::setChildren));
		//typeMap.addMappings(mapper -> mapper.skip(TaskDefinitionDTO::setProperties));

	}
	
	private static Logger logger = LoggerFactory.getLogger(TaskDefinitionBean.class);
	
	@PersistenceContext(unitName="processDefinition") 
	protected EntityManager em;
	
	// private UserTransaction ut;
	
	private ModelMapper modelMapper = new ModelMapper();

	@Override
	public List<TaskDefinitionDTO> getTaskDefinitionList() {
		return getTaskDefinitionList(0,0);
	}
	
	@Override
	public List<TaskDefinitionDTO> getTaskDefinitionList(int pageNum, int pageSize) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		List<nrsoft.tasks.model.TaskDefinition> list = processDAO.getTaskDefinitionList(pageNum, pageSize);
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
	public List<TaskDefinitionDTO> getTaskChildren(long taskId,int pageNum, int pageSize) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		List<nrsoft.tasks.model.TaskDefinition> list = processDAO.getTaskDefinitionChildrenList(taskId,  pageNum, pageSize);
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
	public List<TaskDefinitionDTO> searchTaskDefinitionList(String name, String description, String className) {
		return this.searchTaskDefinitionList(name, description, className, 0, 0 );
	}
	
	@Override
	public List<TaskDefinitionDTO> searchTaskDefinitionList(String name, String description, String className, int pageNum, int pageSize) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		List<nrsoft.tasks.model.TaskDefinition> list = processDAO.searchTaskDefinitionList(name, description, className, pageNum, pageSize);
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
	public TaskDefinitionDTO saveTaskDefinition(TaskDefinitionDTO taskDefinition) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		nrsoft.tasks.model.TaskDefinition taskDef;
		
		
		
		if(taskDefinition.getTaskId()==0) {
			taskDef = mapDtoToEntity(taskDefinition);
			
		} else {
			
			taskDef = processDAO.getTaskDefinitionById(taskDefinition.getTaskId());
			taskDef.setChangeTime(OffsetDateTime.now());
			taskDef.setChangeUser(taskDefinition.getChangeUser());
			Map<String, String> props = taskDefinition.getProperties();
			
			for(InitialProperty initProp: taskDef.getInitialProperties()) {
				
				
				
				if(props.containsKey(initProp.initialPropertyId.getName())) {
				
					initProp.setValue(props.get(initProp.initialPropertyId.getName()));
					initProp.setChangeUser(taskDefinition.getChangeUser());
					initProp.setChangeTime(OffsetDateTime.now());
				}
			}
			
		}
		
		
		taskDef.setConnectorName(taskDefinition.getConnectorName());
		taskDef.setDescription(taskDefinition.getDescription());
		
		processDAO.saveTaskDefinition(taskDef);
		
		TaskDefinitionDTO newDTO = convertToDto(taskDef);
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing processDAO",e);
		}
		

		return newDTO;
	}

	private nrsoft.tasks.model.TaskDefinition mapDtoToEntity(TaskDefinitionDTO taskDefinition) {
		
		Map<String, String> props = taskDefinition.getProperties();
		nrsoft.tasks.model.TaskDefinition taskDef;
		taskDef = new nrsoft.tasks.model.TaskDefinition();
		taskDef.setCreationTime(OffsetDateTime.now());
		taskDef.setCreationUser(taskDefinition.getCreationUser());
		taskDef.setClassName(taskDefinition.getClassName());
		taskDef.setName(taskDefinition.getName());
		
		for(Entry<String, String> entry : props.entrySet()) {
		
			InitialProperty initProp = new InitialProperty(taskDef, entry.getKey(), entry.getValue());
			taskDef.getInitialProperties().add(initProp);
		}
		return taskDef;
	}

	@Override
	public TaskDefinitionDTO getTaskDefinition(long taskId) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		
		nrsoft.tasks.model.TaskDefinition taskDef = processDAO.getTaskDefinitionById(taskId);
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing processDAO",e);
		}

		return convertToDto(taskDef);

	}
	
	@Override
	public TaskDefinitionDTO addChildrenTaskDefinition(long parentTaskId, TaskDefinitionDTO taskDefinition) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		
		nrsoft.tasks.model.TaskDefinition parentTaskDef = processDAO.getTaskDefinitionById(parentTaskId);
		
		nrsoft.tasks.model.TaskDefinition childTask = mapDtoToEntity(taskDefinition);
		
		processDAO.saveTaskDefinition(childTask);
		
		
		if(parentTaskDef.getTaskCollection()==null) {
			TaskCollection collection = new TaskCollection(parentTaskDef);
			parentTaskDef.setTaskCollection(collection );
			
			List<TaskCollectionMember> members = new LinkedList<>();
			collection.setMembers(members );
		}
		

		int newPosition = 0;
		for(TaskCollectionMember member : parentTaskDef.getTaskCollection().getMembers()) {
			if(member.getPosition()>newPosition)
				newPosition = member.getPosition();
			
		}
		
		
		TaskCollectionMember member = new TaskCollectionMember(childTask, newPosition+1);
		member.setTaskCollection(parentTaskDef.getTaskCollection());
		parentTaskDef.getTaskCollection().getMembers().add(member );
		
		processDAO.saveTaskDefinition(parentTaskDef);
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing processDAO",e);
		}
		
		return convertToDto(parentTaskDef);
	}
	
	@Override
	public boolean detachChildrenTaskDefinition(long parentTaskId, long childTaskId) {
		
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		boolean ok = true;
		
		nrsoft.tasks.model.TaskDefinition parentTask = processDAO.getTaskDefinitionById(parentTaskId);
		
		if(parentTask.getTaskCollection()!=null) {
			 TaskCollection taskCollection = parentTask.getTaskCollection();
			 
			 List<TaskCollectionMember> members = taskCollection.getMembers();
			 if(members!=null) {
				 for(TaskCollectionMember member : members) {
					 if(member.getTaskDefinition().getTaskId() == childTaskId) {
						 processDAO.removeTaskCollectionMember(member);
					 }
					 
				 }
			 }
		}
		
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing processDAO",e);
		}
		
		return ok;
		
	}

	@Override
	public boolean removeTaskDefinitionById(long taskId) {
		// TODO Auto-generated method stub
		return false;
	}
	
	private TaskDefinitionDTO convertToDto(nrsoft.tasks.model.TaskDefinition taskDefinition) {
		if(taskDefinition==null)
			return null;
				
		TaskDefinitionDTO dto = modelMapper.map(taskDefinition, TaskDefinitionDTO.class);
		if(taskDefinition.getTaskCollection()!=null)
			if(taskDefinition.getTaskCollection().getMembers()!=null)
				dto.setChildren(taskDefinition.getTaskCollection().getMembers().size());
		
		for(MetadataDefinition metadataDefinition : Metadata.metadataDefinitions) {
			if(metadataDefinition.getClassName().equals(taskDefinition.getClassName())) {
				dto.setAllowsChildren( metadataDefinition.isAllowsChildren());
			}
			
		}
		
		Map<String, String> properties = new LinkedHashMap<>();
		for(int i=0;i<taskDefinition.getInitialProperties().size();i++)  {
			
			properties.put(taskDefinition.getInitialProperties().get(i).initialPropertyId.getName(),
					taskDefinition.getInitialProperties().get(i).getValue());
		}
		dto.setProperties(properties);
		//dto.setTaskDefinitionName(processDefinition.getTaskDefinition().getName());
		//dto.setTaskDefinitionId(processDefinition.getTaskDefinition().getTaskId());
	    return dto;
	}





	

}
