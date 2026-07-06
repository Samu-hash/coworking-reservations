package com.cuscatlan.coworking.space;

import com.cuscatlan.coworking.space.dto.SpaceResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SpaceMapper {

    SpaceResponse toResponse(Space space);

    List<SpaceResponse> toResponseList(List<Space> spaces);
}
