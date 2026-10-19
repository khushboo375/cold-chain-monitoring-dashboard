package com.coldchain.dashboard.config;

import com.coldchain.dashboard.model.ColdChainEvent;
import com.coldchain.dashboard.repository.ColdChainEventRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleDataLoader {
    @Bean
    CommandLineRunner loadSampleData(ColdChainEventRepository repository) {
        return args -> {
            if (repository.count() > 0) return;
            repository.save(make("VAX-001", "Vaccine Store A", 4.2, 2.0, 8.0, "Within permitted range"));
            repository.save(make("VAX-002", "Vaccine Store B", 9.1, 2.0, 8.0, "Temperature above configured limit"));
            repository.save(make("FOOD-003", "Cold Room C", -19.0, -25.0, -15.0, "Frozen goods within range"));
            repository.save(make("MED-004", "Medicine Store D", 1.2, 2.0, 8.0, "Temperature below configured limit"));
        };
    }

    private ColdChainEvent make(String unitId, String location, double temperature,
                                double min, double max, String notes) {
        ColdChainEvent event = new ColdChainEvent();
        event.setUnitId(unitId);
        event.setLocation(location);
        event.setTemperatureC(temperature);
        event.setMinTempC(min);
        event.setMaxTempC(max);
        event.setNotes(notes);
        return event;
    }
}
