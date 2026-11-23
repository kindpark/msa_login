package com.example.login.follow;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class InternalFollowController {
    private final FollowService followService;
    @GetMapping("/users/{id}/following-ids")
    public ResponseEntity<List<Long>> getFollowingIds(
            @PathVariable Long id){
        return ResponseEntity.ok(followService.getFollows(id));
    }
}
