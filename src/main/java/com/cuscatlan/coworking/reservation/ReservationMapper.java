package com.cuscatlan.coworking.reservation;

import com.cuscatlan.coworking.reservation.dto.ReservationResponse;
import com.cuscatlan.coworking.space.Space;
import com.cuscatlan.coworking.user.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    ReservationResponse toResponse(Reservation reservation);

    ReservationResponse.SpaceSummary toSummary(Space space);

    ReservationResponse.UserSummary toSummary(User user);
}
