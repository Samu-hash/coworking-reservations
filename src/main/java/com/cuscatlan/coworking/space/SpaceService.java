package com.cuscatlan.coworking.space;

import com.cuscatlan.coworking.space.dto.SpaceRequest;
import com.cuscatlan.coworking.space.dto.SpaceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SpaceService {

    private final SpaceRepository repository;
    private final SpaceMapper mapper;

    public SpaceService(SpaceRepository repository, SpaceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public SpaceResponse create(SpaceRequest req) {
        Space space = new Space(req.name(), req.type(), req.capacity(), req.location(), req.hourlyRate());
        return mapper.toResponse(repository.save(space));
    }

    @Transactional
    public SpaceResponse update(Long id, SpaceRequest req) {
        Space space = find(id);
        space.update(req.name(), req.type(), req.capacity(), req.location(), req.hourlyRate());
        return mapper.toResponse(space);
    }

    @Transactional
    public void deactivate(Long id) {
        find(id).deactivate();
    }

    @Transactional(readOnly = true)
    public SpaceResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<SpaceResponse> listActive() {
        return mapper.toResponseList(repository.findByActiveTrueOrderByName());
    }

    public Space getEntity(Long id) {
        return find(id);
    }

    private Space find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espacio no encontrado: " + id));
    }
}
