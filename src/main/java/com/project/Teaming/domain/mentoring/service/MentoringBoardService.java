package com.project.Teaming.domain.mentoring.service;

import com.project.Teaming.domain.mentoring.dto.request.RqBoardDto;
import com.project.Teaming.domain.mentoring.dto.response.RsBoardDto;
import com.project.Teaming.domain.mentoring.entity.*;
import com.project.Teaming.domain.mentoring.repository.MentoringBoardRepository;
import com.project.Teaming.domain.mentoring.repository.MentoringParticipationRepository;
import com.project.Teaming.domain.mentoring.repository.MentoringTeamRepository;
import com.project.Teaming.domain.user.entity.User;
import com.project.Teaming.domain.user.repository.UserRepository;
import com.project.Teaming.global.error.ErrorCode;
import com.project.Teaming.global.error.exception.MentoringPostNotFoundException;
import com.project.Teaming.global.error.exception.MentoringTeamNotFoundException;
import com.project.Teaming.global.error.exception.NoAuthorityException;
import com.project.Teaming.global.result.pagenateResponse.PaginatedResponse;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MentoringBoardService {

    private final MentoringBoardRepository mentoringBoardRepository;
    private final MentoringTeamRepository mentoringTeamRepository;
    private final UserRepository userRepository;
    private final MentoringParticipationRepository mentoringParticipationRepository;

    /**
     * 게시물을 저장하는 로직
     * 저장된 post Id 반환
     * @param teamId
     * @param boardDto
     */
    @Transactional
    public Long saveMentoringPost(Long userId,Long teamId, RqBoardDto boardDto) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("회원이 존재하지 않습니다."));
        MentoringTeam mentoringTeam = mentoringTeamRepository.findById(teamId).orElseThrow(MentoringTeamNotFoundException::new);  //명시적 조회, 최신 데이터 반영
        Optional<MentoringParticipation> TeamUser = mentoringParticipationRepository.findByMentoringTeamAndUserAndParticipationStatus(mentoringTeam, user, MentoringParticipationStatus.ACCEPTED);
        if (TeamUser.isPresent()) {
            MentoringBoard mentoringBoard = MentoringBoard.builder()
                    .title(boardDto.getTitle())
                    .contents(boardDto.getContents())
                    .role(boardDto.getRole())
                    .mentoringCnt(boardDto.getMentoringCnt())
                    .build();
            mentoringBoard.setLink(Optional.ofNullable(boardDto.getLink())
                    .orElse(mentoringTeam.getLink()));
            mentoringBoard.addMentoringBoard(mentoringTeam);  // 멘토링 팀과 연관관계 매핑

            MentoringBoard savedPost = mentoringBoardRepository.save(mentoringBoard);
            return savedPost.getId();
        } else throw new NoAuthorityException(ErrorCode.NO_AUTHORITY);
    }

    /**
     * 특정 게시물을 조회하는 로직
     * @param postId
     * @return
     */
    @Transactional
    public MentoringBoard findMentoringPost(Long postId) {
        MentoringBoard mentoringBoard = mentoringBoardRepository.findById(postId)
                .orElseThrow(() -> new MentoringPostNotFoundException("이미 삭제되었거나 존재하지 않는 글 입니다."));
        if (mentoringBoard.getMentoringTeam().getFlag() == Status.FALSE) { //team의 최신 데이터 업데이트
            return mentoringBoard;
        } else {
            throw new MentoringTeamNotFoundException("이미 삭제된 팀의 글 입니다.");
        }
    }

    /**
     * 특정 멘토링 팀의 삭제되지않은 모든 게시물들을 가져오는 로직
     * @param teamId
     * @return
     */
    public List<RsBoardDto> findAllMyMentoringPost(Long teamId) {
        List<RsBoardDto> boards = mentoringBoardRepository.findAllByMentoringTeamId(teamId);
        List<Object[]> categoryResults = mentoringBoardRepository.findAllCategoriesByMentoringTeamId(teamId);
        MentoringTeam mentoringTeam = mentoringTeamRepository.findById(teamId).orElseThrow(MentoringTeamNotFoundException::new);
        if (mentoringTeam.getFlag() == Status.FALSE) {
            Map<Long, List<String>> categoryMap = new ConcurrentHashMap<>();
            for (Object[] row : categoryResults) {
                Long boardId = (Long) row[0];
                String categoryName = (String) row[1];
                categoryMap.computeIfAbsent(boardId, k -> new ArrayList<>()).add(categoryName);
            }
            boards.forEach(post -> {
                List<String> categories = categoryMap.getOrDefault(post.getId(), Collections.emptyList());
                post.setCategory(categories);
            });
            return boards;
        }else throw new MentoringTeamNotFoundException("이미 삭제 된 멘토링 팀 입니다.");
    }

    /**
     * 삭제되지 않은 모든 게시물들을 가져오는 로직
     *
     * @return
     */
    public PaginatedResponse<RsBoardDto> findAllPosts(MentoringStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        List<Long> boardIds = mentoringBoardRepository.findMentoringBoardIds(status, Status.FALSE, pageable);

        List<Tuple> results = mentoringBoardRepository.findAllByIds(boardIds);
        //메모리에서 정렬
        Map<Long, List<Tuple>> groupedResults = results.stream()
                .collect(Collectors.groupingBy(tuple -> tuple.get(QMentoringBoard.mentoringBoard.id)));

        List<RsBoardDto> dtoResults = boardIds.stream()
                .map(boardId -> {
                    List<Tuple> groupedTuples = groupedResults.get(boardId);
                    if (groupedTuples == null || groupedTuples.isEmpty()) {
                        return null;
                    }
                    Tuple firstTuple = groupedTuples.get(0);

                    // 각 보드에 대해 카테고리 리스트 생성
                    List<String> categories = groupedTuples.stream()
                            .map(tuple -> tuple.get(QCategory.category.name))
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());

                    // RsBoardDto 객체 생성
                    return new RsBoardDto(
                            firstTuple.get(QMentoringBoard.mentoringBoard.id),
                            firstTuple.get(QMentoringBoard.mentoringBoard.title),
                            firstTuple.get(QMentoringTeam.mentoringTeam.name),
                            firstTuple.get(QMentoringTeam.mentoringTeam.startDate),
                            firstTuple.get(QMentoringTeam.mentoringTeam.endDate),
                            categories,
                            firstTuple.get(QMentoringBoard.mentoringBoard.contents)
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        long total = mentoringBoardRepository.countAllByStatus(status, Status.FALSE);

        Page<RsBoardDto> pageDto = new PageImpl<>(dtoResults, pageable, total);
        return new PaginatedResponse<>(
                pageDto.getContent(),
                pageDto.getTotalPages(),
                pageDto.getTotalElements(),
                pageDto.getSize(),
                pageDto.getNumber(),
                pageDto.isFirst(),
                pageDto.isLast(),
                pageDto.getNumberOfElements()
        );

    }

    /**
     *  게시물을 수정하는 로직
     * @param postId
     * @param dto
     * @return
     */
    @Transactional
    public MentoringAuthority updateMentoringPost(Long userId, Long postId, RqBoardDto dto) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("회원이 존재하지 않습니다."));
        MentoringBoard mentoringBoard = mentoringBoardRepository.findById(postId)
                .orElseThrow(() -> new MentoringPostNotFoundException("이미 삭제되었거나 존재하지 않는 글 입니다."));
        Optional<MentoringParticipation> teamUser = mentoringParticipationRepository.findByMentoringTeamAndUserAndParticipationStatus(mentoringBoard.getMentoringTeam(), user, MentoringParticipationStatus.ACCEPTED);
        if (teamUser.isPresent()) {
            if (mentoringBoard.getMentoringTeam().getFlag() == Status.FALSE) {  //team의 최신 데이터 업데이트
                mentoringBoard.updateBoard(dto);
                return teamUser.get().getAuthority();
            } else {
                throw new MentoringTeamNotFoundException("이미 삭제된 팀의 글 입니다.");
            }
        }
        else throw new NoAuthorityException(ErrorCode.NO_AUTHORITY);
    }

    /**
     * 게시물을 삭제하는 로직
     * @param postId
     */
    @Transactional
    public void deleteMentoringPost(Long userId,Long postId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("회원이 존재하지 않습니다."));
        MentoringBoard mentoringBoard = mentoringBoardRepository.findById(postId)
                .orElseThrow(() -> new MentoringPostNotFoundException("이미 삭제되었거나 존재하지 않는 글 입니다."));
        Optional<MentoringParticipation> teamUser = mentoringParticipationRepository.findByMentoringTeamAndUserAndParticipationStatus(mentoringBoard.getMentoringTeam(), user, MentoringParticipationStatus.ACCEPTED);
        if (teamUser.isPresent()) {
            if (mentoringBoard.getMentoringTeam().getFlag() == Status.FALSE) { //team의 최신 데이터 업데이트
                mentoringBoardRepository.delete(mentoringBoard);
            } else {
                throw new MentoringTeamNotFoundException("이미 삭제된 팀의 글 입니다.");
            }
        }
        else throw new NoAuthorityException(ErrorCode.NO_AUTHORITY);
    }

}
