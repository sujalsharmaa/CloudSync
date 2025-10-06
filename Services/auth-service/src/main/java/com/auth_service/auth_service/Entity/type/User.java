package com.auth_service.auth_service.Entity.type;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    private String picture;

    @Column(name = "google_id")
    private String googleId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)   // ✅ store enum as text ("DEFAULT","BASIC" etc.)
    @Column(name = "plan", nullable = false)
    private Plan plan = Plan.DEFAULT; // ✅ default value in Java (not SQL)
}
