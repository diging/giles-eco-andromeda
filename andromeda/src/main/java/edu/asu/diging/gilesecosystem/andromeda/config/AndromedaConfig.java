package edu.asu.diging.gilesecosystem.andromeda.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.asu.diging.gilesecosystem.andromeda.util.Properties;
import edu.asu.diging.gilesecosystem.septemberutil.service.ISystemMessageHandler;
import edu.asu.diging.gilesecosystem.septemberutil.service.impl.SystemMessageHandler;
import edu.asu.diging.gilesecosystem.util.properties.IPropertiesManager;

@Configuration
public class AndromedaConfig {
    
    @Autowired
    private IPropertiesManager propertiesManager;

    @Bean
    public ISystemMessageHandler getMessageHandler() {
        return new SystemMessageHandler(propertiesManager.getProperty(Properties.APPLICATION_ID));
    }
}
