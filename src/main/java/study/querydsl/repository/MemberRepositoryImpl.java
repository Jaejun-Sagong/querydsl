package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.Dto.MemberSearchCondition;
import study.querydsl.Dto.MemberTeamDto;
import study.querydsl.Dto.QMemberTeamDto;
import study.querydsl.entity.Member;


import javax.persistence.EntityManager;
import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class MemberRepositoryImpl implements MemberRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    //회원명, 팀명, 나이(ageGoe, ageLoe)
    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetch();
    }

    /**
     * 단순한 페이징, fetchResults() 사용
     */
    //전체 카운트를 한번에 조회하는 단순한 방법
    //searchPageSimple(), fetchResults() 사용
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition,
                                                Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults(); //fetch를 쓰면 반환타입이 데이터 content를 리스트로 가져오는데( List<MemberTeamDto> )
                                 //fetchResults를 쓰면 querydsl이 컨텐츠형 쿼리도 날리고 카운트형 쿼리도 날린다. (2번 날림)

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total); //PageImpl은 Spring Data Page의 구현체체

    }

    //데이터 내용과 전체 카운트를 별도로 조회하는 방법
    //searchPageComplex()
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition,
                                                 Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();  //fetchResults가 아닌 fetch를 보내 content만 뽑고
        long total = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetchCount(); //count를 따로 뽑는다.

        return new PageImpl<>(content, pageable, total);
        //어떤 이득이 있냐
        //상황에 따라 다른데
        //전체 카운트를 조회 하는 방법을 최적화 할 수 있으면 이렇게 분리하면 된다. (예를 들어서 전체 카운트를 조회할 때 조인 쿼리를 줄일 수 있다면 상당한 효과가 있다.)
        //fetchResults를 안쓰고 fetchjoin없이(또는 로직을 효율적으로) 따로 뽑을 수 있다면 따로 로직을 구성하는게 성능면에서 우수하다.
    }
        @Override
        public Page<MemberTeamDto> usePageableExecutionUtils(MemberSearchCondition condition, Pageable pageable) {
            List<MemberTeamDto> content = queryFactory
                    .select(new QMemberTeamDto(
                            member.id,
                            member.username,
                            member.age,
                            team.id,
                            team.name))
                    .from(member)
                    .leftJoin(member.team, team)
                    .where(usernameEq(condition.getUsername()),
                            teamNameEq(condition.getTeamName()),
                            ageGoe(condition.getAgeGoe()),
                            ageLoe(condition.getAgeLoe()))
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .fetch();  //fetchResults가 아닌 fetch를 보내 content만 뽑고
        JPAQuery<Member> countQuery = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount); //searchPageComplex 코드와 여기만 다름.
            //스프링 데이터 라이브러리가 제공
            //count 쿼리가 생략 가능한 경우 생략해서 처리(countQuery 실행이 필요없을(비효율적일) 경우)
            //페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때 또는
            //마지막 페이지 일 때 (offset + 컨텐츠 사이즈를 더해서 전체 사이즈 구해줌)

    }

    private BooleanExpression usernameEq(String username) {
        return isEmpty(username) ? null : member.username.eq(username);
    }

    private BooleanExpression teamNameEq(String teamName) {
        return isEmpty(teamName) ? null : team.name.eq(teamName);
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe == null ? null : member.age.goe(ageGoe);
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe == null ? null : member.age.loe(ageLoe);
    }
}