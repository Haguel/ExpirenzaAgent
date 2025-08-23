package dev.haguel.expirenza_agent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class Category {
    private final String category;
    private final String subCategory;
}
