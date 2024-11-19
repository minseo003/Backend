package com.project.Teaming.domain.project.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "project_team_stack")
@NoArgsConstructor
@AllArgsConstructor
public class TeamStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_stack_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stack_id")
    private Stack stack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_team_id", referencedColumnName = "project_id")
    private ProjectTeam projectTeam;
}
