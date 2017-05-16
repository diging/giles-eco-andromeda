package edu.asu.diging.gilesecosystem.andromeda.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.asu.diging.gilesecosystem.util.files.IFileStorageManager;

@RestController
public class DownloadFileController {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    public final static String FILENAME_PLACEHOLDER = "{filename:.+}";
    public final static String DOCUMENT_ID_PLACEHOLDER = "{documentId}";
    public final static String REQUEST_ID_PLACEHOLDER = "{requestId}";
    public final static String GET_FILE_URL = "/rest/texts/" + REQUEST_ID_PLACEHOLDER + "/" + DOCUMENT_ID_PLACEHOLDER + "/" + FILENAME_PLACEHOLDER;
   
    @Autowired
    private IFileStorageManager storageManager;

    @RequestMapping(value = GET_FILE_URL)
    public ResponseEntity<String> getFile(
            @PathVariable String filename, @PathVariable String documentId, @PathVariable String requestId,
            HttpServletResponse response,
            HttpServletRequest request) {

        byte[] content = storageManager.getFileContent(requestId, documentId, null, filename);
        
        if (content == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        storageManager.deleteFile(requestId, documentId, null, filename, true);
        
        String contentType = null;
        if (content != null) {
            Tika tika = new Tika();
            contentType = tika.detect(content);
         }

        if (contentType != null) {
            response.setContentType(contentType);
        }
        response.setContentLength(content.length);
        response.setHeader("Content-disposition", "filename=\"" + filename + "\""); 
        try {
            response.getOutputStream().write(content);
            response.getOutputStream().close();
        } catch (IOException e) {
            logger.error("Could not write to output stream.", e);
            return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<String>(HttpStatus.OK);
    }
}
