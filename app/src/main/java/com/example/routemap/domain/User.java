package com.example.routemap.domain;

public class User {

    private String email;
    private String alias;
    private String password;

    public User(String email, String user, String password) {
        this.email = email;
        this.alias = user;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "User{" +
                "email='" + email + '\'' +
                ", user='" + alias + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
