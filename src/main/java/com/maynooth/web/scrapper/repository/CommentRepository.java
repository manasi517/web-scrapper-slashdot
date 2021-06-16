package com.maynooth.web.scrapper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maynooth.web.scrapper.entity.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {

}
