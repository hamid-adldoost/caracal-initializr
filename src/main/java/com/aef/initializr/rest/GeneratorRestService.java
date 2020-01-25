package com.aef.initializr.rest;

import com.aef.initializr.AEFGenerator;
import com.aef.initializr.types.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;

@RestController
@RequestMapping("/generator")
public class GeneratorRestService {


    private final AEFGenerator aefGenerator;

    @Autowired
    public GeneratorRestService(AEFGenerator aefGenerator) {
        this.aefGenerator = aefGenerator;
    }

    @GetMapping("/sample")
    public SystemDefinition generateSampleJson() {

        SystemDefinition systemDefinition = new SystemDefinition();

        //backend project execution config
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setBackendPortNumber("9090");
        backendConfig.setBasePackage("com.adldoost.generator.product");
        backendConfig.setContextPath("product");
        backendConfig.setTargetPath("E:\\adldoost\\generator\\backend\\980920\\");
        systemDefinition.setBackendConfig(backendConfig);

        //database connection config
        backendConfig.setDatabaseConnection(new DatabaseConnection());
        backendConfig.getDatabaseConnection().setDatasourceUrl("jdbc:mysql://localhost:3306/product?useUnicode=yes&characterEncoding=UTF8");
        backendConfig.getDatabaseConnection().setDatasourceUsername("root");
        backendConfig.getDatabaseConnection().setDatasourcePassword("adldoost@2015");
        backendConfig.getDatabaseConnection().setSchemaName("product");

        //maven config
        backendConfig.setMavenConfig(new MavenConfig());
        backendConfig.getMavenConfig().setMavenGroupId("com.adldoost.generator.product");
        backendConfig.getMavenConfig().setMavenArtifactId("product-management");
        backendConfig.getMavenConfig().setProjectName("product-management");
        backendConfig.getMavenConfig().setProjectDescription("Managing products input");

        //jwt config
        backendConfig.setSecurityConfig(new SecurityConfig());
        backendConfig.getSecurityConfig().setJwtKey("151s5d1sa5d15as1d5as1d5ad1");
        backendConfig.getSecurityConfig().setProvider("jwt");
        backendConfig.getSecurityConfig().setTokenExpiration("1800");


        //backend generation config
        BackendGenerationConfig backendGenerationConfig = new BackendGenerationConfig();

        backendGenerationConfig.setGenerateMaven(true);
        backendGenerationConfig.setGeneratePropertiesFile(true);
        backendGenerationConfig.setGenerateRunnerClass(true);

        backendGenerationConfig.setGenerateCommonClasses(true);
        backendGenerationConfig.setGenerateConfigPropertiesFile(true);
        backendGenerationConfig.setGenerateErrorCodeFile(true);

        backendGenerationConfig.setGenerateEntities(true);
        backendGenerationConfig.setGenerateDao(true);
        backendGenerationConfig.setGenerateService(true);
        backendGenerationConfig.setGenerateGeneralService(true);
        backendGenerationConfig.setGenerateDto(true);
        backendGenerationConfig.setGenerateRest(true);

        backendGenerationConfig.setGeneratePermissions(true);
        backendGenerationConfig.setGenerateSecurityAuthorities(true);
        backendGenerationConfig.setGenerateSecurityConfigClass(true);
        backendGenerationConfig.setGenerateSecurityService(true);
        backendGenerationConfig.setGenerateJwtClasses(true);
        backendGenerationConfig.setGenerateLoginRest(true);

        systemDefinition.getBackendConfig().setBackendGenerationConfig(backendGenerationConfig);
        // end backend generation config

        systemDefinition.setEntityDefinitionList(new ArrayList<>());

        EntityDefinition entityDefinition = new EntityDefinition();
        entityDefinition.setName("Product");
        entityDefinition.setFarsiName("کالا");
        entityDefinition.setEnableValidation(true);
        entityDefinition.setHasForm(true);
        entityDefinition.setLabel("name");
        entityDefinition.setEntityFieldDefinitionList(new ArrayList<>());

        EntityFieldDefinition fieldDefinition = new EntityFieldDefinition();
        fieldDefinition.setName("id");
        fieldDefinition.setFarsiName("شناسه");
        fieldDefinition.setFieldType(new FieldType());
        Choice c = new Choice();
        c.setLabel("Long");
        c.setValue("Long");
        fieldDefinition.getFieldType().setType(c);
        entityDefinition.getEntityFieldDefinitionList().add(fieldDefinition);

        fieldDefinition = new EntityFieldDefinition();
        fieldDefinition.setFieldType(new FieldType());
        fieldDefinition.setName("name");
        fieldDefinition.setFarsiName("نام");
        Choice c2 = new Choice();
        c2.setLabel("String");
        c2.setLabel("String");
        fieldDefinition.getFieldType().setType(c2);
        entityDefinition.getEntityFieldDefinitionList().add(fieldDefinition);
        systemDefinition.getEntityDefinitionList().add(entityDefinition);
        fieldDefinition.setLength(100);
        fieldDefinition.setNullable(true);
        fieldDefinition.setReadOnly(false);
        fieldDefinition.setUnique(false);
        fieldDefinition.setVisible(true);


        systemDefinition.setFrontendConfig(new FrontendConfig());
        systemDefinition.getFrontendConfig().setProjectName("product-management");
        systemDefinition.getFrontendConfig().setProjectFarsiName("مدیریت کالا");
        systemDefinition.getFrontendConfig().setTargetPath("E:\\adldoost\\generator\\frontend\\980920\\");

        systemDefinition.setGenerateBackend(true);
        systemDefinition.setGenerateFrontend(true);

        return systemDefinition;
    }

    @PostMapping("/generate")
    public void generateFromJson(@RequestBody SystemDefinition systemDefinition) throws IOException {

        aefGenerator.generateAll(systemDefinition);
    }

}
