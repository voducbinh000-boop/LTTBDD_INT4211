package com.example.quanlychitieu;

public class User {
    public long id;
    public String name;
    public String email;
    public String firebaseUid;

    public User() {}

    public User(long id, String name, String email, String firebaseUid) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.firebaseUid = firebaseUid;
    }
}
