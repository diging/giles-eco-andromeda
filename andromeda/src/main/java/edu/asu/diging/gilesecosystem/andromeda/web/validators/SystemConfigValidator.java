package edu.asu.diging.gilesecosystem.andromeda.web.validators;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import edu.asu.diging.gilesecosystem.andromeda.web.pages.SystemConfigPage;

public class SystemConfigValidator implements Validator {

    @Override
    public boolean supports(Class<?> arg0) {
        return arg0 == SystemConfigPage.class;
    }

    @Override
    public void validate(Object arg0, Errors arg1) {
        ValidationUtils.rejectIfEmptyOrWhitespace(arg1, "baseUrl", "base_url_missing");
    }

}
