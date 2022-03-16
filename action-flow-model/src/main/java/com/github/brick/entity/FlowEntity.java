package com.github.brick.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FlowEntity {
    private String id;
    private List<WorkEntity> workEntities =new ArrayList<>();
}
