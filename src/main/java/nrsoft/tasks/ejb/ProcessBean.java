package nrsoft.tasks.ejb;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nrsoft.tasks.dto.ProcessDTO;
import nrsoft.tasks.logger.LoggerProvider;
import nrsoft.tasks.logger.LoggersProvider;
import nrsoft.tasks.model.ProcessDefinition;
import nrsoft.tasks.persistance.TasksDaoJPA;
import nrsoft.tasks.runtime.ProcessObserver;

@Stateless
public class ProcessBean implements Process  {
	
	private static Logger logger = LoggerFactory.getLogger(ProcessBean.class);
	
	private @Inject Instance<ProcessObserverPersistanceManaged> processObserverPersistanceManagedInstance;
	
	@Resource(name = "DefaultManagedExecutorService")
	private ManagedExecutorService executor;
	
	@PersistenceContext(unitName="processDefinition") 
	private EntityManager entityManager;
	
	private ModelMapper modelMapper = new ModelMapper();
	
	
	public ProcessBean() {
		modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
	}
	
	@Override
	public String run(long processDefinitionId, long version, String user) {
		
		TasksDaoJPA processDAO = new TasksDaoJPA.Builder().setEntityManager(entityManager).build(); 

		ProcessDefinition processDef = processDAO.getProcessDefinitionById(processDefinitionId, version);
		
		
		/*
		ProcessObserver processObserver = new ProcessObserverPersistanceManaged(processDef
				, ProcessObserverPersistanceManaged.USER_TRANSACTION_ACTIVE_TRUE);
		
		*/
		
		ProcessObserverPersistanceManaged processObserver = processObserverPersistanceManagedInstance.get();
		processObserver.setProcessDefinition(processDef);
		processObserver.setUserTransactionActive(ProcessObserverPersistanceManaged.USER_TRANSACTION_ACTIVE_TRUE);

		nrsoft.tasks.runtime.Process process = new nrsoft.tasks.runtime.Process();
		
		nrsoft.tasks.model.Process processMdl = processDAO.createProcess(process.getUUID(), user, processDef);
		processObserver.setProcessModel(processMdl);
				
		nrsoft.tasks.runtime.Process.setup(process
				, user
				, processDef
				, Arrays.asList(new ProcessObserver[] { processObserver }));
		
		LoggersProvider.buildLogger(process.getUUID());
		
		process.setLoggerProvider(new LoggerProvider(process.getUUID()));
		
		executor.execute(process);

		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}			
			
		return process.getUUID().toString();
	}

	@Override
	public ProcessDTO getProcessResult(String processId) {

		TasksDaoJPA processDAO = new TasksDaoJPA(entityManager);
		
		UUID uuid = UUID.fromString(processId);
		nrsoft.tasks.model.Process process = processDAO.getProcess(uuid);
		
		ProcessDTO dto = modelMapper.map(process, ProcessDTO.class);
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}
		
		
		
		return dto;
	}
	
	
	

}
