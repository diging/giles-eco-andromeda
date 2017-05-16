package edu.asu.diging.gilesecosystem.andromeda.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.asu.diging.gilesecosystem.andromeda.util.Properties;
import edu.asu.diging.gilesecosystem.andromeda.web.pages.SystemConfigPage;
import edu.asu.diging.gilesecosystem.andromeda.web.validators.SystemConfigValidator;
import edu.asu.diging.gilesecosystem.util.exceptions.PropertiesStorageException;
import edu.asu.diging.gilesecosystem.util.properties.IPropertiesManager;

@Controller
public class EditPropertiesController {
    
    @Autowired
    private IPropertiesManager propertyManager;
    
    @InitBinder
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder, WebDataBinder validateBinder) {
        validateBinder.addValidators(new SystemConfigValidator());
    }

    @RequestMapping(value = "/admin/system/config", method = RequestMethod.GET)
    public String getConfigPage(Model model) {
        SystemConfigPage page = new SystemConfigPage();
        
        page.setBaseUrl(propertyManager.getProperty(Properties.ANDROMEDA_URL));
        
        model.addAttribute("systemConfigPage", page);
        return "admin/system/config";
    }
    
    @RequestMapping(value = "/admin/system/config", method = RequestMethod.POST)
    public String storeSystemConfig(@Validated @ModelAttribute SystemConfigPage systemConfigPage, BindingResult results, Model model, RedirectAttributes redirectAttrs) {
        model.addAttribute("systemConfigPage", systemConfigPage);
        
        if (results.hasErrors()) {
            model.addAttribute("show_alert", true);
            model.addAttribute("alert_type", "danger");
            model.addAttribute("alert_msg", "System Configuration could not be saved. Please check the error messages below.");
            return "admin/system/config";
        }
        
        Map<String, String> propertiesMap = new HashMap<String, String>();
        propertiesMap.put(Properties.ANDROMEDA_URL, systemConfigPage.getBaseUrl());
        
        try {
            propertyManager.updateProperties(propertiesMap);
        } catch (PropertiesStorageException  e) {
            model.addAttribute("show_alert", true);
            model.addAttribute("alert_type", "danger");
            model.addAttribute("alert_msg", "An unexpected error occurred. System Configuration could not be saved.");
            return "admin/system/config";
        }
        
        redirectAttrs.addFlashAttribute("show_alert", true);
        redirectAttrs.addFlashAttribute("alert_type", "success");
        redirectAttrs.addFlashAttribute("alert_msg", "System Configuration was successfully saved.");
        
        return "redirect:/admin/system/config";
    }
}
