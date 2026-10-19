package com.itsupportoperationsdashboard.capstoneProject.ticket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.itsupportoperationsdashboard.capstoneProject.model.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class CommentDtos {
    private CommentDtos() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateRequest(@NotBlank @Size(max = 10000) String text) {}

    public record Response(Long id, String text, Long authorId, String authorName, LocalDateTime createdAt) {}

    public static Response response(Comment comment) {
        return new Response(comment.getCommentId(), comment.getCommentText(),
                comment.getUser().getUserId(), comment.getUser().getName(), comment.getCreatedAt());
    }
}
