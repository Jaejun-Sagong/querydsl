package study.querydsl.Dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

//검색 결과가 담길 DTO (조회 최적화용 DTO)
@Data
public class MemberTeamDto {
    private Long memberId;
    private String username;
    private int age;
    private Long teamId;
    private String teamName;


    //@QueryProjection 을 추가했다. QMemberTeamDto 를 생성하기 위해 .
    //gradlew compileQuerydsl 을 한 번 실행하자.
    @QueryProjection
    public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
