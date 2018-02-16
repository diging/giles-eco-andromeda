package edu.asu.diging.gilesecosystem.andromeda.service.impl;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.asu.diging.gilesecosystem.andromeda.exception.ExtractionException;
import edu.asu.diging.gilesecosystem.andromeda.rest.DownloadFileController;
import edu.asu.diging.gilesecosystem.andromeda.service.ITextExtractionManager;
import edu.asu.diging.gilesecosystem.andromeda.util.Properties;
import edu.asu.diging.gilesecosystem.requests.ICompletedTextExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.IRequestFactory;
import edu.asu.diging.gilesecosystem.requests.ITextExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.PageStatus;
import edu.asu.diging.gilesecosystem.requests.RequestStatus;
import edu.asu.diging.gilesecosystem.requests.exceptions.MessageCreationException;
import edu.asu.diging.gilesecosystem.requests.impl.CompletedTextExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.kafka.IRequestProducer;
import edu.asu.diging.gilesecosystem.septemberutil.properties.MessageType;
import edu.asu.diging.gilesecosystem.septemberutil.service.ISystemMessageHandler;

@Service
public class TextExtractionManager extends AExtractionManager
        implements ITextExtractionManager {

    @Autowired
    private IRequestFactory<ICompletedTextExtractionRequest, CompletedTextExtractionRequest> requestFactory;

    @Autowired
    private IRequestProducer requestProducer;

    @Autowired
    private ISystemMessageHandler messageHandler;

    @PostConstruct
    public void init() {
        requestFactory.config(CompletedTextExtractionRequest.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.asu.diging.cepheus.service.pdf.impl.ITextExtractionManager#
     * extractText
     * (edu.asu.diging.gilesecosystem.requests.ITextExtractionRequest)
     */
    @Override
    public void extractText(ITextExtractionRequest request) throws ExtractionException {
        logger.info("Extracting text for: " + request.getDownloadUrl());
        PDDocument pdfDocument = null;
        RequestStatus status = RequestStatus.COMPLETE;
        String errorMsg = "";
        try {
            pdfDocument = PDDocument.load(
                    new ByteArrayInputStream(downloadFile(request.getDownloadUrl())),
                    MemoryUsageSetting.setupTempFileOnly());
        } catch (IOException e) {
            messageHandler.handleMessage("Could not load PDF.", e, MessageType.ERROR);
            status = RequestStatus.FAILED;
            errorMsg = e.getMessage();
        }

        String restEndpoint = getRestEndpoint();

        String fileName = request.getFilename() + ".txt";
        List<edu.asu.diging.gilesecosystem.requests.impl.Page> pages = new ArrayList<>();
        Text extractedText = null;

        if (pdfDocument != null) {
            try {
                extractedText = extractText(pdfDocument, request.getRequestId(),
                        request.getDocumentId(), fileName);
            } catch (IOException e1) {
                messageHandler.handleMessage("Could not extract text.", e1,
                        MessageType.ERROR);
                status = RequestStatus.FAILED;
                errorMsg = e1.getMessage();
            }

            int numPages = pdfDocument.getNumberOfPages();
            for (int i = 0; i < numPages; i++) {
                // if there is embedded text, let's use that one before
                // OCRing
                if (extractedText != null) {
                    Page page = null;
                    PageStatus pageStatus = PageStatus.COMPLETE;
                    String pageErrorMsg = "";
                    try {
                        page = extractPageText(pdfDocument, i, request.getRequestId(),
                                request.getDocumentId(), request.getFilename());
                    } catch (IOException e) {
                        messageHandler.handleMessage(
                                "Could not extract text from page " + i + ".", e,
                                MessageType.ERROR);
                        pageStatus = PageStatus.FAILED;
                        pageErrorMsg = e.getMessage();
                    }

                    edu.asu.diging.gilesecosystem.requests.impl.Page requestPage = buildRequestPage(
                            request, restEndpoint, i, page, pageStatus, pageErrorMsg);
                    pages.add(requestPage);

                }
            }

            try {
                pdfDocument.close();
            } catch (IOException e) {
                messageHandler.handleMessage("Error closing document " + fileName + ".",
                        e, MessageType.ERROR);
            }
        }

        ICompletedTextExtractionRequest completedRequest = null;
        try {
            completedRequest = requestFactory.createRequest(request.getRequestId(),
                    request.getUploadId());
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Could not create request.", e);
            // this should never happen if used correctly
        }

        // generate download url
        String fileEndpoint = restEndpoint + DownloadFileController.GET_FILE_URL
                .replace(DownloadFileController.REQUEST_ID_PLACEHOLDER,
                        request.getRequestId())
                .replace(DownloadFileController.DOCUMENT_ID_PLACEHOLDER,
                        request.getDocumentId())
                .replace(DownloadFileController.FILENAME_PLACEHOLDER, fileName);

        completedRequest.setDocumentId(request.getDocumentId());
        completedRequest.setFilename(request.getFilename());
        completedRequest.setStatus(status);
        completedRequest.setErrorMsg(errorMsg);
        completedRequest
                .setExtractionDate(OffsetDateTime.now(ZoneId.of("UTC")).toString());
        completedRequest.setPages(pages);

        if (extractedText != null) {
            completedRequest.setDownloadPath(extractedText.path);
            completedRequest.setSize(extractedText.size);
            completedRequest.setDownloadUrl(fileEndpoint);
            completedRequest.setTextFilename(fileName);
        }

        try {
            requestProducer.sendRequest(completedRequest, propertiesManager
                    .getProperty(Properties.KAFKA_EXTRACTION_COMPLETE_TOPIC));
        } catch (MessageCreationException e) {
            logger.error("Could not send message.", e);
        }
    }

    public edu.asu.diging.gilesecosystem.requests.impl.Page buildRequestPage(
            ITextExtractionRequest request, String restEndpoint, int i, Page page,
            PageStatus status, String errorMsg) {
        edu.asu.diging.gilesecosystem.requests.impl.Page requestPage = new edu.asu.diging.gilesecosystem.requests.impl.Page();
        if (page != null) {
            requestPage.setDownloadUrl(restEndpoint + DownloadFileController.GET_FILE_URL
                    .replace(DownloadFileController.REQUEST_ID_PLACEHOLDER,
                            request.getRequestId())
                    .replace(DownloadFileController.DOCUMENT_ID_PLACEHOLDER,
                            request.getDocumentId())
                    .replace(DownloadFileController.FILENAME_PLACEHOLDER, page.filename));
            requestPage.setPathToFile(page.path);
            requestPage.setFilename(page.filename);
        }
        requestPage.setPageNr(i);
        requestPage.setStatus(status);
        requestPage.setErrorMsg(errorMsg);
        return requestPage;
    }

    private Text extractText(PDDocument pdfDocument, String requestId, String documentId,
            String filename) throws IOException {
        String docFolder = fileStorageManager.getAndCreateStoragePath(requestId,
                documentId, null);
        String filePath = docFolder + File.separator + filename;
        File fileObject = new File(filePath);
        fileObject.createNewFile();

        FileWriter writer = new FileWriter(fileObject);
        BufferedWriter bfWriter = new BufferedWriter(writer);
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.writeText(pdfDocument, bfWriter);
        bfWriter.close();
        writer.close();

        String contents = null;
        contents = FileUtils.readFileToString(fileObject, Charset.forName("UTF-8"));

        if (contents == null || contents.trim().isEmpty()) {
            fileStorageManager.deleteFile(requestId, documentId, null, filename, true);
            return null;
        }

        Text text = new Text();
        text.size = fileObject.length();

        String relativePath = fileStorageManager.getFileFolderPathInBaseFolder(requestId,
                documentId, null);
        text.path = relativePath + File.separator + filename;

        return text;
    }

    private Page extractPageText(PDDocument pdfDocument, int pageNr, String requestId,
            String documentId, String filename) throws IOException {
         String pageText = null;
        
         PDFTextStripper stripper;
         stripper = new PDFTextStripper();
         // pdfbox starts counting at 1 for the text extraction
         stripper.setStartPage(pageNr + 1);
         stripper.setEndPage(pageNr + 1);
        
         pageText = stripper.getText(pdfDocument);
        
         if (pageText == null) {
         return null;
         }
        
         return saveTextToFile(pageNr, requestId, documentId, pageText,
         filename, ".txt");
    }

    class Text {
        public String path;
        public long size;
    }
}
