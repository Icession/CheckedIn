package edu.cit.checkedin.layout;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.checkedin.layout.dto.CreateLayoutRequest;
import edu.cit.checkedin.layout.dto.LayoutResponse;
import edu.cit.checkedin.layout.dto.LayoutSummaryResponse;
import edu.cit.checkedin.layout.dto.SaveLayoutRequest;

/**
 * REST endpoints for room layouts. Covers FR-017 – FR-021.
 * Only professors may call them (SecurityConfig). Every professor can search and open any layout;
 * LayoutService lets only the creator save changes. All rules live in LayoutService.
 *
 * GET  /api/professor/layouts?search=NGE   -> layouts whose room name contains the text (all if empty)
 * POST /api/professor/layouts              -> create a layout (a full grid, numbered automatically)
 * GET  /api/professor/layouts/{id}         -> one layout with its whole grid
 * PUT  /api/professor/layouts/{id}         -> save the edited room details and grid (creator only)
 */
@RestController
@RequestMapping("/api/professor/layouts")
public class LayoutController {

    private final LayoutService layoutService;

    public LayoutController(LayoutService layoutService) {
        this.layoutService = layoutService;
    }

    @GetMapping
    public List<LayoutSummaryResponse> searchLayouts(@RequestParam(name = "search", defaultValue = "") String search,
                                                     Authentication authentication) {
        return layoutService.searchLayouts(authentication.getName(), search);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LayoutResponse createLayout(@Valid @RequestBody CreateLayoutRequest request, Authentication authentication) {
        return layoutService.createLayout(authentication.getName(), request);
    }

    @GetMapping("/{layoutId}")
    public LayoutResponse getLayout(@PathVariable("layoutId") Long layoutId, Authentication authentication) {
        return layoutService.getLayout(authentication.getName(), layoutId);
    }

    @PutMapping("/{layoutId}")
    public LayoutResponse saveLayout(@PathVariable("layoutId") Long layoutId,
                                     @Valid @RequestBody SaveLayoutRequest request, Authentication authentication) {
        return layoutService.saveLayout(authentication.getName(), layoutId, request);
    }
}
