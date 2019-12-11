package com.aef.initializr.types;


public class BackendGenerationConfig {

    private boolean generateMaven;
    private boolean generateRunnerClass;
    private boolean generatePropertiesFile;
    private boolean generateConfigPropertiesFile;
    private boolean generateErrorCodeFile;
    private boolean generateSecurityConfigClass;
    private boolean generateSecurityAuthorities;

    private boolean generateEntities;
    private boolean generateDto;
    private boolean generateDao;
    private boolean generateService;
    private boolean generateRest;
    private boolean generateGeneralService;

    private boolean generateSecurityService;
    private boolean generateLoginRest;

    private boolean generatePermissions;

    private boolean generateCommonClasses;
    private boolean generateJwtClasses;

    public boolean isGenerateMaven() {
        return generateMaven;
    }

    public void setGenerateMaven(boolean generateMaven) {
        this.generateMaven = generateMaven;
    }

    public boolean isGenerateRunnerClass() {
        return generateRunnerClass;
    }

    public void setGenerateRunnerClass(boolean generateRunnerClass) {
        this.generateRunnerClass = generateRunnerClass;
    }

    public boolean isGeneratePropertiesFile() {
        return generatePropertiesFile;
    }

    public void setGeneratePropertiesFile(boolean generatePropertiesFile) {
        this.generatePropertiesFile = generatePropertiesFile;
    }

    public boolean isGenerateConfigPropertiesFile() {
        return generateConfigPropertiesFile;
    }

    public void setGenerateConfigPropertiesFile(boolean generateConfigPropertiesFile) {
        this.generateConfigPropertiesFile = generateConfigPropertiesFile;
    }

    public boolean isGenerateErrorCodeFile() {
        return generateErrorCodeFile;
    }

    public void setGenerateErrorCodeFile(boolean generateErrorCodeFile) {
        this.generateErrorCodeFile = generateErrorCodeFile;
    }

    public boolean isGenerateSecurityConfigClass() {
        return generateSecurityConfigClass;
    }

    public void setGenerateSecurityConfigClass(boolean generateSecurityConfigClass) {
        this.generateSecurityConfigClass = generateSecurityConfigClass;
    }

    public boolean isGenerateSecurityAuthorities() {
        return generateSecurityAuthorities;
    }

    public void setGenerateSecurityAuthorities(boolean generateSecurityAuthorities) {
        this.generateSecurityAuthorities = generateSecurityAuthorities;
    }

    public boolean isGenerateEntities() {
        return generateEntities;
    }

    public void setGenerateEntities(boolean generateEntities) {
        this.generateEntities = generateEntities;
    }

    public boolean isGenerateDto() {
        return generateDto;
    }

    public void setGenerateDto(boolean generateDto) {
        this.generateDto = generateDto;
    }

    public boolean isGenerateDao() {
        return generateDao;
    }

    public void setGenerateDao(boolean generateDao) {
        this.generateDao = generateDao;
    }

    public boolean isGenerateService() {
        return generateService;
    }

    public void setGenerateService(boolean generateService) {
        this.generateService = generateService;
    }

    public boolean isGenerateRest() {
        return generateRest;
    }

    public void setGenerateRest(boolean generateRest) {
        this.generateRest = generateRest;
    }

    public boolean isGenerateGeneralService() {
        return generateGeneralService;
    }

    public void setGenerateGeneralService(boolean generateGeneralService) {
        this.generateGeneralService = generateGeneralService;
    }

    public boolean isGenerateSecurityService() {
        return generateSecurityService;
    }

    public void setGenerateSecurityService(boolean generateSecurityService) {
        this.generateSecurityService = generateSecurityService;
    }

    public boolean isGeneratePermissions() {
        return generatePermissions;
    }

    public void setGeneratePermissions(boolean generatePermissions) {
        this.generatePermissions = generatePermissions;
    }

    public boolean isGenerateLoginRest() {
        return generateLoginRest;
    }

    public void setGenerateLoginRest(boolean generateLoginRest) {
        this.generateLoginRest = generateLoginRest;
    }

    public boolean isGenerateCommonClasses() {
        return generateCommonClasses;
    }

    public void setGenerateCommonClasses(boolean generateCommonClasses) {
        this.generateCommonClasses = generateCommonClasses;
    }

    public boolean isGenerateJwtClasses() {
        return generateJwtClasses;
    }

    public void setGenerateJwtClasses(boolean generateJwtClasses) {
        this.generateJwtClasses = generateJwtClasses;
    }
}
