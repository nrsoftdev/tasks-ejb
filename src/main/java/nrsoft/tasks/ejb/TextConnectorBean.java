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

import nrsoft.tasks.dto.TextConnectorDTO;
import nrsoft.tasks.persistance.TasksDaoJPA;

@Stateless
public class TextConnectorBean implements TextConnector {
	
	private static Logger logger = LogManager.getLogger(TextConnectorBean.class);
	
	@PersistenceContext(unitName="processDefinition") 
	protected EntityManager em;
	
	// private UserTransaction ut;
	
	private ModelMapper modelMapper = new ModelMapper();

	@Override
	public List<TextConnectorDTO> getTextConnectorList() {
		TasksDaoJPA dao = new TasksDaoJPA(em);
		List<nrsoft.tasks.model.TextConnector> list = dao.getTextConnectorList();
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
	
	private TextConnectorDTO convertToDto(nrsoft.tasks.model.TextConnector textConnector) {
		if(textConnector==null)
			return null;
				
		TextConnectorDTO tcDto = modelMapper.map(textConnector, TextConnectorDTO.class);
	    return tcDto;
	}
	
	private nrsoft.tasks.model.TextConnector convertToEntity(TextConnectorDTO textConnector) {

	    return modelMapper.map(textConnector, nrsoft.tasks.model.TextConnector.class);
	}

	@Override
	public TextConnectorDTO saveTextConnector(TextConnectorDTO textConnector) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		nrsoft.tasks.model.TextConnector textConn = null;
		long connId = textConnector.getConnId();
		
		if(connId>0)
			try {
				textConn = processDAO.getTextConnector(connId);
			} catch(jakarta.persistence.NoResultException e) {
				logger.warn("Text connector not found " + connId, e);
			}
		boolean changed = false;
		boolean insert = false;
		if(textConn!=null) {
			
			if(textConnector.getName()!=null) { textConn.setName(textConnector.getName()); changed = true; }
			if(textConnector.getFilename()!=null) { textConn.setFilename(textConnector.getFilename()); changed = true; }
			if(textConnector.getDescription()!=null) { textConn.setDescription(textConnector.getDescription()); changed = true; }
			if(changed) {
				textConn.setChangeTime(OffsetDateTime.now());
				textConn.setChangeUser(textConnector.getChangeUser());
			}
		} else  {
			
			textConn = new nrsoft.tasks.model.TextConnector();
			textConn.setName(textConnector.getName());
			textConn.setFilename(textConnector.getFilename());
			textConn.setDescription(textConnector.getDescription());
			textConn.setCreationTime(OffsetDateTime.now());
			textConn.setCreationUser(textConnector.getCreationUser());
			insert = true;
		}
		if(changed || insert)
			processDAO.saveTextConnector(textConn);
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}
		
		return convertToDto(textConn);
	}

	@Override
	public TextConnectorDTO getTextConnector(long connId) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		nrsoft.tasks.model.TextConnector textConn = null;
		try {
			textConn = processDAO.getTextConnector(connId);
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
	public boolean removeTextConnectorById(long connId) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		boolean ok = false;
		
		nrsoft.tasks.model.TextConnector textConnector = null;
		try {
			textConnector = processDAO.getTextConnector(connId);
		} catch(jakarta.persistence.NoResultException e) {
			logger.info("Text connector not found " + connId, e);
		}
		if(textConnector!=null) {
			processDAO.removeTextConnector(textConnector);
			ok = true;
		}
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}
		
		return ok;

		
	}

	@Override
	public TextConnectorDTO getTextConnectorByName(String name) {
		TasksDaoJPA processDAO = new TasksDaoJPA(em);
		nrsoft.tasks.model.TextConnector textConn = null;
		try {
			textConn = processDAO.getTextConnectorByName(name);
		} catch(jakarta.persistence.NoResultException e) {
			logger.info("Text connector not found " + name, e);
		}
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}
		
		return convertToDto(textConn);	}

	
	


}
