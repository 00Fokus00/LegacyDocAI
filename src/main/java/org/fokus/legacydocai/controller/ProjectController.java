package org.fokus.legacydocai.controller;

import jakarta.servlet.http.HttpSession;
import org.fokus.legacydocai.services.DocumentLoaderService;
import org.fokus.legacydocai.services.ProjectService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@Controller
@RequestMapping("/project")
public class ProjectController {

    private ProjectService projectService;

    private final DocumentLoaderService documentLoaderService;

    public ProjectController(ProjectService projectService, DocumentLoaderService documentLoaderService) {
        this.projectService = projectService;
        this.documentLoaderService = documentLoaderService;
    }

    private static final String SESSION_PROJECT_PATH = "currentProjectPath";
    private static final String SESSION_FILES_COUNT = "projectFilesCount";
    private static final String SESSION_CODE_COUNT = "CodeClassesCount";
    private static final String SESSION_LAST_INDEX = "lastIndexation";

    @PostMapping("/select")
    public String selectProject(@RequestParam("projectPath") String projectPath,
                                HttpSession session,
                                RedirectAttributes redirectAttributes,
                                @RequestHeader(value = "Referer", required = false)
                                String referer) {
        if (projectPath != null) {
            projectPath = projectPath.trim().replace("\"", "");
        }

        if (projectService.isValidDirectory(projectPath)) {
            documentLoaderService.loadDocuments(projectPath);
            long files = projectService.countFiles(projectPath);
            long codeClasses = projectService.countCodeClasses(projectPath);
            String lastIndex = projectService.getCurrentTimestamp();

            session.setAttribute(SESSION_PROJECT_PATH, projectPath);
            session.setAttribute(SESSION_FILES_COUNT, files);
            session.setAttribute(SESSION_CODE_COUNT, codeClasses);
            session.setAttribute(SESSION_LAST_INDEX, lastIndex);
        } else {
            redirectAttributes.addFlashAttribute("error", "Некорректный путь к проекту или директория не существует");
        }
        if (referer != null) {

            URI uri = URI.create(referer);

            return "redirect:" + uri.getPath();
        }

        return "redirect:/";

    }
}
