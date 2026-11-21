package com.example.login.integration.x.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class XTimelineResponse {
    List<XPost> items;
    String nextCursor;
}
