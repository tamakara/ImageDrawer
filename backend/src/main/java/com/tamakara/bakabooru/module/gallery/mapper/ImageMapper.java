package com.tamakara.bakabooru.module.gallery.mapper;

import com.tamakara.bakabooru.module.gallery.dto.ImageDto;
import com.tamakara.bakabooru.module.gallery.entity.Image;
import com.tamakara.bakabooru.module.tags.mapper.TagMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {TagMapper.class})
public interface ImageMapper {
    ImageDto toDto(Image image);

    @AfterMapping
    default void setUrl(@MappingTarget ImageDto dto, Image image) {
        if (image.getHash() != null) {
            dto.setUrl("/api/file/" + image.getHash());
            dto.setThumbnailUrl("/api/file/thumb/" + image.getHash());
        }
    }
}

