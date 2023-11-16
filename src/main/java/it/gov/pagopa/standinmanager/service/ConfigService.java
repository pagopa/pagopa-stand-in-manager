package it.gov.pagopa.standinmanager.service;

import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.client.api.CacheApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@Service
public class ConfigService {

    private ConfigDataV1 configData;

    @Autowired
    private CacheApi cacheApi;

    public ConfigDataV1 getCache(){
        if(configData==null){
            loadCache();
        }
        return configData;
    }

    public void loadCache(){
        log.info("loadCache");
        configData = cacheApi.cache(false, new ArrayList<>());
    }

}
