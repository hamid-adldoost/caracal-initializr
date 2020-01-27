package com.aef.initializr.dto;

import com.aef.initializr.model.Project;
import com.aef3.data.api.DomainDto;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotEmpty;
import java.util.Date;


/* Generated By Nicico System Generator ( Powered by Dr.Adldoost :D ) */

public class ProjectDto implements DomainDto<Project, ProjectDto> {


    @NotNull(message = "{id.should.not.be.null}")
    private Long id;
    private String name;
    private String jsonMessage;
    @NotNull(message = "{generationDate.should.not.be.null}")
    private Date generationDate;
    @NotEmpty(message = "{backendGenerationPath.should.not.be.Empty}")
    private String backendGenerationPath;
    @NotEmpty(message = "{frontendGenerationPath.should.not.be.Empty}")
    private String frontendGenerationPath;
 

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getJsonMessage() {
        return jsonMessage;
    }
    public void setJsonMessage(String jsonMessage) {
        this.jsonMessage = jsonMessage;
    }

    public Date getGenerationDate() {
        return generationDate;
    }
    public void setGenerationDate(Date generationDate) {
        this.generationDate = generationDate;
    }

    public String getBackendGenerationPath() {
        return backendGenerationPath;
    }
    public void setBackendGenerationPath(String backendGenerationPath) {
        this.backendGenerationPath = backendGenerationPath;
    }

    public String getFrontendGenerationPath() {
        return frontendGenerationPath;
    }
    public void setFrontendGenerationPath(String frontendGenerationPath) {
        this.frontendGenerationPath = frontendGenerationPath;
    }



    public static ProjectDto toDto(Project project) {

        if(project == null)
            return null; 
        ProjectDto dto = new ProjectDto();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setJsonMessage(project.getJsonMessage());
        dto.setGenerationDate(project.getGenerationDate());
        dto.setBackendGenerationPath(project.getBackendGenerationPath());
        dto.setFrontendGenerationPath(project.getFrontendGenerationPath());
        return dto;
  }


    public static Project toEntity(ProjectDto dto) {

        if(dto == null)
            return null; 
        Project project = new Project();
        project.setId(dto.getId());
        project.setName(dto.getName());
        project.setJsonMessage(dto.getJsonMessage());
        project.setGenerationDate(dto.getGenerationDate());
        project.setBackendGenerationPath(dto.getBackendGenerationPath());
        project.setFrontendGenerationPath(dto.getFrontendGenerationPath());
        return project;
  }
    @Override
    public Project toEntity() {
        return ProjectDto.toEntity(this);
    }

    @JsonIgnore
    @Override
    public ProjectDto getInstance(Project project) {
        return ProjectDto.toDto(project);
    }

    @JsonIgnore
    @Override
    public ProjectDto getInstance() {
        return new ProjectDto();
    }
}
