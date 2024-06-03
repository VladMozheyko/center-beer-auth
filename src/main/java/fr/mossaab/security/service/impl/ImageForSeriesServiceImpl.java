package fr.mossaab.security.service.impl;

import fr.mossaab.security.entities.ImageForSeries;
import fr.mossaab.security.repository.ImageForSeriesRepository;
import fr.mossaab.security.service.api.ImageForSeriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImageForSeriesServiceImpl implements ImageForSeriesService {

    private ImageForSeriesRepository imageForSeriesRepository;

    @Autowired
    public void setImageForSeriesRepository(ImageForSeriesRepository imageForSeriesRepository) {
        this.imageForSeriesRepository = imageForSeriesRepository;
    }

    @Override
    public void addAll(List<ImageForSeries> imagesForSeries) {
        if(!imagesForSeries.isEmpty()){
            imageForSeriesRepository.saveAllAndFlush(imagesForSeries);
        }
    }
}
