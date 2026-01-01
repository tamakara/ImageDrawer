package com.tamakara.imagedrawer.module.search.mapper;

import com.tamakara.imagedrawer.module.search.dto.TagDto;
import com.tamakara.imagedrawer.module.search.entity.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagDto toDto(Tag tag);
    Tag toEntity(TagDto tagDto);
}

