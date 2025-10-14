package com.pdg.sigma.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "professor")
public class Professor {
    
    @Id
    @Column(name = "id", nullable = false, columnDefinition = "varchar(255)")
    private String id;

    @Column(name = "name", nullable = false, columnDefinition = "varchar(100)")
    private String name;

    @Column(name = "password", nullable = false)
    private String password;

    public Professor(String id) {
        this.id = id;
    }
}