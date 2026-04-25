package PSM.Location.api.stopschedule;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import PSM.Location.StopSchedule;

@Service
public class StopScheduleService {
    private final StopScheduleRepository repository;

    public StopScheduleService(StopScheduleRepository repository) {
        this.repository = repository;
    }

    public List<StopSchedule> findAll() {
        return repository.findAll();
    }

    public StopSchedule findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("StopSchedule not found"));
    }

    public StopSchedule create(StopSchedule entity) {
        return repository.save(entity);
    }

    public StopSchedule update(UUID id, StopSchedule entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
