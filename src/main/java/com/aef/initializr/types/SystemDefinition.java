package com.aef.initializr.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemDefinition implements Serializable {

    private List<EntityDefinition> entityDefinitionList;
    private BackendConfig backendConfig;
    private FrontendConfig frontendConfig;
    private Boolean generateBackend;
    private Boolean generateFrontend;

    public List<EntityDefinition> getEntityDefinitionList() {
        return entityDefinitionList;
    }

    public void setEntityDefinitionList(List<EntityDefinition> entityDefinitionList) {
        this.entityDefinitionList = entityDefinitionList;
    }

    public FrontendConfig getFrontendConfig() {
        return frontendConfig;
    }

    public void setFrontendConfig(FrontendConfig frontendConfig) {
        this.frontendConfig = frontendConfig;
    }

    public BackendConfig getBackendConfig() {
        return backendConfig;
    }

    public void setBackendConfig(BackendConfig backendConfig) {
        this.backendConfig = backendConfig;
    }

    public Boolean getGenerateBackend() {
        return generateBackend;
    }

    public void setGenerateBackend(Boolean generateBackend) {
        this.generateBackend = generateBackend;
    }

    public Boolean getGenerateFrontend() {
        return generateFrontend;
    }

    public void setGenerateFrontend(Boolean generateFrontend) {
        this.generateFrontend = generateFrontend;
    }
}
