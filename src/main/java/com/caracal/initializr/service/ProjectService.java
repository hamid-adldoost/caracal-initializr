package com.caracal.initializr.service;

import com.caracal.initializr.dao.ProjectDao;
import com.caracal.initializr.dto.ProjectDto;
import com.caracal.initializr.model.Project;
import com.caracal.data.api.GenericEntityDAO;
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
