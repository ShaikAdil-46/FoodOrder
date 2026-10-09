package com.foodorder.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_mobile", columnNames = "mobile")
})
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(nullable = false, length = 20)
    private String mobile;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.CUSTOMER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected UserEntity() {}

    public UserEntity(String firstName, String lastName, String mobile, String email, String password, UserRole role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.mobile = mobile;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getMobile() { return mobile; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public UserRole getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setMobile(String mobile) { this.mobile = mobile; }
}
