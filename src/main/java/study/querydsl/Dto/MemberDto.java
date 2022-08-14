package study.querydsl.Dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
/*
@Data 에 포함되어 있는 lombok은 다음과 같다.

@ToString
@EqualsAndHashCode
@Getter : 모든 필드
@Setter : 정적 필드가 아닌 모든 필드
@RequiredArgsConstructor
@Data와 함께 포함되어 있는 lombok의 설정 예를 들어, callSuper, includeFieldNames 그리고 exclude 등을 지정할 수는 없다.

따라서 개별 어노테이션의 설정 값을 기본값이 아닌 값을 사용할 때에는 @Data 대신 개별 어노테이션을 사용하도록 한다.
 */
@Data
public class MemberDto {
    private String username;
    private int age;

    public MemberDto() {  //@NoArg 로 대체가능
    }

    @QueryProjection
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}