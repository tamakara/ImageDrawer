package com.imagedrawer.service;

import com.imagedrawer.entity.Tag;
import com.imagedrawer.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public List<String> queryTags(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return tagRepository.findAll()
                    .stream()
                    .map(Tag::getName)
                    .toList();
        }
        return tagRepository.findTagsOrderByMatch(keyword);
    }


    @Transactional
    public List<Tag> resolveTags(List<String> names) {
        List<Tag> existingTags = tagRepository.findByNameIn(names);
        Set<String> existingNames = existingTags.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        List<Tag> newTags = names.stream()
                .filter(name -> !existingNames.contains(name))
                .map(Tag::new)
                .toList();

        if (!newTags.isEmpty()) {
            tagRepository.saveAll(newTags);
        }

        return Stream.concat(existingTags.stream(), newTags.stream())
                .toList();
    }
}
