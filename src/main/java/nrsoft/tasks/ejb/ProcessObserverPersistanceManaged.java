package nrsoft.tasks.ejb;

import java.io.Serializable;

import javax.annotation.ManagedBean;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.SessionScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import nrsoft.tasks.ProcessData;
import nrsoft.tasks.TaskResult;
import nrsoft.tasks.model.ProcessDefinition;
import nrsoft.tasks.persistance.TasksDAO;
import nrsoft.tasks.persistance.TasksDaoJPA;
import nrsoft.tasks.runtime.Process;
import nrsoft.tasks.runtime.ProcessObserver;
import nrsoft.tasks.runtime.ProcessObserverPersistanceCommon;

@Dependent
public class ProcessObserverPersistanceManaged 
	extends ProcessObserverPersistanceCommon
	implements Serializable {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8108078069966367207L;

	public ProcessObserverPersistanceManaged() {
		super(null);
	}
	
	public static final boolean USER_TRANSACTION_ACTIVE_TRUE = true;

	public ProcessObserverPersistanceManaged(ProcessDefinition processDefinition, boolean userTransactionActive) {
		super(processDefinition);
		this.userTransactionActive = userTransactionActive; 
	}


	@Override
	protected TasksDAO createTaskDao(Process process) {
		if(tasksDAO==null)
			tasksDAO = new TasksDaoJPA.Builder()
			.setEntityManager(entityManager)
			.build();
		return tasksDAO;	
	}


	
	
	
	
	

}
