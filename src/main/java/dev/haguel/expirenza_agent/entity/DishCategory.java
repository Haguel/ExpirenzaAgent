package dev.haguel.expirenza_agent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class DishCategory {
    private final String category;
    private final String subCategory;
}
