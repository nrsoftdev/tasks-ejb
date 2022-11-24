package nrsoft.tasks.ejb;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.modelmapper.ModelMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nrsoft.tasks.dto.JdbcConnectorDTO;
import nrsoft.tasks.persistance.TasksDaoJPA;

@Stateless
public class JdbcConnectorBean implements JdbcConnector {
	
	private static Logger logger = LogManager.getLogger(JdbcConnectorBean.class);
	
	@PersistenceContext(unitName="processDefinition") 
	protected EntityManager em;
	
	// private UserTransaction ut;
	
	private ModelMapper modelMapper = new ModelMapper();

	@Override
	public List<JdbcConnectorDTO> getJdbcConnectorList() {
		TasksDaoJPA dao = new TasksDaoJPA(em);
		List<nrsoft.tasks.model.JdbcConnector> list = dao.getJdbcConnectorList();
		try {
			dao.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
			e.printStackTrace();
		}
		return list.stream()
		        .map(this::convertToDto)
		        .collect(Collectors.toList());
	}
	
	private JdbcConnectorDTO convertToDto(nrsoft.tasks.model.JdbcConnector jdbcConnector) {
		if(jdbcConnector==null)
			return null;
				
		JdbcConnectorDTO tcDto = modelMapper.map(jdbcConnector, JdbcConnectorDTO.class);
		tcDto.setDbuser(jdbcConnector.getUser());
	    return tcDto;
	}
	
	private nrsoft.tasks.model.JdbcConnector convertToEntity(JdbcConnectorDTO jdbcConnector) {

	    return modelMapper.map(jdbcConnector, nrsoft.tasks.model.JdbcConnector.class);
	}

	@Override
	public JdbcConnectorDTO saveJdbcConnector(JdbcConnectorDTO jdbcConnector) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		nrsoft.tasks.model.JdbcConnector jdbcConn = null;
		long connId = jdbcConnector.getConnId();
		
		if(connId>0)
			try {
				jdbcConn = processDAO.getJdbcConnector(connId);
			} catch(jakarta.persistence.NoResultException e) {
				logger.warn("Text connector not found " + connId, e);
			}
		
		if(jdbcConn==null) {
			jdbcConn = new nrsoft.tasks.model.JdbcConnector();
			jdbcConn.setCreationTime(OffsetDateTime.now());
			jdbcConn.setCreationUser(jdbcConnector.getChangeUser());
			
		} else {
			jdbcConn.setChangeTime(OffsetDateTime.now());
			jdbcConn.setChangeUser(jdbcConnector.getChangeUser());
		}
		
		jdbcConn.setName(jdbcConnector.getName());
		
		jdbcConn.setDescription(jdbcConnector.getDescription());
		
		jdbcConn.setDriver(jdbcConnector.getDriver());
		jdbcConn.setUrl(jdbcConnector.getUrl());
		jdbcConn.setUser(jdbcConnector.getDbuser());
		jdbcConn.setPassword(jdbcConnector.getPassword());
		
		
		processDAO.saveJdbcConnector(jdbcConn);
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}
		
		return convertToDto(jdbcConn);
	}

	@Override
	public JdbcConnectorDTO getJdbcConnector(long connId) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		nrsoft.tasks.model.JdbcConnector textConn = null;
		try {
			textConn = processDAO.getJdbcConnector(connId);
		} catch(jakarta.persistence.NoResultException e) {
			logger.info("Text connector not found " + connId, e);
		}
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}
		
		return convertToDto(textConn);
	}

	@Override
	public boolean removeJdbcConnectorById(long connId) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		boolean ok = false;
		
		nrsoft.tasks.model.JdbcConnector JdbcConnector = null;
		try {
			JdbcConnector = processDAO.getJdbcConnector(connId);
		} catch(jakarta.persistence.NoResultException e) {
			logger.info("Text connector not found " + connId, e);
		}
		if(JdbcConnector!=null) {
			processDAO.removeJdbcConnector(JdbcConnector);
			ok = true;
		}
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}
		
		return ok;

		
	}

	
	


}
