package dev.haguel.expirenza_agent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Builder
@Getter
public class Restaurant {
    private String name;
    List<Dish> dishes;
}
