package org.example.expert.domain.todo.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.comment.entity.QComment;
import org.example.expert.domain.manager.entity.QManager;
import org.example.expert.domain.todo.dto.TodoGetCondition;
import org.example.expert.domain.todo.dto.TodoSearchCondition;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.QTodo;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.entity.QUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class TodoRepositoryQueryImpl implements TodoRepositoryQuery {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Todo> findTodosByCondition(TodoGetCondition condition, Pageable pageable) {
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

    @Override
    public Page<TodoSearchResponse> searchTodos(TodoSearchCondition condition, Pageable pageable) {
        QTodo todo = QTodo.todo;
        QUser user = QUser.user;
        QManager manager = QManager.manager;
        QComment comment = QComment.comment;

        BooleanBuilder builder = new BooleanBuilder();

        if (condition.getKeyword() != null) {
            builder.and(todo.title.contains(condition.getKeyword()));
        }
        if (condition.getStart() != null) {
            builder.and(todo.createdAt.goe(condition.getStart().atStartOfDay())); // 생성일 기준 시작일
        }
        if (condition.getEnd() != null) {
            builder.and(todo.createdAt.loe(condition.getEnd().atTime(23, 59, 59))); // 생성일 기준 종료일
        }
        if (condition.getNickname() != null) {
            builder.and(todo.user.nickname.contains(condition.getNickname()));
        }

        List<TodoSearchResponse> results = queryFactory
                .select(
                        Projections.constructor(TodoSearchResponse.class,
                                todo.title,
                                ExpressionUtils.as(JPAExpressions
                                        .select(manager.count())
                                        .from(manager)
                                        .where(manager.todo.eq(todo)), "managerCount"), // manager count
                                ExpressionUtils.as(JPAExpressions
                                        .select(comment.count())
                                        .from(comment)
                                        .where(comment.todo.eq(todo)), "commentCount")  // comment count
                        )
                )
                .from(todo)
                .leftJoin(todo.user, user)
                .where(builder)
                .groupBy(todo.id)
                .orderBy(todo.createdAt.desc()) // 생성일 기준 내림차순 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 데이터 수를 구하는 쿼리
        long total = queryFactory
                .select(todo.count())
                .from(todo)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public Optional<Todo> findByIdWithUser(Long todoId) {
        QTodo todo = QTodo.todo;
        QUser user = QUser.user;

        Todo result = queryFactory
                .selectFrom(todo)
                .leftJoin(todo.user, user).fetchJoin()
                .where(todo.id.eq(todoId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

}
