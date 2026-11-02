package com.itsupportoperationsdashboard.capstoneProject.technician;

import com.itsupportoperationsdashboard.capstoneProject.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/technicians")
public class TechnicianController {
    private final UserService users;

    public TechnicianController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public List<TechnicianDtos.Response> list() {
        return users.getActiveTechnicians().stream().map(TechnicianDtos::response).toList();
    }
}
