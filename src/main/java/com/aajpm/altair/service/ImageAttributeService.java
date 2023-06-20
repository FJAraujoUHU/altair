package com.aajpm.altair.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aajpm.altair.entity.ImageAttribute;
import com.aajpm.altair.repository.ImageAttributeRepository;

@Service
@Transactional
public class ImageAttributeService extends BasicEntityCRUDService<ImageAttribute> {

    /////////////////////////// MANAGED REPOSITORY ////////////////////////////

    @Autowired
    private ImageAttributeRepository imageAttributeRepository;

    @Override
    protected ImageAttributeRepository getManagedRepository() {
        return imageAttributeRepository;
    }

    /////////////////////////// SUPPORTING SERVICES ///////////////////////////

    // None

    /////////////////////////////// CONSTRUCTORS //////////////////////////////

    public ImageAttributeService() {
        super();
    }

    @Override
    public ImageAttribute create() {
        return new ImageAttribute();
    }

    /////////////////////////////// SAVE METHODS //////////////////////////////

    // Use inherited

    ///////////////////////////////// METHODS /////////////////////////////////

    public ImageAttribute findByName(String name) {
        return imageAttributeRepository.findByName(name);
    }

    public ImageAttribute findByKeyword(String keyword) {
        return imageAttributeRepository.findByFitsKeyword(keyword);
    }

}
