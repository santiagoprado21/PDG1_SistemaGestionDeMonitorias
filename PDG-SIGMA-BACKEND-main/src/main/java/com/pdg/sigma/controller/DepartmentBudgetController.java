package com.pdg.sigma.controller;

import com.pdg.sigma.domain.DepartmentBudget;
import com.pdg.sigma.domain.Program;
import com.pdg.sigma.repository.DepartmentBudgetRepository;
import com.pdg.sigma.repository.MonitoringRepository;
import com.pdg.sigma.repository.ProgramRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:3000", "https://pdg-sigma.vercel.app/"})
@RestController
@RequestMapping("/budget")
public class DepartmentBudgetController {

    private final DepartmentBudgetRepository budgetRepo;
    private final ProgramRepository programRepo;
    private final MonitoringRepository monitoringRepo;

    public DepartmentBudgetController(DepartmentBudgetRepository budgetRepo,
                                      ProgramRepository programRepo,
                                      MonitoringRepository monitoringRepo) {
        this.budgetRepo = budgetRepo;
        this.programRepo = programRepo;
        this.monitoringRepo = monitoringRepo;
    }

    @GetMapping("/{programName}/{semester}")
    public ResponseEntity<?> getBudget(@PathVariable String programName, @PathVariable String semester) {
        Program program = programRepo.findByName(programName).orElse(null);
        if (program == null) return ResponseEntity.badRequest().body("Programa no encontrado");

        var budgetOpt = budgetRepo.findByProgramAndSemester(program, semester);
        if (budgetOpt.isEmpty()) return ResponseEntity.status(404).body("No hay presupuesto configurado para este programa y semestre");

        var budget = budgetOpt.get();
        int used = monitoringRepo.findByProgram(program).stream()
                .filter(m -> semester.equals(m.getSemester()))
                .map(m -> m.getEstimatedHours() == null ? 0 : m.getEstimatedHours())
                .reduce(0, Integer::sum);
        int remaining = Math.max(0, budget.getTotalHours() - used);

        Map<String, Object> payload = new HashMap<>();
        payload.put("program", programName);
        payload.put("semester", semester);
        payload.put("totalHours", budget.getTotalHours());
        payload.put("usedHours", used);
        payload.put("remainingHours", remaining);
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/set")
    public ResponseEntity<?> setBudget(@RequestBody Map<String, Object> req) {
        String programName = (String) req.get("programName");
        String semester = (String) req.get("semester");
        Integer totalHours = (Integer) req.get("totalHours");
        if (programName == null || semester == null || totalHours == null || totalHours < 0) {
            return ResponseEntity.badRequest().body("Datos inválidos para configurar presupuesto");
        }
        Program program = programRepo.findByName(programName).orElse(null);
        if (program == null) return ResponseEntity.badRequest().body("Programa no encontrado");

        var budget = budgetRepo.findByProgramAndSemester(program, semester)
                .map(b -> { b.setTotalHours(totalHours); return b; })
                .orElseGet(() -> new DepartmentBudget(program, semester, totalHours));
        budgetRepo.save(budget);
        return ResponseEntity.ok("Presupuesto actualizado");
    }
}
