package com.tamakara.bakabooru.module.tags.mapper;

import com.tamakara.bakabooru.module.tags.dto.TagDto;
import com.tamakara.bakabooru.module.tags.entity.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagDto toDto(Tag tag);
    Tag toEntity(TagDto tagDto);
}

