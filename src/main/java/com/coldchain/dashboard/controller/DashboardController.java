package com.coldchain.dashboard.controller;

import com.coldchain.dashboard.model.ColdChainEvent;
import com.coldchain.dashboard.repository.ColdChainEventRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class DashboardController {
    private final ColdChainEventRepository repository;

    public DashboardController(ColdChainEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String q,
                            @RequestParam(required = false) String status,
                            Model model) {
        List<ColdChainEvent> all = repository.findAll()
                .stream()
                .sorted(Comparator.comparing(ColdChainEvent::getRecordedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        long normal = all.stream().filter(e -> "NORMAL".equals(e.getStatus())).count();
        long exceptions = all.stream().filter(e -> !"NORMAL".equals(e.getStatus())).count();
        long units = all.stream().map(ColdChainEvent::getUnitId).filter(Objects::nonNull).distinct().count();

        String query = normalizeQuery(q);
        String statusFilter = normalizeStatus(status);

        List<ColdChainEvent> filtered = all.stream()
                .filter(e -> matchesFilters(e, query, statusFilter))
                .collect(Collectors.toList());

        model.addAttribute("events", filtered);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("statusFilter", statusFilter.isEmpty() ? "ALL" : statusFilter);
        model.addAttribute("totalReadings", all.size());
        model.addAttribute("unitCount", units);
        model.addAttribute("normalCount", normal);
        model.addAttribute("exceptionCount", exceptions);
        model.addAttribute("newEvent", new ColdChainEvent());
        return "dashboard";
    }

    @PostMapping("/events")
    public String createEvent(@Valid @ModelAttribute("newEvent") ColdChainEvent event,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (event.getMinTempC() != null && event.getMaxTempC() != null
                && event.getMinTempC() >= event.getMaxTempC()) {
            bindingResult.rejectValue("maxTempC", "range", "Maximum must be greater than minimum.");
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please enter valid details. Minimum temperature must be less than maximum.");
            return "redirect:/";
        }
        event.setRecordedAt(LocalDateTime.now());
        repository.save(event);
        redirectAttributes.addFlashAttribute("successMessage", "Temperature event saved successfully.");
        return "redirect:/";
    }

    @GetMapping("/events/{id}")
    public String eventDetails(@PathVariable Long id, Model model) {
        ColdChainEvent event = repository.findById(id).orElse(null);
        if (event == null) return "redirect:/";
        model.addAttribute("event", event);
        return "event-detail";
    }

    @GetMapping("/alerts")
    public String alerts(Model model) {
        List<ColdChainEvent> alerts = repository.findAll().stream()
                .filter(e -> !"NORMAL".equals(e.getStatus()))
                .sorted(Comparator.comparing(ColdChainEvent::getRecordedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        model.addAttribute("events", alerts);
        return "alerts";
    }

    @GetMapping("/health")
    @ResponseBody
    public Map<String, String> health() {
        return Map.of("status", "UP", "application", "cold-chain-dashboard");
    }

    @GetMapping("/api/events")
    @ResponseBody
    public List<ColdChainEvent> apiEvents(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) String status) {
        String query = normalizeQuery(q);
        String statusFilter = normalizeStatus(status);
        return repository.findAll().stream()
                .filter(e -> matchesFilters(e, query, statusFilter))
                .sorted(Comparator.comparing(ColdChainEvent::getRecordedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @PostMapping("/api/events")
    @ResponseBody
    public ResponseEntity<?> createApiEvent(@Valid @RequestBody ColdChainEvent event) {
        if (event.getMinTempC() == null || event.getMaxTempC() == null
                || event.getMinTempC() >= event.getMaxTempC()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "minTempC must be less than maxTempC"));
        }
        event.setId(null);
        event.setRecordedAt(LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(event));
    }

    @GetMapping("/api/summary")
    @ResponseBody
    public Map<String, Object> apiSummary() {
        List<ColdChainEvent> all = repository.findAll();
        long normal = all.stream().filter(e -> "NORMAL".equals(e.getStatus())).count();
        long exceptions = all.stream().filter(e -> !"NORMAL".equals(e.getStatus())).count();
        long units = all.stream().map(ColdChainEvent::getUnitId).filter(Objects::nonNull).distinct().count();
        return Map.of("totalReadings", all.size(), "unitCount", units,
                "normalReadings", normal, "exceptions", exceptions);
    }

    private static String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean matchesFilters(ColdChainEvent event, String query, String status) {
        boolean matchesQuery = query.isEmpty()
                || safe(event.getUnitId()).contains(query)
                || safe(event.getLocation()).contains(query)
                || safe(event.getNotes()).contains(query);
        boolean matchesStatus = status.isEmpty() || "ALL".equals(status)
                || status.equals(event.getStatus());
        return matchesQuery && matchesStatus;
    }
}
