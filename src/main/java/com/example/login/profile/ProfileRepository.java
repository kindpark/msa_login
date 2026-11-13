package com.example.login.profile;


import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<ProfileRequestDto, Long> {

}
