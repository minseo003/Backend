package com.project.Teaming.domain.mentoring.controller;

import com.project.Teaming.domain.mentoring.dto.response.LeaderResponseDto;
import com.project.Teaming.domain.mentoring.dto.response.RsTeamParticipationDto;
import com.project.Teaming.domain.mentoring.dto.response.RsTeamUserDto;
import com.project.Teaming.domain.mentoring.dto.response.RsUserParticipationDto;
import com.project.Teaming.domain.mentoring.entity.*;
import com.project.Teaming.domain.mentoring.service.MentoringBoardService;
import com.project.Teaming.domain.mentoring.service.MentoringParticipationService;
import com.project.Teaming.domain.mentoring.service.MentoringTeamService;
import com.project.Teaming.domain.user.entity.User;
import com.project.Teaming.domain.user.service.UserService;
import com.project.Teaming.global.jwt.dto.SecurityUserDto;
import com.project.Teaming.global.result.ResultCode;
import com.project.Teaming.global.result.ResultDetailResponse;
import com.project.Teaming.global.result.ResultListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/mentoring")
@Tag(name = "MentoringParticipation", description = "멘토링 지원 관련 API")
public class MentoringParticipationController {

    private final MentoringParticipationService mentoringParticipationService;
    private final MentoringBoardService mentoringBoardService;
    private final MentoringTeamService mentoringTeamService;
    private final UserService userService;

    @PostMapping("/{post_id}/join")
    @Operation(summary = "멘토링 지원자 등록", description = "멘토링 팀에 지원하는 API , 지원자 ID 반환")
    public ResultDetailResponse<String> saveMentoringParticipation(@PathVariable Long post_id) {
        User user = getUser();
        MentoringBoard mentoringPost = mentoringBoardService.findMentoringPost(post_id);
        MentoringTeam mentoringTeam = mentoringPost.getMentoringTeam();
        Long id = mentoringParticipationService.saveMentoringParticipation(user.getId(), mentoringTeam.getId(), mentoringPost.getRole());
        return new ResultDetailResponse<>(ResultCode.REGISTER_MENTORING_PARTICIPATION, String.valueOf(id));
    }

    @PostMapping("/{team_id}/cancel")
    @Operation(summary = "멘토링 지원 취소" , description = "멘토링 팀 지원취소 하는 API")
    public ResultDetailResponse<Void> cancelMentoringParticipation(@PathVariable Long team_id) {
        User user = getUser();
        mentoringParticipationService.cancelMentoringParticipation(user.getId(), team_id);
        return new ResultDetailResponse<>(ResultCode.CANCEL_MENTORING_PARTICIPATION, null);
    }

    @PostMapping("/team/{team_id}/{participant_id}/accept")
    @Operation(summary = "리더의 멘토링 지원 수락" , description = "멘토링 팀 리더가 지원을 수락 하는 API")
    public ResultDetailResponse<Void> acceptParticipant(@PathVariable Long team_id, @PathVariable Long participant_id) {
        User user = getUser();
        mentoringParticipationService.acceptMentoringParticipation(user.getId(), team_id, participant_id);
        return new ResultDetailResponse<>(ResultCode.ACCEPT_MENTORING_PARTICIPATION, null);
    }

    @PostMapping("/team/{team_id}/{participant_id}/reject")
    @Operation(summary = "리더의 멘토링 지원 거절" , description = "멘토링 팀 리더가 지원을 거절 하는 API")
    public ResultDetailResponse<Void> rejectParticipant(@PathVariable Long team_id, @PathVariable Long participant_id) {
        User user = getUser();
        mentoringParticipationService.rejectMentoringParticipation(user.getId(), team_id, participant_id);
        return new ResultDetailResponse<>(ResultCode.REJECT_MENTORING_PARTICIPATION, null);
    }

    @PostMapping("/team/{team_id}/quit")
    @Operation(summary = "팀 구성원의 탈퇴", description = "팀 구성원들이 탈퇴하는 API")
    public ResultDetailResponse<Void> deleteParticipant(@PathVariable Long team_id) {
        User user = getUser();
        mentoringParticipationService.deleteUser(user.getId(), team_id);
        return new ResultDetailResponse<>(ResultCode.DELETE_PARTICIPATION, null);
    }

    @GetMapping("/{team_id}/status")
    @Operation(summary = "멘토링팀 멤버 및 지원자 현황 조회", description = "멘토링 팀 멤버나 지원자 현황을 조회하는 API " +
            "조회하는 사람이 팀장이면 팀원과 지원자 정보 반환, 팀원이면 팀원 정보만 반환, 지원한사용자, 일반사용자는 지원자 현황만 반환 " +
            "Authority를 같이 반환하니 이거에 따라 다른 페이지 보여주세요.")
    public ResultListResponse<?> getParticipationAppliers(@PathVariable Long team_id) {
        User user = getUser();
        MentoringTeam mentoringTeam = mentoringTeamService.findMentoringTeam(team_id);
        Optional<MentoringParticipation> teamUser = mentoringParticipationService.findByTeamAndUser(mentoringTeam, user);
        if (teamUser.isPresent()) {  //팀과의 연관관계가 있으면
            if (teamUser.get().getAuthority() == MentoringAuthority.LEADER) {  //팀의 리더인 유저
                List<RsTeamUserDto> allTeamUsers = mentoringParticipationService.findAllTeamUsers(mentoringTeam);
                List<RsTeamParticipationDto> participations = mentoringParticipationService.findForLeader(mentoringTeam.getId());
                LeaderResponseDto dto = new LeaderResponseDto();
                dto.setMembers(allTeamUsers);
                dto.setParticipations(participations);
                return new ResultListResponse<>(ResultCode.GET_MEMBER_INFO_FOR_LEADER, List.of(MentoringAuthority.LEADER, dto));
            } else if (teamUser.get().getAuthority() == MentoringAuthority.CREW && !teamUser.get().getIsDeleted()) {  //팀의 멤버인 유저
                List<RsTeamUserDto> members = mentoringParticipationService.findAllTeamUsers(mentoringTeam);
                return new ResultListResponse<>(ResultCode.GET_MEMBER_INFO_FOR_CREW, List.of(MentoringAuthority.CREW, members));
            } else {  //지원만 한 유저 , 수정필요
                List<RsUserParticipationDto> forUser = mentoringParticipationService.findForUser(mentoringTeam.getId());

                List<Object> responseList = new ArrayList<>();
                responseList.add(MentoringAuthority.NoAuth);
                responseList.add(forUser);
                return new ResultListResponse<>(ResultCode.GET_MEMBER_INFO_FOR_PENDING, responseList);
            }
        } else { //팀과 무관한 사용자
            List<RsUserParticipationDto> forUser = mentoringParticipationService.findForUser(mentoringTeam.getId());
            List<Object> responseList = new ArrayList<>();
            responseList.add(MentoringAuthority.NoAuth);
            responseList.add(forUser);
            return new ResultListResponse<>(ResultCode.GET_MEMBER_INFO_FOR_NoAuth, responseList);
}
    }


    private User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUserDto securityUser = (SecurityUserDto) authentication.getPrincipal();
        Long userId = securityUser.getUserId();
        User user = userService.findById(userId).orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return user;
    }
}
