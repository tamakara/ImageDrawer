package com.imagedrawer.entity;

import com.imagedrawer.dto.ImageResponse;
import com.imagedrawer.util.ImageInfo;
import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "image")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "mimetype", nullable = false)
    private String mimetype;

    @Column(name = "hash", nullable = false)
    private String hash;

    @Column(name = "width", nullable = false)
    private Integer width;

    @Column(name = "height", nullable = false)
    private Integer height;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "rating", nullable = false)
    private String rating;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "image_tag", joinColumns = @JoinColumn(name = "image_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<Tag> tags;

    public Image(ImageInfo imageInfo, String rating, List<Tag> tags) {
        this.filename = imageInfo.getFilename();
        this.mimetype = imageInfo.getMimetype();
        this.hash = imageInfo.getHash();
        this.width = imageInfo.getWidth();
        this.height = imageInfo.getHeight();
        this.size = imageInfo.getSize();
        this.rating = rating;
        this.tags = tags;
    }

    public ImageResponse toResponse() {
        return new ImageResponse(filename, mimetype, hash, width, height, size, rating, tags.stream().map(Tag::getName).toList());
    }
}
