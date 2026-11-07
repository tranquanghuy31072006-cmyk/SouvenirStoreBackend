package com.learning.souvenirstorebackend.mapper;

import com.learning.souvenirstorebackend.dto.request.RegisterRequest;
import com.learning.souvenirstorebackend.dto.request.UserCreationRequest;
import com.learning.souvenirstorebackend.dto.request.UserUpdateRequest;
import com.learning.souvenirstorebackend.dto.response.UserResponse;
import com.learning.souvenirstorebackend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toUser(UserCreationRequest request);
    User toUser(RegisterRequest registerRequest);

    UserResponse toUserResponse(User user);

    void updateUser(@MappingTarget User user, UserUpdateRequest userUpdateRequest);
}
