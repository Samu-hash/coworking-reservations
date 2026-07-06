package com.cuscatlan.coworking.report;

import com.cuscatlan.coworking.report.dto.OccupancyReportResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
public class OccupancyReportController {

    private final OccupancyReportService service;

    public OccupancyReportController(OccupancyReportService service) {
        this.service = service;
    }

    @GetMapping("/occupancy")
    @PreAuthorize("hasRole('ADMIN')")
    public OccupancyReportResponse occupancy(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.occupancy(from, to);
    }
}
