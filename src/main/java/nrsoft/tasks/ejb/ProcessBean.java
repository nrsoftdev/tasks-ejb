package nrsoft.tasks.ejb;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import jakarta.annotation.Resource;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import nrsoft.tasks.dto.ProcessDTO;
import nrsoft.tasks.logger.LoggerProvider;
import nrsoft.tasks.logger.LoggersProvider;
import nrsoft.tasks.model.ProcessDefinition;
import nrsoft.tasks.persistance.TasksDaoJPA;
import nrsoft.tasks.runtime.ProcessObserver;
import nrsoft.tasks.runtime.ProcessObserverOutputManager;
import nrsoft.tasks.runtime.Processes;

@Stateless
public class ProcessBean implements Process  {
	
	private static Logger logger = LogManager.getLogger(ProcessBean.class);
	
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
		
		synchronized(Processes.class) {
			Processes.bootstrap(processDAO);
		}
		
		
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
		
		ProcessObserverOutputManager processOutputObserver = new ProcessObserverOutputManager();
		
				
		nrsoft.tasks.runtime.Process.setup(process
				, user
				, processDef
				, Arrays.asList(new ProcessObserver[] { processObserver, processOutputObserver }));
		
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
