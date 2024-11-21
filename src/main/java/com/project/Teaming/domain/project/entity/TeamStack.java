package com.project.Teaming.domain.project.entity;

import com.project.Teaming.domain.project.dto.request.CreateTeamDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;

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
    @JoinColumn(name = "stack_id",nullable = false)
    private Stack stack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id",nullable = false)
    private ProjectTeam projectTeam;

    public static TeamStack addStacks(ProjectTeam projectTeam, Stack stack) {
        TeamStack teamStack = new TeamStack();
        teamStack.projectTeam = projectTeam;
        teamStack.stack = stack;
        return teamStack;
    }
}
