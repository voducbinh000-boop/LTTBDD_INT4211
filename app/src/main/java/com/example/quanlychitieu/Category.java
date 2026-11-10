package com.example.quanlychitieu;

public class Category {
    public long id;
    public long userId; // 0 = hệ thống mặc định; > 0 = danh mục của user
    public String name;
    public String type; // "expense" hoặc "income"
    public String icon;

    public Category(long id, String name, String type, String icon) {
        this(id, 0, name, type, icon);
    }

    public Category(long id, long userId, String name, String type, String icon) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.icon = icon;
    }

    @Override
    public String toString() { return name; } // để Spinner hiển thị tên
}
