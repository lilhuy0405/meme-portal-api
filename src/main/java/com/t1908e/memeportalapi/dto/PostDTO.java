package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDTO {
    private int id;
    private String title;
    private String description;
    private String image;
    private double upHotTokenNeeded;
    private int status;
    private Date createdAt;
    private Date updatedAt;
    private String category;
    private UserDTO creator;
    private int likeCounts;
    private int categoryId;


    public PostDTO(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.description = post.getDescription();
        this.image = post.getImage();
        this.upHotTokenNeeded = post.getUpHotTokenNeeded();
        this.status = post.getStatus();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
        this.category = post.getCategory().getName();
        this.categoryId = post.getCategoryId();
        this.likeCounts = 0;
        if (post.getPostLikes() != null) {
            this.likeCounts = post.getPostLikes().size();
        }
        this.creator = new UserDTO(post.getUser());
    }



    @Data
    public static class CreatePostDTO {
        @NotBlank(message = "Title is required")
        private String title;
        private String description;
        @NotBlank(message = "Image is required")
        private String image;
        @NotNull(message = "CategoryId is required")
        private int categoryId;
    }

    @Data
    public static class VerifyPostDTO {
        @NotEmpty(message = "post id is required")
        private ArrayList<Integer> postIds = new ArrayList<>();
    }

}