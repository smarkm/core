package com.dotmarketing.portlets.languagesmanager.business;

import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import  org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.languagesmanager.business.LanguageFactoryImpl;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import org.apache.commons.lang.RandomStringUtils;

public class LanguageFactoryTest {

    private static LanguageFactoryImpl fac = new LanguageFactoryImpl();



    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private String ran(int len) {
        return RandomStringUtils.randomAlphabetic(len);
    }


    private Language randomLanguage() {
        return new Language(0,ran(2),ran(2), ran(8), ran(10));
        
        
        
    }


    @Test
    public void test_saveLanguage() throws Exception{
        
        int size  = fac.getLanguages().size();
        
        
        System.out.println("testing saving a language");
        Language lang = randomLanguage();
        fac.saveLanguage(lang);
        assert(size == fac.getLanguages().size()-1);
        
        
        
    }



}