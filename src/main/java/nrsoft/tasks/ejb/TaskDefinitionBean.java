package nrsoft.tasks.ejb;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.convention.MatchingStrategies;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nrsoft.tasks.dto.ProcessDefinitionDTO;
import nrsoft.tasks.dto.TaskDefinitionDTO;
import nrsoft.tasks.metadata.Metadata;
import nrsoft.tasks.metadata.MetadataDefinition;
import nrsoft.tasks.model.InitialProperty;
import nrsoft.tasks.model.TaskCollection;
import nrsoft.tasks.model.TaskCollectionMember;
import nrsoft.tasks.model.TaskDefinition;
import nrsoft.tasks.persistance.TasksDaoJPA;

@Stateless
public class TaskDefinitionBean implements nrsoft.tasks.ejb.TaskDefinition {
	
	public TaskDefinitionBean() {
		modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
		TypeMap<nrsoft.tasks.model.TaskDefinition, TaskDefinitionDTO> typeMap = modelMapper.createTypeMap(nrsoft.tasks.model.TaskDefinition.class
				, TaskDefinitionDTO.class);
		
		
		typeMap.addMappings(mapper -> mapper.skip(TaskDefinitionDTO::setChildren));
		//typeMap.addMappings(mapper -> mapper.skip(TaskDefinitionDTO::setProperties));

	}
	
	private static Logger logger = LogManager.getLogger(TaskDefinitionBean.class);
	
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
	public TaskDefinitionDTO createTaskDefinition(TaskDefinitionDTO taskDefinitionDTO) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		nrsoft.tasks.model.TaskDefinition taskDef = mapDtoToEntity(taskDefinitionDTO);
		
		taskDef.setCreationTime(OffsetDateTime.now());
		taskDef.setCreationUser(taskDefinitionDTO.getCreationUser());
		
		
		processDAO.saveTaskDefinition(taskDef);
		
		TaskDefinitionDTO newDTO = convertToDto(taskDef);
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing processDAO",e);
		}
		

		return newDTO;
	}
	
	
	@Override
	public TaskDefinitionDTO updateTaskDefinition(TaskDefinitionDTO taskDefinitionDTO) {
		
		
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		
		
		nrsoft.tasks.model.TaskDefinition taskDef = processDAO.getTaskDefinitionById(taskDefinitionDTO.getTaskId());
		
		taskDef.setChangeTime(OffsetDateTime.now());
		taskDef.setChangeUser(taskDefinitionDTO.getChangeUser());
		
		taskDef.setName(taskDefinitionDTO.getName());
		taskDef.setDescription(taskDefinitionDTO.getDescription());
		
		processDAO.saveTaskDefinition(taskDef);
		
		TaskDefinitionDTO newDTO = convertToDto(taskDef);		

		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing processDAO",e);
		}		

		return newDTO;
	}

	@Override
	public TaskDefinitionDTO updateTaskDefinitionProperties(TaskDefinitionDTO taskDefinitionDTO) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		nrsoft.tasks.model.TaskDefinition taskDef = processDAO.getTaskDefinitionById(taskDefinitionDTO.getTaskId());
		
		Map<String, String> newInitProp = taskDefinitionDTO.getProperties();
		
		
		// new value for existing properties
		
		for(InitialProperty initProp : taskDef.getInitialProperties()) {
			
			String newValue = newInitProp.get(initProp.initialPropertyId.getName());
			String oldValue = initProp.getValue();
			if(newValue!=null && !oldValue.equals(newValue)) {
				
				initProp.setValue(newValue);
				initProp.setChangeUser(taskDefinitionDTO.getChangeUser());
				initProp.setChangeTime(OffsetDateTime.now());
			}
		}
		
		
		List<InitialProperty> toRemove = new LinkedList<>();
		// remove properties
		for(InitialProperty initProp : taskDef.getInitialProperties()) {
			if(!taskDefinitionDTO.getProperties().containsKey(initProp.initialPropertyId.getName())) {
				toRemove.add(initProp);
				processDAO.removeInitialProperty(initProp);
			}
			
		}
		taskDef.getInitialProperties().removeAll(toRemove);		
		
		List<InitialProperty> toAdd = new LinkedList<>();
		for(Entry<String, String> property : taskDefinitionDTO.getProperties().entrySet()) {
			boolean found = false;
			for(InitialProperty initProp : taskDef.getInitialProperties()) {
				if(property.getKey().equals(initProp.initialPropertyId.getName())) {
					found = true;
				}
			}
			if(!found) {
				toAdd.add(new InitialProperty(taskDef, property.getKey(), property.getValue()));
			}
		}
		
		

		taskDef.getInitialProperties().addAll(toAdd);
		
		
		
		
		processDAO.saveTaskDefinition(taskDef);
		
		TaskDefinitionDTO newDTO = convertToDto(taskDef);		
		
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing processDAO",e);
		}		

		return newDTO;
	}	

	private nrsoft.tasks.model.TaskDefinition mapDtoToEntity(TaskDefinitionDTO taskDefDto) {
		
		Map<String, String> propsDto = taskDefDto.getProperties();
		nrsoft.tasks.model.TaskDefinition taskDef = new nrsoft.tasks.model.TaskDefinition();
		taskDef.setTaskId(taskDefDto.getTaskId());
		taskDef.setClassName(taskDefDto.getClassName());
		taskDef.setName(taskDefDto.getName());
		taskDef.setConnectorName(taskDefDto.getConnectorName());
		
		for(Entry<String, String> propDto : propsDto.entrySet()) {
		
			InitialProperty initProp = new InitialProperty(taskDef, propDto.getKey(), propDto.getValue());
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
				 TaskCollectionMember memberToRemove = null;
				 for(TaskCollectionMember member : members) {
					 if(member.getTaskDefinition().getTaskId() == childTaskId) {
						 processDAO.removeTaskCollectionMember(member);
						 memberToRemove = member;
					 }
					 
				 }
				 if(memberToRemove!=null)
					 members.remove(memberToRemove);
			 }
		}
		
		processDAO.saveTaskDefinition(parentTask);
		
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
	
	@Override
	public boolean attachChildrenTaskDefinition(long parentTaskId, long childTaskId) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);	
		boolean ok = true;
		
		nrsoft.tasks.model.TaskDefinition parentTask = processDAO.getTaskDefinitionById(parentTaskId);
		nrsoft.tasks.model.TaskDefinition childTask = processDAO.getTaskDefinitionById(childTaskId);
		
		if(parentTask.getTaskCollection()==null) {
			parentTask.setTaskCollection( new TaskCollection(parentTask));
		}
		 TaskCollection taskCollection = parentTask.getTaskCollection();
		 
		 List<TaskCollectionMember> members = taskCollection.getMembers();
		 if(members==null) {
			 members = new LinkedList<TaskCollectionMember>();
		 }
		 int position = 0;
		 for(TaskCollectionMember member : members) {
			 if(member.getPosition()>position)
				 position = member.getPosition(); 
		 }
		 TaskCollectionMember newMember = new TaskCollectionMember(childTask, position+1);
		 newMember.setTaskCollection(taskCollection);
		members.add(newMember);

		processDAO.saveTaskDefinition(parentTask);
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing processDAO",e);
		}
		
		return ok;
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
