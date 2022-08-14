package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    //select m from Member m where m.username(username은 메소드의 Username에서 가져온 것)
    List<Member> findByUsername(String username);
}
