package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.annotations.QueryProjection;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.Dto.MemberDto;
import study.querydsl.Dto.QMemberDto;
import study.querydsl.Dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;  //import static
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @Autowired
    EntityManager em;


    JPAQueryFactory queryFactory; //리팩토링하면서 추가 된 부분 //멀티스레드에서 접근해도 문제없게 설계되어있기 때문에 필드로 뺴서 사용하는것을 권장장

    @BeforeEach //데이터를 미리 넣어놓는 작업
    public void before() {
        queryFactory = new JPAQueryFactory(em); //리팩토링하면서 추가 된 부분
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    //훑기
    @Test
    public void startJPQL() {
        //member1을 찾아라.
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    //member1을 찾아라.
    //우선 JPAQueryFactory를 만들 때 EntityManager를 생성자에 같이 넘겨줘야한다. 그래야 em에서 데이터를 찾을 수 있다.
    //JPAQueryFactory queryFactory = new JPAQueryFactory(em); //리팩토링 과정에서 필드로 뺏다.

    //QMember m = new QMember("m"); // 어떤 QMember인지 구문하기 위해 variable: "m" 값을 준 것, 나중에는 Qmember.member로 생성할 수 있기 때문에 안쓴다.
    //QMember m = QMember.member; //이 방식보다 더 깔끔한 방식은 이 녀석을 지우고 아래 괄호() 안에 Qmember.member로 채우고 static import를 하면 깔끔하게 사용 가능
    @Test
    public void startQuerydsl() {  //querydsl 은 JPQL을 빌더 역활하기 때문에 결국은 JPQL로 반환한다.
        Member findMember = queryFactory
                .select(member)  //큰 장점 1. querydsl을 사용하면 컴파일 시 오타를 포함한 오류를 잡아준다.
                .from(member)
                .where(member.username.eq("member1"))//큰 장점 2. 자동으로 파라미터 바인딩 처리
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    //select와 from을 합칠 수 있다.
    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")  //eq = equal
                        .and(member.age.eq(10)))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    //and조건을 ,로 대신할 수 있다.
    @Test
    public void searchAndParam() {
        List<Member> result1 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10))
                .fetch();
        assertThat(result1.size()).isEqualTo(1);
    }

    /*fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환 (조회 대상이 여러건일 경우. 컬렉션 반환)
    fetchOne() : 단 건 조회 결과가 없으면 : null
                           결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException
    fetchFirst() : limit(1).fetchOne() 조회 대상이 1건이든 1건 이상이든 무조건 1건만 반환.
    fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행 (조회한 리스트 + 전체 개수를 포함한 QueryResults 반환. count 쿼리가 추가로 실행된다.)
    fetchCount() : count 쿼리로 변경해서 count 수 조회  (개수 조회. long 타입 반환) */
    @Test
    public void resultFetch() {
        //List
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();
//단 건
        Member findMember1 = queryFactory
                .selectFrom(member)
                .fetchOne();
//처음 한 건 조회
        Member findMember2 = queryFactory
                .selectFrom(member)
                .fetchFirst();
//페이징에서 사용
        QueryResults<Member> results = queryFactory //성능을 최대화하기 위해서는 이 방식보다 query문을 두 번 날려서 하는게 더욱 좋다.
                .selectFrom(member)
                .fetchResults();
//count 쿼리로 변경
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    //조회 건수 제한
    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터 시작(zero index)인데 1번째 로우부터 시작하겠다는 뜻
                .limit(2) //최대 2건 조회 즉, 1번째 로우부터 2건 조회  = 1, 2 번째 로우를 가져오겠다는 뜻
                .fetch();
        assertThat(result.size()).isEqualTo(2);
    }

    //전체 조회 수
    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
        /*  실무에서 페이징 쿼리를 작성할 때, 데이터를 조회하는 쿼리는 여러 테이블을 조인해야 하지만,
            count 쿼리는 조인이 필요 없는 경우도 있다. 그런데 이렇게 자동화된 count 쿼리는 원본 쿼리와 같이 모두
            조인을 해버리기 때문에 성능이 안나올 수 있다. count 쿼리에 조인이 필요없는 성능 최적화가 필요하다면,
            count 전용 쿼리를 별도로 작성해야 한다. */
    }

    /**
     * JPQL
     * select
     * COUNT(m), //회원수
     * SUM(m.age), //나이 합
     * AVG(m.age), //평균 나이
     * MAX(m.age), //최대 나이
     * MIN(m.age) //최소 나이
     * from Member m
     */

    //tuple은 여러개 타입을 가져와야 할 때 사용, 여러 개 저장할 수 있음
    @Test
    public void aggregation() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /*
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)   //회원이 가지고 있는 연관 필드로 팀과 조인한다.
                .groupBy(team.name)
                .fetch();
        //test
        Tuple teamA = result.get(0);  //memeber는 4명이지만 팀은 2개이기 떄문에 groubBy(team.name)을 한 순간 두 줄의 데이터가 나온다.
        Tuple teamB = result.get(1);
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }
    /* groupBy(), having() 예시
     …
    .groupBy(item.price)            //item.price로 그룹화해서 1000이 넘는 것만 뽑아라.
    .having(item.price.gt(1000))
     */

    /*
    기본 조인
    조인의 기본 문법은 첫 번째 파라미터에 조인 대상을 지정하고, 두 번째 파라미터에 별칭(alias)으로 사용할
    Q 타입을 지정하면 된다.

    join() , innerJoin() : 내부 조인(inner join)
    leftJoin() : left 외부 조인(left outer join)
    rightJoin() : right 외부 조인(rigth outer join)
    JPQL의 on 과 성능 최적화를 위한 fetch 조인 제공 다음 on 절에서 설명
     */

    /*
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() throws Exception {
        QMember member = QMember.member;
        QTeam team = QTeam.team;
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /*
     * 세타 조인(연관관계가 없는 필드로 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        List<Member> result = queryFactory
                .select(member)
                .from(member, team) //from에서 그냥 2개를
                .where(member.username.eq(team.name)) // 연관관계가 없는 필드로 조인 (member.username <-> team.name)
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }
    /*
    from 절에 여러 엔티티를 선택해서 세타 조인
    외부 조인 불가능 But 다음에 설명할 조인 on을 사용하면 외부 조인 가능 (하이버네이트 최신버전이 출시되면서 방법이 생김)
     */

    /*
    ON절을 활용한 조인(JPA 2.1부터 지원)
    1. 조인 대상 필터링
    2. 연관관계 없는 엔티티 외부 조인 -> 이 경우에 주로 쓰임
     */

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and
     * t.name='teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                //.join(member.team, team)      // leftjoin이 아닌 inner join일 경우
                //where(team.name.eq("TeamA"))  //on대신 이걸 써도 결과가 같다.
                .on(team.name.eq("teamA"))

                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    /*
    on 절을 활용해 조인 대상을 필터링 할 때, 외부조인이 아니라 내부조인(inner join)을 사용하면,
    where 절에서 필터링 하는 것과 기능이 동일하다. 따라서 on 절을 활용한 조인 대상 필터링을 사용할 때,
    내부조인 이면 익숙한 where 절로 해결하고, 정말 외부조인이 필요한 경우에만 이 기능을 사용하자
     */

    /**
     * 2. 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name)) //보통 leftjoin(member.team, team) 이렇게 들어가는데 연관관계 없는 엔티티 외부 조인을 할 때 쓰는 방법(member.username과 team.name은 연관이없다.)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
    }
    /*
    t=[Member(id=3, username=member1, age=10), null]
    t=[Member(id=4, username=member2, age=20), null]
    t=[Member(id=5, username=member3, age=30), null]
    t=[Member(id=6, username=member4, age=40), null]
    t=[Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
    t=[Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]
    멤버는 다 나오고 멤버이름과 팀 이름이 같은 경우에는 해당 팀을 조인
    leftjoin(team)의 경우 on 조건에 한해서만 외부 조인을 한다.

    leftjoin이 아닌 inner join(team)일 경우
    t=[Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
    t=[Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]

    하이버네이트 5.1부터 on 을 사용해서 서로 관계가 없는 필드로 외부 조인하는 기능이 추가되었다. 물론
    내부 조인도 가능하다.
    주의! 문법을 잘 봐야 한다. leftJoin() 부분에 일반 조인과 다르게 엔티티 하나만 들어간다.
    일반조인: leftJoin(member.team, team)
    on조인: from(member).leftJoin(team).on(xxx)
     */

    /*
    페치 조인은 SQL에서 제공하는 기능은 아니다. SQL조인을 활용해서 연관된 엔티티를 SQL 한번에
    조회하는 기능이다. 주로 성능 최적화에 사용하는 방법이다. 많이 사용함
     */

    //    페치 조인 미적용
//    지연로딩으로 Member, Team SQL 쿼리 각각 실행
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {
        em.flush();
        em.clear();
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();  //Member Entity를 보면 team이 Lazy이기 때문에 Member조회할 때 team은 조회가 안된다.
        boolean loaded =
                emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); //findMember가 team을 갖는 로딩된 entity인지 아직 로딩(초기화) 안 된 entity인지 판별해줌줌
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    //    페치 조인 적용
//    즉시로딩으로 Member, Team SQL 쿼리 조인으로 한번에 조회
    @Test
    public void fetchJoinUse() throws Exception {
        em.flush();
        em.clear();
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()//어떤 join이든 뒤에 fetchjoin을 넣어주면 즉시로딩되어 연관 필드(team)을 한 query로 한 번에 끌고온다.(연관필드도 select)
                .where(member.username.eq("member1"))
                .fetchOne();
        boolean loaded =
                emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

//    서브 쿼리 eq 사용

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() throws Exception {
        QMember memberSub = new QMember("memberSub"); // alias가 중복되면 안되기 때문에 직접 생성해주면 된다.
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max()) //JPAExpressions을 static import한 것 , 동적 처리 가능
                                .from(memberSub)  //40 return
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 나이 이상인 회원
     */
    @Test
    public void subQueryGoe() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 서브쿼리 여러 건 처리, in 사용
     */
    @Test
    public void subQueryIn() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))  //굉장히 효율적이지 못한 쿼리이지만 in 사용을 보여주기 위한 쿼리
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    //select절에 subquery 사용 예시
    @Test
    public void selectSubQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> fetch = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())  //JPAExpressions을 static import한 것 -> select 내에 select 가능하게 해줌
                                .from(memberSub)
                ).from(member)
                .fetch();
        for (Tuple tuple : fetch) {
            System.out.println("username = " + tuple.get(member.username));
            System.out.println("age = " +
                    tuple.get(select(memberSub.age.avg())
                            .from(memberSub)));
        }
    }
    /*
    from 절의 서브쿼리 한계
    JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다. 당연히 Querydsl
    도 지원하지 않는다. 하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원한다. Querydsl도
    하이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다.

    from 절의 서브쿼리 해결방안
    1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.) // 보통 가능하다.
    2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
    3. nativeSQL을 사용한다.
     */


    /*
    Case 문
    select, 조건절(where), order by에서 사용 가능
    But 가급적이면 DB에서 이런 문제를 해결하기보다는
    데이터 filter, grouping 정도의 전달에 초점을 맞추기
     */
    //실무에서는 member를 그대로 전달해주고 application에서 조작을 하는게 맞다.
    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
    }

    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
    }

    /*
    상수, 문자 더하기
    상수가 필요하면 Expressions.constant(xxx) 사용
     */

    @Test
    public void constant() {
        Tuple result = queryFactory
                .select(member.username, Expressions.constant("A")) //tuple = [member1, A], tuple = [member2, A].....
                .from(member)
                .fetchFirst();
    }

    @Test
    public void concat() {
        String result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))  //age 데이터타입 변형 필수
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
    }
    // member1_10

    //member.age.stringValue() 부분이 중요한데, 문자가 아닌 다른 타입들은 stringValue() 로
    //문자로 변환할 수 있다. 이 방법은 ENUM을 처리할 때도 자주 사용한다.

    /*
    중급 문법
    프로젝션과 결과 반환 - 기본
    프로젝션: select 대상 지정 (select절에서 뭘 가져올지 대상을 지정하는 것)

    if 프로젝션 대상이 하나 -> 그 대상에 맞는 데이터타입으로 받을 수 있다.(String으로 받았다.)
    List<String> result = queryFactory
         .select(member.username)
         .from(member)
         .fetch();

         프로젝션 대상이 하나면 타입을 명확하게 지정할 수 있음 (String)
        프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회 (튜플 : 한 번에 여러 개 담아서 막 꺼내쓸 수 있는 상자)
     */

    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory // 여러 타입을 받아야하기 때문에 tuple
                .select(member.username, member.age) //2개 가져와야함
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            String username = tuple.get(member.username); //일반 객체 or Dto에서 데이터 꺼내는 것 처럼 꺼내면 된다.
            Integer age = tuple.get(member.age);
            System.out.println("username=" + username);
            System.out.println("age=" + age);
        }
    }
    /*
    Tuple은 Querydsl에서 구현되는 것(package com.querydsl.core)이기 때문에 Repo or Dao 계층 안에서만 사용하고
    Service, Controller에서는 Tuple뿐만 아니라 제한이 있는 타입에 의존성이 없도록 설계하는 것이 옳바르다.
    바깥 계층으로 던질 때에는 DTO로 변환해서 던지는 것을 권장함.
     */

    /*
    실무에서 가장 많이 쓰이는 부분!

    프로젝션과 결과 반환 - DTO 조회
    순수 JPA에서 DTO 조회
     */

//    @Test // JQPL로 조회해야 할 경우 (참고만)
//    public void findDtoByJPQL(){
//        List<MemberDto> result = em.createQuery(
//                        "select new study.querydsl.dto.MemberDto(m.username, m.age) " +
//                                "from Member m", MemberDto.class)
//                .getResultList();
//    }

//    순수 JPA에서 DTO를 조회할 때는 new 명령어를 사용해야함
//    DTO의 package이름을 다 적어줘야해서 지저분함
//    생성자 방식만 지원함



    /*
    Querydsl은 위의 방식보다 훨씬 편리하게 할 수 있다.

    Querydsl 빈 생성(Bean population)

    결과를 DTO로 반환하고싶을 때 사용하는 방법 소개
    다음 3가지 방법 지원

    프로퍼티 접근
    필드 직접 접근
    생성자 사용
     */

    //프로퍼티 접근 - Setter
    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,   //Projections.bean 방식은 setter 기반으로 동작
                        member.username,                    //setter 메서드를 이용할때는 이렇게 dto의 멤버변수들과 entity의 멤버 변수가 일치해야한다.
                        member.age))                        //하지만 Dto에 @Getter @Setter 필요
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);

            //일반적으로 Response, Request 객체는 불변 객체를 지향하는 것이 바람직하다고 생각하기 때문에 권장하는 패턴은 아닙니다.
            //MemberDto(username=member1, age=10)
            //MemberDto(username=member2, age=20) .... 생성됨
        }
    }

    //    필드 직접 접근
    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, //bean 대신 fields를 쓰면 필드에 바로 값이 쭉 들어간다. (Getter Setter 필요 x)
                        member.username,
                        member.age))
                .from(member)
                .fetch();
    }

    // 생성자 사용
    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, //bean 대신 constuctor
                        member.username,
                        member.age))
                .from(member)
                .fetch();
    }

    //추가) 프로퍼티나, 필드 접근 생성 방식에서 이름이 다를 때 해결 방안
    //      Dto의 필드네임이 username 이 아닌 name일 경우 위와 똑같이 작성하면 name에 null이 들어간다
    //      생성자 방식의 경우 필드네임이 같은 것 보다 데이터타입이 맞는 것이 중요하기 때문에 데이터 타입만 맞으면 잘 들어간다.

    @Test
    public void findUserDto() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> fetch = queryFactory
                .select(Projections.fields(UserDto.class,
                                member.username.as("name"), // 데이터 타입은 같은데 별칭이 다르면 이 방식으로 별칭을 맞춰주면 잘 들어감(userDto는 username이 아닌 name을 갖고있음)
                                //
                                ExpressionUtils.as(              //Dto의 age에 내가 원하는 값으로 넣고 싶을 때(데이터처리가 필요할 때)
                                        JPAExpressions           //데이터 가공을 해서 마지막에 alias "필드네임" 을 잘 맞춰주면 된다.
                                                .select(memberSub.age.max())
                                                .from(memberSub), "age")
                        )
                ).from(member)
                .fetch();
    }

    /*
    프로젝션과 결과 반환 - @QueryProjection(위의 3가지 방식의 상위호환 개념)
    가장 깔끔한 방법이긴 하나 약간의 단점 존재

    -> MemberDto에  @QueryProjection 어노테이션만 달아주고 ./gradlew compileQuerydsl 실행하면
        MemberDto의 Q파일이 생성됨.

    @QueryProjection
     public MemberDto(String username, int age) {
     this.username = username;
     this.age = age;
     }
     */
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
    }
    //이 방법은 컴파일러로 타입을 체크할 수 있으므로 가장 안전한 방법이다. 다만 DTO에 QueryDSL
    //어노테이션을 유지해야 하는 점과 DTO까지 Q 파일을 생성해야 하는 단점이 있다.(큰 단점은 아님)

    //MemberDto(username=member1, age=10)
    //MemberDto(username=member2, age=20) .... 생성됨


    /*
    동적 쿼리 - BooleanBuilder 사용
    동적 쿼리를 해결하는 두가지 방식
    BooleanBuilder
    Where 다중 파라미터 사용
     */

    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
//        em.persist(new Member(null, 10));
        //위의 주석을 풀고난 후 아래 로직에서 usernameParam=null , expected를 2로 설정하면 테스트 통과한다. (조건에 맞는 검색기능같은 느낌)
        //조건 설정
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember1(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    //Cond는 condition 줄인 것,  parameter의 값이 null이냐 아니냐에 따라 쿼리가 동적으로 변하는 것을 보여주려는 것
    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
//        BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond)); // BooleanBuilder()내에 조건을 걸어 필수로 받을 파라미터를 명시할 수 있다.(null이 오면 안된다.)
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            //BooleanBuiler에 and조건을 넣어서 member1 and 상태를 만듬
            builder.and(member.username.eq(usernameCond));
        }
        //null이 아니기 때문에 username=member1 and age=10이라는 문장이 where()안에 들어가기 떄문에 조건이 걸린다.
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    //동적 쿼리 - Where 다중 파라미터 사용(굉장히 좋은 방법)

    @Test
    public void dynamicQuery_WhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember2(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                //where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    //생성된 메소드들을 조립 가능  ->조립을 해서 하나의 서비스를 담당하는 메소드를 만들 수 있음.
    //하지만 null 체크는 주의해서 처리해야하는 단점 존재
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

//    where 조건에 null 값은 무시된다. -> 만약 usernameCond 에 Null이 들어갈 경우 Null을 조건으로 포함시키는 것이 아니라 무시된다.
//    메서드를 다른 쿼리에서도 재활용 할 수 있다.
//    쿼리 자체의 가독성이 높아진다.
// 메소드가 늘어나서 지저분해지는 것 처럼 보이지만 개발할 때 메소드들은 다른 곳에 모아놓고 queryFactory부분을 보기 때문에 보이는 부분을 가독성 높게 유지해주는 것이 좋다.


    /*
    수정, 삭제 벌크 연산(배치 쿼리)
    쿼리 한번으로 대량 데이터 수정 (JPA에서 더티체킹(변경감지) 즉, 객체를 수정했을 때 transaction commit이 되면서 flush가 일어나 DB에 반영을 하지만 그건 건 by 건 으로 작동하기 때문에 DB의 접근 횟수가 많다.
    if 모든 개발자 연봉 50% 인상하고 싶다면 건 by 건으로 처리하는 것보다 한 번에 처리하는 것이 성능적으로 우수하다.
    */
    @Test
    public void bulkUpdate() {
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28)) //나이가 28살 미만
                .execute(); //영향을 받은 row 수 반환 (member1, member2 를 비회원으로 수정 -> 2 반환)

        em.flush();
        em.clear();
    }

    //JPQL 배치와 마찬가지로, 영속성 컨텍스트에 있는 엔티티를 무시하고 실행되기 때문에
//    DB에는
//    비회원 = 10;
//    비회원 = 20;
//    member3 = 30;
//    member4 = 40;  이렇게 수정이 되었지만

//    영속성 컨텍스트에는 변화가 일어나지 않아
//    member1 = 10;
//    member2 = 20;
//    member3 = 30;
//    member4 = 40; 이렇게 되어있으므로 DB와 영속성 컨텍스트의 상태가 달라지게 된다.

//    그러므로 배치 쿼리를 실행하고 나면 영속성 컨텍스트를 초기화를 해주어야 수정이 안 된 영속성컨텍스트에서 값을 가져오는 불상사를 막을 수 있고(영속성컨텍스트가 우선권을 갖기때문)
//    DB의 수정된 값을 가져오게 된다.

    /*
    기존 숫자에 1 더하기
     */
    @Test
    public void bulkAdd() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))  //곱하기는 multiply
                .execute();
    }

    /*
    쿼리 한번으로 대량 데이터 삭제
     */
    @Test
    public void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    /*
    SQL function 호출하기
    SQL function은 JPA와 같이 Dialect에 등록된 내용만 호출할 수 있다.
     */

//    member -> M으로 변경하는 replace 함수 사용
//    기본적인 sql 내장함수는 사용 가능.
    @Test
    public void sqlFunction() {
        String result = queryFactory
                //숫자면 numberTemplate써야한다.                   sql의 replace 함수 사용 {문자열 or 열 이름} , {바꾸려는 문자열} , {바뀔 문자열}
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                        member.username, " member ", " M "))
                .from(member)
                .fetchFirst();
    }
    //member테이블이 M1 = 10; M2 = 20; ... 이렇게 변경됨.

    /*
    소문자로 변경해서 비교해라.
     */
    @Test
    public void sqlFunction2() {
        String result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username))) //이런 기본 기능은 querydsl에서도 내장함수로 담고있다.
                .where(member.username.eq(member.username.lower()))
                .fetchFirst();
    }

    /*
    실무 활용 - 순수 JPA와 Querydsl
    순수 JPA 리포지토리와 Querydsl
    동적쿼리 Builder 적용
    동적쿼리 Where 적용
    조회 API 컨트롤러 개발
     */


}