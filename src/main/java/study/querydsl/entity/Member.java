package study.querydsl.entity;
import lombok.*;
import javax.persistence.*;
@Entity
//Setter는 실무에서 왠만하면 안쓰는게 좋고 안쓴다.
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
//toString을 자동으로 만들어주는 lombok
@ToString(of = {"id", "username", "age"})
public class Member {
    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    //ManyToOne에서는 fetch= Lazy 설정해주는게 좋다.
    //연관관계 주인이므로 이 Entity에서 쓰기 기능을 맡는다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public Member(String username) {
        this(username, 0);
    }
    public Member(String username, int age) {
        this(username, age, null);
    }
    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
    }
    public void changeTeam(Team team) {
        this.team = team;
        //this 는 해당 객체를 가리킨다.
        //양방향 연관관계이기 때문에 반대쪽도 추가해줘야한다.
        team.getMembers().add(this);
    }

//아래 toString 을 @ToString(of = {"id", "username", "age"}) lombok이 대신 생성해준다.
//주의해야 할 점은 매개변수로 team과 같은 연관관계 핕드가 들어갈 경우 무한회귀가 발생한다.
//    @Override
//    public String toString() {
//        return "Member{" +
//                "id=" + id +
//                ", username='" + username + '\'' +
//                ", age=" + age +
//                '}';
//    }
}