package nrsoft.tasks.ejb;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nrsoft.tasks.dto.TextConnectorDTO;
import nrsoft.tasks.persistance.TasksDaoJPA;

@Stateless
public class TextConnectorBean implements TextConnector {
	
	private static Logger logger = LoggerFactory.getLogger(TextConnectorBean.class);
	
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
			} catch(javax.persistence.NoResultException e) {
				logger.warn("Text connector not found " + connId, e);
			}
		
		if(textConn!=null) {
		
			textConn.setName(textConnector.getName());
			textConn.setFilename(textConnector.getFilename());
			textConn.setDescription(textConnector.getDescription());
			textConn.setChangeTime(OffsetDateTime.now());
			textConn.setChangeUser(textConnector.getChangeUser());
		} else  {
			
			textConn = new nrsoft.tasks.model.TextConnector();
			textConn.setName(textConnector.getName());
			textConn.setFilename(textConnector.getFilename());
			textConn.setDescription(textConnector.getDescription());
			textConn.setCreationTime(OffsetDateTime.now());
			textConn.setCreationUser(textConnector.getCreationUser());
		}
		
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
		} catch(javax.persistence.NoResultException e) {
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
		} catch(javax.persistence.NoResultException e) {
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
		} catch(javax.persistence.NoResultException e) {
			logger.info("Text connector not found " + name, e);
		}
		
		try {
			processDAO.close();
		} catch (IOException e) {
			logger.warn("Error closing dao", e);
		}
		
		return convertToDto(textConn);	}

	
	


}
