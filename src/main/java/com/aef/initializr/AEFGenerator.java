package com.aef.initializr;

import com.google.common.base.CaseFormat;
import javafx.util.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AEFGenerator {

    public static void main(String args[]) throws IOException {


        String dataSourceUrl = InitializrReaderUtility.getResourceProperity("datasource.url");
        String dataSourceUsername = InitializrReaderUtility.getResourceProperity("datasource.username");
        String dataSourcePassword = InitializrReaderUtility.getResourceProperity("datasource.password");
        String contextPath = InitializrReaderUtility.getResourceProperity("context.path");
        String portNumber = InitializrReaderUtility.getResourceProperity("port.number");
        String basePackage = InitializrReaderUtility.getResourceProperity("base.package");
        String targetPath = InitializrReaderUtility.getResourceProperity("target.path");
        String projectName = InitializrReaderUtility.getResourceProperity("project.name");
        String projectDescription = InitializrReaderUtility.getResourceProperity("project.description");
        String artifactId = InitializrReaderUtility.getResourceProperity("maven.artifact.id");
        String groupId = InitializrReaderUtility.getResourceProperity("maven.group.id");
        String jwtKey = InitializrReaderUtility.getResourceProperity("jwt.key");
        String jwtExpiration = InitializrReaderUtility.getResourceProperity("jwt.expiration");
        boolean validationEnabled = Boolean.parseBoolean(InitializrReaderUtility.getResourceProperity("validation.enabled"));

        String frontProjectName = InitializrReaderUtility.getResourceProperity("front.project.name");
        String frontProjectPath = InitializrReaderUtility.getResourceProperity("front.target.path");
        String frontProjectFarsiPath = InitializrReaderUtility.getResourceProperity("front.project.farsi.name");

        List<String> entities = findEntities();
        LinkedHashMap<String, String> farsiNames = findEntitiesFarsiNames();
        LinkedHashMap<String, LinkedHashMap<String, String>> farsiFields = findFieldsFarsiNames();


        FrontGenerator frontGenerator = new FrontGenerator();
        frontGenerator.generateStructureOfProject(frontProjectName, frontProjectPath);

        generateAll(basePackage,
                entities,
                targetPath,
                projectName,
                projectDescription,
                artifactId,
                groupId,
                dataSourceUrl,
                dataSourceUsername,
                dataSourcePassword,
                contextPath,
                portNumber,
                jwtKey,
                jwtExpiration,
                validationEnabled,
                frontProjectPath,
                farsiNames,
                farsiFields);



    }

    public static void generateAll( String basePackage,
                                    List<String> entityNameList,
                                    String targetPath,
                                    String projectName,
                                    String projectDescription,
                                    String artifcatId,
                                    String groupId,
                                    String dataSourceUrl,
                                    String dataSourceUsername,
                                    String dataSourcePassword,
                                    String contextPath,
                                    String portNumber,
                                    String jwtKey,
                                    String jwtExpiration,
                                    boolean validationEnabled,
                                    String frontProjectPath,
                                    LinkedHashMap<String, String> farsiNames,
                                    LinkedHashMap<String, LinkedHashMap<String, String>> farsiFields) throws IOException {


        generateStructureOfProject(projectName, basePackage, targetPath);
        String sourcePackageTarget = generateSourceTargetPackagePath(targetPath, basePackage, projectName);

        File modelPath = new File( sourcePackageTarget + "/model");
        File daoPath = new File(sourcePackageTarget + "/dao");
        File servicePath = new File(sourcePackageTarget + "/service");
        File dtoPath = new File(sourcePackageTarget + "/dto");
        File restPath = new File(sourcePackageTarget + "/rest");
        File commonPath = new File(sourcePackageTarget + "/common");
        File jwtPath = new File(sourcePackageTarget + "/jwt");
        File securityPath = new File(sourcePackageTarget + "/security");
        File rootPath = new File(targetPath + "/" + projectName);
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


        if(checkGeneration("generate.pom")) {
            generatePOMFile(rootPath.getPath(), projectName, projectDescription, artifcatId, groupId);
        }

        if(checkGeneration("generate.runner")) {
            generateRunnerClass(sourcePackageTarget, basePackage, projectName);
        }

        if(checkGeneration("generate.properties")) {
            generateApplicationDotPropertiesFile(resourcePath.getPath(), dataSourceUrl, dataSourceUsername, dataSourcePassword, contextPath, portNumber);
        }


        if(checkGeneration("generate.config")) {
            generateConfigPropertiesFile(resourcePath.getPath(), jwtKey, jwtExpiration);
        }

        if(checkGeneration("generate.errorcodes")) {
            generateErrorCodeProperties(resourcePath.getPath());
        }

        if(checkGeneration("generate.security.config")) {
            generateSecurityConfig(basePackage, securityPath.getPath());
        }

        if(checkGeneration("generate.security.roles")) {
            generateAccessRoles(basePackage, securityPath.getPath());
        }

        entityNameList.stream().forEach(entityName -> {
            LinkedHashMap<String, String> fields = findFieldsForEntity(entityName);
            try {
                if(checkGeneration("generate.entity")) {
                    generateEntity(basePackage, entityName, fields, modelPath.getPath());
                }
                if(checkGeneration("generate.dto")) {
                    generateDto(basePackage, entityName, fields, dtoPath.getPath(), validationEnabled);
                }
                if(checkGeneration("generate.dao")) {
                    generateDao(basePackage, entityName, daoPath.getPath());
                }
                if(checkGeneration("generate.service")) {
                    generateService(basePackage, entityName, servicePath.getPath());
                }
                if(checkGeneration("generate.general.service")) {
                    //fill service package
                    generateGeneralServiceInterface(servicePath.getPath(), basePackage);
                    generateGeneralServiceImplClass(servicePath.getPath(), basePackage);
                    generatePagedResultClass(servicePath.getPath(), basePackage);
                }
                if(checkGeneration("generate.rest")) {
                    generateRestService(basePackage, entityName, fields, restPath.getPath());
                }

                FrontGenerator.generateEntityComponent(frontProjectPath , entityName, entityName, fields);
                FrontGenerator.generateEntityService(frontProjectPath , entityName);
                FrontGenerator.generateEntityHtmlView(frontProjectPath, entityName, farsiNames.get(entityName), fields, farsiFields.get(entityName));


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
        FrontGenerator.refactorAppModule(frontProjectPath, entityNameList);
        FrontGenerator.generateRouter(frontProjectPath, entityNameList);
        FrontGenerator.generateSidebarComponent(frontProjectPath, entityNameList, farsiNames);



        //fill common package
        if(checkGeneration("generate.common")){
            generateBusinessExceptionCodeClass(commonPath.getPath(), basePackage);
            generateConfigReaderUtilClass(commonPath.getPath(), basePackage);
            generateAEFExceptionHandler(commonPath.getPath(), basePackage);
            generateFarsiCodeReaderUtility(commonPath.getPath(), basePackage);
            generateFarsiCodeReaderUtility(commonPath.getPath(), basePackage);
            generateErrorCodeReaderUtilClass(commonPath.getPath(), basePackage);
            generateRandomStringCodeUtility(commonPath.getPath(), basePackage);
            generateRestErrorMessageClass(commonPath.getPath(), basePackage);
            generateSecurityServiceExceptionClass(commonPath.getPath(), basePackage);
        }

        //fill jwt package
        if(checkGeneration("generate.jwt")) {
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

        if(checkGeneration("generate.security.service")) {
            generateSecurityServiceClass(servicePath.getPath(), basePackage);
        }
        if(checkGeneration("generate.login.rest")) {
            generateLoginRestService(restPath.getPath(), basePackage);
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

    private static String generateAccessRoles(String basePackage, String path) throws FileNotFoundException {

        List<String> entities = findEntities();
        if(entities == null || entities.isEmpty())
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

                for (String e : entities) {
                    content += "    public static final String ROLE_FIND_" + camelToSnake(e).toUpperCase() + " = \"ROLE_FIND_" + camelToSnake(e).toUpperCase() + "\";\n";
                    content += "    public static final String ROLE_SEARCH_" + camelToSnake(e).toUpperCase() + " = \"ROLE_SEARCH_" + camelToSnake(e).toUpperCase() + "\";\n";
                    content += "    public static final String ROLE_SAVE_" + camelToSnake(e).toUpperCase() + " = \"ROLE_SAVE_" + camelToSnake(e).toUpperCase() + "\";\n";
                    content += "    public static final String ROLE_REMOVE_" + camelToSnake(e).toUpperCase() + " = \"ROLE_REMOVE_" + camelToSnake(e).toUpperCase() + "\";\n";
                }

                content += "\n\n";
                content += "    public static List<String> getAllRoles() {\n";
                content += "        List<String> roles = new ArrayList<>();\n";
                for (String e : entities) {
                    content += "        roles.add(ROLE_FIND_" + camelToSnake(e).toUpperCase() + ");\n";
                    content += "        roles.add(ROLE_SEARCH_" + camelToSnake(e).toUpperCase() + ");\n";
                    content += "        roles.add(ROLE_SAVE_" + camelToSnake(e).toUpperCase() + ");\n";
                    content += "        roles.add(ROLE_REMOVE_" + camelToSnake(e).toUpperCase() + ");\n";
                }
                content += "    return roles;";
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

    private static boolean checkGeneration(String key) {

        try {
            String value = InitializrReaderUtility.getResourceProperity(key);
            if(value.equalsIgnoreCase("true"))
                return true;

        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private static LinkedHashMap<String, String> findFieldsForEntity(String entityName) {
        Enumeration<String> keys = InitializrReaderUtility.getResourceKeys();
        List<String> keyList = Collections.list(keys);

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();

        keyList.stream().forEach(k -> {
            if(k.contains(entityName + ".field.type.")) {
                String[] parts = k.split("\\.");
                String type = InitializrReaderUtility.getResourceProperity(k);
                fields.put(parts[3], type);
            }
        });
        return fields;
    }

    private static List<String> findEntities() {
        Enumeration<String> keys = InitializrReaderUtility.getResourceKeys();
        List<String> keyList = Collections.list(keys);

        List<String> entities = new ArrayList<>();

        keyList.stream().forEach(k -> {
            if(k.contains("entity.name.")) {
                String[] parts = k.split("\\.");
                String type = InitializrReaderUtility.getResourceProperity(k);
                entities.add(parts[2]);
            }
        });
        return entities;
    }

    private static LinkedHashMap<String, String> findEntitiesFarsiNames() {
        Enumeration<String> keys = InitializrReaderUtility.getResourceKeys();
        List<String> keyList = Collections.list(keys);

        LinkedHashMap<String, String> entities = new LinkedHashMap<>();

        keyList.stream().forEach(k -> {
            if(k.contains("entity.farsi.name.")) {
                String[] parts = k.split("\\.");
                String farsi = InitializrReaderUtility.getResourceProperity(k);
                entities.put(parts[3], farsi);
            }
        });
        return entities;
    }

    private static LinkedHashMap<String, LinkedHashMap<String, String>> findFieldsFarsiNames() {

        List<String> entites = findEntities();
        LinkedHashMap<String, LinkedHashMap<String, String>> result = new LinkedHashMap<>();

        entites.forEach(e -> {
            Enumeration<String> keys = InitializrReaderUtility.getResourceKeys();
            List<String> keyList = Collections.list(keys);

            LinkedHashMap<String, String> fields = new LinkedHashMap<>();

            keyList.stream().forEach(k -> {
                if(k.contains(e + ".field.farsi.")) {
                    String[] parts = k.split("\\.");
                    String farsi = InitializrReaderUtility.getResourceProperity(k);
                    fields.put(parts[3], farsi);
                }
            });
            result.put(e, fields);
        });


        return result;
    }

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

    private static String generatePOMFile(String path, String projectName, String projectDescription, String artifactId, String groupId ) throws FileNotFoundException {

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
                "            <groupId>org.springframework.ws</groupId>\n" +
                "            <artifactId>spring-ws-core</artifactId>\n" +
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
                "            <artifactId>aef3-data</artifactId>\n" +
                "            <version>1.2-SNAPSHOT</version>\n" +
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
                "            <plugin>\n" +
                "                <groupId>org.apache.maven.plugins</groupId>\n" +
                "                <artifactId>maven-dependency-plugin</artifactId>\n" +
                "                <executions>\n" +
                "                    <execution>\n" +
                "                        <id>unpack</id>\n" +
                "                        <phase>package</phase>\n" +
                "                        <goals>\n" +
                "                            <goal>unpack</goal>\n" +
                "                        </goals>\n" +
                "                        <configuration>\n" +
                "                            <artifactItems>\n" +
                "                                <artifactItem>\n" +
                "                                    <groupId>${project.groupId}</groupId>\n" +
                "                                    <artifactId>${project.artifactId}</artifactId>\n" +
                "                                    <version>${project.version}</version>\n" +
                "                                </artifactItem>\n" +
                "                            </artifactItems>\n" +
                "                        </configuration>\n" +
                "                    </execution>\n" +
                "                </executions>\n" +
                "            </plugin>\n" +
                "        </plugins>\n" +
                "    </build>\n" +
                "\n" +
                "</project>";

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

    public static String generateEntity(String basePackage, String entityName, LinkedHashMap<String, String> fields, String targetPath) throws FileNotFoundException {
        StringBuilder content = new StringBuilder("package #package.model;\n" +
                "\n" +
                "import com.aef3.data.api.DomainEntity;\n" +
                "\n" +
                "import javax.persistence.*;\n" +
                "import java.util.Date;\n" +
                "import java.util.Objects;\n" +
                "\n" +
                "\n" +
                "/* Generated By AEF Generator ( Powered by Dr.Adldoost :D ) */\n" +
                "\n" +
                "@Entity\n" +
                "@Table(name = \"").append(camelToSnake(entityName)).append("\")\n")

                .append("public class #Entity implements DomainEntity {\n")
                .append("\n");

        for (Map.Entry<String, String> entry : fields.entrySet())
        {
            content.append("\n");
            if(entry.getKey().equalsIgnoreCase("id")) {
                content.append("    @Id\n")
                        .append("    @Column(name = \"id\")\n")
                        .append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n")
                        .append("    private ").append(entry.getValue()).append(" ").append(entry.getKey()).append(";")
                        .append("\n");
            }

            else {
                if(getBaseTypes().contains(entry.getValue())) {
                    content.append("    @Column(name = \"").append(camelToSnake(entry.getKey())).append("\"");
                    if(!findFieldNullability(entityName, entry.getKey())) {
                        content.append(", nullable = " + false );
                    }
                    //no need to write true, its default value
//                    {
//                        content.append(", nullable = " + true );
//                    }
                    String length = findFieldLength(entityName, entry.getKey());
                    if(length != null) {
                        content.append(", length = ").append(length);
                    }
                    content.append(")\n");
                    if(entry.getValue().equals("Date")) {
                        content.append("    @Temporal(TemporalType.TIMESTAMP)\n");
                    }
                } else {
                    content.append("    @JoinColumn(name = \"").append(camelToSnake(entry.getKey())).append("\", referencedColumnName = \"id\")\n")
                            .append("    @ManyToOne\n");
                }
                content.append("    private ").append(entry.getValue()).append(" ").append(entry.getKey()).append(";\n");
            }
        }

        content.append("\n\n");

        for (Map.Entry<String, String> entry : fields.entrySet())
        {
            String firstCharFieldName = entry.getKey().substring(0, 1);
            String upperCaseCharFieldName = entry.getKey().replaceFirst(firstCharFieldName, firstCharFieldName.toUpperCase());
            content.append("\n")
                    .append("    public ").append(entry.getValue()).append(" get").append(upperCaseCharFieldName).append("() {\n")
                    .append("        return ").append(entry.getKey()).append(";\n")
                    .append("    }\n")
                    .append("\n")
                    .append("    public void set").append(upperCaseCharFieldName).append("(").append(entry.getValue()).append(" ").append(entry.getKey()).append(") {\n")
                    .append("       this.").append(entry.getKey()).append(" = ").append(entry.getKey()).append(";\n")
                    .append("    }\n\n");
        }
        content.append("\n");

        content.append("    @Override\n")
                .append("    public boolean equals(Object o) {\n")
                .append("        if (this == o) return true;\n")
                .append("        if (!(o instanceof #Entity)) return false;\n")
                .append("        #Entity #entity = (#Entity) o;\n")
                .append("        return Objects.equals(id, #entity.id);\n")
                .append("    }\n")
                .append("\n")
                .append("    @Override\n")
                .append("    public int hashCode() {\n")
                .append("\n")
                .append("        return Objects.hash(id);\n")
                .append("    }\n")
                .append("\n")
                .append("    @Override\n")
                .append("    public String toString() {\n")
                .append("        return \"#Entity{\" +\n")
                .append("                \"id=\" + id +\n")
                .append("                '}';\n")
                .append("    }\n")
                .append("}");

        String firstChar = entityName.substring(0, 1);
        String entityInstanceName = entityName.replaceFirst(firstChar, firstChar.toLowerCase());
        String result = content.toString();
        result = result.replaceAll("#package", basePackage)
                .replaceAll("#Entity", entityName)
                .replaceAll("#entity", entityInstanceName);

        System.out.printf(result);
        try (PrintStream out = new PrintStream(new FileOutputStream(targetPath + "/" + entityName + ".java"))) {
            out.print(result);
        }
        return result;
    }

    private static boolean findFieldNullability(String entityName, String fieldName) {
        try {
            String s = InitializrReaderUtility.getResourceProperity(entityName + ".field.nullable." + fieldName);
            if(s.equalsIgnoreCase("false"))
                return false;
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private static String findFieldLength(String entityName, String fieldName) {
        try {
            String s = InitializrReaderUtility.getResourceProperity(entityName + ".field.length." + fieldName);
            if(Integer.parseInt(s) > 0){
                return s;
            }
            String type = InitializrReaderUtility.getResourceProperity(entityName + ".field.type." + fieldName);
            if(type.equalsIgnoreCase("Integer")
                    || type.equalsIgnoreCase("int")
                    || type.equalsIgnoreCase("double")
                    || type.equalsIgnoreCase("long")
                    || type.equalsIgnoreCase("float")
                    || type.equalsIgnoreCase("byte")) {
                return "10";
            }
            if(type.equalsIgnoreCase("String")
                    || type.equalsIgnoreCase("char")) {
                return "100";
            }
        } catch (Exception e) {
            return null;
        }
        return "10";
    }

    private static String generateRunnerClass(String path, String basePackage, String projectName) throws FileNotFoundException {
        String content = "package #package;\n" +
                "\n" +
                "import org.springframework.boot.SpringApplication;\n" +
                "import org.springframework.boot.autoconfigure.SpringBootApplication;\n" +
                "\n" +
                "@SpringBootApplication\n" +
                "public class #projNameApplication {\n" +
                "\n" +
                "\tpublic static void main(String[] args) {\n" +
                "\t\tSpringApplication.run(#projNameApplication.class, args);\n" +
                "\t}\n" +
                "}\n";

        String camelCaseProjectName = AEFGenerator.snakeToCamel(projectName.replaceAll("-", "_"));
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

    private static void generateDao(String basePackage, String entityName, String targetPath) throws FileNotFoundException {
        String content =

                "package #package.dao;\n" +
                        "\n" +
                        "import com.aef3.data.impl.AbstractDAOImpl;\n" +
                        "import #package.model.#Entity;\n" +
                        "import org.springframework.stereotype.Repository;\n" +
                        "\n" +
                        "\n" +
                        "/* Generated By AEF Generator ( Powered by Dr.Adldoost :D ) */\n" +
                        "\n" +
                        "@Repository\n" +
                        "public class #EntityDao extends AbstractDAOImpl<#Entity, Long> {\n" +
                        "\n" +
                        "    public #EntityDao() {\n" +
                        "        super(#Entity.class);\n" +
                        "    }\n" +
                        "}";

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

    private static void generateService(String basePackage, String entityName, String targetPath) throws FileNotFoundException {
        String content =

                "package #package.service;\n" +
                        "\n" +
                        "import com.aef3.data.api.GenericEntityDAO;\n" +
                        "import #package.dao.#EntityDao;\n" +
                        "import #package.dto.#EntityDto;\n" +
                        "import #package.model.#Entity;\n" +
                        "import org.springframework.beans.factory.annotation.Autowired;\n" +
                        "import org.springframework.stereotype.Service;\n" +
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
                        "}\n";

        String firstChar = entityName.substring(0, 1);
        String entityInstanceName = entityName.replaceFirst(firstChar, firstChar.toLowerCase());

        String result = content.replaceAll("#package", basePackage)
                .replaceAll("#Entity", entityName)
                .replaceAll("#entity", entityInstanceName);

        System.out.printf(result);

        try (PrintStream out = new PrintStream(new FileOutputStream(targetPath + "/" + entityName + "Service.java"))) {
            out.print(result);
        }

    }

    private static String generateDto(String basePackage, String entityName, LinkedHashMap<String, String> fieldMap, String targetPath, boolean validationEnabled) throws FileNotFoundException {
        String result = generateDtoHeader();
        result += generateDtoFields(fieldMap, validationEnabled, entityName);
        result += generateDtoMappings(fieldMap, entityName);
        result += generateDtoOverrideMethods(entityName);
        result += generateFooter();

        String firstChar = entityName.substring(0, 1);
        String entityInstanceName = entityName.replaceFirst(firstChar, firstChar.toLowerCase());
        result = result.replaceAll("#package", basePackage)
                .replaceAll("#Entity", entityName)
                .replaceAll("#entity", entityInstanceName);

        System.out.printf(result);
        try (PrintStream out = new PrintStream(new FileOutputStream( targetPath + "/" + entityName + "Dto.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateDtoHeader() {

        String content = "package #package.dto;\n" +
                "\n" +
                "import com.aef3.data.api.DomainDto;\n" +
                "import com.fasterxml.jackson.annotation.JsonIgnore;\n" +
                "import #package.model.#Entity;\n" +
                "import javax.validation.constraints.NotNull;\n" +
                "import javax.validation.constraints.NotEmpty;\n" +
                "import java.util.Date;\n" +
                "\n" +
                "\n" +
                "/* Generated By AEF Generator ( Powered by Dr.Adldoost :D ) */\n" +
                "\n" +
                "public class #EntityDto implements DomainDto<#Entity, #EntityDto> {";


        return content;

    }

    private static String generateDtoFields(LinkedHashMap<String, String> fields, boolean validationEnabled, String entityName) {

        StringBuilder content = new StringBuilder();

        content.append("\n\n");

        for (Map.Entry<String, String> entry : fields.entrySet())
        {
            if(getBaseTypes().contains(entry.getValue())) {
                if(validationEnabled) {
                    if(!findFieldNullability(entityName, entry.getKey())) {
                        if(entry.getValue().trim().equalsIgnoreCase("String")) {
                            content.append("\n    @NotEmpty(message = \"{").append(entry.getKey()).append(".should.not.be.Empty}\")");
                        } else {
                            content.append("\n    @NotNull(message = \"{").append(entry.getKey()).append(".should.not.be.null}\")");
                        }
                    }
                }
                content.append("\n    private ").append(entry.getValue()).append(" ").append(entry.getKey()).append(";");
            } else {
                content.append("\n    private ").append(entry.getValue() + "Dto").append(" ").append(entry.getKey()).append(";");
            }
        }

        content.append("\n \n");

        for (Map.Entry<String, String> entry : fields.entrySet())
        {

            String firstCharFieldName = entry.getKey().substring(0, 1);
            String upperCaseCharFieldName = entry.getKey().replaceFirst(firstCharFieldName, firstCharFieldName.toUpperCase());

            if(getBaseTypes().contains(entry.getValue())) {
                content.append("\n    public " + entry.getValue() + " get" + upperCaseCharFieldName + "() {\n" +
                        "        return " + entry.getKey() + ";\n" +
                        "    }\n" +

                        "    public void set" + upperCaseCharFieldName + "(" + entry.getValue() + " " + entry.getKey() + ") {\n" +
                        "        this." + entry.getKey() + " = " + entry.getKey() + ";\n" +
                        "    }" +
                        "\n");

            } else {

                content.append("\n    public " + entry.getValue() + "Dto" + " get" + upperCaseCharFieldName + "() {\n" +
                        "        return " + entry.getKey() + ";\n" +
                        "    }\n" +

                        "    public void set" + upperCaseCharFieldName + "(" + entry.getValue() + "Dto" + " " + entry.getKey() + ") {\n" +
                        "        this." + entry.getKey() + " = " + entry.getKey() + ";\n" +
                        "    }" +
                        "\n");
            }


        }
        String result = content.toString();
        return result;

    }

    private static String generateDtoMappings(LinkedHashMap<String, String> fields, String entityName) {

        String firstChar = entityName.substring(0, 1);
        String entityInstanceName = entityName.replaceFirst(firstChar, firstChar.toLowerCase());
        StringBuilder content = new StringBuilder();

        content.append("\n\n");


        //toDto Method

        content.append("\n    public static #EntityDto toDto(#Entity #entity) {\n\n" +
                "        if(#entity == null)\n" +
                "            return null; \n" +
                "        #EntityDto dto = new #EntityDto();");


        for (Map.Entry<String, String> entry : fields.entrySet())
        {
            String firstCharFieldName = entry.getKey().substring(0, 1);
            String upperCaseCharFieldName = entry.getKey().replaceFirst(firstCharFieldName, firstCharFieldName.toUpperCase());
            if(getBaseTypes().contains(entry.getValue())) {
                content.append("\n        dto.set").append(upperCaseCharFieldName).append("(").append(entityInstanceName).append(".get").append(upperCaseCharFieldName).append("()").append(");");
            }
            else {
                content.append("\n        dto.set").append(upperCaseCharFieldName).append("(")
                        .append(entry.getValue()).append("Dto.toDto(")
                        .append(entityInstanceName).append(".get").append(upperCaseCharFieldName).append("()")
                        .append(")")
                        .append(");");
            }
        }

        content.append("\n        return dto;");

        content.append("\n  }");


        content.append("\n\n");


        //toEntity method
        content.append("\n    public static #Entity toEntity(#EntityDto dto) {\n\n" +
                "        if(dto == null)\n" +
                "            return null; \n" +
                "        #Entity #entity = new #Entity();");


        for (Map.Entry<String, String> entry : fields.entrySet())
        {
            String firstCharFieldName = entry.getKey().substring(0, 1);
            String upperCaseCharFieldName = entry.getKey().replaceFirst(firstCharFieldName, firstCharFieldName.toUpperCase());
            if(getBaseTypes().contains(entry.getValue())) {
                content.append("\n        #entity.set").append(upperCaseCharFieldName).append("(").append("dto").append(".get").append(upperCaseCharFieldName).append("()").append(");");
            }
            else {
                content.append("\n        #entity.set").append(upperCaseCharFieldName).append("(")
                        .append(entry.getValue()).append("Dto.toEntity(")
                        .append("dto").append(".get").append(upperCaseCharFieldName).append("()")
                        .append(")")
                        .append(");");
            }
        }

        content.append("\n        return #entity;");

        content.append("\n  }");


        String result = content.toString();
        return result;

    }

    private static String generateDtoOverrideMethods(String entityName) {
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

    private static void generateRestService(String basePackage, String entityName, LinkedHashMap<String, String> fieldMap, String targetPath) throws FileNotFoundException {

        String firstChar = entityName.substring(0, 1);
        String entityInstanceName = entityName.replaceFirst(firstChar, firstChar.toLowerCase());

        String result = generateRestHeader();
        result += generateRestGetMethods(fieldMap, entityName);
        result += generateRestPostAndRemove(entityName);
        result += generateFooter();


        result = result.replaceAll("#package", basePackage)
                .replaceAll("#Entity", entityName)
                .replaceAll("#entity", entityInstanceName);

        System.out.printf(result);
        try (PrintStream out = new PrintStream(new FileOutputStream(targetPath + "/" + entityName + "RestService.java"))) {
            out.print(result);
        }

    }

    private static String generateRestHeader() {
        String content = "package #package.rest;\n" +
                "\n" +
                "import com.aef3.data.SortUtil;\n" +
                "import com.aef3.data.api.qbe.SortObject;\n" +
                "import com.aef3.data.api.qbe.StringSearchType;\n" +
                "import #package.dto.#EntityDto;\n" +
                "import #package.service.#EntityService;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.web.bind.annotation.*;\n" +
                "import #package.service.PagedResult;" +
                "import java.util.Date;\n" +
                "\n";

                if(checkGeneration("generate.security.roles")) {
                    content += "import org.springframework.security.access.annotation.Secured;\n";
                    content += "import #package.security.AccessRoles;\n";

                }

                content += "import java.text.ParseException;\n" +
                "import java.util.Collections;\n" +
                "import java.util.List;\n" +
                "\n" +
                "\n" +
                "/* Generated By AEF Generator ( Powered by Dr.Adldoost :D ) */\n" +
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

    private static String generateRestGetMethods(LinkedHashMap<String, String> fields, String entity) {

        StringBuilder content = new StringBuilder(
                "\n\n");
                if(checkGeneration("generate.security.roles")) {
                    content.append("    @Secured(AccessRoles.ROLE_FIND_").append(camelToSnake(entity).toUpperCase()).append(")\n");
                    }
                content.append("    @GetMapping(\"/{id}\")\n" +
                "    public #EntityDto findById(@PathVariable(name = \"id\")Long id) {\n" +
                "        return #entityService.findByPrimaryKey(id);\n" +
                "    }\n" +
                "\n");
                if(checkGeneration("generate.security.roles")) {
                    content.append("    @Secured(AccessRoles.ROLE_SEARCH_").append(camelToSnake(entity).toUpperCase()).append(")\n");
                }
                content.append("    @GetMapping(\"/search\")\n" +
                "    public PagedResult search(");

                for (Map.Entry<String, String> entry : fields.entrySet())
                {
                    if(getBaseTypes().contains(entry.getValue())) {
                        content.append("\n                                      @RequestParam(value = \"").append(entry.getKey()).append("\", required = false) ");
                        content.append(entry.getValue()).append(" ").append(entry.getKey()).append(",");
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
                for (Map.Entry<String, String> entry : fields.entrySet())
                {
                    if(getBaseTypes().contains(entry.getValue())) {
                        String firstCharFieldName = entry.getKey().substring(0, 1);
                        String upperCaseCharFieldName = entry.getKey().replaceFirst(firstCharFieldName, firstCharFieldName.toUpperCase());
                        content.append("            #entity.set" + upperCaseCharFieldName + "(" + entry.getKey() + "); \n");
                    }
                }

                content.append("\n            return #entityService.findPagedByExample(#entity,\n" +
                "                   sortObjectList,\n" +
                "                   firstIndex,\n" +
                "                   pageSize,\n" +
                "                   StringSearchType.EXPAND_BOTH_SIDES,\n" +
                "                   null,\n" +
                "                   null\n" +
                "                   );\n");
                content.append("    }\n\n");
                return content.toString();
    }

    private static String generateRestPostAndRemove(String entity) {
        StringBuilder content = new StringBuilder("\n");
                if(checkGeneration("generate.security.roles")) {
                    content.append("    @Secured(AccessRoles.ROLE_SAVE_").append(camelToSnake(entity).toUpperCase()).append(")\n");
                }
                content.append("    @PostMapping(path = \"/save\")\n" +
                "    public #EntityDto save(@RequestBody #EntityDto #entity) {\n" +
                "        return #entityService.save(#entity);\n" +
                "    }\n" +
                "\n");
                if(checkGeneration("generate.security.roles")) {
                    content.append("\n    @Secured(AccessRoles.ROLE_REMOVE_").append(camelToSnake(entity).toUpperCase()).append(")\n");
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
                "// Generated By AEF3 Framework, Powered by Dr.Adldoost :D" +
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
                "    ;\n" +
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
                "// Generated By AEF3 Framework, Powered by Dr.Adldoost :D" +
                "\n" +
                "import java.util.Enumeration;\n" +
                "import java.util.ResourceBundle;\n" +
                "\n" +
                "/**\n" +
                " *\n" +
                " * @author Generated By AEF Framework , powered by Dr.Adldoost :D \n" +
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
                "// Generated By AEF3 Framework, powered by Dr.Adldoost :D" +
                "\n" +
                "import java.util.Enumeration;\n" +
                "import java.util.ResourceBundle;\n" +
                "\n" +
                "public class ErrorCodeReaderUtil {\n" +
                "\n" +
                "    static ResourceBundle rb = ResourceBundle.getBundle(\"errorcodes\");\n" +
                "\n" +
                "    public static String getResourceProperity(String key)  {\n" +
                "            return rb.getString(key);\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "    public static Enumeration<String> getResourceKeys(String key) {\n" +
                "            return rb.getKeys();\n" +
                "    }\n" +
                "}";

        String result = content.replaceAll("#package", basePackage);

        System.out.printf(result);
        File file = new File(path);
        file.mkdirs();

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/ErrorCodeReaderUtil.java"))) {
            out.print(result);
        }
        return result;

    }

    private static String generateAEFExceptionHandler(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.common;\n" +
                "\n" +
                "// Generated By AEF3 Framework, powered by Dr.Adldoost :D" +
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
                "public class AEFExceptionHandler extends ResponseEntityExceptionHandler {\n" +
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

        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/AEFExceptionHandler.java"))) {
            out.print(result);
        }
        return result;
    }

    private static String generateFarsiCodeReaderUtility(String path, String basePackage) throws FileNotFoundException {
        String content = "package #package.common;\n" +
                "\n" +
                "import java.util.Enumeration;\n" +
                "import java.util.ResourceBundle;\n" +
                "\n" +
                "public class FarsiCodeReaderUtility {\n" +
                "\n" +
                "    static ResourceBundle rb = ResourceBundle.getBundle(\"farsicodes\");\n" +
                "\n" +
                "    public static String getResourceProperity(String key) {\n" +
                "        return rb.getString(key);\n" +
                "    }\n" +
                "\n" +
                "    public static Enumeration<String> getResourceKeys() {\n" +
                "        return rb.getKeys();\n" +
                "    }\n" +
                "\n" +
                "}";
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
                "// Generated by AEF3 Framework, powered by Dr.Adldoost :D" +
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
                " * Generated by AEF3 Framework, powered by Dr.Adldoost :D .\n" +
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

    private static String generateCustomClaims(String path, String basePackage) throws FileNotFoundException {


        String content = "package #package.jwt;\n" +
                "\n" +
                "// Generated AEF3 Framework, powered by Dr.Adldoost :D" +
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
                "// Generated by AEF3 framework, powered by Dr.Adldoost :D" +
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
                " * @author Generated By AEF3 Frameword, powered by Dr.Adldoost :D\n" +
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
                " * @author Generated By AEF3 frameword, powered by Dr.Adldoost :D \n" +
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
                " * @author Generated By AEF3 Framework, powered by Dr.Adldoost :D\n" +
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
                "// Generated By AEF3 Framework, powered by Dr.Adldoost :D" +
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
                "import com.aef3.data.Filter;\n" +
                "import com.aef3.data.api.DomainDto;\n" +
                "import com.aef3.data.api.qbe.CompareObject;\n" +
                "import com.aef3.data.api.qbe.RangeObject;\n" +
                "import com.aef3.data.api.qbe.SortObject;\n" +
                "import com.aef3.data.api.qbe.StringSearchType;\n" +
                "\n" +
//                "import #package.service.PagedResult;" +
                "\n" +
                "import java.io.Serializable;\n" +
                "import java.util.List;\n" +
                "\n" +
                "/**\n" +
                " *\n" +
                " * @author Generated by AEF3 Framework, powered by Dr.Adldoost :D \n" +
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
                "import com.aef3.data.Filter;\n" +
                "import com.aef3.data.api.DomainDto;\n" +
                "import com.aef3.data.api.DomainEntity;\n" +
                "import com.aef3.data.api.GenericEntityDAO;\n" +
                "import com.aef3.data.api.qbe.CompareObject;\n" +
                "import com.aef3.data.api.qbe.RangeObject;\n" +
                "import com.aef3.data.api.qbe.SortObject;\n" +
                "import com.aef3.data.api.qbe.StringSearchType;\n" +
                "import org.springframework.transaction.annotation.Transactional;\n" +
                "import java.io.Serializable;\n" +
                "import java.util.ArrayList;\n" +
                "import java.util.List;\n" +
                "\n" +
                "/**\n" +
                " *\n" +
                " * @author Generated By AEF3 Framework, powered by Dr.Adldoost :D \n" +
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
                                                               String portNumber) throws FileNotFoundException {
        String content = "spring.datasource.url=" + datasourceUrl + "\n" +
                "spring.datasource.username=" + dataSourceUserName + "\n" +
                "spring.datasource.password=" + dataSourcePassword + "\n" +
                "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver\n" +
                "spring.jpa.hibernate.ddl-auto=update\n" +
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL5InnoDBDialect\n" +
                "spring.jpa.properties.hibernate.dialect.storage_engine=innodb\n" +
                "\n" +
                "server.servlet.context-path=" + contextPath + "\n" +
                "server.port=" + portNumber + "\n" +
                "\n" +
                "logging.file=target/logs/application.log\n" +
                "logging.level.de.larmic=DEBUG";

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
                "\n";
        File file = new File(path);
        file.mkdirs();

        try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(path + "/errorcodes.properties"), StandardCharsets.UTF_8)) {
            out.write(content);
        }
        return content;
    }

    public static String camelToSnake(String phrase)
    {
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

    private static StringBuilder removeLastChar(StringBuilder str) {
        return new StringBuilder(str.substring(0, str.length() - 1));
    }

}
