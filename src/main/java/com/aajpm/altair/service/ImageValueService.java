package com.aajpm.altair.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.aajpm.altair.entity.AstroImage;
import com.aajpm.altair.entity.ImageAttribute;
import com.aajpm.altair.entity.ImageValue;
import com.aajpm.altair.repository.ImageValueRepository;

@Service
@Transactional
public class ImageValueService extends BasicEntityCRUDService<ImageValue> {

    /////////////////////////// MANAGED REPOSITORY ////////////////////////////

    @Autowired
    private ImageValueRepository imageValueRepository;

    @Override
    protected ImageValueRepository getManagedRepository() {
        return imageValueRepository;
    }

    /////////////////////////// SUPPORTING SERVICES ///////////////////////////

    // None

    /////////////////////////////// CONSTRUCTORS //////////////////////////////

    public ImageValueService() {
        super();
    }

    @Override
    public ImageValue create() {
        return new ImageValue();
    }

    /////////////////////////////// SAVE METHODS //////////////////////////////

    @Override
    public ImageValue save(ImageValue value) {
        Assert.notNull(value, "The value cannot be null.");
        Assert.hasText(value.getValue(), "The value cannot be empty.");
        Assert.notNull(value.getAttribute(), "The attribute cannot be null.");
        Assert.notNull(value.getImage(), "The AstroImage cannot be null.");

        return super.save(value);
    }

    @Override
    public ImageValue update(ImageValue value) {
        Assert.notNull(value, "The value cannot be null.");
        Assert.hasText(value.getValue(), "The value cannot be empty.");
        Assert.notNull(value.getAttribute(), "The attribute cannot be null.");
        Assert.notNull(value.getImage(), "The AstroImage cannot be null.");

        return super.update(value);
    }

    ///////////////////////////////// METHODS /////////////////////////////////

    /**
     * Finds all {@link ImageValue} associated with the given {@link AstroImage}.
     *
     * @param image the {@link AstroImage} to find {@link ImageValue} for
     * @return a {@link Collection} of {@link ImageValue}s associated with the given {@link AstroImage}
     * @throws IllegalArgumentException if the given {@link AstroImage} is {@code null}
     */
    public Collection<ImageValue> findByImage(AstroImage image) {
        Assert.notNull(image, "The image cannot be null.");

        return imageValueRepository.findByImageId(image.getId());
    }

    /**
     * Finds all {@link ImageValue} associated with the given {@link ImageAttribute}.
     *
     * @param attr the {@link ImageAttribute} to find {@link ImageValue} for
     * @return a {@link Collection} of {@link ImageValue} associated with the given {@link ImageAttribute}
     * @throws IllegalArgumentException if the given {@link ImageAttribute} is {@code null}
     */
    public Collection<ImageValue> findByAttribute(ImageAttribute attr) {
        Assert.notNull(attr, "The attribute cannot be null.");

        return imageValueRepository.findByAttributeId(attr.getId());
    }

    /**
     * Finds the {@link ImageValue} associated with the given {@link AstroImage} and {@link ImageAttribute}.
     *
     * @param image the {@link AstroImage} to find the {@link ImageValue} for
     * @param attr the {@link ImageAttribute} to find the {@link ImageValue} for
     * @return the {@link ImageValue} associated with the given {@link AstroImage} and {@link ImageAttribute}
     * @throws IllegalArgumentException if either the given {@link AstroImage} or {@link ImageAttribute} is {@code null}
     */
    public ImageValue findByImageAndAttribute(AstroImage image, ImageAttribute attr) {
        Assert.notNull(image, "The image cannot be null.");
        Assert.notNull(attr, "The attribute cannot be null.");

        return imageValueRepository.findByImageIdAndAttributeId(image.getId(), attr.getId());
    }
    
}
