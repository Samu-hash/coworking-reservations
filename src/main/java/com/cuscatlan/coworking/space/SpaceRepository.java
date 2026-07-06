package com.cuscatlan.coworking.space;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpaceRepository extends JpaRepository<Space, Long> {

    List<Space> findByActiveTrueOrderByName();
}
