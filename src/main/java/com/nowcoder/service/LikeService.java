package com.nowcoder.service;

import com.nowcoder.model.Comment;
import com.nowcoder.model.EntityType;
import com.nowcoder.model.Question;
import com.nowcoder.util.JedisAdapter;
import com.nowcoder.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LikeService {
  @Autowired JedisAdapter jedisAdapter;

  @Autowired CommentService commentService;

  @Autowired QuestionService questionService;

  public long getLikeCount(int entityType, int entityId) {
    String likeKey = RedisKeyUtil.getLikeKey(entityType, entityId);
    return jedisAdapter.scard(likeKey);
  }

  public int getLikeStatus(int userId, int entityType, int entityId) {
    String likeKey = RedisKeyUtil.getLikeKey(entityType, entityId);
    if (jedisAdapter.sismember(likeKey, String.valueOf(userId))) {
      return 1;
    }
    String disLikeKey = RedisKeyUtil.getDisLikeKey(entityType, entityId);
    return jedisAdapter.sismember(disLikeKey, String.valueOf(userId)) ? -1 : 0;
  }

  public long like(int userId, int entityType, int entityId) {
    String likeKey = RedisKeyUtil.getLikeKey(entityType, entityId);
    jedisAdapter.sadd(likeKey, String.valueOf(userId));

    String disLikeKey = RedisKeyUtil.getDisLikeKey(entityType, entityId);
    jedisAdapter.srem(disLikeKey, String.valueOf(userId));

    return jedisAdapter.scard(likeKey);
  }

  public long disLike(int userId, int entityType, int entityId) {
    String disLikeKey = RedisKeyUtil.getDisLikeKey(entityType, entityId);
    jedisAdapter.sadd(disLikeKey, String.valueOf(userId));

    String likeKey = RedisKeyUtil.getLikeKey(entityType, entityId);
    jedisAdapter.srem(likeKey, String.valueOf(userId));

    return jedisAdapter.scard(likeKey);
  }

  public long getUserLikedCount(int userId) {
    int likeCount = 0;
    // 获取该用户所有评论
    List<Comment> comments = commentService.getCommentByUserId(userId);
    for (Comment comment : comments) {
      likeCount += getLikeCount(EntityType.ENTITY_COMMENT, comment.getId());
    }

    // 获取该用户所有问题
    List<Question> questions = questionService.getQuestionsByUserId(userId);
    for (Question question : questions) {
      likeCount += getLikeCount(EntityType.ENTITY_QUESTION, question.getId());
    }
    return likeCount;
  }
}
