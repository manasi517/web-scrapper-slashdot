package com.maynooth.web.scrapper.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import com.maynooth.web.scrapper.enums.CommentClass;

@Entity
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	private int commentCode;
	
	@Enumerated(EnumType.STRING)
	private CommentClass cls;
	
	private String title;
	private int score;
	private String type;
	private String postedBy;
	private Date postTime;
	private int replyCount;
	private boolean isInappropriate;
	
	@Lob
	private String content;
	
	@ManyToOne
	@JoinColumn(name = "article_id")
	private Article article;
	
	@ManyToOne
	@JoinColumn(name = "parent_comment_id")
	private Comment parentComment;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getCommentCode() {
		return commentCode;
	}

	public void setCommentCode(int commentCode) {
		this.commentCode = commentCode;
	}

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public String getPostedBy() {
		return postedBy;
	}

	public void setPostedBy(String postedBy) {
		this.postedBy = postedBy;
	}

	public Date getPostTime() {
		return postTime;
	}

	public void setPostTime(Date postTime) {
		this.postTime = postTime;
	}

	public int getReplyCount() {
		return replyCount;
	}

	public void setReplyCount(int replyCount) {
		this.replyCount = replyCount;
	}

	public Comment getParentComment() {
		return parentComment;
	}

	public void setParentComment(Comment parentComment) {
		this.parentComment = parentComment;
	}

	public boolean isInappropriate() {
		return isInappropriate;
	}

	public void setInappropriate(boolean isInappropriate) {
		this.isInappropriate = isInappropriate;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public CommentClass getCls() {
		return cls;
	}

	public void setCls(CommentClass cls) {
		this.cls = cls;
	}
	
	
}
