package fr.mossaab.security.service.impl;

import fr.mossaab.security.entities.Series;
import fr.mossaab.security.repository.SeriesRepository;
import fr.mossaab.security.service.api.SeriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeriesServiceImpl implements SeriesService {

    private SeriesRepository seriesRepository;

    @Autowired
    public void setSeriesRepository(SeriesRepository seriesRepository) {
        this.seriesRepository = seriesRepository;
    }

    @Override
    public void addAll(List<Series> series) {
        if(!series.isEmpty()){
            seriesRepository.saveAllAndFlush(series);
        }
    }
}
