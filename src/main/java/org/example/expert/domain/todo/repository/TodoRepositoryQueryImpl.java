package org.example.expert.domain.todo.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.todo.dto.TodoSearchCondition;
import org.example.expert.domain.todo.entity.QTodo;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.entity.QUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.example.expert.domain.todo.entity.QTodo.todo;
import static org.example.expert.domain.user.entity.QUser.user;

@RequiredArgsConstructor
public class TodoRepositoryQueryImpl implements TodoRepositoryQuery {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Todo> findTodosByCondition(TodoSearchCondition condition, Pageable pageable) {
        QTodo todo = QTodo.todo;
        QUser user = QUser.user;

        BooleanBuilder builder = new BooleanBuilder();

        if (condition.getWeather() != null) {
            builder.and(todo.weather.eq(condition.getWeather()));
        }
        if (condition.getStart() != null) {
            builder.and(todo.modifiedAt.goe(condition.getStart().atStartOfDay())); // 수정일 기준 시작일
        }
        if (condition.getEnd() != null) {
            builder.and(todo.modifiedAt.loe(condition.getEnd().atTime(23, 59, 59))); // 수정일 기준 종료일
        }

        List<Todo> results = queryFactory
                .selectFrom(todo)
                .leftJoin(todo.user, user).fetchJoin()
                .where(builder)
                .orderBy(todo.modifiedAt.desc()) // 수정일 기준 내림차순 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(todo)
                .where(builder)
                .fetchCount();

        return new PageImpl<>(results, pageable, total);
    }

}
