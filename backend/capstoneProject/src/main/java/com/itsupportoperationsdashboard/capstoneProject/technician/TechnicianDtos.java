package com.itsupportoperationsdashboard.capstoneProject.technician;

import com.itsupportoperationsdashboard.capstoneProject.model.User;

public final class TechnicianDtos {
    private TechnicianDtos() {}

    public record Response(Long id, String name) {}

    public static Response response(User user) {
        return new Response(user.getUserId(), user.getName());
    }
}
