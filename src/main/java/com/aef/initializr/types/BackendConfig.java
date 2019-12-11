package com.aef.initializr.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BackendConfig implements Serializable {

    private String contextPath;
    private String backendPortNumber;
    private String basePackage;
    private String targetPath;
    private MavenConfig mavenConfig;
    private SecurityConfig securityConfig;
    private DatabaseConnection databaseConnection;
    private BackendGenerationConfig backendGenerationConfig;

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getBackendPortNumber() {
        return backendPortNumber;
    }

    public void setBackendPortNumber(String backendPortNumber) {
        this.backendPortNumber = backendPortNumber;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public MavenConfig getMavenConfig() {
        return mavenConfig;
    }

    public void setMavenConfig(MavenConfig mavenConfig) {
        this.mavenConfig = mavenConfig;
    }

    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    public DatabaseConnection getDatabaseConnection() {
        return databaseConnection;
    }

    public void setDatabaseConnection(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public BackendGenerationConfig getBackendGenerationConfig() {
        return backendGenerationConfig;
    }

    public void setBackendGenerationConfig(BackendGenerationConfig backendGenerationConfig) {
        this.backendGenerationConfig = backendGenerationConfig;
    }
}
