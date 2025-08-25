package dev.haguel.expirenza_agent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@AllArgsConstructor
@Builder
@Getter
public class Dish {
    private String name;
    private DishCategory dishCategory;
    private String description;
    private BigDecimal price;
}
