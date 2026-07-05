package com.thebyline.domain.post;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(length = 300)
    private String description;

    /** Hex color used for the category tag (e.g. "#185FA5") */
    @Column(name = "color_hex", length = 7)
    private String colorHex;

    /** Background hex for the tag pill */
    @Column(name = "bg_hex", length = 7)
    private String bgHex;

    @Column(name = "display_order")
    private int displayOrder = 0;
}
