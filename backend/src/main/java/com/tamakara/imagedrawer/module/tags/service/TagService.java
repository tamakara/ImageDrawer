package com.tamakara.imagedrawer.module.tags.service;

import com.tamakara.imagedrawer.module.tags.dto.TagDto;
import com.tamakara.imagedrawer.module.tags.entity.Tag;
import com.tamakara.imagedrawer.module.tags.mapper.TagMapper;
import com.tamakara.imagedrawer.module.tags.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public List<TagDto> listTags() {
        return tagRepository.findAll().stream()
                .map(tagMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<TagDto> searchTags(String query) {
        return tagRepository.findByNameContainingIgnoreCase(query).stream()
                .map(tagMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TagDto createTag(TagDto tagDto) {
        Tag tag = tagMapper.toEntity(tagDto);
        return tagMapper.toDto(tagRepository.save(tag));
    }

    @Transactional
    public void deleteTag(Long id) {
        tagRepository.deleteById(id);
    }
}
