package com.maynooth.web.scrapper.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.maynooth.web.scrapper.dto.ArticleDto;
import com.maynooth.web.scrapper.entity.Article;

@Mapper(componentModel = "spring")
public interface ArticleMapper {

	public List<Article> articleDtoToEntity(List<ArticleDto> articleDto);
}
