package study.querydsl.entity;
import lombok.*;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
@Entity
@Getter @Setter
//protected 기본 생성자를 만들어준다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "name"})
public class Team {

    @Id
    @GeneratedValue
    @Column(name = "team_id")
    private Long id;
    private String name;

    //mappedBy는 주인이 아님을 설정하는 것
    //member는 연관관계의 주인이 아니기때문에 team테이블에 외래키값을 업데이트 하지 않는다.
    //읽기만 수행한다.
    @OneToMany(mappedBy = "team")
    private List<Member> members = new ArrayList<>();

    //protected Team(){}


    public Team(String name) {
        this.name = name;
    }
}
