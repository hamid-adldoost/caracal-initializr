package com.caracal.initializr;


import com.caracal.initializr.dto.ProjectDto;
import com.caracal.initializr.service.ProjectService;
import com.caracal.initializr.types.ComponentTypes;
import com.caracal.initializr.types.EntityDefinition;
import com.caracal.initializr.types.EntityFieldDefinition;
import com.caracal.initializr.types.SystemDefinition;
import com.google.common.base.CaseFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CaracalGenerator {

    private final ProjectService projectService;

    @Autowired
    public CaracalGenerator(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProjectDto saveProject(SystemDefinition systemDefinition) {
        ProjectDto project = new ProjectDto();
        project.setName(systemDefinition.getBackendConfig().getMavenConfig().getProjectName());
        project.setGenerationDate(new Date());
        project.setBackendGenerationPath(systemDefinition.getBackendConfig().getContextPath());
        project.setFrontendGenerationPath(systemDefinition.getFrontendConfig().getTargetPath());
        Gson gson = new Gson();
        project.setJsonMessage(gson.toJson(systemDefinition));
        return projectService.save(project);
    }

    @Transactional
    public void generateAll(String jsobBody) throws IOException {


        Gson gson = new Gson();
        SystemDefinition systemDefinition = gson.fromJson(jsobBody, SystemDefinition.class);

        saveProject(systemDefinition);

        validateDefinition(systemDefinition);

        generateStructureOfProject(systemDefinition.getBackendConfig().getMavenConfig().getProjectName(),
                systemDefinition.getBackendConfig().getBasePackage(),
                systemDefinition.getBackendConfig().getTargetPath());
        String sourcePackageTarget = generateSourceTargetPackagePath(systemDefinition.getBackendConfig().getTargetPath(),
                systemDefinition.getBackendConfig().getBasePackage(),
                systemDefinition.getBackendConfig().getMavenConfig().getProjectName());

        File modelPath = new File(sourcePackageTarget + "/model");
        File daoPath = new File(sourcePackageTarget + "/dao");
        File servicePath = new File(sourcePackageTarget + "/service");
        File dtoPath = new File(sourcePackageTarget + "/dto");
        File restPath = new File(sourcePackageTarget + "/rest");
        File commonPath = new File(sourcePackageTarget + "/common");
        File jwtPath = new File(sourcePackageTarget + "/jwt");
        File securityPath = new File(sourcePackageTarget + "/security");
        File rootPath = new File(systemDefinition.getBackendConfig().getTargetPath()
                + "/" + systemDefinition.getBackendConfig().getMavenConfig().getProjectName());
        File resourcePath = new File(rootPath.getPath() + "/src/main/resources");

        modelPath.mkdirs();
        daoPath.mkdirs();
        servicePath.mkdirs();
        dtoPath.mkdirs();
        restPath.mkdirs();
        commonPath.mkdirs();
        jwtPath.mkdirs();
        rootPath.mkdirs();
        resourcePath.mkdirs();

        List<String> entityNameList = systemDefinition.getEntityDefinitionList().stream()
                .map(EntityDefinition::getName).collect(Collectors.toList());


        if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateMaven()) {
            generatePOMFile(rootPath.getPath(), systemDefinition.getBackendConfig().getMavenConfig().getProjectName(),
                    systemDefinition.getBackendConfig().getMavenConfig().getProjectDescription(),
                    systemDefinition.getBackendConfig().getMavenConfig().getMavenArtifactId(),
                    systemDefinition.getBackendConfig().getMavenConfig().getMavenGroupId());
        }

        if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateRunnerClass()) {
            generateRunnerClass(sourcePackageTarget,
                    systemDefinition.getBackendConfig().getBasePackage(),
                    systemDefinition.getBackendConfig().getMavenConfig().getProjectName());
        }

        if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGeneratePropertiesFile()) {
            generateApplicationDotPropertiesFile(resourcePath.getPath(),
                    systemDefinition.getBackendConfig().getDatabaseConnection().getDatasourceUrl(),
                    systemDefinition.getBackendConfig().getDatabaseConnection().getDatasourceUsername(),
                    systemDefinition.getBackendConfig().getDatabaseConnection().getDatasourcePassword(),
                    systemDefinition.getBackendConfig().getContextPath(),
                    systemDefinition.getBackendConfig().getBackendPortNumber(),
                    systemDefinition.getBackendConfig().getBasePackage(),
                    systemDefinition.getBackendConfig().getFileUploadPath());
        }


        if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateConfigPropertiesFile()) {
            generateConfigPropertiesFile(resourcePath.getPath(),
                    systemDefinition.getBackendConfig().getSecurityConfig().getJwtKey(),
                    systemDefinition.getBackendConfig().getSecurityConfig().getTokenExpiration());
        }

        if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateErrorCodeFile()) {
            generateErrorCodeProperties(resourcePath.getPath());
            generateFarsiCodesProperties(resourcePath.getPath(), systemDefinition.getEntityDefinitionList());
        }

        if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateSecurityConfigClass()) {
            generateSecurityConfig(systemDefinition.getBackendConfig().getBasePackage(), securityPath.getPath());
        }

        if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateSecurityAuthorities()) {
            generateSecurityAuthorities(systemDefinition.getBackendConfig().getBasePackage(),
                    securityPath.getPath(), entityNameList);
        }

        String basePackage = systemDefinition.getBackendConfig().getBasePackage();

        //fill common package
        if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateCommonClasses()) {
            generateBusinessExceptionCodeClass(commonPath.getPath(), basePackage);
            generateConfigReaderUtilClass(commonPath.getPath(), basePackage);
            generateCaracalExceptionHandler(commonPath.getPath(), basePackage);
            generateFarsiCodeReaderUtility(commonPath.getPath(), basePackage);
            generateErrorCodeReaderUtilClass(commonPath.getPath(), basePackage);
            generateRandomStringCodeUtility(commonPath.getPath(), basePackage);
            generateRestErrorMessageClass(commonPath.getPath(), basePackage);
            generateSecurityServiceExceptionClass(commonPath.getPath(), basePackage);
            generateFileStoragePropertiesFile(commonPath.getPath(), basePackage);
            generateValidationExceptionFile(commonPath.getPath(), basePackage);
            generateValidationTypeEnum(commonPath.getPath(), basePackage);
        }

        //fill jwt package
        if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateJwtClasses()) {
            generateCustomClaims(jwtPath.getPath(), basePackage);
            generateJwtAuthenticationEntryClass(jwtPath.getPath(), basePackage);
            generateJwtAuthenticationFilterClass(jwtPath.getPath(), basePackage);
            generateJwtAuthenticationProvider(jwtPath.getPath(), basePackage);
            generateJwtAuthenticationTokenClass(jwtPath.getPath(), basePackage);
            generateJwtUserDetails(jwtPath.getPath(), basePackage);
            generateJwtUtilClass(jwtPath.getPath(), basePackage);
            generateSecurityWrapperClass(jwtPath.getPath(), basePackage);
            generateTokenRepository(jwtPath.getPath(), basePackage);
        }

        if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateSecurityService()) {
            generateSecurityServiceClass(servicePath.getPath(), basePackage);
        }
        if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateLoginRest()) {
            generateLoginRestService(restPath.getPath(), basePackage);
        }

        generateGeneralCaracalDaoImpl(basePackage, daoPath.getPath());

        systemDefinition.getEntityDefinitionList().forEach(entity -> {
            try {
                if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateEntities()) {

                    generateAttachmentEntity(basePackage, modelPath.getPath());
                    generateEntity(basePackage, entity, modelPath.getPath());
                }
                if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateDto()) {
                    generateDownloadAttachmentDto(basePackage, dtoPath.getPath());
                    generateAttachmentDto(basePackage, dtoPath.getPath());
                    generateDto(basePackage, entity, dtoPath.getPath(), entityNameList);
                }
                if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateDao()) {
                    generateAttachmentDao(basePackage, daoPath.getPath());
                    generateDao(basePackage, entity.getName(), daoPath.getPath());
                }
                if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateService()) {
                    generateAttachmentService(basePackage, servicePath.getPath());
                    generateFileStorageService(basePackage, servicePath.getPath());
                    generateService(basePackage, entity, servicePath.getPath());
                }
                if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateGeneralService()) {
                    //fill service package
                    generateGeneralServiceInterface(servicePath.getPath(), basePackage);
                    generateGeneralServiceImplClass(servicePath.getPath(), basePackage);
                    generatePagedResultClass(servicePath.getPath(), basePackage);
                }
                if (systemDefinition.getBackendConfig().getBackendGenerationConfig().isGenerateRest()) {
                    generateAttachmentRestService(basePackage, restPath.getPath());
                    generateRestService(basePackage, entity, restPath.getPath(), systemDefinition.getBackendConfig().getBackendGenerationConfig().isGeneratePermissions());
                }

                FrontGenerator frontGenerator = new FrontGenerator();
                frontGenerator.generateStructureOfProject(systemDefinition.getFrontendConfig().getProjectName(), systemDefinition.getFrontendConfig().getTargetPath());

                FrontGenerator.generateEntityComponent(entityNameList, systemDefinition.getFrontendConfig().getTargetPath(), entity);
                FrontGenerator.generateEntityService(systemDefinition.getFrontendConfig().getTargetPath(), entity.getName());
                FrontGenerator.generateEntityHtmlView(systemDefinition.getFrontendConfig().getTargetPath(), systemDefinition, entity);


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
        FrontGenerator.refactorAppModule(systemDefinition.getFrontendConfig().getTargetPath(), entityNameList);
        FrontGenerator.generateRouter(systemDefinition.getFrontendConfig().getTargetPath(), entityNameList);
        FrontGenerator.generateSidebarComponent(systemDefinition.getFrontendConfig().getTargetPath(), systemDefinition.getEntityDefinitionList());
        FrontGenerator.generateSidebarComponentView(systemDefinition.getFrontendConfig().getTargetPath(), systemDefinition.getFrontendConfig().getProjectFarsiName());
        FrontGenerator.refactorIndexHtml(systemDefinition.getFrontendConfig().getTargetPath(), systemDefinition.getFrontendConfig().getProjectFarsiName());
        FrontGenerator.refactorLoginHtml(systemDefinition.getFrontendConfig().getTargetPath(), systemDefinition.getFrontendConfig().getProjectFarsiName());
        FrontGenerator.generateProxyConf(systemDefinition.getFrontendConfig().getTargetPath(),
                systemDefinition.getBackendConfig().getContextPath(),
                systemDefinition.getBackendConfig().getBackendPortNumber());
        FrontGenerator.generateEnvironment(systemDefinition.getFrontendConfig().getTargetPath(),
                systemDefinition.getBackendConfig().getContextPath());
        FrontGenerator.generateProductionEnvironment(systemDefinition.getFrontendConfig().getTargetPath(),
                systemDefinition.getBackendConfig().getContextPath());

    }

    private void validateDefinition(SystemDefinition systemDefinition) {
        //todo: check and set defauls

        if (systemDefinition.getBackendConfig().getFileUploadPath() == null ||
                systemDefinition.getBackendConfig().getFileUploadPath().isEmpty()) {
            systemDefinition.getBackendConfig().setFileUploadPath("C:\\\\upload\\\\");
        }
    }

    private static String generateLoginRestService(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.rest;\n" +
                "\n" +
                "import #package.jwt.SecurityWrapper;\n" +
                "import org.springframework.http.HttpHeaders;\n" +
                "import #package.jwt.JWTUserDetails;\n" +
                "import #package.jwt.JWTUtil;\n" +
                "import #package.service.SecurityService;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.web.bind.annotation.PostMapping;\n" +
                "import org.springframework.web.bind.annotation.RequestMapping;\n" +
                "import org.springframework.web.bind.annotation.RequestParam;\n" +
                "import org.springframework.web.bind.annotation.RestController;\n" +
                "import javax.servlet.http.HttpServletResponse;\n" +
                "\n" +
                "@RestController\n" +
                "@RequestMapping(path = \"/auth\")\n" +
                "public class LoginRestService {\n" +
                "\n" +
                "    private final SecurityService securityService;\n" +
                "\n" +
                "    @Autowired\n" +
                "    public LoginRestService(SecurityService securityService) {\n" +
                "        this.securityService = securityService;\n" +
                "    }\n" +
                "\n" +
                "    @PostMapping(path = \"/login\")\n" +
                "    public SecurityWrapper login(@RequestParam(name = \"username\") String username,\n" +
                "                                 @RequestParam(name = \"password\") String password,\n" +
                "                                 final HttpServletResponse response) {\n" +
                "\n" +
                "        SecurityWrapper securityWrapper = securityService.authenticate(username, password);\n" +
                "        JWTUserDetails userDetails = new JWTUserDetails(securityWrapper);\n" +
                "        // Issue a token for the user\n" +
                "        // String token = issueToken(username);\n" +
                "        String token = JWTUtil.generateToken(userDetails);\n" +
                "        // Return the token on the response\n" +
                "        response.setHeader(HttpHeaders.AUTHORIZATION, \"Bearer \" + token);\n" +
                "        return securityWrapper;\n" +
                "    }\n" +
                "}\n";
        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/LoginRestService.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateSecurityConfig(String basePackage, String path) throws FileNotFoundException {

        String content = "package #package.security;\n" +
                "\n" +
                "import #package.jwt.JwtAuthenticationEntryPoint;\n" +
                "import #package.jwt.JwtAuthenticationFilter;\n" +
                "import #package.jwt.JwtAuthenticationProvider;\n" +
                "import #package.jwt.TokenRepository;\n" +
                "import #package.common.SecurityServiceException;\n" +
                "import org.slf4j.Logger;\n" +
                "import org.slf4j.LoggerFactory;\n" +
                "import org.springframework.beans.factory.InjectionPoint;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.context.MessageSource;\n" +
                "import org.springframework.context.annotation.Bean;\n" +
                "import org.springframework.context.support.ReloadableResourceBundleMessageSource;\n" +
                "import org.springframework.context.annotation.Configuration;\n" +
                "import org.springframework.context.annotation.Scope;\n" +
                "import org.springframework.security.authentication.AuthenticationManager;\n" +
                "import org.springframework.security.config.BeanIds;\n" +
                "import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;\n" +
                "import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;\n" +
                "import org.springframework.security.config.annotation.web.builders.HttpSecurity;\n" +
                "import org.springframework.security.config.annotation.web.builders.WebSecurity;\n" +
                "import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;\n" +
                "import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;\n" +
                "import org.springframework.security.config.http.SessionCreationPolicy;\n" +
                "import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;\n" +
                "import org.springframework.security.crypto.password.PasswordEncoder;\n" +
                "import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;\n" +
                "\n" +
                "@Configuration\n" +
                "@EnableWebSecurity\n" +
                "@EnableGlobalMethodSecurity(\n" +
                "        securedEnabled = true,\n" +
                "        jsr250Enabled = true,\n" +
                "        prePostEnabled = true\n" +
                ")\n" +
                "public class SecurityConfig extends WebSecurityConfigurerAdapter {\n" +
                "\n" +
                "\n" +
                "    private final JwtAuthenticationEntryPoint unauthorizedHandler;\n" +
                "\n" +
                "    private final JwtAuthenticationProvider jwtAuthenticationProvider;\n" +
                "\n" +
                "    private final TokenRepository tokenRepository;\n" +
                "\n" +
                "    @Bean\n" +
                "    public PasswordEncoder passwordEncoder() {\n" +
                "        return new BCryptPasswordEncoder();\n" +
                "    }\n" +
                "\n" +
                "//    @Bean\n" +
                "//    public MessageSource messageSource() {\n" +
                "//        ReloadableResourceBundleMessageSource messageSource\n" +
                "//           = new ReloadableResourceBundleMessageSource();\n" +
                "//\n" +
                "//       messageSource.setBasename(\"classpath:messages\");\n" +
                "//       messageSource.setDefaultEncoding(\"UTF-8\");\n" +
                "//       return messageSource;\n" +
                "//    }" +

                "\n" +
                "\n" +
                "    @Autowired\n" +
                "    public SecurityConfig(JwtAuthenticationEntryPoint unauthorizedHandler, JwtAuthenticationProvider jwtAuthenticationProvider, TokenRepository tokenRepository) {\n" +
                "        this.unauthorizedHandler = unauthorizedHandler;\n" +
                "        this.jwtAuthenticationProvider = jwtAuthenticationProvider;\n" +
                "        this.tokenRepository = tokenRepository;\n" +
                "    }\n" +
                "\n" +
                "\n" +
                "    @Bean\n" +
                "    @Scope(\"prototype\")\n" +
                "    Logger logger(InjectionPoint injectionPoint){\n" +
                "        return LoggerFactory.getLogger(injectionPoint.getMethodParameter().getContainingClass());\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {\n" +
                "        authenticationManagerBuilder.authenticationProvider(jwtAuthenticationProvider);\n" +
                "    }\n" +
                "\n" +
                "    @Bean(BeanIds.AUTHENTICATION_MANAGER)\n" +
                "    @Override\n" +
                "    public AuthenticationManager authenticationManagerBean() throws Exception {\n" +
                "        return super.authenticationManagerBean();\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected void configure(HttpSecurity http) throws Exception {\n" +
                "\n" +
                "        http\n" +
                "                .csrf()\n" +
                "                .disable()\n" +
                "                .headers()\n" +
                "                .frameOptions()\n" +
                "                .disable()\n" +
                "                .and()\n" +
                "                .sessionManagement()\n" +
                "                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)\n" +
                "                .and()\n" +
                "                .authorizeRequests()\n" +
                "                .antMatchers(\"**\").permitAll()\n" +
                "                .and()\n" +
                "                .addFilterBefore(new JwtAuthenticationFilter(tokenRepository), UsernamePasswordAuthenticationFilter.class)\n" +
                "                .antMatcher(\"**\")\n" +
                "                .authorizeRequests();\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void configure(WebSecurity web) throws Exception {\n" +
                "        web\n" +
                "                .ignoring()\n" +
                "                .antMatchers(\"/auth/**\")\n" +
                "                .antMatchers(\"/public/**\");\n" +
                "    }\n" +
                "\n" +
                "}";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/SecurityConfig.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateSecurityAuthorities(String basePackage, String path, List<String> entityNamesList) throws FileNotFoundException {

        if (entityNamesList == null || entityNamesList.isEmpty())
            return null;

        String content = "package #package.security;\n" +
                "\n" +
                "\n" +
                "\n" +
                "import java.util.ArrayList;\n" +
                "import java.util.List;\n" +
                "\n" +
                "public class AccessRoles {\n" +
                "\n";

        for (String e : entityNamesList) {
            content += "    public static final String AUTHORITY_FIND_" + camelToSnake(e).toUpperCase() + " = \"AUTHORITY_FIND_" + camelToSnake(e).toUpperCase() + "\";\n";
            content += "    public static final String AUTHORITY_SEARCH_" + camelToSnake(e).toUpperCase() + " = \"AUTHORITY_SEARCH_" + camelToSnake(e).toUpperCase() + "\";\n";
            content += "    public static final String AUTHORITY_SAVE_" + camelToSnake(e).toUpperCase() + " = \"AUTHORITY_SAVE_" + camelToSnake(e).toUpperCase() + "\";\n";
            content += "    public static final String AUTHORITY_REMOVE_" + camelToSnake(e).toUpperCase() + " = \"AUTHORITY_REMOVE_" + camelToSnake(e).toUpperCase() + "\";\n";
        }

        content += "    public static final String AUTHORITY_FIND_ATTACHMENT = \"AUTHORITY_FIND_ATTACHMENT\";\n" +
                "    public static final String AUTHORITY_SEARCH_ATTACHMENT = \"AUTHORITY_SEARCH_ATTACHMENT\";\n" +
                "    public static final String AUTHORITY_SAVE_ATTACHMENT = \"AUTHORITY_SAVE_ATTACHMENT\";\n" +
                "    public static final String AUTHORITY_REMOVE_ATTACHMENT = \"AUTHORITY_REMOVE_ATTACHMENT\";";

        content += "\n\n";
        content += "    public static List<String> getAllRoles() {\n";
        content += "        List<String> roles = new ArrayList<>();\n";
        for (String e : entityNamesList) {
            content += "        roles.add(AUTHORITY_FIND_" + camelToSnake(e).toUpperCase() + ");\n";
            content += "        roles.add(AUTHORITY_SEARCH_" + camelToSnake(e).toUpperCase() + ");\n";
            content += "        roles.add(AUTHORITY_SAVE_" + camelToSnake(e).toUpperCase() + ");\n";
            content += "        roles.add(AUTHORITY_REMOVE_" + camelToSnake(e).toUpperCase() + ");\n";
        }
        content += "        roles.add(AUTHORITY_FIND_ATTACHMENT);\n" +
                "        roles.add(AUTHORITY_SEARCH_ATTACHMENT);\n" +
                "        roles.add(AUTHORITY_SAVE_ATTACHMENT);\n" +
                "        roles.add(AUTHORITY_REMOVE_ATTACHMENT);\n";

        content += "    return roles;\n";
        content += "    }\n";
        content += "}\n";


        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/AccessRoles.java"))) {
            out.print(result);
        }
        return result;

    }

//    private static boolean checkGeneration(String key) {
//
//        try {
//            String value = InitializrReaderUtility.getResourceProperity(key);
//            if(value.equalsIgnoreCase("true"))
//                return true;
//
//        } catch (Exception e) {
//            return false;
//        }
//        return false;
//    }

//    private static LinkedHashMap<String, String> findFieldsForEntity(String entityName) {
//        Enumeration<String> keys = InitializrReaderUtility.getResourceKeys();
//        List<String> keyList = Collections.list(keys);
//
//        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
//
//        keyList.stream().forEach(k -> {
//            if(k.contains(entityName + ".field.type.")) {
//                String[] parts = k.split("\\.");
//                String type = InitializrReaderUtility.getResourceProperity(k);
//                fields.put(parts[3], type);
//            }
//        });
//        return fields;
//    }

//    private static List<String> findEntities() {
//        Enumeration<String> keys = InitializrReaderUtility.getResourceKeys();
//        List<String> keyList = Collections.list(keys);
//
//        List<String> entities = new ArrayList<>();
//
//        keyList.stream().forEach(k -> {
//            if(k.contains("entity.name.")) {
//                String[] parts = k.split("\\.");
//                String type = InitializrReaderUtility.getResourceProperity(k);
//                entities.add(parts[2]);
//            }
//        });
//        return entities;
//    }

//    private static LinkedHashMap<String, String> findEntitiesFarsiNames() {
//        Enumeration<String> keys = InitializrReaderUtility.getResourceKeys();
//        List<String> keyList = Collections.list(keys);
//
//        LinkedHashMap<String, String> entities = new LinkedHashMap<>();
//
//        keyList.stream().forEach(k -> {
//            if(k.contains("entity.farsi.name.")) {
//                String[] parts = k.split("\\.");
//                String farsi = InitializrReaderUtility.getResourceProperity(k);
//                entities.put(parts[3], farsi);
//            }
//        });
//        return entities;
//    }

//    private static LinkedHashMap<String, String> findEntitiesLabels() {
//        Enumeration<String> keys = InitializrReaderUtility.getResourceKeys();
//        List<String> keyList = Collections.list(keys);
//
//        LinkedHashMap<String, String> entities = new LinkedHashMap<>();
//
//        keyList.stream().forEach(k -> {
//            if(k.contains("entity.label.")) {
//                String[] parts = k.split("\\.");
//                String label = InitializrReaderUtility.getResourceProperity(k);
//                entities.put(parts[2], label);
//            }
//        });
//        return entities;
//    }

//    private static LinkedHashMap<String, LinkedHashMap<String, String>> findFieldsFarsiNames() {
//
//        List<String> entites = findEntities();
//        LinkedHashMap<String, LinkedHashMap<String, String>> result = new LinkedHashMap<>();
//
//        entites.forEach(e -> {
//            Enumeration<String> keys = InitializrReaderUtility.getResourceKeys();
//            List<String> keyList = Collections.list(keys);
//
//            LinkedHashMap<String, String> fields = new LinkedHashMap<>();
//
//            keyList.stream().forEach(k -> {
//                if(k.contains(e + ".field.farsi.")) {
//                    String[] parts = k.split("\\.");
//                    String farsi = InitializrReaderUtility.getResourceProperity(k);
//                    fields.put(parts[3], farsi);
//                }
//            });
//            result.put(e, fields);
//        });
//
//
//        return result;
//    }

    private static String generateSourceTargetPackagePath(String targetPath, String basePackage, String projectName) {
        String rootPath = targetPath + "/" + projectName;
        String[] packages = basePackage.split("\\.");
        StringBuilder packagesPath = new StringBuilder(rootPath);
        packagesPath.append("/src").append("/main").append("/java");
        for (String aPackage : packages) {
            packagesPath.append("/");
            packagesPath.append(aPackage);
        }
        return packagesPath.toString();
    }

    private static String generateStructureOfProject(String projectName, String basePackage, String targetPath) {

        String rootPath = targetPath + "/" + projectName;
        String[] packages = basePackage.split("\\.");
        StringBuilder packagesPath = new StringBuilder(rootPath);
        packagesPath.append("/src").append("/main").append("/java");
        for (String aPackage : packages) {
            packagesPath.append("/");
            packagesPath.append(aPackage);
        }
        StringBuilder resourcePath = new StringBuilder(rootPath);
        resourcePath.append("/src").append("/main").append("/resources");

        File filePath = new File(packagesPath.toString());
        filePath.mkdirs();

        filePath = new File(resourcePath.toString());
        filePath.mkdirs();

        StringBuilder testPath = new StringBuilder(rootPath);
        testPath.append("/src").append("/test");
        filePath = new File(testPath.toString());
        filePath.mkdirs();
        return rootPath;

    }

    private static String generatePOMFile(String path, String projectName, String projectDescription, String artifactId, String groupId) throws FileNotFoundException {

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "\n" +
                "    <groupId>#groupId</groupId>\n" +
                "    <artifactId>#artifactId</artifactId>\n" +
                "    <version>0.0.1-SNAPSHOT</version>\n" +
                "    <packaging>jar</packaging>\n" +
                "\n" +
                "    <name>#projectName</name>\n" +
                "    <description>#projectDescription</description>\n" +
                "\n" +
                "    <parent>\n" +
                "        <groupId>org.springframework.boot</groupId>\n" +
                "        <artifactId>spring-boot-starter-parent</artifactId>\n" +
                "        <version>2.0.4.RELEASE</version>\n" +
                "        <relativePath/> <!-- lookup parent from repository -->\n" +
                "    </parent>\n" +
                "\n" +
                "    <properties>\n" +
                "        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
                "        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>\n" +
                "        <java.version>1.8</java.version>\n" +
                "        <docker.image.prefix>springio</docker.image.prefix>\n" +
                "    </properties>\n" +
                "\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>org.springframework.boot</groupId>\n" +
                "            <artifactId>spring-boot-starter-data-jpa</artifactId>\n" +
                "        </dependency>\n" +
                "        <dependency>\n" +
                "            <groupId>org.springframework.boot</groupId>\n" +
                "            <artifactId>spring-boot-starter-security</artifactId>\n" +
                "        </dependency>\n" +
                "        <dependency>\n" +
                "            <groupId>org.springframework.boot</groupId>\n" +
                "            <artifactId>spring-boot-starter-web</artifactId>\n" +
                "        </dependency>\n" +
                "\n" +
                "        <dependency>\n" +
                "            <groupId>org.springframework.boot</groupId>\n" +
                "            <artifactId>spring-boot-starter-test</artifactId>\n" +
                "            <scope>test</scope>\n" +
                "        </dependency>\n" +
                "        <dependency>\n" +
                "            <groupId>org.springframework.security</groupId>\n" +
                "            <artifactId>spring-security-test</artifactId>\n" +
                "            <scope>test</scope>\n" +
                "        </dependency>\n" +
                "\n" +
                "        <dependency>\n" +
                "           <groupId>com.ibm.icu</groupId>\n" +
                "           <artifactId>icu4j</artifactId>\n" +
                "           <version>61.1</version>\n" +
                "        </dependency>\n" +
                "        <dependency>\n" +
                "           <groupId>net.sourceforge.jexcelapi</groupId>\n" +
                "           <artifactId>jxl</artifactId>\n" +
                "           <version>2.6</version>\n" +
                "        </dependency>\n" +
                "        <dependency>\n" +
                "           <groupId>com.google.guava</groupId>\n" +
                "           <artifactId>guava</artifactId>\n" +
                "           <version>19.0</version>\n" +
                "        </dependency>" +
                "        <dependency>\n" +
                "            <groupId>com.adldoost</groupId>\n" +
                "            <artifactId>caracal-data</artifactId>\n" +
                "            <version>2.3-SNAPSHOT</version>\n" +
                "        </dependency>\n" +
                "        <dependency>\n" +
                "           <groupId>mysql</groupId>\n" +
                "           <artifactId>mysql-connector-java</artifactId>\n" +
                "           <version>8.0.13</version>\n" +
                "        </dependency>" +
                "\n" +
                "        <dependency>\n" +
                "            <groupId>io.jsonwebtoken</groupId>\n" +
                "            <artifactId>jjwt</artifactId>\n" +
                "            <version>0.8.0</version>\n" +
                "        </dependency>\n" +
                "   </dependencies>\n" +
                "\n" +
                "\n" +
                "    <build>\n" +
                "        <plugins>\n" +
                "            <plugin>\n" +
                "                <groupId>org.springframework.boot</groupId>\n" +
                "                <artifactId>spring-boot-maven-plugin</artifactId>\n" +
                "            </plugin>\n" +
                "        </plugins>\n" +
                "    </build>\n" +
                "\n" +
                "</project>\n";

        String result = content.replaceAll("#groupId", groupId)
                .replaceAll("#artifactId", artifactId)
                .replaceAll("#projectName", projectName)
                .replaceAll("#projectDescription", projectDescription);

        System.out.printf(result);

        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/pom.xml"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateAttachmentEntity(String basePackage, String path) throws FileNotFoundException {
        StringBuilder content = new StringBuilder("");
        content.append("package #package.model;\n" +
                "\n" +
                "import com.caracal.data.api.DomainEntity;\n" +
                "\n" +
                "import javax.persistence.*;\n" +
                "import java.util.Date;\n" +
                "import java.util.Objects;\n" +
                "\n" +
                "\n" +
                "/* Generated By Caracal Generator ( Powered by Dr.Adldoost :D ) */\n" +
                "\n" +
                "@Entity\n" +
                "@Table(name = \"attachment\")\n" +
                "public class Attachment implements DomainEntity {\n" +
                "\n" +
                "\n" +
                "    @Id\n" +
                "    @Column(name = \"id\")\n" +
                "    @GeneratedValue(strategy = GenerationType.IDENTITY)\n" +
                "    private Long id;\n" +
                "\n" +
                "    @Column(name = \"name\")\n" +
                "    private String name;\n" +
                "\n" +
                "    @Column(name = \"upload_path\", length = 255)\n" +
                "    private String uploadPath;\n" +
                "\n" +
                "    @Column(name = \"description\", length = 500)\n" +
                "    private String description;\n" +
                "\n" +
                "    @Column(name = \"related_entity\")\n" +
                "    private String relatedEntity;\n" +
                "\n" +
                "    @Column(name = \"related_record_id\")\n" +
                "    private Long relatedRecordId;\n" +
                "\n" +
                "    @Column(name = \"file_md5\", length = 512)\n" +
                "    private String fileMd5;\n" +
                "\n" +
                "\n" +
                "    @Override\n" +
                "    public boolean isAuditable() {\n" +
                "        return true;\n" +
                "    }" +
                "\n" +
                "    public Long getId() {\n" +
                "        return id;\n" +
                "    }\n" +
                "\n" +
                "    public void setId(Long id) {\n" +
                "       this.id = id;\n" +
                "    }\n" +
                "\n" +
                "\n" +
                "    public String getName() {\n" +
                "        return name;\n" +
                "    }\n" +
                "\n" +
                "    public void setName(String name) {\n" +
                "       this.name = name;\n" +
                "    }\n" +
                "\n" +
                "\n" +
                "    public String getUploadPath() {\n" +
                "        return uploadPath;\n" +
                "    }\n" +
                "\n" +
                "    public void setUploadPath(String uploadPath) {\n" +
                "       this.uploadPath = uploadPath;\n" +
                "    }\n" +
                "\n" +
                "\n" +
                "    public String getDescription() {\n" +
                "        return description;\n" +
                "    }\n" +
                "\n" +
                "    public void setDescription(String description) {\n" +
                "       this.description = description;\n" +
                "    }\n" +
                "\n" +
                "\n" +
                "    public String getRelatedEntity() {\n" +
                "        return relatedEntity;\n" +
                "    }\n" +
                "\n" +
                "    public void setRelatedEntity(String relatedEntity) {\n" +
                "       this.relatedEntity = relatedEntity;\n" +
                "    }\n" +
                "\n" +
                "\n" +
                "    public Long getRelatedRecordId() {\n" +
                "        return relatedRecordId;\n" +
                "    }\n" +
                "\n" +
                "    public void setRelatedRecordId(Long relatedRecordId) {\n" +
                "       this.relatedRecordId = relatedRecordId;\n" +
                "    }\n" +
                "\n" +
                "\n" +
                "    public String getFileMd5() {\n" +
                "        return fileMd5;\n" +
                "    }\n" +
                "\n" +
                "    public void setFileMd5(String fileMd5) {\n" +
                "       this.fileMd5 = fileMd5;\n" +
                "    }\n" +
                "\n" +
                "\n" +
                "    @Override\n" +
                "    public boolean equals(Object o) {\n" +
                "        if (this == o) return true;\n" +
                "        if (!(o instanceof Attachment)) return false;\n" +
                "        Attachment attachment = (Attachment) o;\n" +
                "        return Objects.equals(id, attachment.id);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public int hashCode() {\n" +
                "\n" +
                "        return Objects.hash(id);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public String toString() {\n" +
                "        return \"Attachment{\" +\n" +
                "                \"id=\" + id +\n" +
                "                '}';\n" +
                "    }\n" +
                "}");


        String result = content.toString().replaceAll("#package", basePackage);

        System.out.println(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/Attachment.java"))) {
            out.print(result);
        }
        return result;
    }

    public static String generateEntity(String basePackage, EntityDefinition entity, String targetPath) throws FileNotFoundException {
        StringBuilder content = new StringBuilder("package #package.model;\n" +
                "\n" +
                "import com.caracal.data.api.DomainEntity;\n" +
                "\n" +
                "import javax.persistence.*;\n" +
                "import java.util.Date;\n" +
                "import java.util.Objects;\n" +
                "\n" +
                "\n" +
                "/* Generated By Caracal Generator ( Powered by Dr.Adldoost :D ) */\n" +
                "\n" +
                "@Entity\n" +
                "@Table(name = \"").append(camelToSnake(entity.getName())).append("\")\n")

                .append("public class #Entity implements DomainEntity {\n")
                .append("\n");

        entity.getEntityFieldDefinitionList().forEach(field -> {
            content.append("\n");
            if (field.getName().equalsIgnoreCase("id")) {
                content.append("    @Id\n")
                        .append("    @Column(name = \"id\")\n")
                        .append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n")
                        .append("    private ").append(field.getFieldType().getType().getValue()).append(" ").append(field.getName()).append(";")
                        .append("\n");
            } else if (field.getFieldType().getType().getValue().toLowerCase().contains(ComponentTypes.DROP_DOWN.getValue().toLowerCase())) {
                content.append("    @Column(name = \"").append(camelToSnake(field.getName())).append("\"");
                if (field.getNullable() != null && !field.getNullable()) {
                    content.append(", nullable = " + false);
                }
                if (field.getLength() != null) {
                    content.append(", length = ").append(field.getLength());
                }
                content.append(")\n");
                content.append("    private Long").append(" ").append(field.getName()).append(";\n");
            } else if (field.getFieldType().getType().getValue().toLowerCase().contains(ComponentTypes.RADIO_BUTTON.getValue().toLowerCase())) {
                content.append("    @Column(name = \"").append(camelToSnake(field.getName())).append("\"");
                if (!field.getNullable()) {
                    content.append(", nullable = " + false);
                }

                if (field.getLength() != null) {
                    String length = field.getLength() + "";
                    content.append(", length = ").append(length);
                }
                content.append(")\n");
                content.append("    private Long").append(" ").append(field.getName()).append(";\n");
            } else if (getBaseTypes().contains(field.getFieldType().getType().getValue())) {
                content.append("    @Column(name = \"").append(camelToSnake(field.getName())).append("\"");
                if (field.getNullable() != null && !field.getNullable()) {
                    content.append(", nullable = " + false);
                }
                if (field.getLength() != null) {
                    content.append(", length = ").append(field.getLength());
                }
                content.append(")\n");
                if (field.getFieldType().getType().getValue().equalsIgnoreCase("Date")) {
                    content.append("    @Temporal(TemporalType.TIMESTAMP)\n");
                }
                content.append("    private ").append(field.getFieldType().getType().getValue()).append(" ").append(field.getName()).append(";\n");
            } else {
                content.append("    @JoinColumn(name = \"").append(camelToSnake(field.getName())).append("\", referencedColumnName = \"id\")\n")
                        .append("    @ManyToOne\n");
                content.append("    private ").append(field.getFieldType().getType().getValue()).append(" ").append(field.getName()).append(";\n");
            }
        });

        content.append("\n\n");

        content.append("     @Override\n" +
                "    public boolean isAuditable() {\n");
        if (entity.isAuditable()) {
            content.append("        return true;\n");
        } else {
            content.append("        return false;\n");
        }
        content.append("    }");

        entity.getEntityFieldDefinitionList().forEach(field -> {
            String firstCharFieldName = field.getName().substring(0, 1);
            String upperCaseCharFieldName = field.getName().replaceFirst(firstCharFieldName, firstCharFieldName.toUpperCase());

            if (field.getFieldType().getType().getValue().toLowerCase().contains("DropDown".toLowerCase())
                    || field.getFieldType().getType().getValue().toLowerCase().contains(ComponentTypes.RADIO_BUTTON.getValue().toLowerCase())) {
                content.append("\n")
                        .append("    public Long get").append(upperCaseCharFieldName).append("() {\n")
                        .append("        return ").append(field.getName()).append(";\n")
                        .append("    }\n")
                        .append("\n")
                        .append("    public void set").append(upperCaseCharFieldName).append("(").append("Long ").append(field.getName()).append(") {\n")
                        .append("       this.").append(field.getName()).append(" = ").append(field.getName()).append(";\n")
                        .append("    }\n\n");
            } else {

                content.append("\n")
                        .append("    public ").append(field.getFieldType().getType().getValue()).append(" get").append(upperCaseCharFieldName).append("() {\n")
                        .append("        return ").append(field.getName()).append(";\n")
                        .append("    }\n")
                        .append("\n")
                        .append("    public void set").append(upperCaseCharFieldName).append("(").append(field.getFieldType().getType().getValue()).append(" ").append(field.getName()).append(") {\n")
                        .append("       this.").append(field.getName()).append(" = ").append(field.getName()).append(";\n")
                        .append("    }\n\n");
            }
        });

        content.append("\n");

        content.append("    @Override\n")
                .

                        append("    public boolean equals(Object o) {\n")
                .

                        append("        if (this == o) return true;\n")
                .

                        append("        if (!(o instanceof #Entity)) return false;\n")
                .

                        append("        #Entity #entity = (#Entity) o;\n")
                .

                        append("        return Objects.equals(id, #entity.id);\n")
                .

                        append("    }\n")
                .

                        append("\n")
                .

                        append("    @Override\n")
                .

                        append("    public int hashCode() {\n")
                .

                        append("\n")
                .

                        append("        return Objects.hash(id);\n")
                .

                        append("    }\n")
                .

                        append("\n")
                .

                        append("    @Override\n")
                .

                        append("    public String toString() {\n")
                .

                        append("        return \"#Entity{\" +\n")
                .

                        append("                \"id=\" + id +\n")
                .

                        append("                '}';\n")
                .

                        append("    }\n")
                .

                        append("}");

        String firstChar = entity.getName().substring(0, 1);
        String entityInstanceName = entity.getName().replaceFirst(firstChar, firstChar.toLowerCase());
        String result = content.toString();
        result = result.replaceAll("#package", basePackage)
                .

                        replaceAll("#Entity", entity.getName())
                .

                        replaceAll("#entity", entityInstanceName);

        System.out.printf(result);
        try (
                PrintStream out = new PrintStream(new FileOutputStream(targetPath + "/" + entity.getName() + ".java"))) {
            out.print(result);
        }
        return result;
    }

//    private static boolean findFieldNullability(String entityName, String fieldName) {
//        try {
//            String s = InitializrReaderUtility.getResourceProperity(entityName + ".field.nullable." + fieldName);
//            if(s.equalsIgnoreCase("false"))
//                return false;
//            return true;
//        } catch (Exception e) {
//            return true;
//        }
//    }

//    private static String findFieldLength(String entityName, String fieldName) {
//        try {
//            String s = InitializrReaderUtility.getResourceProperity(entityName + ".field.length." + fieldName);
//            if(Integer.parseInt(s) > 0){
//                return s;
//            }
//            String type = InitializrReaderUtility.getResourceProperity(entityName + ".field.type." + fieldName);
//            if(type.equalsIgnoreCase("Integer")
//                    || type.equalsIgnoreCase("int")
//                    || type.equalsIgnoreCase("double")
//                    || type.equalsIgnoreCase("long")
//                    || type.equalsIgnoreCase("float")
//                    || type.equalsIgnoreCase("byte")) {
//                return "10";
//            }
//            if(type.equalsIgnoreCase("String")
//                    || type.equalsIgnoreCase("char")) {
//                return "100";
//            }
//        } catch (Exception e) {
//            return null;
//        }
//        return "10";
//    }

    private static String generateRunnerClass(String path, String basePackage, String projectName) throws FileNotFoundException {
        String content = "package #package;\n" +
                "\n" +
                "import org.springframework.boot.SpringApplication;\n" +
                "import org.springframework.boot.autoconfigure.SpringBootApplication;\n" +
                "import #package.common.FileStorageProperties;\n" +
                "import org.springframework.boot.context.properties.EnableConfigurationProperties;\n" +
                "import org.springframework.boot.autoconfigure.domain.EntityScan;\n" +
                "\n" +
                "@SpringBootApplication\n" +
                "@EnableConfigurationProperties({\n" +
                "\t\tFileStorageProperties.class\n" +
                "})\n" +
                "@EntityScan({\"" + basePackage + "\", \"com.caracal\"})\n" +
                "public class #projNameApplication {\n" +
                "\n" +
                "\tpublic static void main(String[] args) {\n" +
                "\t\tSpringApplication.run(#projNameApplication.class, args);\n" +
                "\t}\n" +
                "}\n";

        String camelCaseProjectName = CaracalGenerator.snakeToCamel(projectName.replaceAll("-", "_"));
        String result = content.replaceAll("#package", basePackage);
        result = result.replaceAll("#projName", camelCaseProjectName);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();


        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/" + camelCaseProjectName + "Application.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateAttachmentDao(String basePackage, String path) throws FileNotFoundException {
        StringBuilder content = new StringBuilder("");
        content.append("package #package.dao;\n" +
                "\n" +
                "import com.caracal.data.impl.AbstractDAOImpl;\n" +
                "import #package.model.Attachment;\n" +
                "import org.springframework.stereotype.Repository;\n" +
                "import org.springframework.security.core.context.SecurityContextHolder;\n" +
                "import java.util.List;\n" +
                "\n" +
                "\n" +
                "/* Generated By Caracal Generator ( Powered by Dr.Adldoost :D ) */\n" +
                "\n" +
                "@Repository\n" +
                "public class AttachmentDao extends AbstractDAOImpl<Attachment, Long> {\n" +
                "\n" +
                "    public AttachmentDao() {\n" +
                "        super(Attachment.class);\n" +
                "    }\n" +
                "    \n" +
                "    public void setAttachmentRecordId(List<Long> attachmentIdList, Long recordId) {\n" +
                "\n" +
                "        if(attachmentIdList == null || attachmentIdList.isEmpty())\n" +
                "            return;\n" +
                "        getEntityManager().createQuery(\"update Attachment at set at.relatedRecordId = :recordId \" +\n" +
                "                \"where at.id in :idList\")\n" +
                "                .setParameter(\"recordId\", recordId)\n" +
                "                .setParameter(\"idList\", attachmentIdList)\n" +
                "                .executeUpdate();\n" +
                "    }" +
                "\n" +
                "    @Override\n" +
                "    public String getCurrentUser() {\n" +
                "        return SecurityContextHolder.getContext().getAuthentication().getName();\n" +
                "    }" +
                "}");

        String result = content.toString().replaceAll("#package", basePackage);

        System.out.println(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/AttachmentDao.java"))) {
            out.print(result);
        }
        return result;
    }

    private static void generateAuditHistoryDao() {
        StringBuilder content = new StringBuilder("package #package.dao;\n" +
                "\n" +
                "import com.caracal.data.audit.GeneralAuditHistoryDao;\n" +
                "import org.springframework.stereotype.Repository;\n" +
                "\n" +
                "@Repository\n" +
                "public class AuditHistoryDao extends GeneralAuditHistoryDao {\n" +
                "\n" +
                "    public AuditHistoryDao() {\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "}\n");
    }

    private static void generateGeneralCaracalDaoImpl(String basePackage, String targetPath) throws FileNotFoundException {

        String content = "package #package.dao;\n" +
                "\n" +
                "import com.caracal.data.api.DomainEntity;\n" +
                "import com.caracal.data.impl.AbstractDAOImpl;\n" +
                "import org.springframework.security.core.context.SecurityContextHolder;\n" +
                "\n" +
                "import java.io.Serializable;\n" +
                "\n" +
                "public abstract class GeneralCaracalDaoImpl<E extends DomainEntity, PK extends Serializable> extends AbstractDAOImpl<E, PK> {\n" +
                "\n" +
                "    public GeneralCaracalDaoImpl(Class<E> entityClass) {\n" +
                "        super(entityClass);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public String getCurrentUser() {\n" +
                "        try {\n" +
                "            return SecurityContextHolder.getContext().getAuthentication().getName();\n" +
                "        } catch (Exception e) {\n" +
                "            return \"Anonymous\";\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);

        try (PrintStream out = new PrintStream(new FileOutputStream(targetPath + "/GeneralCaracalDaoImpl.java"))) {
            out.print(result);
        }
    }

    private static void generateDao(String basePackage, String entityName, String targetPath) throws FileNotFoundException {
        String content =

                "package #package.dao;\n" +
                        "\n" +
                        "import #package.model.#Entity;\n" +
                        "import org.springframework.stereotype.Repository;\n" +
                        "import org.springframework.security.core.context.SecurityContextHolder;\n" +
                        "\n" +
                        "\n" +
                        "/* Generated By Caracal Generator ( Powered by Dr.Adldoost :D ) */\n" +
                        "\n" +
                        "@Repository\n" +
                        "public class #EntityDao extends GeneralCaracalDaoImpl<#Entity, Long> {\n" +
                        "\n" +
                        "    public #EntityDao() {\n" +
                        "        super(#Entity.class);\n" +
                        "    }\n" +
                        "}\n";

        String firstChar = entityName.substring(0, 1);
        String entityInstanceName = entityName.replaceFirst(firstChar, firstChar.toLowerCase());

        String result = content.replaceAll("#package", basePackage)
                .replaceAll("#Entity", entityName)
                .replaceAll("#entity", entityInstanceName);

        System.out.printf(result);

        try (PrintStream out = new PrintStream(new FileOutputStream(targetPath + "/" + entityName + "Dao.java"))) {
            out.print(result);
        }

    }

    private static String generateFileStorageService(String basePackage, String path) throws FileNotFoundException {
        StringBuilder content = new StringBuilder("");
        content.append("package #package.service;\n" +
                "\n" +
                "import #package.common.BusinessExceptionCode;\n" +
                "import #package.common.ErrorCodeReaderUtil;\n" +
                "import #package.common.FileStorageProperties;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.beans.factory.annotation.Value;\n" +
                "import org.springframework.core.io.Resource;\n" +
                "import org.springframework.core.io.UrlResource;\n" +
                "import org.springframework.stereotype.Service;\n" +
                "import org.springframework.util.StringUtils;\n" +
                "import org.springframework.web.multipart.MultipartFile;\n" +
                "\n" +
                "import java.io.File;\n" +
                "import java.io.IOException;\n" +
                "import java.io.InputStream;\n" +
                "import java.net.MalformedURLException;\n" +
                "import java.nio.file.Files;\n" +
                "import java.nio.file.Path;\n" +
                "import java.nio.file.Paths;\n" +
                "import java.nio.file.StandardCopyOption;\n" +
                "import java.text.MessageFormat;\n" +
                "import java.text.SimpleDateFormat;\n" +
                "import java.util.Date;\n" +
                "import java.util.UUID;\n" +
                "\n" +
                "@Service\n" +
                "public class FileStorageService {\n" +
                "\n" +
                "    @Value(\"${file.upload-dir}\")\n" +
                "    String uploadBaseUrl;\n" +
                "\n" +
                "    @Autowired\n" +
                "    FileStorageProperties storageProperties;\n" +
                "\n" +
                "    public String storeFile(MultipartFile file) {\n" +
                "        // Normalize file name\n" +
                "        if(file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) {\n" +
                "            throw new RuntimeException(BusinessExceptionCode.BAD_INPUT.name());\n" +
                "        }\n" +
                "        String fileName = StringUtils.cleanPath(file.getOriginalFilename());\n" +
                "        System.out.println(file.getContentType());\n" +
                "\n" +
                "        try {\n" +
                "//            UUID uuid = UUID.randomUUID();\n" +
                "//            String randomName = uuid.toString() + fileName.substring(fileName.length()-5);\n" +
                "            Path uploadLocation = Paths.get(storageProperties.getUploadDir());\n" +
                "            String fileSafeName = new Date().getTime() + file.getOriginalFilename();\n" +
                "            Path targetLocation = Paths.get(storageProperties.getUploadDir() + fileSafeName);\n" +
                "            try {\n" +
                "                if(!Files.exists(uploadLocation)) {\n" +
                "                    Files.createDirectories(uploadLocation);\n" +
                "                }\n" +
                "            } catch (Exception ex) {\n" +
                "\n" +
                "                throw new RuntimeException(BusinessExceptionCode.COULD_NOT_CREATE_DIRECTORY.name(),ex);\n" +
                "            }\n" +
                "            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);\n" +
                "            java.text.DateFormat dateFormat = new SimpleDateFormat(\"yyyy-MM-dd\");\n" +
                "            String dateString = dateFormat.format(new Date());\n" +
                "            return dateString + \"/\" + fileSafeName;\n" +
                "        } catch (IOException ex) {\n" +
                "            throw new RuntimeException(MessageFormat.format(ErrorCodeReaderUtil.getResourceProperity(BusinessExceptionCode.COULD_NOT_STORED_FILE_RETRY.name()),fileName));\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    public String storeFile(String originalFilename, InputStream inputStream) {\n" +
                "        // Normalize file name\n" +
                "        String fileName = StringUtils.cleanPath(originalFilename);\n" +
                "//        System.out.println(file.getContentType());\n" +
                "\n" +
                "        try {\n" +
                "            // Check if the file's name contains invalid characters\n" +
                "            //            if(fileName.contains(\"..\")) {\n" +
                "            //                throw new RuntimeException(\"Sorry! Filename contains invalid path sequence \" + fileName);\n" +
                "            //            }\n" +
                "            // Copy file to the target location (Replacing existing file with the same name)\n" +
                "\n" +
                "            UUID uuid = UUID.randomUUID();\n" +
                "            String randomName = uuid.toString() + fileName.substring(fileName.length() - 5, fileName.length());\n" +
                "            Path uploadLocation = Paths.get(storageProperties.getUploadDir());\n" +
                "            Path targetLocation = Paths.get(storageProperties.getUploadDir() + randomName);\n" +
                "            try {\n" +
                "                if (!Files.exists(uploadLocation)) {\n" +
                "                    Files.createDirectories(uploadLocation);\n" +
                "                }\n" +
                "            } catch (Exception ex) {\n" +
                "                throw new RuntimeException(BusinessExceptionCode.COULD_NOT_CREATE_DIRECTORY.name(),ex);\n" +
                "//                throw new RuntimeException(\"Could not create the directory where the uploaded files will be stored.\", ex);\n" +
                "            }\n" +
                "            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);\n" +
                "            java.text.DateFormat dateFormat = new SimpleDateFormat(\"yyyy-MM-dd\");\n" +
                "            String dateString = dateFormat.format(new Date());\n" +
                "            return dateString + \"/\" + randomName;\n" +
                "        } catch (IOException ex) {\n" +
                "            throw  new RuntimeException(BusinessExceptionCode.COULD_NOT_STORED_FILE_RETRY.name(),ex);\n" +
                "//            throw new RuntimeException(\"Could not store file \" + fileName + \". Please try again!\", ex);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    public Resource loadFileAsResource(String relativePath) {\n" +
                "        try {\n" +
                "            if(!(uploadBaseUrl.endsWith(\"/\") || uploadBaseUrl.endsWith(\"\\\\\"))) {\n" +
                "                uploadBaseUrl += \"/\";\n" +
                "            }\n" +
                "            Path filePath = Paths.get(uploadBaseUrl + relativePath);\n" +
                "            Resource resource = new UrlResource(filePath.toUri());\n" +
                "            if(resource.exists()) {\n" +
                "                return resource;\n" +
                "            } else {\n" +
                "                throw new RuntimeException(MessageFormat.format(BusinessExceptionCode.FILE_NOT_FOUND_PATH.name(),relativePath));\n" +
                "//                throw new RuntimeException(\"File not found \" + relativePath);\n" +
                "            }\n" +
                "        } catch (MalformedURLException ex) {\n" +
                "            throw new RuntimeException(MessageFormat.format(BusinessExceptionCode.FILE_NOT_FOUND_PATH.name(),relativePath),ex);\n" +
                "            //                throw new RuntimeException(\"File not found \" + relativePath, ex);\n" +
                "\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    public File loadFileAsFile(String fileName) {\n" +
                "//        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();\n" +
                "        return new File(uploadBaseUrl +  \"/\" + fileName);\n" +
                "    }\n" +
                "}");
        String result = content.toString().replaceAll("#package", basePackage);

        System.out.println(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/FileStorageService.java"))) {
            out.print(result);
        }
        return result;

    }

    private static String generateAttachmentService(String basePackage, String path) throws FileNotFoundException {
        StringBuilder content = new StringBuilder("");
        content.append("package #package.service;\n" +
                "\n" +
                "import com.caracal.data.api.GenericEntityDAO;\n" +
                "import com.google.common.hash.HashCode;\n" +
                "import com.google.common.hash.Hashing;\n" +
                "import com.google.common.io.Files;\n" +
                "import #package.dao.AttachmentDao;\n" +
                "import #package.dto.AttachmentDto;\n" +
                "import #package.dto.DownloadAttachmentDto;\n" +
                "import #package.model.Attachment;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.stereotype.Service;\n" +
                "import org.springframework.web.multipart.MultipartFile;\n" +
                "import org.springframework.core.io.Resource;\n" +
                "\n" +
                "import javax.transaction.Transactional;\n" +
                "import java.io.File;\n" +
                "import java.io.IOException;\n" +
                "import java.util.List;\n" +
                "\n" +
                "@Service\n" +
                "public class AttachmentService extends GeneralServiceImpl<AttachmentDto, Attachment, Long> {\n" +
                "\n" +
                "\n" +
                "    private final AttachmentDao attachmentDao;\n" +
                "    private final FileStorageService fileStorageService;\n" +
                "\n" +
                "    @Autowired\n" +
                "    public AttachmentService(AttachmentDao attachmentDao, FileStorageService fileStorageService) {\n" +
                "        this.attachmentDao = attachmentDao;\n" +
                "        this.fileStorageService = fileStorageService;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected GenericEntityDAO getDAO() {\n" +
                "        return attachmentDao;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public AttachmentDto getDtoInstance() {\n" +
                "        return new AttachmentDto();\n" +
                "    }\n" +
                "\n" +
                "    @Transactional\n" +
                "    public DownloadAttachmentDto uploadFile(MultipartFile file, String entity, String description) throws IOException {\n" +
                "        String path = fileStorageService.storeFile(file);\n" +
                "        File savedFile = fileStorageService.loadFileAsFile(path);\n" +
                "        Attachment attachment = new Attachment();\n" +
                "        attachment.setName(file.getOriginalFilename());\n" +
                "        attachment.setDescription(description);\n" +
                "        HashCode hashCode = Files.hash(savedFile, Hashing.md5());\n" +
                "        attachment.setFileMd5(hashCode.toString());\n" +
                "        attachment.setRelatedEntity(entity);\n" +
                "        attachment.setUploadPath(path);\n" +
                "        attachment = attachmentDao.save(attachment);\n" +
                "\n" +
                "        DownloadAttachmentDto dto = new DownloadAttachmentDto();\n" +
                "        dto.setDownloadPath(attachment.getUploadPath());\n" +
                "        dto.setFileName(attachment.getName());\n" +
                "        dto.setId(attachment.getId());\n" +
                "        return dto;\n" +
                "    }\n" +
                "   public Resource downloadFile(String url) {\n" +
                "        return fileStorageService.loadFileAsResource(url);\n" +
                "    }\n" +
                "    \n" +
                "    @Transactional\n" +
                "    public void updateAttachmentRecordId(List<Long> attachmentIdList, Long recordId) {\n" +
                "\n" +
                "        attachmentDao.setAttachmentRecordId(attachmentIdList, recordId);\n" +
                "    }" +
                "}\n");

        String result = content.toString().replaceAll("#package", basePackage);

        System.out.println(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/AttachmentService.java"))) {
            out.print(result);
        }
        return result;

    }

    private static void generateService(String basePackage, EntityDefinition entity, String targetPath) throws FileNotFoundException {
        StringBuilder content = new StringBuilder(
                "package #package.service;\n" +
                        "\n" +
                        "import com.caracal.data.api.GenericEntityDAO;\n" +
                        "import #package.dao.#EntityDao;\n" +
                        "import #package.dto.#EntityDto;\n" +
                        "import #package.model.#Entity;\n" +
                        "import org.springframework.beans.factory.annotation.Autowired;\n" +
                        "import org.springframework.stereotype.Service;\n" +
                        "import #package.common.ValidationType;\n" +
                        "import #package.common.BusinessExceptionCode;\n" +
                        "import #package.common.ValidationException;\n" +
                        "\n" +
                        "@Service\n" +
                        "public class #EntityService extends GeneralServiceImpl<#EntityDto, #Entity, Long> {\n" +
                        "\n" +
                        "\n" +
                        "    private final #EntityDao #entityDao;\n" +
                        "\n" +
                        "    @Autowired\n" +
                        "    public #EntityService(#EntityDao #entityDao) {\n" +
                        "        this.#entityDao = #entityDao;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    protected GenericEntityDAO getDAO() {\n" +
                        "        return #entityDao;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public #EntityDto getDtoInstance() {\n" +
                        "        return new #EntityDto();\n" +
                        "    }\n" +
                        "    public void validateBeforeSave(#EntityDto " + entity.getName().toLowerCase() + "Dto) {\n");
        entity.getEntityFieldDefinitionList().forEach(field -> {
            if (field.getNullable() == false) {
                String firstCharFieldName = field.getName().substring(0, 1);
                String upperCaseCharFieldName = field.getName().replaceFirst(firstCharFieldName, firstCharFieldName.toUpperCase());
                content.append("        if(" + entity.getName().toLowerCase() + "Dto.get" + upperCaseCharFieldName + "() == null) {\n" +
                        "            throw new ValidationException(BusinessExceptionCode.IS_MANDETORY.name(), \"" + field.getName() + "\", \"" + entity.getName() + "\", ValidationType.IS_MANDATORY);\n" +
                        "        }\n");
            }
        });
        content.append("    }\n");
        content.append("}");

        String firstChar = entity.getName().substring(0, 1);
        String entityInstanceName = entity.getName().replaceFirst(firstChar, firstChar.toLowerCase());

        String result = content.toString().replaceAll("#package", basePackage)
                .replaceAll("#Entity", entity.getName())
                .replaceAll("#entity", entityInstanceName);

        System.out.printf(result);

        try (PrintStream out = new PrintStream(new FileOutputStream(targetPath + "/" + entity.getName() + "Service.java"))) {
            out.print(result);
        }

    }

    private static String generateDto(String basePackage, EntityDefinition entity, String targetPath, List<String> entityNamesList) throws FileNotFoundException {
        String result = generateDtoHeader();
        result += generateDtoFields(entity.getEntityFieldDefinitionList(), entity.isEnableValidation(), entityNamesList);
        result += generateDtoMappings(entity.getEntityFieldDefinitionList(), entity.getName(), entityNamesList);
        result += generateDtoOverrideMethods();
        result += generateFooter();

        String firstChar = entity.getName().substring(0, 1);
        String entityInstanceName = entity.getName().replaceFirst(firstChar, firstChar.toLowerCase());
        result = result.replaceAll("#package", basePackage)
                .replaceAll("#Entity", entity.getName())
                .replaceAll("#entity", entityInstanceName);

        System.out.printf(result);
        try (PrintStream out = new PrintStream(new FileOutputStream(targetPath + "/" + entity.getName() + "Dto.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateDownloadAttachmentDto(String basePackage, String targetPath) throws FileNotFoundException {
        StringBuilder content = new StringBuilder("");
        content.append("package #package.dto;\n" +
                "\n" +
                "import com.fasterxml.jackson.annotation.JsonIgnoreProperties;\n" +
                "\n" +
                "import java.io.Serializable;\n" +
                "\n" +
                "@JsonIgnoreProperties(ignoreUnknown = true)\n" +
                "public class DownloadAttachmentDto implements Serializable {\n" +
                "\n" +
                "    private Long id;\n" +
                "    private String downloadPath;\n" +
                "    private String fileName;\n" +
                "    private String description;\n" +
                "\n" +
                "    public Long getId() {\n" +
                "        return id;\n" +
                "    }\n" +
                "\n" +
                "    public void setId(Long id) {\n" +
                "        this.id = id;\n" +
                "    }\n" +
                "\n" +
                "    public String getDownloadPath() {\n" +
                "        return downloadPath;\n" +
                "    }\n" +
                "\n" +
                "    public void setDownloadPath(String downloadPath) {\n" +
                "        this.downloadPath = downloadPath;\n" +
                "    }\n" +
                "\n" +
                "    public String getFileName() {\n" +
                "        return fileName;\n" +
                "    }\n" +
                "\n" +
                "    public void setFileName(String fileName) {\n" +
                "        this.fileName = fileName;\n" +
                "    }\n" +
                "\n" +
                "    public String getDescription() {\n" +
                "        return description;\n" +
                "    }\n" +
                "\n" +
                "    public void setDescription(String description) {\n" +
                "        this.description = description;\n" +
                "    }" +
                "}\n");

        String result = content.toString().replaceAll("#package", basePackage);

        System.out.println(result);
        File file = new File(targetPath);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(targetPath + "/DownloadAttachmentDto.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateAttachmentDto(String basePackage, String path) throws FileNotFoundException {
        StringBuilder content = new StringBuilder("");
        content.append("package #package.dto;\n" +
                "\n" +
                "import com.caracal.data.api.DomainDto;\n" +
                "import com.fasterxml.jackson.annotation.JsonIgnore;\n" +
                "import #package.model.Attachment;\n" +
                "import javax.validation.constraints.NotNull;\n" +
                "import javax.validation.constraints.NotEmpty;\n" +
                "import java.util.Date;\n" +
                "\n" +
                "\n" +
                "/* Generated By Caracal Generator ( Powered by Dr.Adldoost :D ) */\n" +
                "\n" +
                "public class AttachmentDto implements DomainDto<Attachment, AttachmentDto> {\n" +
                "\n" +
                "\n" +
                "    private Long id;\n" +
                "    private String name;\n" +
                "    private String uploadPath;\n" +
                "    private String description;\n" +
                "    private String relatedEntity;\n" +
                "    private Long relatedRecordId;\n" +
                "    private String fileMd5;\n" +
                " \n" +
                "\n" +
                "    public Long getId() {\n" +
                "        return id;\n" +
                "    }\n" +
                "    public void setId(Long id) {\n" +
                "        this.id = id;\n" +
                "    }\n" +
                "\n" +
                "    public String getName() {\n" +
                "        return name;\n" +
                "    }\n" +
                "    public void setName(String name) {\n" +
                "        this.name = name;\n" +
                "    }\n" +
                "\n" +
                "    public String getUploadPath() {\n" +
                "        return uploadPath;\n" +
                "    }\n" +
                "    public void setUploadPath(String uploadPath) {\n" +
                "        this.uploadPath = uploadPath;\n" +
                "    }\n" +
                "\n" +
                "    public String getDescription() {\n" +
                "        return description;\n" +
                "    }\n" +
                "    public void setDescription(String description) {\n" +
                "        this.description = description;\n" +
                "    }\n" +
                "\n" +
                "    public String getRelatedEntity() {\n" +
                "        return relatedEntity;\n" +
                "    }\n" +
                "    public void setRelatedEntity(String relatedEntity) {\n" +
                "        this.relatedEntity = relatedEntity;\n" +
                "    }\n" +
                "\n" +
                "    public Long getRelatedRecordId() {\n" +
                "        return relatedRecordId;\n" +
                "    }\n" +
                "    public void setRelatedRecordId(Long relatedRecordId) {\n" +
                "        this.relatedRecordId = relatedRecordId;\n" +
                "    }\n" +
                "\n" +
                "    public String getFileMd5() {\n" +
                "        return fileMd5;\n" +
                "    }\n" +
                "    public void setFileMd5(String fileMd5) {\n" +
                "        this.fileMd5 = fileMd5;\n" +
                "    }\n" +
                "\n" +
                "\n" +
                "\n" +
                "    public static AttachmentDto toDto(Attachment attachment) {\n" +
                "\n" +
                "        if(attachment == null)\n" +
                "            return null; \n" +
                "        AttachmentDto dto = new AttachmentDto();\n" +
                "        dto.setId(attachment.getId());\n" +
                "        dto.setName(attachment.getName());\n" +
                "        dto.setUploadPath(attachment.getUploadPath());\n" +
                "        dto.setDescription(attachment.getDescription());\n" +
                "        dto.setRelatedEntity(attachment.getRelatedEntity());\n" +
                "        dto.setRelatedRecordId(attachment.getRelatedRecordId());\n" +
                "        dto.setFileMd5(attachment.getFileMd5());\n" +
                "        return dto;\n" +
                "  }\n" +
                "\n" +
                "\n" +
                "    public static Attachment toEntity(AttachmentDto dto) {\n" +
                "\n" +
                "        if(dto == null)\n" +
                "            return null; \n" +
                "        Attachment attachment = new Attachment();\n" +
                "        attachment.setId(dto.getId());\n" +
                "        attachment.setName(dto.getName());\n" +
                "        attachment.setUploadPath(dto.getUploadPath());\n" +
                "        attachment.setDescription(dto.getDescription());\n" +
                "        attachment.setRelatedEntity(dto.getRelatedEntity());\n" +
                "        attachment.setRelatedRecordId(dto.getRelatedRecordId());\n" +
                "        attachment.setFileMd5(dto.getFileMd5());\n" +
                "        return attachment;\n" +
                "  }\n" +
                "    @Override\n" +
                "    public Attachment toEntity() {\n" +
                "        return AttachmentDto.toEntity(this);\n" +
                "    }\n" +
                "\n" +
                "    @JsonIgnore\n" +
                "    @Override\n" +
                "    public AttachmentDto getInstance(Attachment attachment) {\n" +
                "        return AttachmentDto.toDto(attachment);\n" +
                "    }\n" +
                "\n" +
                "    @JsonIgnore\n" +
                "    @Override\n" +
                "    public AttachmentDto getInstance() {\n" +
                "        return new AttachmentDto();\n" +
                "    }\n" +
                "}");

        String result = content.toString().replaceAll("#package", basePackage);

        System.out.println(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/AttachmentDto.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateDtoHeader() {

        String content = "package #package.dto;\n" +
                "\n" +
                "import com.caracal.data.api.DomainDto;\n" +
                "import com.fasterxml.jackson.annotation.JsonIgnore;\n" +
                "import #package.model.#Entity;\n" +
                "import javax.validation.constraints.NotNull;\n" +
                "import javax.validation.constraints.NotEmpty;\n" +
                "import java.util.Date;\n" +
                "\n" +
                "\n" +
                "/* Generated By Caracal Generator ( Powered by Dr.Adldoost :D ) */\n" +
                "\n" +
                "public class #EntityDto implements DomainDto<#Entity, #EntityDto> {";


        return content;

    }

    private static String generateDtoFields(List<EntityFieldDefinition> entityFieldDefinitionList, boolean validationEnabled, List<String> entityNames) {

        StringBuilder content = new StringBuilder();

        content.append("\n\n");

        entityFieldDefinitionList.forEach(field -> {
            if (getBaseTypes().contains(field.getFieldType().getType().getValue())) {
                if (validationEnabled) {
                    if (field.getNullable() != null && field.getNullable() != true) {
                        if (field.getFieldType().getType().getValue().trim().equalsIgnoreCase("String")) {
                            content.append("\n    @NotEmpty(message = \"{").append(field.getName()).append(".should.not.be.Empty}\")");
                        } else {
                            content.append("\n    @NotNull(message = \"{").append(field.getName()).append(".should.not.be.null}\")");
                        }
                    }
                }
                content.append("\n    private ").append(field.getFieldType().getType().getValue()).append(" ").append(field.getName()).append(";");
            } else if (field.getFieldType().getType().getValue().toLowerCase().contains(ComponentTypes.DROP_DOWN.getValue().toLowerCase())
                    || field.getFieldType().getType().getValue().toLowerCase().contains(ComponentTypes.RADIO_BUTTON.getValue().toLowerCase())) {
                content.append("\n    private Long").append(" ").append(field.getName()).append(";");
            } else {
                content.append("\n    private ").append(field.getFieldType().getType().getValue() + "Dto").append(" ").append(field.getName()).append(";");
            }
        });

        content.append("\n \n");

        entityFieldDefinitionList.forEach(field -> {
            String firstCharFieldName = field.getName().substring(0, 1);
            String upperCaseCharFieldName = field.getName().replaceFirst(firstCharFieldName, firstCharFieldName.toUpperCase());

            if (field.getFieldType().getType().getValue().toLowerCase().contains(ComponentTypes.DROP_DOWN.getValue().toLowerCase())) {
                content.append("\n")
                        .append("    public Long get").append(upperCaseCharFieldName).append("() {\n")
                        .append("        return ").append(field.getName()).append(";\n")
                        .append("    }\n")
                        .append("\n")
                        .append("    public void set").append(upperCaseCharFieldName).append("(").append("Long ").append(field.getName()).append(") {\n")
                        .append("       this.").append(field.getName()).append(" = ").append(field.getName()).append(";\n")
                        .append("    }\n\n");
            } else if (field.getFieldType().getType().getValue().toLowerCase().contains(ComponentTypes.RADIO_BUTTON.getValue().toLowerCase())) {
                content.append("\n")
                        .append("    public Long get").append(upperCaseCharFieldName).append("() {\n")
                        .append("        return ").append(field.getName()).append(";\n")
                        .append("    }\n")
                        .append("\n")
                        .append("    public void set").append(upperCaseCharFieldName).append("(").append("Long ").append(field.getName()).append(") {\n")
                        .append("       this.").append(field.getName()).append(" = ").append(field.getName()).append(";\n")
                        .append("    }\n\n");
            } else if (getBaseTypes().contains(field.getFieldType().getType().getValue())) {
                content.append("\n    public " + field.getFieldType().getType().getValue() + " get" + upperCaseCharFieldName + "() {\n" +
                        "        return " + field.getName() + ";\n" +
                        "    }\n" +

                        "    public void set" + upperCaseCharFieldName + "(" + field.getFieldType().getType().getValue() + " " + field.getName() + ") {\n" +
                        "        this." + field.getName() + " = " + field.getName() + ";\n" +
                        "    }" +
                        "\n");

            } else if (entityNames.contains(field.getFieldType().getType().getValue())) {

                content.append("\n    public " + field.getFieldType().getType().getValue() + "Dto" + " get" + upperCaseCharFieldName + "() {\n" +
                        "        return " + field.getName() + ";\n" +
                        "    }\n" +

                        "    public void set" + upperCaseCharFieldName + "(" + field.getFieldType().getType().getValue() + "Dto" + " " + field.getName() + ") {\n" +
                        "        this." + field.getName() + " = " + field.getName() + ";\n" +
                        "    }" +
                        "\n");
            } else {
                System.err.println("generateDtoFields getter setter : Unkown field type : " + field.getFieldType().getType().getValue());
            }

        });

        String result = content.toString();
        return result;

    }

    private static String generateDtoMappings(List<EntityFieldDefinition> entityFieldDefinitionList, String entityName, List<String> entityNames) {

        String firstChar = entityName.substring(0, 1);
        String entityInstanceName = entityName.replaceFirst(firstChar, firstChar.toLowerCase());
        StringBuilder content = new StringBuilder();

        content.append("\n\n");


        //toDto Method

        content.append("\n    public static #EntityDto toDto(#Entity #entity) {\n\n" +
                "        if(#entity == null)\n" +
                "            return null; \n" +
                "        #EntityDto dto = new #EntityDto();");


        entityFieldDefinitionList.forEach(field -> {

            String fieldType = field.getFieldType().getType().getValue();

            String firstCharFieldName = field.getName().substring(0, 1);
            String upperCaseCharFieldName = field.getName().replaceFirst(firstCharFieldName, firstCharFieldName.toUpperCase());
            if (fieldType.toLowerCase().contains(ComponentTypes.DROP_DOWN.getValue().toLowerCase())) {
                content.append("\n        dto.set").append(upperCaseCharFieldName).append("(").append(entityInstanceName).append(".get").append(upperCaseCharFieldName).append("()").append(");");
            } else if (fieldType.toLowerCase().contains(ComponentTypes.RADIO_BUTTON.getValue().toLowerCase())) {
                content.append("\n        dto.set").append(upperCaseCharFieldName).append("(").append(entityInstanceName).append(".get").append(upperCaseCharFieldName).append("()").append(");");
            } else if (getBaseTypes().contains(fieldType)) {
                content.append("\n        dto.set").append(upperCaseCharFieldName).append("(").append(entityInstanceName).append(".get").append(upperCaseCharFieldName).append("()").append(");");
            } else if (entityNames.contains(fieldType)) {
                content.append("\n        dto.set").append(upperCaseCharFieldName).append("(")
                        .append(fieldType).append("Dto.toDto(")
                        .append(entityInstanceName).append(".get").append(upperCaseCharFieldName).append("()")
                        .append(")")
                        .append(");");
            } else {
                System.err.println("generateDtoMappings getter setter : Unkown field type : " + fieldType);
            }
        });


        content.append("\n        return dto;");
        content.append("\n  }");
        content.append("\n\n");

        //toEntity method
        content.append("\n    public static #Entity toEntity(#EntityDto dto) {\n\n" +
                "        if(dto == null)\n" +
                "            return null; \n" +
                "        #Entity #entity = new #Entity();");

        entityFieldDefinitionList.forEach(field -> {
            String fieldType = field.getFieldType().getType().getValue();

            String firstCharFieldName = field.getName().substring(0, 1);
            String upperCaseCharFieldName = field.getName().replaceFirst(firstCharFieldName, firstCharFieldName.toUpperCase());
            if (getBaseTypes().contains(fieldType)) {
                content.append("\n        #entity.set").append(upperCaseCharFieldName).append("(").append("dto").append(".get").append(upperCaseCharFieldName).append("()").append(");");
            } else if (fieldType.toLowerCase().contains(ComponentTypes.DROP_DOWN.getValue().toLowerCase())) {
                content.append("\n        #entity.set").append(upperCaseCharFieldName).append("(").append("dto").append(".get").append(upperCaseCharFieldName).append("()").append(");");
            } else if (fieldType.toLowerCase().contains(ComponentTypes.RADIO_BUTTON.getValue().toLowerCase())) {
                content.append("\n        #entity.set").append(upperCaseCharFieldName).append("(").append("dto").append(".get").append(upperCaseCharFieldName).append("()").append(");");
            } else if (entityNames.contains(fieldType)) {
                content.append("\n        #entity.set").append(upperCaseCharFieldName).append("(")
                        .append(fieldType).append("Dto.toEntity(")
                        .append("dto").append(".get").append(upperCaseCharFieldName).append("()")
                        .append(")")
                        .append(");");
            } else {
                System.err.println("generateDtoMappings converters: Unkown field type : " + fieldType);
            }
        });

        content.append("\n        return #entity;");
        content.append("\n  }");

        return content.toString();

    }

    private static String generateDtoOverrideMethods() {
        String content = "\n    @Override\n" +
                "    public #Entity toEntity() {\n" +
                "        return #EntityDto.toEntity(this);\n" +
                "    }\n" +
                "\n" +
                "    @JsonIgnore\n" +
                "    @Override\n" +
                "    public #EntityDto getInstance(#Entity #entity) {\n" +
                "        return #EntityDto.toDto(#entity);\n" +
                "    }\n" +
                "\n" +
                "    @JsonIgnore\n" +
                "    @Override\n" +
                "    public #EntityDto getInstance() {\n" +
                "        return new #EntityDto();\n" +
                "    }";
        return content;

    }

    private static String generateAttachmentRestService(String basePackage, String path) throws FileNotFoundException {
        StringBuilder content = new StringBuilder("");
        content.append("package #package.rest;\n" +
                "\n" +
                "import com.caracal.data.SortUtil;\n" +
                "import com.caracal.data.api.qbe.SortObject;\n" +
                "import com.caracal.data.api.qbe.StringSearchType;\n" +
                "import #package.dto.AttachmentDto;\n" +
                "import #package.dto.DownloadAttachmentDto;\n" +
                "import #package.service.AttachmentService;\n" +
                "import #package.service.PagedResult;\n" +
                "import jxl.read.biff.BiffException;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.http.MediaType;\n" +
                "import org.springframework.security.access.prepost.PreAuthorize;\n" +
                "import org.springframework.web.bind.annotation.*;\n" +
                "import org.springframework.web.multipart.MultipartFile;\n" +
                "import org.springframework.http.ResponseEntity;\n" +
                "import org.springframework.core.io.Resource;\n" +
                "import org.springframework.http.HttpHeaders;\n" +
                "import org.apache.catalina.servlet4preview.http.HttpServletRequest;\n" +
                "import #package.common.BusinessExceptionCode;\n" +
                "\n" +
                "import java.io.IOException;\n" +
                "import java.util.Collections;\n" +
                "import java.util.List;\n" +
                "\n" +
                "\n" +
                "/* Generated By Caracal Generator ( Powered by Dr.Adldoost :D ) */\n" +
                "\n" +
                "@RestController\n" +
                "@RequestMapping(\"/attachment\")\n" +
                "public class AttachmentRestService {\n" +
                "\n" +
                "    private final AttachmentService attachmentService;\n" +
                "\n" +
                "    @Autowired\n" +
                "    public AttachmentRestService(AttachmentService attachmentService) {\n" +
                "        this.attachmentService = attachmentService;\n" +
                "    }\n" +
                "\n" +
                "    @PreAuthorize(\"hasAuthority('AUTHORITY_FIND_ATTACHMENT')\")\n" +
                "    @GetMapping(\"/{id}\")\n" +
                "    public AttachmentDto findById(@PathVariable(name = \"id\")Long id) {\n" +
                "        return attachmentService.findByPrimaryKey(id);\n" +
                "    }\n" +
                "\n" +
                "     @PreAuthorize(\"hasAuthority('AUTHORITY_SEARCH_ATTACHMENT')\")\n" +
                "    @GetMapping(\"/search\")\n" +
                "    public PagedResult search(\n" +
                "                                      @RequestParam(value = \"id\", required = false) Long id,\n" +
                "                                      @RequestParam(value = \"name\", required = false) String name,\n" +
                "                                      @RequestParam(value = \"uploadPath\", required = false) String uploadPath,\n" +
                "                                      @RequestParam(value = \"description\", required = false) String description,\n" +
                "                                      @RequestParam(value = \"relatedEntity\", required = false) String relatedEntity,\n" +
                "                                      @RequestParam(value = \"relatedRecordId\", required = false) Long relatedRecordId,\n" +
                "                                      @RequestParam(value = \"fileMd5\", required = false) String fileMd5,\n" +
                "                                      @RequestParam(value = \"firstIndex\", required = false) Integer firstIndex,\n" +
                "                                      @RequestParam(value = \"pageSize\", required = false) Integer pageSize,\n" +
                "                                      @RequestParam(value = \"sortField\", required = false) String sortField,\n" +
                "                                      @RequestParam(value = \"sortOrder\", required = false) String sortOrder) {\n" +
                "\n" +
                "            SortObject sortObject = SortUtil.generateSortObject(sortField, sortOrder);\n" +
                "            List<SortObject> sortObjectList = null;\n" +
                "            if(sortObject != null)\n" +
                "               sortObjectList = Collections.singletonList(sortObject);\n" +
                "\n" +
                "            if(firstIndex == null)\n" +
                "               firstIndex = 0;\n" +
                "            if(pageSize == null)\n" +
                "               pageSize = Integer.MAX_VALUE;\n" +
                "            AttachmentDto attachment = new AttachmentDto();\n" +
                "            attachment.setId(id); \n" +
                "            attachment.setName(name); \n" +
                "            attachment.setUploadPath(uploadPath); \n" +
                "            attachment.setDescription(description); \n" +
                "            attachment.setRelatedEntity(relatedEntity); \n" +
                "            attachment.setRelatedRecordId(relatedRecordId); \n" +
                "            attachment.setFileMd5(fileMd5); \n" +
                "\n" +
                "            return attachmentService.findPagedByExample(attachment,\n" +
                "                   sortObjectList,\n" +
                "                   firstIndex,\n" +
                "                   pageSize,\n" +
                "                   StringSearchType.EXPAND_BOTH_SIDES,\n" +
                "                   null,\n" +
                "                   null\n" +
                "                   );\n" +
                "    }\n" +
                "\n" +
                "\n" +
                "//    @PreAuthorize(\"hasAuthority('AUTHORITY_SAVE_ATTACHMENT')\")\n" +
                "//    @PostMapping(path = \"/save\")\n" +
                "//    public AttachmentDto save(@RequestBody AttachmentDto attachment) {\n" +
                "//        return attachmentService.save(attachment);\n" +
                "//    }\n" +
                "\n" +
                "    @PostMapping(value = \"/upload\", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)\n" +
                "    public DownloadAttachmentDto upload(@RequestParam(\"file\") MultipartFile file, @RequestParam(\"entity\") String entity,\n" +
                "                                        @RequestParam(value = \"description\", required = false)String description) throws IOException, BiffException {\n" +
                "\n" +
                "        return attachmentService.uploadFile(file, entity, description);\n" +
                "    }\n" +
                "\n" +
                "\n" +
                "    @PreAuthorize(\"hasAuthority('AUTHORITY_REMOVE_ATTACHMENT')\")\n" +
                "    @DeleteMapping(path = \"/delete/{id}\")\n" +
                "    public void remove(@PathVariable(name = \"id\")Long id) {\n" +
                "        attachmentService.remove(id);\n" +
                "    }\n" +
                "   \n" +
                "    @GetMapping(\"/download/{id}\")\n" +
                "    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, HttpServletRequest request) {\n" +
                "\n" +
                "\n" +
                "        AttachmentDto attachmentDto = attachmentService.findByPrimaryKey(id);\n" +
                "        if(attachmentDto == null)\n" +
                "            return ResponseEntity.notFound().build();\n" +
                "        Resource resource = attachmentService.downloadFile(attachmentDto.getUploadPath());\n" +
                "\n" +
                "        String contentType = null;\n" +
                "        try {\n" +
                "            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());\n" +
                "        } catch (IOException ex) {\n" +
                "//            logger.info(\"Could not determine file type.\");\n" +
                "        }\n" +
                "        if (contentType == null) {\n" +
                "            contentType = \"application/octet-stream\";\n" +
                "        }\n" +
                "        return ResponseEntity.ok()\n" +
                "                .contentType(MediaType.parseMediaType(contentType))\n" +
                "                .header(HttpHeaders.CONTENT_DISPOSITION, \"attachment; filename=\\\"\" + resource.getFilename() + \"\\\"\")\n" +
                "                .header(\"file_name\", attachmentDto.getName())\n" +
                "                .body(resource);\n" +
                "    }\n" +
                "\n" +
                "   \n" +
                "    @PostMapping(\"/set-record-id/{recordId}\")\n" +
                "    public void setAttachmentRecordId(@RequestBody List<Long> attachmentIdList, @PathVariable Long recordId) {\n" +
                "\n" +
                "        if(recordId == null || recordId < 0) {\n" +
                "            throw new RuntimeException(BusinessExceptionCode.BAD_INPUT.name());\n" +
                "        }\n" +
                "        attachmentService.updateAttachmentRecordId(attachmentIdList, recordId);\n" +
                "    }" +
                "}");

        String result = content.toString().replaceAll("#package", basePackage);

        System.out.println(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/AttachmentRestService.java"))) {
            out.print(result);
        }
        return result;
    }

    private static void generateRestService(String basePackage, EntityDefinition entity, String targetPath, boolean generatePermissions) throws FileNotFoundException {

        String firstChar = entity.getName().substring(0, 1);
        String entityInstanceName = entity.getName().replaceFirst(firstChar, firstChar.toLowerCase());

        String result = generateRestHeader(generatePermissions);
        result += generateRestGetMethods(entity, generatePermissions);
        result += generateRestPostAndRemove(entity.getName(), generatePermissions);
        result += generateFooter();


        result = result.replaceAll("#package", basePackage)
                .replaceAll("#Entity", entity.getName())
                .replaceAll("#entity", entityInstanceName);

        System.out.printf(result);
        try (PrintStream out = new PrintStream(new FileOutputStream(targetPath + "/" + entity.getName() + "RestService.java"))) {
            out.print(result);
        }

    }

    private static String generateRestHeader(Boolean generatePermissions) {
        String content = "package #package.rest;\n" +
                "\n" +
                "import com.caracal.data.SortUtil;\n" +
                "import com.caracal.data.api.qbe.SortObject;\n" +
                "import com.caracal.data.api.qbe.StringSearchType;\n" +
                "import #package.dto.#EntityDto;\n" +
                "import #package.service.#EntityService;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.web.bind.annotation.*;\n" +
                "import #package.service.PagedResult;" +
                "import java.util.Date;\n" +
                "import com.caracal.data.api.qbe.CompareObject;\n" +
                "import java.util.ArrayList;\n" +
                "\n";

        if (generatePermissions) {
            content += "import org.springframework.security.access.annotation.Secured;\n";
            content += "import org.springframework.security.access.prepost.PreAuthorize;\n";
            content += "import #package.security.AccessRoles;\n";

        }

        content += "import java.text.ParseException;\n" +
                "import java.util.Collections;\n" +
                "import java.util.List;\n" +
                "\n" +
                "\n" +
                "/* Generated By Caracal Generator ( Powered by Dr.Adldoost :D ) */\n" +
                "\n" +
                "@RestController\n" +
                "@RequestMapping(\"/#entity\")\n" +
                "public class #EntityRestService {\n" +
                "\n" +
                "    private final #EntityService #entityService;\n" +
                "\n" +
                "    @Autowired\n" +
                "    public #EntityRestService(#EntityService #entityService) {\n" +
                "        this.#entityService = #entityService;\n" +
                "    }";
        return content;
    }

    private static String generateRestGetMethods(EntityDefinition entity, Boolean generatePermissions) {

        StringBuilder content = new StringBuilder(
                "\n\n");
        if (generatePermissions) {
            content.append("    @PreAuthorize(\"hasAuthority('AUTHORITY_FIND_").append(camelToSnake(entity.getName()).toUpperCase()).append("')\"").append(")\n");
        }
        content.append("    @GetMapping(\"/{id}\")\n" +
                "    public #EntityDto findById(@PathVariable(name = \"id\")Long id) {\n" +
                "        return #entityService.findByPrimaryKey(id);\n" +
                "    }\n" +
                "\n");
        if (generatePermissions) {
            content.append("     @PreAuthorize(\"hasAuthority('AUTHORITY_SEARCH_").append(camelToSnake(entity.getName()).toUpperCase()).append("')\"").append(")\n");
        }
        content.append("    @GetMapping(\"/search\")\n" +
                "    public PagedResult search(");

        for (EntityFieldDefinition field : entity.getEntityFieldDefinitionList()) {
            if (field.getFieldType().getType().getValue().contains("Date")) {
                content.append("\n                                      @RequestParam(value = \"").append(field.getName() + "From").append("\", required = false) ");
                content.append(field.getFieldType().getType().getValue()).append(" ").append(field.getName() + "From").append(",");

                content.append("\n                                      @RequestParam(value = \"").append(field.getName() + "To").append("\", required = false) ");
                content.append(field.getFieldType().getType().getValue()).append(" ").append(field.getName() + "To").append(",");
            } else if (getBaseTypes().contains(field.getFieldType().getType().getValue())) {
                content.append("\n                                      @RequestParam(value = \"").append(field.getName()).append("\", required = false) ");
                content.append(field.getFieldType().getType().getValue()).append(" ").append(field.getName()).append(",");
            } else if (field.getFieldType().getType().getValue().contains(ComponentTypes.DROP_DOWN.getValue())) {
                content.append("\n                                      @RequestParam(value = \"").append(field.getName()).append("\", required = false) ");
                content.append("Long").append(" ").append(field.getName()).append(",");
            } else if (field.getFieldType().getType().getValue().contains(ComponentTypes.RADIO_BUTTON.getValue())) {
                content.append("\n                                      @RequestParam(value = \"").append(field.getName()).append("\", required = false) ");
                content.append("Long").append(" ").append(field.getName()).append(",");
            }
        }

        content.append("\n                                      @RequestParam(value = \"").append("firstIndex").append("\", required = false) ");
        content.append("Integer").append(" ").append("firstIndex").append(",");
        content.append("\n                                      @RequestParam(value = \"").append("pageSize").append("\", required = false) ");
        content.append("Integer").append(" ").append("pageSize").append(",");
        content.append("\n                                      @RequestParam(value = \"").append("sortField").append("\", required = false) ");
        content.append("String").append(" ").append("sortField").append(",");
        content.append("\n                                      @RequestParam(value = \"").append("sortOrder").append("\", required = false) ");
        content.append("String").append(" ").append("sortOrder").append(",");


        content = removeLastChar(content);
        content.append(") {\n\n");
        content.append("            SortObject sortObject = SortUtil.generateSortObject(sortField, sortOrder);\n" +
                "            List<SortObject> sortObjectList = null;\n" +
                "            if(sortObject != null)\n" +
                "               sortObjectList = Collections.singletonList(sortObject);\n" +
                "\n" +
                "            if(firstIndex == null)\n" +
                "               firstIndex = 0;\n" +
                "            if(pageSize == null)\n" +
                "               pageSize = Integer.MAX_VALUE;\n");


        content.append("            #EntityDto #entity = new #EntityDto();\n");
        content.append("            List<CompareObject> compareObjectList = new ArrayList<>();\n");
        for (EntityFieldDefinition field : entity.getEntityFieldDefinitionList()) {
            if (field.getFieldType().getType().getValue().contains("Date")) {
                String toSearchFieldName = field.getName() + "To";
                String fromSearchFieldName = field.getName() + "From";
                content.append("            if (" + toSearchFieldName + " != null) {\n" +
                        "               CompareObject co" + toSearchFieldName + " = new CompareObject();\n" +
                        "               co" + toSearchFieldName + ".setFieldName(\"" + field.getName() + "\");\n" +
                        "               co" + toSearchFieldName + ".setOperator(CompareObject.Operator.LE);\n" +
                        "               co" + toSearchFieldName + ".setTarget(" + toSearchFieldName + ");\n" +
                        "               compareObjectList.add(co" + toSearchFieldName + ");\n" +
                        "           }\n" +
                        "\n" +
                        "            if(" + fromSearchFieldName + " != null) {\n" +
                        "               CompareObject co" + fromSearchFieldName + " = new CompareObject();\n" +
                        "               co" + fromSearchFieldName + ".setFieldName(\"" + field.getName() + "\");\n" +
                        "               co" + fromSearchFieldName + ".setOperator(CompareObject.Operator.GE);\n" +
                        "               co" + fromSearchFieldName + ".setTarget(" + fromSearchFieldName + ");\n" +
                        "               compareObjectList.add(co" + fromSearchFieldName + ");\n" +
                        "           }\n");
            } else if (getBaseTypes().contains(field.getFieldType().getType().getValue())) {
                String firstCharFieldName = field.getName().substring(0, 1);
                String upperCaseCharFieldName = field.getName().replaceFirst(firstCharFieldName, firstCharFieldName.toUpperCase());
                content.append("            #entity.set" + upperCaseCharFieldName + "(" + field.getName() + "); \n");
            } else if (field.getFieldType().getType().getValue().contains(ComponentTypes.DROP_DOWN.getValue())) {
                String firstCharFieldName = field.getName().substring(0, 1);
                String upperCaseCharFieldName = field.getName().replaceFirst(firstCharFieldName, firstCharFieldName.toUpperCase());
                content.append("            #entity.set" + upperCaseCharFieldName + "(" + field.getName() + "); \n");
            } else if (field.getFieldType().getType().getValue().contains(ComponentTypes.RADIO_BUTTON.getValue())) {
                String firstCharFieldName = field.getName().substring(0, 1);
                String upperCaseCharFieldName = field.getName().replaceFirst(firstCharFieldName, firstCharFieldName.toUpperCase());
                content.append("            #entity.set" + upperCaseCharFieldName + "(" + field.getName() + "); \n");
            }
        }

        content.append("\n            return #entityService.findPagedByExample(#entity,\n" +
                "                   sortObjectList,\n" +
                "                   firstIndex,\n" +
                "                   pageSize,\n" +
                "                   StringSearchType.EXPAND_BOTH_SIDES,\n" +
                "                   null,\n" +
                "                   compareObjectList\n" +
                "                   );\n");
        content.append("    }\n\n");
        return content.toString();
    }

    private static String generateRestPostAndRemove(String entity, Boolean generatePermissions) {
        StringBuilder content = new StringBuilder("\n");
        if (generatePermissions) {
            content.append("    @PreAuthorize(\"hasAuthority('AUTHORITY_SAVE_").append(camelToSnake(entity).toUpperCase()).append("')\"").append(")\n");
        }
        content.append("    @PostMapping(path = \"/save\")\n" +
                "    public #EntityDto save(@RequestBody #EntityDto #entity) {\n" +
                "        #entityService.validateBeforeSave(#entity);\n" +
                "        return #entityService.save(#entity);\n" +
                "    }\n" +
                "\n");
        if (generatePermissions) {
            content.append("\n    @PreAuthorize(\"hasAuthority('AUTHORITY_REMOVE_").append(camelToSnake(entity).toUpperCase()).append("')\"").append(")\n");
        }
        content.append("    @DeleteMapping(path = \"/delete/{id}\")\n" +
                "    public void remove(@PathVariable(name = \"id\")Long id) {\n" +
                "        #entityService.remove(id);\n" +
                "    }");
        return content.toString();

    }

    private static String generateFooter() {
        return "\n}";
    }

    private static String generateBusinessExceptionCodeClass(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.common;\n" +
                "\n" +
                "// Generated By caracal Framework, Powered by Dr.Adldoost :D" +
                "\n" +
                "public enum BusinessExceptionCode {\n" +
                "\n" +
                "\n" +
                "    GENERAL_ERROR(-1),\n" +
                "    SECURITY_UNHANDLED_EXCEPTION(-2),\n" +
                "    JWT_PARSE_EXCEPTION(-3),\n" +
                "    INVALID_LOGIN_TOKEN(-4),\n" +
                "    ACCESS_DENIED(-5),\n" +
                "\n" +
                "    NOT_VALID_DATA(2000),\n" +
                "\n" +
                "\n" +
                "    //Validation\n" +
                "    BAD_INPUT(3000),\n" +
                "    COULD_NOT_STORED_FILE_RETRY(3001),\n" +
                "    COULD_NOT_CREATE_DIRECTORY(3002),\n" +
                "    FILE_NOT_FOUND_PATH(3003),\n" +
                "    IS_MANDETORY(3004),\n" +
                "\n" +
                "    ;" +
                "\n" +
                "    private int value;\n" +
                "\n" +
                "    public int getValue() {\n" +
                "\n" +
                "        return this.value;\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "    BusinessExceptionCode(int value) {\n" +
                "\n" +
                "        this.value = value;\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "    public static BusinessExceptionCode findByName(String name){\n" +
                "        for(BusinessExceptionCode v : values()){\n" +
                "            if( v.name() == name){\n" +
                "                return v;\n" +
                "            }\n" +
                "        }\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "}\n";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/BusinessExceptionCode.java"))) {
            out.print(result);
        }
        return result;

    }

    private static String generateConfigReaderUtilClass(String path, String basePackage) throws FileNotFoundException {
        String content = "\n" +
                "package #package.common;\n" +
                "\n" +
                "\n" +
                "// Generated By caracal Framework, Powered by Dr.Adldoost :D" +
                "\n" +
                "import java.util.Enumeration;\n" +
                "import java.util.ResourceBundle;\n" +
                "\n" +
                "/**\n" +
                " *\n" +
                " * @author Generated By Caracal Framework , powered by Dr.Adldoost :D \n" +
                " */\n" +
                "public class InitializrReaderUtility {\n" +
                "\n" +
                "    static ResourceBundle rb = ResourceBundle.getBundle(\"config\");\n" +
                "\n" +
                "    public static String getResourceProperity(String key) {\n" +
                "        return rb.getString(key);\n" +
                "    }\n" +
                "\n" +
                "    public static Enumeration<String> getResourceKeys() {\n" +
                "        return rb.getKeys();\n" +
                "    }\n" +
                "\n" +
                "}\n";
        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/InitializrReaderUtility.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateErrorCodeReaderUtilClass(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.common;\n" +
                "\n" +
                "// Generated By Caracal Framework, powered by Dr.Adldoost :D" +
                "\n" +
                "import java.util.Enumeration;\n" +
                "import java.util.ResourceBundle;\n" +
                "import java.io.UnsupportedEncodingException;" +
                "\n" +
                "public class ErrorCodeReaderUtil {\n" +
                "\n" +
                "    static ResourceBundle rb = ResourceBundle.getBundle(\"errorcodes\");\n" +
                "\n" +
                "    public static String getResourceProperity(String key)  {\n" +
                "        String val = rb.getString(key);\n" +
                "        try {\n" +
                "            val = new String(val.getBytes(\"ISO-8859-1\"), \"UTF-8\");\n" +
                "        } catch (UnsupportedEncodingException e) {\n" +
                "            e.printStackTrace();\n" +
                "        }\n" +
                "        return val;\n" +
                "    }\n" +
                "\n" +
                "    public static Enumeration<String> getResourceKeys(String key) {\n" +
                "            return rb.getKeys();\n" +
                "    }\n" +
                "}\n";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/ErrorCodeReaderUtil.java"))) {
            out.print(result);
        }
        return result;

    }

    private static String generateCaracalExceptionHandler(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.common;\n" +
                "\n" +
                "// Generated By Caracal Framework, powered by Dr.Adldoost :D" +
                "\n" +
                "import com.google.common.base.Throwables;\n" +
                "import org.springframework.http.HttpStatus;\n" +
                "import org.springframework.http.ResponseEntity;\n" +
                "import org.springframework.web.bind.annotation.ControllerAdvice;\n" +
                "import org.springframework.web.bind.annotation.ExceptionHandler;\n" +
                "import org.springframework.web.bind.annotation.RestController;\n" +
                "import org.springframework.web.context.request.WebRequest;\n" +
                "import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;\n" +
                "\n" +
                "@ControllerAdvice\n" +
                "@RestController\n" +
                "public class CaracalExceptionHandler extends ResponseEntityExceptionHandler {\n" +
                "\n" +
                "    @ExceptionHandler(RuntimeException.class)\n" +
                "    public final ResponseEntity<RestErrorMessage> handleRestException(RuntimeException ex, WebRequest request) {\n" +
                "\n" +
                "        String errorMessage = \"\";\n" +
                "        Integer code = -1;\n" +
                "\n" +
                "        if(ex instanceof SecurityServiceException) {\n" +
                "            errorMessage = ErrorCodeReaderUtil.getResourceProperity(\"UNAUTHORIZED\");\n" +
                "            code = 0;\n" +
                "            return new ResponseEntity<>(new RestErrorMessage(errorMessage, code), HttpStatus.UNAUTHORIZED);\n" +
                "        }\n" +
                "\n" +
                "        if(ex instanceof ValidationException) {\n" +
                "            String entityName = ((ValidationException) ex).getEntityName();\n" +
                "            String fieldName = ((ValidationException) ex).getFieldName();\n" +
                "            ValidationType validationType = ((ValidationException) ex).getValidationType();\n" +
                "            if(entityName == null)\n" +
                "                entityName = \"\";\n" +
                "            if(fieldName == null)\n" +
                "                fieldName = \"\";\n" +
                "            try {\n" +
                "                errorMessage = FarsiCodeReaderUtility.getResourceProperity(entityName + \".\" + fieldName) + \" \";\n" +
                "                errorMessage += ErrorCodeReaderUtil.getResourceProperity(validationType.name());\n" +
                "                code = BusinessExceptionCode.NOT_VALID_DATA.getValue();\n" +
                "                return new ResponseEntity<>(new RestErrorMessage(errorMessage, code), HttpStatus.BAD_REQUEST);\n" +
                "            } catch (Exception e) {\n" +
                "                errorMessage = ErrorCodeReaderUtil.getResourceProperity(BusinessExceptionCode.BAD_INPUT.name());\n" +
                "                code = BusinessExceptionCode.NOT_VALID_DATA.getValue();\n" +
                "                return new ResponseEntity<>(new RestErrorMessage(errorMessage, code), HttpStatus.BAD_REQUEST);\n" +
                "            }\n" +
                "\n" +
                "        }\n" +
                "\n" +
                "        try {\n" +
                "            errorMessage = ErrorCodeReaderUtil.getResourceProperity(ex.getMessage());\n" +
                "            BusinessExceptionCode bec = BusinessExceptionCode.findByName(ex.getMessage());\n" +
                "            if(bec != null)\n" +
                "                code = bec.getValue();\n" +
                "            else\n" +
                "                code = 0;\n" +
                "            return new ResponseEntity<>(new RestErrorMessage(errorMessage, code), HttpStatus.BAD_REQUEST);\n" +
                "\n" +
                "        } catch (Exception e) {\n" +
                "//            errorMessage = ErrorCodeReaderUtil.getResourceProperity(BusinessExceptionCode.GENERAL_ERROR.name());\n" +
                "\n" +
                "            errorMessage = ex.getMessage();\n" +
                "            if(errorMessage == null)\n" +
                "                errorMessage = Throwables.getStackTraceAsString ( ex ) ;\n" +
                "            code = -1;\n" +
                "        }\n" +
                "\n" +
                "        return new ResponseEntity<>(new RestErrorMessage(errorMessage, code), HttpStatus.BAD_REQUEST);\n" +
                "\n" +
                "    }\n" +
                "}\n";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/CaracalExceptionHandler.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateFarsiCodeReaderUtility(String path, String basePackage) throws FileNotFoundException {

        //fixme : utf8 reading...
        String content = "package #package.common;\n" +
                "\n" +
                "import java.io.UnsupportedEncodingException;\n" +
                "import java.nio.charset.StandardCharsets;\n" +
                "import java.util.Enumeration;\n" +
                "import java.util.ResourceBundle;\n" +
                "\n" +
                "public class FarsiCodeReaderUtility {\n" +
                "\n" +
                "    static ResourceBundle rb = ResourceBundle.getBundle(\"farsicodes\");\n" +
                "\n" +
                "    public static String getResourceProperity(String key)  {\n" +
                "        String val = rb.getString(key);\n" +
                "        val = new String(val.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);\n" +
                "        return val;\n" +
                "    }\n" +
                "\n" +
                "    public static Enumeration<String> getResourceKeys(String key) {\n" +
                "        return rb.getKeys();\n" +
                "    }\n" +
                "\n" +
                "}\n";
        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/FarsiCodeReaderUtility.java"))) {
            out.print(result);
        }
        return result;

    }

    private static String generateRandomStringCodeUtility(String path, String basePackage) throws FileNotFoundException {

        String content = "package #package.common;\n" +
                "\n" +
                "// Generated by Caracal Framework, powered by Dr.Adldoost :D" +
                "\n" +
                "import java.util.Random;\n" +
                "\n" +
                "public class GenerateRandomStringUtil {\n" +
                "\n" +
                "    public static String getSaltString(int lengh) {\n" +
                "        String SALTCHARS = \"ABCDEFGHJKMNPQRSTUVWXYZ23456789\";\n" +
                "        StringBuilder salt = new StringBuilder();\n" +
                "        Random rnd = new Random();\n" +
                "        while (salt.length() < lengh) { // length of the random string.\n" +
                "            int index = (int) (rnd.nextFloat() * SALTCHARS.length());\n" +
                "            salt.append(SALTCHARS.charAt(index));\n" +
                "        }\n" +
                "        String saltStr = salt.toString();\n" +
                "        return saltStr;\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "}\n";
        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/GenerateRandomStringUtil.java"))) {
            out.print(result);
        }
        return result;

    }

    private static String generateRestErrorMessageClass(String path, String basePackage) throws FileNotFoundException {

        String content = "package #package.common;\n" +
                "\n" +
                "import java.io.Serializable;\n" +
                "\n" +
                "/**\n" +
                " * Generated by Caracal Framework, powered by Dr.Adldoost :D .\n" +
                " */\n" +
                "public class RestErrorMessage implements Serializable {\n" +
                "\n" +
                "    public RestErrorMessage(String message, Integer code){\n" +
                "        this.setMessage(message);\n" +
                "        this.setCode(code);\n" +
                "    }\n" +
                "\n" +
                "    private String message;\n" +
                "    private Integer code;\n" +
                "\n" +
                "    public String getMessage() {\n" +
                "        return message;\n" +
                "    }\n" +
                "\n" +
                "    public void setMessage(String message) {\n" +
                "        this.message = message;\n" +
                "    }\n" +
                "\n" +
                "    public Integer getCode() {\n" +
                "        return code;\n" +
                "    }\n" +
                "\n" +
                "    public void setCode(Integer code) {\n" +
                "        this.code = code;\n" +
                "    }\n" +
                "}\n";
        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/RestErrorMessage.java"))) {
            out.print(result);
        }
        return result;

    }

    private static String generateSecurityServiceExceptionClass(String path, String basePackage) throws FileNotFoundException {

        String content = "package #package.common;\n" +
                "\n" +
                "\n" +
                "public class SecurityServiceException extends RuntimeException {\n" +
                "\n" +
                "    public SecurityServiceException() {\n" +
                "    }\n" +
                "\n" +
                "    public SecurityServiceException(String message) {\n" +
                "        super(message);\n" +
                "    }\n" +
                "\n" +
                "    public SecurityServiceException(String message, Throwable cause) {\n" +
                "        super(message, cause);\n" +
                "    }\n" +
                "\n" +
                "    public SecurityServiceException(Throwable cause) {\n" +
                "        super(cause);\n" +
                "    }\n" +
                "\n" +
                "    public SecurityServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {\n" +
                "        super(message, cause, enableSuppression, writableStackTrace);\n" +
                "    }\n" +
                "}\n";
        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/SecurityServiceException.java"))) {
            out.print(result);
        }
        return result;

    }

    private static String generateFileStoragePropertiesFile(String path, String basePackage) throws FileNotFoundException {
        StringBuilder content = new StringBuilder("");
        content.append("package #package.common;\n" +
                "\n" +
                "import org.springframework.boot.context.properties.ConfigurationProperties;\n" +
                "\n" +
                "import java.text.DateFormat;\n" +
                "import java.text.SimpleDateFormat;\n" +
                "import java.util.Date;\n" +
                "\n" +
                "@ConfigurationProperties(prefix = \"file\")\n" +
                "public class FileStorageProperties {\n" +
                "    private String uploadDir;\n" +
                "\n" +
                "    public String getUploadDir() {\n" +
                "\n" +
                "        Date date = new Date();\n" +
                "        DateFormat dateFormat = new SimpleDateFormat(\"yyyy-MM-dd\");\n" +
                "\n" +
                "        String strDate = dateFormat.format(date);\n" +
                "        return uploadDir + \"/\" + strDate + \"/\";\n" +
                "    }\n" +
                "\n" +
                "    public void setUploadDir(String uploadDir) {\n" +
                "        this.uploadDir = uploadDir;\n" +
                "    }\n" +
                "}");

        String result = content.toString().replaceAll("#package", basePackage);

        System.out.println(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/FileStorageProperties.java"))) {
            out.print(result);
        }
        return result;

    }

    private static String generateValidationExceptionFile(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.common;\n" +
                "\n" +
                "import #package.common.ValidationType;\n" +
                "public class ValidationException extends RuntimeException {\n" +
                "\n" +
                "    private String fieldName;\n" +
                "    private String entityName;\n" +
                "    private ValidationType validationType;\n" +
                "\n" +
                "    public ValidationException(String fieldName, String entityName, ValidationType validationType) {\n" +
                "        this.fieldName = fieldName;\n" +
                "        this.entityName = entityName;\n" +
                "        this.validationType = validationType;\n" +
                "    }\n" +
                "\n" +
                "    public ValidationException(String message, String fieldName, String entityName, ValidationType validationType) {\n" +
                "        super(message);\n" +
                "        this.fieldName = fieldName;\n" +
                "        this.entityName = entityName;\n" +
                "        this.validationType = validationType;\n" +
                "    }\n" +
                "\n" +
                "    public ValidationException(String message, Throwable cause, String fieldName, String entityName, ValidationType validationType) {\n" +
                "        super(message, cause);\n" +
                "        this.fieldName = fieldName;\n" +
                "        this.entityName = entityName;\n" +
                "        this.validationType = validationType;\n" +
                "    }\n" +
                "\n" +
                "    public ValidationException(Throwable cause, String fieldName, String entityName, ValidationType validationType) {\n" +
                "        super(cause);\n" +
                "        this.fieldName = fieldName;\n" +
                "        this.entityName = entityName;\n" +
                "        this.validationType = validationType;\n" +
                "    }\n" +
                "\n" +
                "    public ValidationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, String fieldName, String entityName, ValidationType validationType) {\n" +
                "        super(message, cause, enableSuppression, writableStackTrace);\n" +
                "        this.fieldName = fieldName;\n" +
                "        this.entityName = entityName;\n" +
                "        this.validationType = validationType;\n" +
                "    }\n" +
                "\n" +
                "    public ValidationType getValidationType() {\n" +
                "        return validationType;\n" +
                "    }\n" +
                "\n" +
                "    public void setValidationType(ValidationType validationType) {\n" +
                "        this.validationType = validationType;\n" +
                "    }\n" +
                "\n" +
                "    public String getEntityName() {\n" +
                "        return entityName;\n" +
                "    }\n" +
                "\n" +
                "    public void setEntityName(String entityName) {\n" +
                "        this.entityName = entityName;\n" +
                "    }\n" +
                "\n" +
                "    public String getFieldName() {\n" +
                "        return fieldName;\n" +
                "    }\n" +
                "\n" +
                "    public void setFieldName(String fieldName) {\n" +
                "        this.fieldName = fieldName;\n" +
                "    }\n" +
                "}\n";
        String result = content.toString().replaceAll("#package", basePackage);

        System.out.println(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/ValidationException.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateValidationTypeEnum(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.common;\n" +
                "\n" +
                "public enum ValidationType {\n" +
                "\n" +
                "    IS_MANDATORY,\n" +
                "    NOT_VALID,\n" +
                "    IS_UNIQUE,\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "    ;\n" +
                "}\n";

        String result = content.toString().replaceAll("#package", basePackage);

        System.out.println(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/ValidationType.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateCustomClaims(String path, String basePackage) throws FileNotFoundException {


        String content = "package #package.jwt;\n" +
                "\n" +
                "// Generated Caracal Framework, powered by Dr.Adldoost :D" +
                "\n" +
                "import io.jsonwebtoken.Claims;\n" +
                "\n" +
                "public interface CustomClaims extends Claims {\n" +
                "\n" +
                "    String PERMISSIONS = \"perms\";\n" +
                "    String ROLES = \"roles\";\n" +
                "\n" +
                "}\n";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/CustomClaims.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateJwtAuthenticationEntryClass(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.jwt;\n" +
                "\n" +
                "import org.slf4j.Logger;\n" +
                "import org.slf4j.LoggerFactory;\n" +
                "import org.springframework.security.core.AuthenticationException;\n" +
                "import org.springframework.security.web.AuthenticationEntryPoint;\n" +
                "import org.springframework.stereotype.Component;\n" +
                "\n" +
                "import javax.servlet.ServletException;\n" +
                "import javax.servlet.http.HttpServletRequest;\n" +
                "import javax.servlet.http.HttpServletResponse;\n" +
                "import java.io.IOException;\n" +
                "\n" +
                "@Component\n" +
                "public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {\n" +
                "\n" +
                "    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);\n" +
                "    @Override\n" +
                "    public void commence(HttpServletRequest httpServletRequest,\n" +
                "                         HttpServletResponse httpServletResponse,\n" +
                "                         AuthenticationException e) throws IOException, ServletException {\n" +
                "        logger.error(\"Responding with unauthorized error. Message - {}\", e.getMessage());\n" +
                "        httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,\n" +
                "                \"Sorry, You're not authorized to access this resource.\");\n" +
                "    }\n" +
                "}";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/JwtAuthenticationEntryPoint.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateJwtAuthenticationFilterClass(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.jwt;\n" +
                "\n" +
                "\n" +
                "// Generated by Caracal framework, powered by Dr.Adldoost :D" +
                "\n" +
                "import com.fasterxml.jackson.core.JsonProcessingException;\n" +
                "import com.fasterxml.jackson.databind.ObjectMapper;\n" +
                "import #package.common.BusinessExceptionCode;\n" +
                "import #package.common.ErrorCodeReaderUtil;\n" +
                "import #package.common.RestErrorMessage;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.http.HttpHeaders;\n" +
                "import org.springframework.http.HttpStatus;\n" +
                "import org.springframework.security.core.context.SecurityContextHolder;\n" +
                "import org.springframework.util.StringUtils;\n" +
                "import org.springframework.web.filter.OncePerRequestFilter;\n" +
                "import javax.servlet.FilterChain;\n" +
                "import javax.servlet.ServletException;\n" +
                "import javax.servlet.http.HttpServletRequest;\n" +
                "import javax.servlet.http.HttpServletResponse;\n" +
                "import java.io.IOException;\n" +
                "import java.util.Date;\n" +
                "\n" +
                "public class JwtAuthenticationFilter extends OncePerRequestFilter {\n" +
                "\n" +
                "    private final TokenRepository tokenRepository;\n" +
                "\n" +
                "    @Autowired\n" +
                "    public JwtAuthenticationFilter(TokenRepository tokenRepository) {\n" +
                "        this.tokenRepository = tokenRepository;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {\n" +
                "\n" +
                "        try {\n" +
                "            String jwt = request.getHeader(HttpHeaders.AUTHORIZATION);\n" +
                "            SecurityWrapper securityWrapper = JWTUtil.getSecurityWrapperFromToken(jwt);\n" +
                "\n" +
                "            Date repositoryDate = tokenRepository.getTokenIssueDate(securityWrapper.getUsername());\n" +
                "            Date jwtTokenIssueDate = JWTUtil.getCreatedDateFromToken(jwt);\n" +
                "\n" +
                "            if(repositoryDate != null) {\n" +
                "                if(jwtTokenIssueDate.after(repositoryDate))\n" +
                "                    tokenRepository.put(securityWrapper.getUsername(), jwtTokenIssueDate);\n" +
                "                else if(jwtTokenIssueDate.before(repositoryDate))\n" +
                "                    throw new RuntimeException(BusinessExceptionCode.INVALID_LOGIN_TOKEN.name());\n" +
                "            } else {\n" +
                "                tokenRepository.put(securityWrapper.getUsername(), jwtTokenIssueDate);\n" +
                "            }\n" +
                "\n" +
                "\n" +
                "            JwtAuthenticationToken authentication = new JwtAuthenticationToken(securityWrapper);\n" +
                "\n" +
                "            SecurityContextHolder.getContext().setAuthentication(authentication);\n" +
                "\n" +
                "            response.setHeader(HttpHeaders.AUTHORIZATION, JWTUtil.refreshToken(jwt));\n" +
                "\n" +
                "            filterChain.doFilter(request, response);\n" +
                "\n" +
                "            Date novelJWTIssueDate = JWTUtil.getCreatedDateFromToken(jwt);\n" +
                "\n" +
                "            //invalidate all older tokens\n" +
                "            tokenRepository.put(securityWrapper.getUsername(), novelJWTIssueDate);\n" +
                "        }\n" +
                "        catch (Exception e) {\n" +
                "            response.setStatus(HttpStatus.UNAUTHORIZED.value());\n" +
                "            response.setContentType(\"text/html; charset=UTF-8\");\n" +
                "            response.setCharacterEncoding(\"UTF-8\");\n" +
                "            String errorMessage = ErrorCodeReaderUtil.getResourceProperity(\"UNAUTHORIZED\");\n" +
                "            Integer code = 0;\n" +
                "            response.getWriter().write(convertObjectToJson(new RestErrorMessage(errorMessage, code)));\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    private String getJwtFromRequest(HttpServletRequest request) {\n" +
                "        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);\n" +
                "        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(\"Bearer \")) {\n" +
                "            return bearerToken.substring(7, bearerToken.length());\n" +
                "        }\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "    public String convertObjectToJson(Object object) throws JsonProcessingException {\n" +
                "        if (object == null) {\n" +
                "            return null;\n" +
                "        }\n" +
                "        ObjectMapper mapper = new ObjectMapper();\n" +
                "        return mapper.writeValueAsString(object);\n" +
                "    }\n" +
                "}\n";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/JwtAuthenticationFilter.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateJwtAuthenticationProvider(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.jwt;\n" +
                "\n" +
                "import org.springframework.security.authentication.AuthenticationProvider;\n" +
                "import org.springframework.security.core.Authentication;\n" +
                "import org.springframework.security.core.AuthenticationException;\n" +
                "import org.springframework.stereotype.Component;\n" +
                "\n" +
                "@Component\n" +
                "public class JwtAuthenticationProvider implements AuthenticationProvider {\n" +
                "\n" +
                "    @Override\n" +
                "    public Authentication authenticate(Authentication authentication) throws AuthenticationException {\n" +
                "\n" +
                "        JwtAuthenticationToken auth = (JwtAuthenticationToken) authentication;\n" +
                "\n" +
                "        return auth;\n" +
                "}\n" +
                "\n" +
                "    @Override\n" +
                "    public boolean supports(Class<?> aClass) {\n" +
                "        return JwtAuthenticationToken.class.isAssignableFrom(aClass);\n" +
                "    }\n" +
                "}\n";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/JwtAuthenticationProvider.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateJwtAuthenticationTokenClass(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.jwt;\n" +
                "\n" +
                "import org.springframework.security.core.Authentication;\n" +
                "import org.springframework.security.core.GrantedAuthority;\n" +
                "import org.springframework.security.core.authority.SimpleGrantedAuthority;\n" +
                "\n" +
                "import java.util.ArrayList;\n" +
                "import java.util.Collection;\n" +
                "import java.util.List;\n" +
                "\n" +
                "public class JwtAuthenticationToken implements Authentication {\n" +
                "\n" +
                "\n" +
                "    private SecurityWrapper securityWrapper;\n" +
                "\n" +
                "    public JwtAuthenticationToken(SecurityWrapper securityWrapper) {\n" +
                "        this.securityWrapper = securityWrapper;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public Collection<? extends GrantedAuthority> getAuthorities() {\n" +
                "\n" +
                "        List<SimpleGrantedAuthority> authorities = new ArrayList<>();\n" +
                "        if(securityWrapper.getPermissions() != null && !securityWrapper.getPermissions().isEmpty()) {\n" +
                "            securityWrapper.getPermissions().stream().forEach(p -> {\n" +
                "                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(p);\n" +
                "                authorities.add(authority);\n" +
                "            });\n" +
                "        }\n" +
                "        if(securityWrapper.getRoles() != null && !securityWrapper.getRoles().isEmpty()) {\n" +
                "            securityWrapper.getRoles().stream().forEach(r -> {\n" +
                "                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(r);\n" +
                "                authorities.add(authority);\n" +
                "            });\n" +
                "        }\n" +
                "        return authorities;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public SecurityWrapper getCredentials() {\n" +
                "        return securityWrapper;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public SecurityWrapper getDetails() {\n" +
                "        return securityWrapper;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public Object getPrincipal() {\n" +
                "        return securityWrapper.getUsername();\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public boolean isAuthenticated() {\n" +
                "\n" +
                "        return securityWrapper.isSecure();\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void setAuthenticated(boolean b) throws IllegalArgumentException {\n" +
                "\n" +
                "        securityWrapper.setSecure(b);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public String getName() {\n" +
                "        return securityWrapper.getUsername();\n" +
                "    }\n" +
                "}\n";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/JwtAuthenticationToken.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateJwtUserDetails(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.jwt;\n" +
                "\n" +
                "import java.util.Date;\n" +
                "import java.util.List;\n" +
                "\n" +
                "/**\n" +
                " *\n" +
                " * @author Generated By Caracal Frameword, powered by Dr.Adldoost :D\n" +
                " */\n" +
                "public class JWTUserDetails {\n" +
                "\n" +
                "    private String username;\n" +
                "    private Date creationDate;\n" +
                "    private List<String> roles;\n" +
                "    private List<String> permissions;\n" +
                "\n" +
                "\n" +
                "   public JWTUserDetails() {\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "    public JWTUserDetails(SecurityWrapper securityWrapper) {\n" +
                "        this.username = securityWrapper.getUsername();\n" +
                "        this.setCreationDate(null);\n" +
                "        this.setPermissions(securityWrapper.getPermissions());\n" +
                "        this.setRoles(securityWrapper.getRoles());\n" +
                "    }\n" +
                "\n" +
                "    public String getUsername() {\n" +
                "        return username;\n" +
                "    }\n" +
                "\n" +
                "    public void setUsername(String username) {\n" +
                "        this.username = username;\n" +
                "    }\n" +
                "\n" +
                "    public Date getCreationDate() {\n" +
                "        return creationDate;\n" +
                "    }\n" +
                "\n" +
                "    public void setCreationDate(Date creationDate) {\n" +
                "        this.creationDate = creationDate;\n" +
                "    }\n" +
                "\n" +
                "    public List<String> getRoles() {\n" +
                "        return roles;\n" +
                "    }\n" +
                "\n" +
                "    public void setRoles(List<String> roles) {\n" +
                "        this.roles = roles;\n" +
                "    }\n" +
                "    public List<String> getPermissions() {\n" +
                "        return permissions;\n" +
                "    }\n" +
                "\n" +
                "    public void setPermissions(List<String> permissions) {\n" +
                "        this.permissions = permissions;\n" +
                "    }\n" +
                "}\n";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/JWTUserDetails.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateJwtUtilClass(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.jwt;\n" +
                "\n" +
                "import #package.common.BusinessExceptionCode;\n" +
                "import #package.common.InitializrReaderUtility;\n" +
                "import io.jsonwebtoken.Claims;\n" +
                "import io.jsonwebtoken.Jwts;\n" +
                "import io.jsonwebtoken.SignatureAlgorithm;\n" +
                "import org.springframework.util.StringUtils;\n" +
                "\n" +
                "import java.util.*;\n" +
                "\n" +
                "/**\n" +
                " * @author Generated By Caracal frameword, powered by Dr.Adldoost :D \n" +
                " */\n" +
                "public class JWTUtil {\n" +
                "\n" +
                "    static String secret = InitializrReaderUtility.getResourceProperity(\"jwtkey\");\n" +
                "\n" +
                "    static long expiration = Long.parseLong(InitializrReaderUtility.getResourceProperity(\"jwtExpiration\"));\n" +
                "\n" +
                "    public static String getUsernameFromToken(String token) {\n" +
                "        if(token.startsWith(\"Bearer \")) {\n" +
                "            token = token.substring(7, token.length());\n" +
                "        }\n" +
                "        String username;\n" +
                "        final Claims claims = getClaimsFromToken(token);\n" +
                "        if (claims == null)\n" +
                "            throw new RuntimeException(\"Invalid token Exception\");\n" +
                "        username = claims.getSubject();\n" +
                "        return username;\n" +
                "    }\n" +
                "\n" +
                "    public static String getCustomKeyFromToken(String token, String key) {\n" +
                "        if(token.startsWith(\"Bearer \")) {\n" +
                "            token = token.substring(7, token.length());\n" +
                "        }\n" +
                "        String result;\n" +
                "        final Claims claims = getClaimsFromToken(token);\n" +
                "        if (claims == null)\n" +
                "            throw new RuntimeException(\"Invalid token Exception\");\n" +
                "        result = (String)claims.get(key);\n" +
                "        return result;\n" +
                "    }\n" +
                "\n" +
                "    public static Date getCreatedDateFromToken(String token) {\n" +
                "        if(token.startsWith(\"Bearer \")) {\n" +
                "            token = token.substring(7, token.length());\n" +
                "        }\n" +
                "        Date created;\n" +
                "        try {\n" +
                "            final Claims claims = getClaimsFromToken(token);\n" +
                "            created = new Date((Long) claims.get(Claims.ISSUED_AT));\n" +
                "        } catch (Exception e) {\n" +
                "            created = null;\n" +
                "            throw new RuntimeException(BusinessExceptionCode.JWT_PARSE_EXCEPTION.name());\n" +
                "        }\n" +
                "        return created;\n" +
                "    }\n" +
                "\n" +
                "    public static Date getExpirationDateFromToken(String token) {\n" +
                "        if(token.startsWith(\"Bearer \")) {\n" +
                "            token = token.substring(7, token.length());\n" +
                "        }\n" +
                "        Date expiration;\n" +
                "        try {\n" +
                "            final Claims claims = getClaimsFromToken(token);\n" +
                "            expiration = claims.getExpiration();\n" +
                "        } catch (Exception e) {\n" +
                "            expiration = null;\n" +
                "            throw new RuntimeException(BusinessExceptionCode.JWT_PARSE_EXCEPTION.name());\n" +
                "        }\n" +
                "        return expiration;\n" +
                "    }\n" +
                "\n" +
                "    private static Claims getClaimsFromToken(String token) {\n" +
                "        if(token.startsWith(\"Bearer \")) {\n" +
                "            token = token.substring(7, token.length());\n" +
                "        }\n" +
                "        Claims claims;\n" +
                "        try {\n" +
                "            claims = Jwts.parser()\n" +
                "                    .setSigningKey(secret)\n" +
                "                    .parseClaimsJws(token)\n" +
                "                    .getBody();\n" +
                "        } catch (Exception e) {\n" +
                "            claims = null;\n" +
                "            throw new RuntimeException(BusinessExceptionCode.JWT_PARSE_EXCEPTION.name());\n" +
                "        }\n" +
                "        return claims;\n" +
                "    }\n" +
                "\n" +
                "    public static SecurityWrapper getSecurityWrapperFromToken(String token) {\n" +
                "\n" +
                "        if(token.startsWith(\"Bearer \")) {\n" +
                "            token = token.substring(7, token.length());\n" +
                "        }\n" +
                "        Claims claims = getClaimsFromToken(token);\n" +
                "        SecurityWrapper securityWrapper = new SecurityWrapper();\n" +
                "        securityWrapper.setUsername(claims.getSubject());\n" +
                "        securityWrapper.setSecure(true);\n" +
                "        String perms = (String)claims.get(CustomClaims.PERMISSIONS);\n" +
                "        securityWrapper.setPermissions(convertCommaSeperatedToList(perms));\n" +
                "        String sRoles = (String) claims.get(CustomClaims.ROLES);\n" +
                "        securityWrapper.setRoles(convertCommaSeperatedToList(sRoles));\n" +
                "        securityWrapper.setFreshToken(\"Bearer \" + token);\n" +
                "        return securityWrapper;\n" +
                "    }\n" +
                "\n" +
                "    public static List<String> convertCommaSeperatedToList(String str) {\n" +
                "        String parts[] = StringUtils.commaDelimitedListToStringArray(str);\n" +
                "        if(parts == null || parts.length == 0) {\n" +
                "            return null;\n" +
                "        }\n" +
                "        else\n" +
                "        {\n" +
                "            return Arrays.asList(parts);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    private static Date generateExpirationDate() {\n" +
                "        return new Date(System.currentTimeMillis() + expiration * 1000);\n" +
                "    }\n" +
                "\n" +
                "    private static Boolean isTokenExpired(String token) {\n" +
                "        final Date expiration = getExpirationDateFromToken(token);\n" +
                "        return expiration.before(new Date());\n" +
                "    }\n" +
                "\n" +
                "    public static String generateToken(JWTUserDetails userDetails) {\n" +
                "        Map<String, Object> claims = new HashMap<>();\n" +
                "        claims.put(Claims.SUBJECT, userDetails.getUsername());\n" +
                "        claims.put(Claims.AUDIENCE, \"web\");\n" +
                "        claims.put(Claims.ISSUED_AT, new Date());\n" +
                "        claims.put(Claims.ISSUER, \"faraz-sso\");\n" +
                "        claims.put(CustomClaims.PERMISSIONS, StringUtils.collectionToCommaDelimitedString(userDetails.getPermissions()));\n" +
                "        claims.put(CustomClaims.ROLES, StringUtils.collectionToCommaDelimitedString(userDetails.getRoles()));\n" +
                "        return generateToken(claims);\n" +
                "    }\n" +
                "\n" +
                "    public static String generateCustomToken(Map<String, String> map) {\n" +
                "        Map<String, Object> claims = new HashMap<>();\n" +
                "        for (Map.Entry<String, String> entry : map.entrySet())\n" +
                "        {\n" +
                "            claims.put(entry.getKey(), entry.getValue());\n" +
                "        }\n" +
                "        return generateToken(claims);\n" +
                "    }\n" +
                "\n" +
                "    private static String generateToken(Map<String, Object> claims) {\n" +
                "        return Jwts.builder()\n" +
                "                .setClaims(claims)\n" +
                "                .setExpiration(generateExpirationDate())\n" +
                "                .signWith(SignatureAlgorithm.HS512, secret)\n" +
                "                .compact();\n" +
                "    }\n" +
                "\n" +
                "\n" +
                "    public static String refreshToken(String token) {\n" +
                "        if(token.startsWith(\"Bearer \")) {\n" +
                "            token = token.substring(7, token.length());\n" +
                "        }\n" +
                "        String refreshedToken;\n" +
                "        final Claims claims = getClaimsFromToken(token);\n" +
                "        claims.put(Claims.EXPIRATION, generateExpirationDate());\n" +
                "        refreshedToken = generateToken(claims);\n" +
                "\n" +
                "        return refreshedToken;\n" +
                "    }\n" +
                "\n" +
                "    public static Boolean validateToken(String token, String username) {\n" +
                "        final String tokenUsername = getUsernameFromToken(token);\n" +
                "        return (tokenUsername.equals(username)\n" +
                "                && !isTokenExpired(token));\n" +
                "    }\n" +
                "\n" +
                "    public static Boolean canTokenBeRefreshed(String token, Date lastPasswordReset) {\n" +
                "        return (!isTokenExpired(token));\n" +
                "    }\n" +
                "}";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/JWTUtil.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateSecurityWrapperClass(String path, String basePackage) throws FileNotFoundException {
        String content =
                "package #package.jwt;\n" +
                        "\n" +
                        "import java.util.List;\n" +
                        "\n" +
                        "/**\n" +
                        " *\n" +
                        " * @author Generated By Caracal Framework, powered by Dr.Adldoost :D\n" +
                        " */\n" +
                        "public class SecurityWrapper {\n" +
                        "    \n" +
                        "    private String username;\n" +
                        "    private List<String> permissions;\n" +
                        "    private List<String> roles;\n" +
                        "    private String freshToken;\n" +
                        "    private boolean isSecure;\n" +
                        "\n" +
                        "    public SecurityWrapper() {\n" +
                        "\n" +
                        "    }\n" +
                        "\n" +
                        "    public SecurityWrapper(String username, List<String> permissions, List<String> roles, String freshToken, boolean isSecure){\n" +
                        "\n" +
                        "        this.username = username;\n" +
                        "        this.permissions = permissions;\n" +
                        "        this.roles = roles;\n" +
                        "        this.freshToken = freshToken;\n" +
                        "        this.isSecure = isSecure;\n" +
                        "    }\n" +
                        "\n" +
                        "    public String getUsername() {\n" +
                        "        return username;\n" +
                        "    }\n" +
                        "\n" +
                        "    public void setUsername(String username) {\n" +
                        "        this.username = username;\n" +
                        "    }   \n" +
                        "\n" +
                        "    public List<String> getPermissions() {\n" +
                        "        return permissions;\n" +
                        "    }\n" +
                        "\n" +
                        "    public void setPermissions(List<String> permissions) {\n" +
                        "        this.permissions = permissions;\n" +
                        "    }\n" +
                        "\n" +
                        "    public List<String> getRoles() {\n" +
                        "        return roles;\n" +
                        "    }\n" +
                        "\n" +
                        "    public void setRoles(List<String> roles) {\n" +
                        "        this.roles = roles;\n" +
                        "    }\n" +
                        "\n" +
                        "    public String getFreshToken() {\n" +
                        "        return freshToken;\n" +
                        "    }\n" +
                        "\n" +
                        "    public void setFreshToken(String freshToken) {\n" +
                        "        this.freshToken = freshToken;\n" +
                        "    }\n" +
                        "\n" +
                        "    public boolean isSecure() {\n" +
                        "        return isSecure;\n" +
                        "    }\n" +
                        "\n" +
                        "    public void setSecure(boolean secure) {\n" +
                        "        isSecure = secure;\n" +
                        "    }\n" +
                        "\n" +
                        "}\n";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/SecurityWrapper.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateTokenRepository(String path, String basePackage) throws FileNotFoundException {

        String content = "package #package.jwt;\n" +
                "\n" +
                "// Generated By Caracal Framework, powered by Dr.Adldoost :D" +
                "\n" +
                "import org.springframework.beans.factory.config.ConfigurableBeanFactory;\n" +
                "import org.springframework.context.annotation.Scope;\n" +
                "import org.springframework.stereotype.Repository;\n" +
                "import java.util.Calendar;\n" +
                "import java.util.Date;\n" +
                "import java.util.Map;\n" +
                "import java.util.concurrent.ConcurrentHashMap;\n" +
                "\n" +
                "@Repository\n" +
                "@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)\n" +
                "public class TokenRepository {\n" +
                "\n" +
                "    private Map<String, Date> tokenMap = new ConcurrentHashMap<>();\n" +
                "\n" +
                "    public void put(String username, Date date) {\n" +
                "        tokenMap.put(username, date);\n" +
                "    }\n" +
                "\n" +
                "    public Date getTokenIssueDate(String username) {\n" +
                "        return tokenMap.get(username);\n" +
                "    }\n" +
                "\n" +
                "    public void invalidateUserToken(String username) {\n" +
                "        Calendar cal = Calendar.getInstance();\n" +
                "        cal.setTime(new Date());\n" +
                "        cal.add(Calendar.SECOND, 2);\n" +
                "        tokenMap.put(username, cal.getTime());\n" +
                "    }\n" +
                "}";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/TokenRepository.java"))) {
            out.print(result);
        }
        return result;

    }

    private static String generateGeneralServiceInterface(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.service;\n" +
                "\n" +
                "\n" +
                "import com.caracal.data.Filter;\n" +
                "import com.caracal.data.api.DomainDto;\n" +
                "import com.caracal.data.api.qbe.CompareObject;\n" +
                "import com.caracal.data.api.qbe.RangeObject;\n" +
                "import com.caracal.data.api.qbe.SortObject;\n" +
                "import com.caracal.data.api.qbe.StringSearchType;\n" +
                "\n" +
//                "import #package.service.PagedResult;" +
                "\n" +
                "import java.io.Serializable;\n" +
                "import java.util.List;\n" +
                "\n" +
                "/**\n" +
                " *\n" +
                " * @author Generated by Caracal Framework, powered by Dr.Adldoost :D \n" +
                " * @param <D>\n" +
                " * @param <PK>\n" +
                " */\n" +
                "public interface GeneralService<D extends DomainDto, PK extends Serializable> {\n" +
                "\n" +
                "    List<D> paginate(Filter<D> filter);\n" +
                "\n" +
                "    D save(D entity);\n" +
                "\n" +
                "    void save(List<D> entities);\n" +
                "//\n" +
                "//    void create(D t);\n" +
                "//\n" +
                "//    void edit(D t);\n" +
                "\n" +
                "    void remove(PK id);\n" +
                "\n" +
                "    void bulkRemove(List<PK> entityIdList);\n" +
                "\n" +
                "    List<D> findByExample(D example, List<SortObject> sortObjectList, int startIndex, int pageSize, StringSearchType searchType);\n" +
                "\n" +
                "    List<D> findByExample(D example, int startIndex, int pageSize, StringSearchType searchType);\n" +
                "\n" +
                "    List<D> findByExample(D example);\n" +
                "\n" +
                "    List<D> findByExample(D example, List<SortObject> sortObjectList);\n" +
                "\n" +
                "    List<D> findByExample(D example, List<SortObject> sortObjectList, StringSearchType searchType);\n" +
                "\n" +
                "    List<D> findByExample(D example, List<SortObject> sortObjectList, int startIndex, int pageSize, StringSearchType searchType, List<RangeObject> rangeObjectList);\n" +
                "\n" +
                "    List<D> findByExample(D example, List<SortObject> sortObjectList, int startIndex, int pageSize, StringSearchType searchType, List<RangeObject> rangeObjectList, List<CompareObject> comparableList);\n" +
                "\n" +
                "    List<D> findByExample(D example, List<SortObject> sortObjectList, StringSearchType searchType, List<RangeObject> rangeObjectList);\n" +
                "\n" +
                "    List<D> findByExample(D example, StringSearchType searchType);\n" +
                "\n" +
                "    D findSingleByExample(D example, List<SortObject> sortObjectList, StringSearchType searchType);\n" +
                "\n" +
                "    D findSingleByExample(D example, List<SortObject> sortObjectList);\n" +
                "\n" +
                "    D findSingleByExample(D example, StringSearchType searchType);\n" +
                "\n" +
                "    D findSingleByExample(D example);\n" +
                "\n" +
                "    long countByExample(D example, StringSearchType searchType);\n" +
                "\n" +
                "    void removeByExample(D example, StringSearchType searchType);\n" +
                "\n" +
                "    D findByPrimaryKey(PK primaryKey);\n" +
                "\n" +
                "    D getDtoInstance();\n" +
                "\n" +
                "    PagedResult<D> findPagedByExample(D example, List<SortObject> sortObjectList, int startIndex, int pageSize, StringSearchType searchType, List<RangeObject> rangeObjectList, List<CompareObject> compareObjects);\n" +
                "\n" +
                "    Long countByExample(D example, StringSearchType searchType, List<RangeObject> rangeObjectList, List<CompareObject> compareObjects);" +
                "\n" +
                "}\n";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/GeneralService.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateSecurityServiceClass(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.service;\n" +
                "\n" +
                "import #package.common.BusinessExceptionCode;\n" +
                "import #package.common.SecurityServiceException;\n" +
                "import #package.jwt.SecurityWrapper;\n" +
                "import org.springframework.stereotype.Service;\n" +
                "\n" +
                "import java.util.ArrayList;\n" +
                "import java.util.Collections;\n" +
                "import #package.security.AccessRoles;\n" +
                "\n" +
                "@Service\n" +
                "public class SecurityService {\n" +
                "\n" +
                "    public SecurityWrapper authenticate(String username, String password) {\n" +
                "\n" +
                "        //Authentication should be done here...\n" +
                "        //Sample Authentication\n" +
                "        if(username.equalsIgnoreCase(\"admin\")\n" +
                "                && password.equalsIgnoreCase(\"admin\")) {\n" +
                "            SecurityWrapper securityWrapper = new SecurityWrapper();\n" +
                "            securityWrapper.setSecure(true);\n" +
                "            securityWrapper.setUsername(\"admin\");\n" +
                "            securityWrapper.setPermissions(new ArrayList<>());\n" +
                "            securityWrapper.setRoles(AccessRoles.getAllRoles());\n" +
                "            return securityWrapper;\n" +
                "        }\n" +
                "        else\n" +
                "            throw new SecurityServiceException(BusinessExceptionCode.ACCESS_DENIED.name());\n" +
                "\n" +
                "\n" +
                "    }\n" +
                "}\n";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/SecurityService.java"))) {
            out.print(result);
        }
        return result;

    }

    private static String generatePagedResultClass(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.service;\n" +
                "\n" +
                "import java.io.Serializable;\n" +
                "import java.util.List;\n" +
                "\n" +
                "public class PagedResult<T extends Serializable> implements Serializable {\n" +
                "\n" +
                "    private List<T> data;\n" +
                "    private Long count;\n" +
                "\n" +
                "    public List<T> getData() {\n" +
                "        return data;\n" +
                "    }\n" +
                "\n" +
                "    public void setData(List<T> data) {\n" +
                "        this.data = data;\n" +
                "    }\n" +
                "\n" +
                "    public Long getCount() {\n" +
                "        return count;\n" +
                "    }\n" +
                "\n" +
                "    public void setCount(Long count) {\n" +
                "        this.count = count;\n" +
                "    }\n" +
                "}";
        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/PagedResult.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateGeneralServiceImplClass(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.service;\n" +
                "\n" +
                "import com.caracal.data.Filter;\n" +
                "import com.caracal.data.api.DomainDto;\n" +
                "import com.caracal.data.api.DomainEntity;\n" +
                "import com.caracal.data.api.GenericEntityDAO;\n" +
                "import com.caracal.data.api.qbe.CompareObject;\n" +
                "import com.caracal.data.api.qbe.RangeObject;\n" +
                "import com.caracal.data.api.qbe.SortObject;\n" +
                "import com.caracal.data.api.qbe.StringSearchType;\n" +
                "import org.springframework.transaction.annotation.Transactional;\n" +
                "import java.io.Serializable;\n" +
                "import java.util.ArrayList;\n" +
                "import java.util.List;\n" +
                "\n" +
                "/**\n" +
                " *\n" +
                " * @author Generated By Caracal Framework, powered by Dr.Adldoost :D \n" +
                " * @param <E>\n" +
                " * @param <PK>\n" +
                " */\n" +
                "\n" +
                "public abstract class GeneralServiceImpl<D extends DomainDto, E extends DomainEntity, PK extends Serializable> implements GeneralService<D , PK> {\n" +
                "\n" +
                "    protected abstract GenericEntityDAO getDAO();\n" +
                "\n" +
                "    @Override\n" +
                "    @Transactional\n" +
                "    public D save(D entity) {\n" +
                "        return (D)entity.getInstance(getDAO().save(entity.toEntity()));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    @Transactional\n" +
                "    public void save(List<D> dtos) {\n" +
                "        if(dtos == null)\n" +
                "            return;\n" +
                "        List<E> entities = new ArrayList<>();\n" +
                "        dtos.forEach(d -> {\n" +
                "            entities.add((E)d.toEntity());\n" +
                "        });\n" +
                "        getDAO().save(entities);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    @Transactional\n" +
                "    public void remove(PK id) {\n" +
                "        getDAO().remove(id);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    @Transactional\n" +
                "    public void bulkRemove(List<PK> entityIdList) {\n" +
                "        getDAO().bulkRemove(entityIdList);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public List<D> findByExample(D example, List<SortObject> sortObjectList, int startIndex, int pageSize, StringSearchType searchType) {\n" +
                "        if(example == null)\n" +
                "            return null;\n" +
                "        List<E> entities = new ArrayList<>();\n" +
                "        return getDtoInstance().getInstance(getDAO().findByExample(example.toEntity(), sortObjectList, startIndex, pageSize, searchType));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public List<D> findByExample(D example, int startIndex, int pageSize, StringSearchType searchType) {\n" +
                "        if(example == null)\n" +
                "            return null;\n" +
                "        return getDtoInstance().getInstance(getDAO().findByExample(example.toEntity(), startIndex, pageSize, searchType));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public List<D> findByExample(D example) {\n" +
                "        if(example == null)\n" +
                "            return null;\n" +
                "        return getDtoInstance().getInstance(getDAO().findByExample(example.toEntity()));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public List<D> findByExample(D example, List<SortObject> sortObjectList) {\n" +
                "        if(example == null)\n" +
                "            return null;\n" +
                "        return getDtoInstance().getInstance(getDAO().findByExample(example.toEntity(), sortObjectList));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public List<D> findByExample(D example, List<SortObject> sortObjectList, StringSearchType searchType) {\n" +
                "        if(example == null)\n" +
                "            return null;\n" +
                "        return getDtoInstance().getInstance(getDAO().findByExample(example.toEntity(), sortObjectList, searchType));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public List<D> findByExample(D example, List<SortObject> sortObjectList, int startIndex, int pageSize, StringSearchType searchType, List<RangeObject> rangeObjectList){\n" +
                "        if(example == null)\n" +
                "            return null;\n" +
                "        return getDtoInstance().getInstance(getDAO().findByExample(example.toEntity(), sortObjectList, startIndex, pageSize, searchType, rangeObjectList, null));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public List<D> findByExample(D example, List<SortObject> sortObjectList, int startIndex, int pageSize, StringSearchType searchType, List<RangeObject> rangeObjectList, List<CompareObject> comparableList){\n" +
                "        if(example == null)\n" +
                "            return null;\n" +
                "        return getDtoInstance().getInstance(getDAO().findByExample(example.toEntity(), sortObjectList, startIndex, pageSize, searchType, rangeObjectList, comparableList));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public List<D> findByExample(D example, List<SortObject> sortObjectList, StringSearchType searchType, List<RangeObject> rangeObjectList) {\n" +
                "        return getDtoInstance().getInstance(getDAO().findByExample(example.toEntity(), sortObjectList, searchType, rangeObjectList));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public List<D> findByExample(D example, StringSearchType searchType) {\n" +
                "        if(example == null)\n" +
                "            return null;\n" +
                "        return getDtoInstance().getInstance(getDAO().findByExample(example.toEntity(), searchType));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public D findSingleByExample(D example, List<SortObject> sortObjectList, StringSearchType searchType) {\n" +
                "\n" +
                "        if(example == null)\n" +
                "            return null;\n" +
                "        return (D)getDtoInstance().getInstance(getDAO().findSingleByExample(example.toEntity(), sortObjectList, searchType));\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public D findSingleByExample(D example, List<SortObject> sortObjectList) {\n" +
                "        if(example == null)\n" +
                "            return null;\n" +
                "        return (D)getDtoInstance().getInstance(getDAO().findSingleByExample(example.toEntity(), sortObjectList));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public D findSingleByExample(D example, StringSearchType searchType) {\n" +
                "        if(example == null)\n" +
                "            return null;\n" +
                "        return (D)getDtoInstance().getInstance(getDAO().findSingleByExample(example.toEntity(), searchType));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public D findSingleByExample(D example) {\n" +
                "        if(example == null)\n" +
                "            return null;\n" +
                "        return (D)getDtoInstance().getInstance(getDAO().findSingleByExample(example.toEntity()));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public long countByExample(D example, StringSearchType searchType) {\n" +
                "        if(example == null)\n" +
                "            return 0;\n" +
                "        return getDAO().countByExample(example.toEntity(), searchType);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    @Transactional\n" +
                "    public void removeByExample(D example, StringSearchType searchType) {\n" +
                "        if(example == null)\n" +
                "            return;\n" +
                "        getDAO().removeByExample(example.toEntity(), searchType);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public D findByPrimaryKey(PK primaryKey) {\n" +
                "        if(primaryKey == null)\n" +
                "            return null;\n" +
                "        E entity = (E)getDAO().findByPrimaryKey(primaryKey);\n" +
                "        if(entity == null)\n" +
                "            return null;\n" +
                "        return (D)getDtoInstance().getInstance(entity);\n" +
                "    }\n" +
                "\n" +
                "    public List<D> paginate(Filter<D> filter) {\n" +
                "\n" +
                "        throw new RuntimeException(\"NOT IMPLEMENTED...\");\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public abstract D getDtoInstance();\n" +
                "    @Override\n" +
                "    public Long countByExample(D example, StringSearchType searchType, List<RangeObject> rangeObjectList, List<CompareObject> compareObjects) {\n" +
                "        if(example == null)\n" +
                "            return 0L;\n" +
                "        return getDAO().countByExample(example.toEntity(), searchType, rangeObjectList, compareObjects);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public PagedResult<D> findPagedByExample(D example, List<SortObject> sortObjectList, int startIndex, int pageSize, StringSearchType searchType, List<RangeObject> rangeObjectList, List<CompareObject> compareObjects) {\n" +
                "        List<D> data = findByExample(example, sortObjectList, startIndex, pageSize, searchType, rangeObjectList, compareObjects);\n" +
                "        Long count = countByExample(example, searchType, rangeObjectList, compareObjects);\n" +
                "        PagedResult<D> pagedResult = new PagedResult<>();\n" +
                "        pagedResult.setData(data);\n" +
                "        pagedResult.setCount(count);\n" +
                "        return pagedResult;\n" +
                "    }" +
                "}\n";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/GeneralServiceImpl.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateApplicationDotPropertiesFile(String path,
                                                               String datasourceUrl,
                                                               String dataSourceUserName,
                                                               String dataSourcePassword,
                                                               String contextPath,
                                                               String portNumber,
                                                               String basePackage,
                                                               String fileUploadPath) throws FileNotFoundException {
        String content = "spring.datasource.url=" + datasourceUrl + "\n" +
                "spring.datasource.username=" + dataSourceUserName + "\n" +
                "spring.datasource.password=" + dataSourcePassword + "\n" +
                "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver\n" +
                "spring.jpa.hibernate.ddl-auto=update\n" +
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL5InnoDBDialect\n" +
                "spring.jpa.properties.hibernate.dialect.storage_engine=innodb\n" +
                "\n" +
                "server.servlet.context-path=/" + contextPath + "\n" +
                "server.port=" + portNumber + "\n" +
                "\n" +
                "logging.file=target/logs/application.log\n" +
                "logging.level." + basePackage + "=INFO\n" +
                "\n" +
                "\nfile.upload-dir=" + fileUploadPath + "\n";

        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/application.properties"))) {
            out.print(content);
        }
        return content;

    }

    private static String generateConfigPropertiesFile(String path, String jwtkey, String jwtExpiration) throws FileNotFoundException {
        String content = "jwtkey = " + jwtkey + "\n" +
                "jwtExpiration = " + jwtExpiration + "\n" +
                "\n";
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/config.properties"))) {
            out.print(content);
        }
        return content;
    }

    private static String generateErrorCodeProperties(String path) throws IOException {
        String content = "UNAUTHORIZED=اطلاعات ورود صحیح نیست\n" +
                "SECURITY_UNHANDLED_EXCEPTION=خطای دسترسی کاربر\n" +
                "JWT_PARSE_ERROR=اطلاعات دسترسی مخدوش می باشد\n" +
                "INVALID_LOGIN_TOKEN=دسترسی امکان پذیر نمی باشد. لطفا مجدد وارد شوید.\n" +
                "ACCESS_DENIED=دسترسی امکان پذیر نمی باشد\n" +
                "COULD_NOT_STORED_FILE_RETRY=در ذخیره فایل مشکل پیش آمد. لطفا مجددا تلاش کنید\n" +
                "COULD_NOT_CREATE_DIRECTORY=ذخیره فایل در مسیر تعیین شده امکان پذیر نیست\n" +
                "FILE_NOT_FOUND_PATH=فایل مورد نظر یافت نشد\n" +
                "BAD_INPUT=اطلاعات وارد شده صحیح نیست\n" +
                "IS_MANDATORY=الزامی است\n" +
                "NOT_VALID=قابل قبول نیست\n" +
                "IS_UNIQUE=تکراری است\n" +
                "\n";
        File file = new File(path);
        file.mkdirs();

        try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(path + "/errorcodes.properties"), StandardCharsets.UTF_8)) {
            out.write(content);
        }
        return content;
    }

    public static String generateFarsiCodesProperties(String path, List<EntityDefinition> entityDefinitionList) throws IOException {
        StringBuilder content = new StringBuilder("");
        entityDefinitionList.forEach(e -> {
            e.getEntityFieldDefinitionList().forEach(f -> {
                content.append(e.getName()).append(".").append(f.getName()).append("=").append(f.getFarsiName()).append("\n");
            });
        });
        File file = new File(path);
        file.mkdirs();

        try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(path + "/farsicodes.properties"), StandardCharsets.UTF_8)) {
            out.write(content.toString());
        }

        return content.toString();
    }

    public static String camelToSnake(String phrase) {
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        String snake = phrase
                .replaceAll(regex, replacement)
                .toLowerCase();
        System.out.println(snake);
        return snake;
    }

    public static String snakeToCamel(String phrase) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, phrase);
    }

    public static List<String> getBaseTypes() {
        List<String> typeNames = new ArrayList<>();
        typeNames.add("Integer");
        typeNames.add("Boolean");
        typeNames.add("Long");
        typeNames.add("Float");
        typeNames.add("Double");
        typeNames.add("Date");
        typeNames.add("Short");
        typeNames.add("String");
        typeNames.add("Char");
        typeNames.add("Decimal");
        typeNames.add("BigInteger");
        typeNames.add("BigDecimal");
        typeNames.add("Byte");
        typeNames.add("Byte[]");
        typeNames.add("int");
        typeNames.add("boolean");
        typeNames.add("long");
        typeNames.add("float");
        typeNames.add("double");
        typeNames.add("short");
        typeNames.add("decimal");
        typeNames.add("byte");
        typeNames.add("byte[]");
        return typeNames;
    }

//    public static List<String> getComponentTypes() {
//        List<String> typeNames = new ArrayList<>();
//        typeNames.add("DropDown");
//        typeNames.add("RadioButtonList");
//        typeNames.add("CheckBoxList");
//        typeNames.add("RadioButton");
//        typeNames.add("CheckBox");
//
//        return typeNames;
//    }

    private static StringBuilder removeLastChar(StringBuilder str) {
        return new StringBuilder(str.substring(0, str.length() - 1));
    }

}
