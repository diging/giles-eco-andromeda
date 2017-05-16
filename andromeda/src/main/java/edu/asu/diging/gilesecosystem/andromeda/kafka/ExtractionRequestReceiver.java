package edu.asu.diging.gilesecosystem.andromeda.kafka;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.asu.diging.gilesecosystem.andromeda.exception.ExtractionException;
import edu.asu.diging.gilesecosystem.andromeda.service.ITextExtractionManager;
import edu.asu.diging.gilesecosystem.requests.ITextExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.impl.TextExtractionRequest;
import edu.asu.diging.gilesecosystem.util.properties.IPropertiesManager;

@PropertySource("classpath:/config.properties")
public class ExtractionRequestReceiver {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Autowired
    private ITextExtractionManager textExtractionManager;
    
    @Autowired
    protected IPropertiesManager propertiesManager;
    
    
    @KafkaListener(id="andromeda.extraction", topics = {"${topic_extract_text_request}"})
    public void receiveMessage(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        ObjectMapper mapper = new ObjectMapper();
        ITextExtractionRequest request = null;
        try {
            request = mapper.readValue(message, TextExtractionRequest.class);
        } catch (IOException e) {
            logger.error("Could not unmarshall request.", e);
            // FIXME: handle this case
            return;
        }
        
        try {
            textExtractionManager.extractText(request);
        } catch (ExtractionException e) {
           logger.error("Could not extract text.");
           // FIXME: send to monitoring app
        }
    }
}
