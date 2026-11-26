package com.example.login.integration.x.dto;

import java.time.LocalDateTime;
import java.util.List;

public record XResponse (
        List<Data> data,
        UserInclude includes,
        MetaData meta
){
    public record Data(
            String id,
            String text,
            LocalDateTime created_at,
            String author_id
    ){}
    public record UserInclude(
            List<XUsers> users
    ){
        public record XUsers(
                String id,
                String username,
                String name,
                String profile_image_url
        ){}
    }
    public record MetaData(
            String next_token
    ){}
}
