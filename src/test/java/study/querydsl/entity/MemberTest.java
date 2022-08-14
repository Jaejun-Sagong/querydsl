package study.querydsl.entity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
@SpringBootTest
@Transactional
//@Commit //테스트 완료했으면 주석처리를 하거나 지워줘야 다른 테스트에 영향을 안준다. commit이 남아있으면 DB에 계속 남아있기 때문에
public class MemberTest {
    @Autowired
    EntityManager em;

    @Test
    public void testEntity() {
        //TeamA, B를 생성하고
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        //TeamA, B를 EntityManager에 저장을 하고
        em.persist(teamA);
        em.persist(teamB);
        //멤버 생성
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        //멤버 EntityManager에 저장
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
        //초기화
        em.flush(); //flush를 하면 영속성 컨테스트에 있는 오브젝트들을 실제 쿼리를 만들어서 DB에 날리게 된다.(DB에 반영시킨다는 뜻인듯)
        em.clear(); //clear를 하면 영속성 컨텍스트를 완전히 초기화해서 cache 가 다 날라간다.
        //확인(쿼리문으로 member 생성)
        List<Member> members = em.createQuery("select m from Member m", Member.class)
                .getResultList();
        for (Member member : members) {
            System.out.println("member=" + member); //assert로 확인해도 되는데 직접 눈으로 보기위해 찍어보는 것
            System.out.println("-> member.team=" + member.getTeam());
        }
    }
}