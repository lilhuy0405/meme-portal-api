package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.User;

public class TopCreatorDTO {
    private UserDTO user;
    private int postCounts;

    public TopCreatorDTO(UserDTO user, int postCounts) {
        this.user = user;
        this.postCounts = postCounts;
    }
    public TopCreatorDTO(User user, int postCounts) {
        this.user = new UserDTO(user);
        this.postCounts = postCounts;
    }

    public UserDTO getUser() {
        return user;
    }

    public int getPostCounts() {
        return postCounts;
    }
}
