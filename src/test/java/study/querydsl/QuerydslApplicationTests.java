package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

import javax.persistence.EntityManager;
import java.util.List;


//test에 @Transactional을 달아놓으면 다 rollback한다. 그래서 commit이 필요함
@Transactional
@SpringBootTest
@Commit
class QuerydslApplicationTests {
    @Autowired
    EntityManager em;

    @Test
    void contextLoads() {
        Hello hello = new Hello();
        em.persist(hello);
		//최신버전에서는 JPAQueryFactory 사용을 권장한다.
        JPAQueryFactory query = new JPAQueryFactory(em);
		//QHello qHello = new QHello("hello") 해도 되는데 QHello에 .hello 메소드로 생성자가 선언되어있다.
        QHello qHello = QHello.hello; //Querydsl Q타입 동작 확인
        Hello result = query
                .selectFrom(qHello)  //쿼리와 관련된 것은 모두 Q타입을 넣어야한다.
                .fetchOne();  //
        //쿼리로 qHello에서 불러온 것이 result에 담겼는데 그게 첫 줄에 생성된 hello객체와 완전 같은것(주소까지)
        Assertions.assertThat(result).isEqualTo(hello);
        //lombok 동작 확인 (getId())
        Assertions.assertThat(result.getId()).isEqualTo(hello.getId());
    }
}
