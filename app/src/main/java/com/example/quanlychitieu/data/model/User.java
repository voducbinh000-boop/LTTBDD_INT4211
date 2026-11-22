package com.example.quanlychitieu.data.model;

public class User {
    public long id;
    public String name;
    public String email;
    public String firebaseUid;
    public String avatar;

    public User() {}

    public User(long id, String name, String email, String firebaseUid) {
        this(id, name, email, firebaseUid, null);
    }

    public User(long id, String name, String email, String firebaseUid, String avatar) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.firebaseUid = firebaseUid;
        this.avatar = avatar;
    }
}
