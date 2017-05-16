package edu.asu.diging.gilesecosystem.andromeda.service.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import edu.asu.diging.gilesecosystem.andromeda.util.Properties;
import edu.asu.diging.gilesecosystem.util.files.IFileStorageManager;
import edu.asu.diging.gilesecosystem.util.properties.IPropertiesManager;

public class AExtractionManager {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected IFileStorageManager fileStorageManager;
    
    
    @Autowired
    protected IPropertiesManager propertiesManager;

    public byte[] downloadFile(String url) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
        HttpEntity<String> entity = new HttpEntity<String>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET,
                entity, byte[].class);
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            return response.getBody();
        }
        return null;
    }
    
    protected Page saveTextToFile(int pageNr, String requestId,
            String documentId, String pageText, String filename, String fileExtentions) {
        String docFolder = fileStorageManager.getAndCreateStoragePath(requestId,
                documentId, null);

        if (pageNr > -1) {
            filename = filename + "." + pageNr;
        }

        if (!fileExtentions.startsWith(".")) {
            fileExtentions = "." + fileExtentions;
        }
        filename = filename + fileExtentions;

        String filePath = docFolder + File.separator + filename;
        File fileObject = new File(filePath);
        try {
            fileObject.createNewFile();
        } catch (IOException e) {
            logger.error("Could not create file.", e);
            return null;
        }

        try {
            FileWriter writer = new FileWriter(fileObject);
            BufferedWriter bfWriter = new BufferedWriter(writer);
            bfWriter.write(pageText);
            bfWriter.close();
            writer.close();
        } catch (IOException e) {
            logger.error("Could not write text to file.", e);
            return null;
        }

        String relativePath = fileStorageManager.getFileFolderPathInBaseFolder(requestId, documentId, null);
        Page page = new Page(relativePath + File.separator + filename, filename);
        page.size = fileObject.length();
        return page;
    }
    
    protected String getRestEndpoint() {
        String restEndpoint = propertiesManager.getProperty(Properties.ANDROMEDA_URL);
        if (restEndpoint.endsWith("/")) {
            restEndpoint = restEndpoint.substring(0, restEndpoint.length()-1);
        }
        return restEndpoint;
    }
    
    class Page {
        public String path;
        public String filename;
        public String contentType;
        public long size;
        
        public Page(String path, String filename) {
            super();
            this.path = path;
            this.filename = filename;
        }  
    }
}
