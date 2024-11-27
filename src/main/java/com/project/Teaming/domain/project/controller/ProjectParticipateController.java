package com.project.Teaming.domain.project.controller;

import com.project.Teaming.domain.project.service.ProjectParticipationService;
import com.project.Teaming.global.result.ResultCode;
import com.project.Teaming.global.result.ResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "ProjectParticipate", description = "프로젝트 팀 대기 목록 관련 API")
public class ProjectParticipateController {

    private final ProjectParticipationService projectParticipationService;

    @PostMapping("/project/join/{team_id}")
    @Operation(summary = "프로젝트 팀에 신청", description = "팀원으로 들어가길 원하는 사용자가 팀에 신청을 하면 대기열에 등록된다.")
    public ResultResponse<Void> joinProjectTeam(@PathVariable Long team_id) {
        projectParticipationService.joinTeam(team_id);
        return new ResultResponse<>(ResultCode.JOIN_MEMBER_PROJECT_TEAM, null);
    }

    @DeleteMapping("/project/join/{team_id}/cancel")
    @Operation(summary = "프로젝트 신청 취소", description = "대기열에 있는 팀원은 원할 때, 신청 취소(status: pending, isDeleted: false 인 경우에만 가능)를 할 수 있다.")
    public ResultResponse<Void> cancelProjectTeam(@PathVariable Long team_id) {
        projectParticipationService.cancelTeam(team_id);
        return new ResultResponse<>(ResultCode.CANCEL_PROJECT_TEAM, null);
    }
}
