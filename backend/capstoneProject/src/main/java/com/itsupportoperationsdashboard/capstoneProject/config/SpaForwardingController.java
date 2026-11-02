package com.itsupportoperationsdashboard.capstoneProject.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardingController {
    @GetMapping({"/", "/login", "/register", "/tickets/new", "/admin/users/new", "/tickets/{ticketId:[0-9]+}"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
