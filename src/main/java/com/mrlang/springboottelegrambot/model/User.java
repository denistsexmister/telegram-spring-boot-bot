package com.mrlang.springboottelegrambot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity(name = "usersDataTable")
public class User {
    @Id
    private Long chatId;
    @Column()
    private String firstName;
    @Column()
    private String lastName;
    @Column()
    private String userName;
    @Column()
    private Timestamp registeredAt;

    public User() {
    }

    @Override
    public String toString() {
        return "User{" +
                "chatId=" + chatId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
                ", registeredAt=" + registeredAt +
                '}';
    }
}
