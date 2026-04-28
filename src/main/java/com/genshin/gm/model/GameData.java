package com.genshin.gm.model;

/**
 * 游戏数据实体类
 * 用于封装从txt文件中读取的数据
 */
public class GameData {
    private Integer id;
    private String name;

    public GameData() {}

    public GameData(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}