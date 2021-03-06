package com.t1908e.memeportalapi.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String title;
    private String description;
    private String image;
    private double upHotTokenNeeded;
    private int status;
    private Date createdAt;
    private Date updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "userId")
    private User user;
    @Column(insertable = false, updatable = false)
    private long userId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "categoryId")
    private Category category;
    @Column(insertable = false, updatable = false)
    private int categoryId;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private Set<Comment> comments;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private Set<PostLike> postLikes;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private Set<PostShare> postShares;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private Set<PushHistory> pushHistories;


    public void addPostLike(PostLike postLike) {
        if (this.postLikes == null) {
            this.postLikes = new HashSet<>();
        }
        this.postLikes.add(postLike);
    }

    public double subTractToken(double amount) {
        double tokenBalance = this.getUpHotTokenNeeded();
        if (amount < 0) {
            return upHotTokenNeeded;
        }
        double newBalance = tokenBalance - amount;
        this.setUpHotTokenNeeded(newBalance);
        return newBalance;
    }
}
