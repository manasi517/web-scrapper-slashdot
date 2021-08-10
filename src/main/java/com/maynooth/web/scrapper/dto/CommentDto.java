package com.maynooth.web.scrapper.dto;

import java.util.Date;

import com.maynooth.web.scrapper.enums.CommentClass;

public class CommentDto {

	private int commentId;
	
	private CommentClass cls;
	
	private String title;
	private int score;
	private String type;
	private String postedBy;
	private Date postTime;
	private int replyCount;
	private boolean isInappropriate;
	private int depth;
	private String content;
	private ArticleDto article;
	private CommentDto parentComment;
	private Long timeDiff;

	public ArticleDto getArticle() {
		return article;
	}

	public void setArticle(ArticleDto article) {
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

	public CommentDto getParentComment() {
		return parentComment;
	}

	public void setParentComment(CommentDto parentComment) {
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

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public int getCommentId() {
		return commentId;
	}

	public void setCommentId(int commentId) {
		this.commentId = commentId;
	}
	
	

	public Long getTimeDiff() {
		return timeDiff;
	}

	public void setTimeDiff(Long timeDiff) {
		this.timeDiff = timeDiff;
	}

	@Override
	public String toString() {
		return "CommentDto [commentId=" + commentId + ", cls=" + cls + ", title=" + title + ", score=" + score
				+ ", type=" + type + ", postedBy=" + postedBy + ", postTime=" + postTime + ", replyCount=" + replyCount
				+ ", isInappropriate=" + isInappropriate + ", depth=" + depth + ", content=" + content + ", article="
				+ article + ", parentComment=" + parentComment + ", timeDiff=" + timeDiff + "]";
	}
	
	
}
