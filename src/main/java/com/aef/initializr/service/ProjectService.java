package com.aef.initializr.service;

import com.aef.initializr.dao.ProjectDao;
import com.aef.initializr.dto.ProjectDto;
import com.aef.initializr.model.Project;
import com.aef3.data.api.GenericEntityDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProjectService extends GeneralServiceImpl<ProjectDto, Project, Long> {


    private final ProjectDao projectDao;

    @Autowired
    public ProjectService(ProjectDao projectDao) {
        this.projectDao = projectDao;
    }

    @Override
    protected GenericEntityDAO getDAO() {
        return projectDao;
    }

    @Override
    public ProjectDto getDtoInstance() {
        return new ProjectDto();
    }
}
