package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.Dto.MemberSearchCondition;
import study.querydsl.Dto.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepository;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {
    private final MemberJpaRepository memberJpaRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) //@RequestParam을 선언 안해줘도 VO를 넣어주면 일치하는 VO의 멤버변수에 값이 들어간다.
    {
        return memberJpaRepository.search(condition);
    }
}