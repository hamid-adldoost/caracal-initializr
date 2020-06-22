package com.caracal.initializr.types;


public class BackendGenerationConfig {

    private boolean generateMaven = true;
    private boolean generateRunnerClass = true;
    private boolean generatePropertiesFile = true;
    private boolean generateConfigPropertiesFile = true;
    private boolean generateErrorCodeFile = true;
    private boolean generateSecurityConfigClass = true;
    private boolean generateSecurityAuthorities = true;

    private boolean generateEntities = true;
    private boolean generateDto = true;
    private boolean generateDao = true;
    private boolean generateService = true;
    private boolean generateRest = true;
    private boolean generateGeneralService = true;

    private boolean generateSecurityService = true;
    private boolean generateLoginRest = true;

    private boolean generatePermissions = true;

    private boolean generateCommonClasses = true;
    private boolean generateJwtClasses = true;

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
