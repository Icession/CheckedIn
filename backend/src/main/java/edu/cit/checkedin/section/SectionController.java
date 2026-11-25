package edu.cit.checkedin.section;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.checkedin.section.dto.AddSessionRequest;
import edu.cit.checkedin.section.dto.ChooseLayoutRequest;
import edu.cit.checkedin.section.dto.SaveSectionRequest;
import edu.cit.checkedin.section.dto.SectionResponse;
import edu.cit.checkedin.section.dto.SessionResponse;

/**
 * REST endpoints for a professor's sections and their weekly sessions. Covers FR-010 – FR-013 and FR-015.
 * Only professors may call them (SecurityConfig); the services check that the section is the
 * professor's own. All rules live in SectionService and ClassSessionService.
 *
 * GET    /api/professor/sections?archived=false        -> the professor's active (or archived) sections
 * POST   /api/professor/sections                       -> create a section
 * GET    /api/professor/sections/{id}                  -> one section with its sessions
 * PUT    /api/professor/sections/{id}                  -> edit a section
 * POST   /api/professor/sections/{id}/archive          -> archive (read-only, hidden; permanent)
 * POST   /api/professor/sections/{id}/sessions         -> add a weekly session
 * PUT    /api/professor/sections/{id}/sessions/{sid}/layout -> choose or change a session's room layout
 * DELETE /api/professor/sections/{id}/sessions/{sid}   -> remove a weekly session
 */
@RestController
@RequestMapping("/api/professor/sections")
public class SectionController {

    private final SectionService sectionService;
    private final ClassSessionService classSessionService;

    public SectionController(SectionService sectionService, ClassSessionService classSessionService) {
        this.sectionService = sectionService;
        this.classSessionService = classSessionService;
    }

    @GetMapping
    public List<SectionResponse> listSections(@RequestParam(name = "archived", defaultValue = "false") boolean archived,
                                              Authentication authentication) {
        return sectionService.listSections(authentication.getName(), archived);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SectionResponse createSection(@Valid @RequestBody SaveSectionRequest request, Authentication authentication) {
        return sectionService.createSection(authentication.getName(), request);
    }

    @GetMapping("/{sectionId}")
    public SectionResponse getSection(@PathVariable("sectionId") Long sectionId, Authentication authentication) {
        return sectionService.getSection(authentication.getName(), sectionId);
    }

    @PutMapping("/{sectionId}")
    public SectionResponse updateSection(@PathVariable("sectionId") Long sectionId,
                                         @Valid @RequestBody SaveSectionRequest request, Authentication authentication) {
        return sectionService.updateSection(authentication.getName(), sectionId, request);
    }

    @PostMapping("/{sectionId}/archive")
    public SectionResponse archiveSection(@PathVariable("sectionId") Long sectionId, Authentication authentication) {
        return sectionService.archiveSection(authentication.getName(), sectionId);
    }

    @PostMapping("/{sectionId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse addSession(@PathVariable("sectionId") Long sectionId,
                                      @Valid @RequestBody AddSessionRequest request, Authentication authentication) {
        return classSessionService.addSession(authentication.getName(), sectionId, request);
    }

    @PutMapping("/{sectionId}/sessions/{sessionId}/layout")
    public SessionResponse chooseLayout(@PathVariable("sectionId") Long sectionId, @PathVariable("sessionId") Long sessionId,
                                        @Valid @RequestBody ChooseLayoutRequest request, Authentication authentication) {
        return classSessionService.chooseLayout(authentication.getName(), sectionId, sessionId, request.layoutId());
    }

    @DeleteMapping("/{sectionId}/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSession(@PathVariable("sectionId") Long sectionId, @PathVariable("sessionId") Long sessionId,
                              Authentication authentication) {
        classSessionService.removeSession(authentication.getName(), sectionId, sessionId);
    }
}
