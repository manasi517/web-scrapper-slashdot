package com.maynooth.web.scrapper.mapper;

import java.util.LinkedHashSet;
import java.util.List;

import org.mapstruct.Mapper;

import com.maynooth.web.scrapper.dto.CommentDto;
import com.maynooth.web.scrapper.entity.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {

	public LinkedHashSet<Comment> articleDtoToEntity(LinkedHashSet<CommentDto> articleDto);
	public Comment commentDtoToEntity(CommentDto articleDto);
}
