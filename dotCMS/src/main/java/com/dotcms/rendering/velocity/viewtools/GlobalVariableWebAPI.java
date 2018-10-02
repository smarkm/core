package com.dotcms.rendering.velocity.viewtools;
import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;

public class GlobalVariableWebAPI implements ViewTool {
	
    private HttpServletRequest request;
	private LanguageAPI langAPI = APILocator.getLanguageAPI();

    public void init(Object obj) {
        ViewContext context = (ViewContext) obj;
        this.request = context.getRequest();

    }
    
    private long getCurrentLanguageId () {
        long language;
        String languageSt = (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
        language = Long.parseLong(languageSt);
        return language;
    }
    
    public String get(String property) {
        long languageId = getCurrentLanguageId();
        Language lang = langAPI.getLanguage(languageId);
        return langAPI.getStringKey(lang, property);
    }

    public int getInt(String property) {
        long languageId = getCurrentLanguageId();
        Language lang = langAPI.getLanguage(languageId);
        return langAPI.getIntKey(lang, property);
    }

    public boolean getBoolean(String property) {
        long languageId = getCurrentLanguageId();
        Language lang = langAPI.getLanguage(languageId);
        return langAPI.getBooleanKey(lang, property);
    }

    public float getFloat(String property) {
        long languageId = getCurrentLanguageId();
        Language lang = langAPI.getLanguage(languageId);
        return langAPI.getFloatKey(lang, property);
    }
}