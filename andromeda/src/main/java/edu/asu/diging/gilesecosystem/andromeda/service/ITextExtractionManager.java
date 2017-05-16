package edu.asu.diging.gilesecosystem.andromeda.service;

import edu.asu.diging.gilesecosystem.andromeda.exception.ExtractionException;
import edu.asu.diging.gilesecosystem.requests.ITextExtractionRequest;

public interface ITextExtractionManager {

    public abstract void extractText(ITextExtractionRequest request) throws ExtractionException;

}