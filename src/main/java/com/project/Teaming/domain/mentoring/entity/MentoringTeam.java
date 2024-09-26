package com.project.Teaming.domain.mentoring.entity;

import com.project.Teaming.global.auditing.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "MentoringTeam")
@NoArgsConstructor
@AllArgsConstructor
public class MentoringTeam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mentoringTeamId")
    private Long id;  // 멘토링 팀 ID

    @Column(name = "mentoringName", length = 100)
    private String name;  // 멘토링 명

    @Column(name = "startDate", length = 50)
    private String startDate;  // 멘토링 시작일

    @Column(name = "endDate", length = 50)
    private String endDate;  // 멘토링 종료일


}